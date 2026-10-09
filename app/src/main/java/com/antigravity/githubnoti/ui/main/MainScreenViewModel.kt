package com.antigravity.githubnoti.ui.main

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.antigravity.githubnoti.BuildConfig
import com.antigravity.githubnoti.data.api.GithubApiClient
import com.antigravity.githubnoti.data.local.PreferenceManager
import com.antigravity.githubnoti.data.model.AppUpdateInfo
import com.antigravity.githubnoti.data.model.DownloadIncreaseEvent
import com.antigravity.githubnoti.data.model.NewReleaseEvent
import com.antigravity.githubnoti.data.model.RepoItemUiState
import com.antigravity.githubnoti.notification.NotificationHelper
import com.antigravity.githubnoti.util.VersionComparator
import com.antigravity.githubnoti.worker.WorkScheduler
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * 리포지토리 목록 정렬 방식 열거형
 */
enum class SortType(val displayName: String) {
    DOWNLOAD_DESC("다운로드 많은 순"),
    STARS_DESC("스타 많은 순"),
    UPDATED_DESC("최근 수정 순"),
    NAME_ASC("이름 가나다순")
}

/**
 * 메인 화면 전체 UI 상태를 담는 데이터 클래스
 */
data class MainUiState(
    val username: String = "",
    val token: String = "",
    val monitorInterval: Long = 15L,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val searchQuery: String = "",
    val sortType: SortType = SortType.DOWNLOAD_DESC,
    val repos: List<RepoItemUiState> = emptyList(),
    val isSettingsOpen: Boolean = false,
    val recentIncreaseEvent: DownloadIncreaseEvent? = null,
    val recentNewReleaseEvent: NewReleaseEvent? = null,
    val appUpdateInfo: AppUpdateInfo? = null,
    val isCheckingAppUpdate: Boolean = false,
    val updateCheckMessage: String? = null,
    val isAutoUpdateCheckEnabled: Boolean = true
) {
    /**
     * 검색어 및 선택된 정렬 기준에 따라 필터링/정렬된 리포지토리 리스트
     */
    val filteredRepos: List<RepoItemUiState>
        get() {
            val filtered = if (searchQuery.isBlank()) {
                repos
            } else {
                repos.filter {
                    it.repo.name.contains(searchQuery, ignoreCase = true) ||
                    (it.repo.description?.contains(searchQuery, ignoreCase = true) == true)
                }
            }

            return when (sortType) {
                SortType.DOWNLOAD_DESC -> filtered.sortedByDescending { it.totalDownloadCount }
                SortType.STARS_DESC -> filtered.sortedByDescending { it.repo.stargazersCount }
                SortType.UPDATED_DESC -> filtered.sortedByDescending { it.repo.updatedAt ?: "" }
                SortType.NAME_ASC -> filtered.sortedBy { it.repo.name.lowercase() }
            }
        }

    val totalReposCount: Int get() = repos.size
    val trackedReposCount: Int get() = repos.count { it.isTracked }
    val totalDownloadCountSum: Long get() = repos.sumOf { it.totalDownloadCount }
}

/**
 * 깃허브 리포지토리 및 릴리즈 다운로드 모니터링의 비즈니스 로직을 총괄하는 ViewModel입니다.
 */
class MainScreenViewModel(application: Application) : AndroidViewModel(application) {

    private val preferenceManager = PreferenceManager(application)
    private val apiClient = GithubApiClient()
    private val notificationHelper = NotificationHelper(application)

    private val _uiState = MutableStateFlow(
        MainUiState(
            username = preferenceManager.username,
            token = preferenceManager.githubToken,
            monitorInterval = preferenceManager.monitorIntervalMinutes,
            isAutoUpdateCheckEnabled = preferenceManager.isAutoUpdateCheckEnabled
        )
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        // 백그라운드 모니터링 스케줄 등록
        WorkScheduler.schedulePeriodicMonitoring(
            getApplication(),
            preferenceManager.monitorIntervalMinutes
        )

        // 저장된 사용자명이 있다면 자동으로 리포지토리 목록 조회 시작
        if (preferenceManager.username.isNotBlank()) {
            fetchRepositories(preferenceManager.username)
        }

        // 앱 자체 신규 릴리즈 업데이트 자동 확인 (설정 활성화 시)
        if (preferenceManager.isAutoUpdateCheckEnabled) {
            checkAppUpdate(isManual = false)
        }
    }

    /**
     * 입력된 사용자명의 공개 리포지토리 및 각 리포지토리의 릴리즈 다운로드 수를 비동기 조회합니다.
     */
    fun fetchRepositories(username: String? = null) {
        val targetUser = (username ?: _uiState.value.username).trim()
        if (targetUser.isBlank()) {
            _uiState.update { it.copy(isSettingsOpen = true) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            val reposResult = apiClient.fetchUserRepositories(targetUser, _uiState.value.token.ifBlank { null })

            reposResult.fold(
                onSuccess = { fetchedRepos ->
                    val trackedSet = preferenceManager.trackedRepos
                    val isFirstRunForUser = preferenceManager.username != targetUser

                    // 1단계: 기본 리포지토리 리스트 생성
                    val initialItems = fetchedRepos.map { repo ->
                        RepoItemUiState(
                            repo = repo,
                            isTracked = if (isFirstRunForUser) true else (repo.fullName in trackedSet),
                            isLoadingReleases = true
                        )
                    }

                    // 모든 리포지토리를 기본 추적 대상으로 저장 (첫 설정 시 편의 제공)
                    if (isFirstRunForUser) {
                        preferenceManager.trackedRepos = fetchedRepos.map { it.fullName }.toSet()
                    }

                    _uiState.update { it.copy(repos = initialItems, username = targetUser) }
                    preferenceManager.username = targetUser

                    // 2단계: 각 리포지토리의 릴리즈를 병렬로 가져와 총 다운로드 수 및 증가 감지
                    val updatedItems = fetchedRepos.map { repo ->
                        async {
                            val releasesResult = apiClient.fetchRepositoryReleases(
                                repo.fullName,
                                _uiState.value.token.ifBlank { null }
                            )

                            val releases = releasesResult.getOrDefault(emptyList())
                            val totalDownloads = releases.sumOf { release ->
                                release.assets.sumOf { it.downloadCount }
                            }

                            // 1. 신규 릴리즈 출시 감지
                            val newReleaseEvent = preferenceManager.checkAndRecordNewReleases(
                                repoFullName = repo.fullName,
                                releases = releases,
                                isInitialLoad = false
                            )

                            val isTracked = repo.fullName in preferenceManager.trackedRepos
                            if (isTracked && newReleaseEvent != null) {
                                notificationHelper.showNewReleaseNotification(newReleaseEvent)
                                _uiState.update { state -> state.copy(recentNewReleaseEvent = newReleaseEvent) }
                            }

                            // 2. 다운로드 수 증가 비교 및 기록
                            val increaseEvents = preferenceManager.checkAndRecordDownloadIncreases(
                                repoFullName = repo.fullName,
                                releases = releases,
                                isInitialLoad = false
                            )

                            // 증가가 발생했고 추적 중인 리포지토리라면 푸시 알림 발송!
                            if (isTracked && increaseEvents.isNotEmpty()) {
                                for (event in increaseEvents) {
                                    notificationHelper.showDownloadIncreaseNotification(event)
                                    _uiState.update { state -> state.copy(recentIncreaseEvent = event) }
                                }
                            }

                            RepoItemUiState(
                                repo = repo,
                                releases = releases,
                                totalDownloadCount = totalDownloads,
                                isTracked = repo.fullName in preferenceManager.trackedRepos,
                                isLoadingReleases = false
                            )
                        }
                    }.awaitAll()

                    _uiState.update {
                        it.copy(
                            repos = updatedItems,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = error.localizedMessage ?: "리포지토리를 불러오지 못했습니다."
                        )
                    }
                }
            )
        }
    }

    /**
     * 특정 리포지토리의 백그라운드 추적 여부를 토글합니다.
     */
    fun toggleRepoTracking(repoFullName: String) {
        val currentItems = _uiState.value.repos
        val target = currentItems.find { it.repo.fullName == repoFullName } ?: return
        val newTrackedState = !target.isTracked

        preferenceManager.setRepoTracked(repoFullName, newTrackedState)

        _uiState.update { state ->
            state.copy(
                repos = state.repos.map { item ->
                    if (item.repo.fullName == repoFullName) {
                        item.copy(isTracked = newTrackedState)
                    } else {
                        item
                    }
                }
            )
        }
    }

    /**
     * 릴리즈 상세 정보 아코디언을 열거나 닫습니다.
     */
    fun toggleRepoExpand(repoFullName: String) {
        _uiState.update { state ->
            state.copy(
                repos = state.repos.map { item ->
                    if (item.repo.fullName == repoFullName) {
                        item.copy(isExpanded = !item.isExpanded)
                    } else {
                        item
                    }
                }
            )
        }
    }

    /**
     * 사용자가 UI에서 즉시 알림 작동 여부를 검증해볼 수 있는 '다운로드 수 +1 시뮬레이션' 기능입니다.
     */
    fun simulateDownloadIncrease(repoFullName: String) {
        val item = _uiState.value.repos.find { it.repo.fullName == repoFullName } ?: return
        val firstRelease = item.releases.firstOrNull { it.assets.isNotEmpty() } ?: return

        val event = preferenceManager.simulateDownloadIncrease(repoFullName, firstRelease)
        if (event != null) {
            // 시스템 알림 팝업 트리거
            notificationHelper.showDownloadIncreaseNotification(event)
            _uiState.update { it.copy(recentIncreaseEvent = event) }

            // UI 상의 다운로드 수도 즉시 반영
            _uiState.update { state ->
                state.copy(
                    repos = state.repos.map { repoItem ->
                        if (repoItem.repo.fullName == repoFullName) {
                            repoItem.copy(totalDownloadCount = repoItem.totalDownloadCount + 1)
                        } else {
                            repoItem
                        }
                    }
                )
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onSortTypeChange(sortType: SortType) {
        _uiState.update { it.copy(sortType = sortType) }
    }

    fun openSettings() {
        _uiState.update { it.copy(isSettingsOpen = true) }
    }

    fun closeSettings() {
        _uiState.update { it.copy(isSettingsOpen = false) }
    }

    fun dismissRecentEvent() {
        _uiState.update { it.copy(recentIncreaseEvent = null) }
    }

    fun dismissRecentNewReleaseEvent() {
        _uiState.update { it.copy(recentNewReleaseEvent = null) }
    }

    fun dismissAppUpdateInfo() {
        _uiState.update { it.copy(appUpdateInfo = null) }
    }

    fun dismissUpdateCheckMessage() {
        _uiState.update { it.copy(updateCheckMessage = null) }
    }

    /**
     * GitHub Noti 앱 자체의 최신 릴리즈를 확인하여 업데이트가 있는지 점검합니다.
     *
     * @param isManual 사용자가 설정 화면 등에서 수동으로 '업데이트 확인' 버튼을 눌렀는지 여부
     */
    fun checkAppUpdate(isManual: Boolean = false) {
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingAppUpdate = true, updateCheckMessage = null) }

            val result = apiClient.fetchLatestRelease(
                repoFullName = "muro-dot/github-noti",
                token = _uiState.value.token.ifBlank { null }
            )

            result.fold(
                onSuccess = { release ->
                    val currentVersion = BuildConfig.VERSION_NAME
                    val hasNewer = VersionComparator.isNewer(currentVersion, release.tagName)

                    if (hasNewer) {
                        val apkAsset = release.assets.firstOrNull { it.name.endsWith(".apk") }
                        val updateInfo = AppUpdateInfo(
                            hasUpdate = true,
                            latestVersion = release.tagName,
                            currentVersion = currentVersion,
                            releaseNotes = release.body,
                            downloadUrl = apkAsset?.browserDownloadUrl,
                            releasePageUrl = release.htmlUrl
                        )
                        _uiState.update {
                            it.copy(
                                appUpdateInfo = updateInfo,
                                isCheckingAppUpdate = false,
                                updateCheckMessage = if (isManual) "새 버전(${release.tagName})이 출시되었습니다!" else null
                            )
                        }
                    } else {
                        _uiState.update {
                            it.copy(
                                isCheckingAppUpdate = false,
                                updateCheckMessage = if (isManual) "현재 최신 버전(v$currentVersion)을 사용 중입니다." else null
                            )
                        }
                    }
                },
                onFailure = { error ->
                    _uiState.update {
                        it.copy(
                            isCheckingAppUpdate = false,
                            updateCheckMessage = if (isManual) "업데이트 확인 실패: ${error.localizedMessage ?: "네트워크 오류"}" else null
                        )
                    }
                }
            )
        }
    }

    /**
     * 사용자 설정 저장 및 스케줄러 갱신
     */
    fun saveSettings(
        username: String,
        token: String,
        intervalMinutes: Long,
        autoUpdateCheck: Boolean = true
    ) {
        val userChanged = preferenceManager.username != username
        preferenceManager.username = username
        preferenceManager.githubToken = token
        preferenceManager.monitorIntervalMinutes = intervalMinutes
        preferenceManager.isAutoUpdateCheckEnabled = autoUpdateCheck

        _uiState.update {
            it.copy(
                username = username,
                token = token,
                monitorInterval = intervalMinutes,
                isAutoUpdateCheckEnabled = autoUpdateCheck,
                isSettingsOpen = false
            )
        }

        // 새 주기로 백그라운드 워커 재스케줄링
        WorkScheduler.schedulePeriodicMonitoring(getApplication(), intervalMinutes)

        if (userChanged || _uiState.value.repos.isEmpty()) {
            fetchRepositories(username)
        }
    }
}
