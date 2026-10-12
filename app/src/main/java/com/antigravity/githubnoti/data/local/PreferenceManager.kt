package com.antigravity.githubnoti.data.local

import android.content.Context
import android.content.SharedPreferences
import com.antigravity.githubnoti.data.model.DownloadIncreaseEvent
import com.antigravity.githubnoti.data.model.GithubRelease
import com.antigravity.githubnoti.data.model.NewReleaseEvent

/**
 * 사용자 설정 및 리포지토리 릴리즈 다운로드 수를 로컬에 안전하게 영속 저장하는 매니저 클래스입니다.
 *
 * 앱이 종료되거나 재부팅되어도 이전 다운로드 카운트와 사용자 설정을 기억하여,
 * 다운로드 수가 증가했을 때 정확하게 감지할 수 있도록 SharedPreferences를 활용합니다.
 */
class PreferenceManager(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    companion object {
        private const val PREF_NAME = "github_noti_prefs"
        private const val KEY_USERNAME = "github_username"
        private const val KEY_TOKEN = "github_token"
        private const val KEY_MAX_RELEASE_COUNT = "max_release_count"
        private const val KEY_TRACKED_REPOS = "tracked_repos"
        private const val KEY_MONITOR_INTERVAL_MINUTES = "monitor_interval_minutes"
        private const val KEY_AUTO_UPDATE_CHECK = "auto_update_check"
        private const val KEY_APP_LANGUAGE = "app_language"
        private const val PREFIX_DOWNLOAD_COUNT = "asset_dl_"
        private const val PREFIX_LATEST_RELEASE_ID = "latest_rel_id_"
    }

    /**
     * 앱 언어 수동 선택 설정 ("system", "ko", "en", 기본값: "system")
     */
    var appLanguage: String
        get() = prefs.getString(KEY_APP_LANGUAGE, "system") ?: "system"
        set(value) = prefs.edit().putString(KEY_APP_LANGUAGE, value).apply()

    /**
     * 조회할 깃허브 계정 아이디 (기본값: "octocat" 또는 사용자 지정)
     */
    var username: String
        get() = prefs.getString(KEY_USERNAME, "") ?: ""
        set(value) = prefs.edit().putString(KEY_USERNAME, value.trim()).apply()

    /**
     * 깃허브 개인 액세스 토큰 (선택 사항).
     * GitHub API는 미인증 시 시간당 60회로 제한되므로 토큰 입력 시 시간당 5,000회까지 안전하게 이용 가능합니다.
     */
    var githubToken: String
        get() = prefs.getString(KEY_TOKEN, "") ?: ""
        set(value) = prefs.edit().putString(KEY_TOKEN, value.trim()).apply()

    /**
     * 리포지토리 카드 펼침 시 표시할 최근 릴리즈 최대 개수 (기본값: 5).
     */
    var maxReleaseCount: Int
        get() = prefs.getInt(KEY_MAX_RELEASE_COUNT, 5)
        set(value) = prefs.edit().putInt(KEY_MAX_RELEASE_COUNT, value.coerceAtLeast(1)).apply()

    /**
     * 백그라운드 모니터링 주기 (분 단위, WorkManager 최소값은 15분).
     */
    var monitorIntervalMinutes: Long
        get() = prefs.getLong(KEY_MONITOR_INTERVAL_MINUTES, 15L)
        set(value) = prefs.edit().putLong(KEY_MONITOR_INTERVAL_MINUTES, value.coerceAtLeast(15L)).apply()

    /**
     * 알림 추적 대상 리포지토리 전체 이름(full_name, e.g. "user/repo") 집합.
     */
    /**
     * 알림 추적 대상 리포지토리 전체 이름(full_name, e.g. "user/repo") 집합.
     * 대소문자 중복(예: muro-dot/LYWSD02... 와 muro-dot/lywsd02...)을 방지하기 위해 정규화합니다.
     */
    var trackedRepos: Set<String>
        get() {
            val raw = prefs.getStringSet(KEY_TRACKED_REPOS, emptySet()) ?: emptySet()
            // 대소문자 무시하고 유일한 레포지토리만 추출 (최신 형태 유지)
            val distinctMap = mutableMapOf<String, String>()
            for (repo in raw) {
                val lower = repo.lowercase()
                if (!distinctMap.containsKey(lower)) {
                    distinctMap[lower] = repo
                }
            }
            return distinctMap.values.toSet()
        }
        set(value) {
            val distinctMap = mutableMapOf<String, String>()
            for (repo in value) {
                val lower = repo.lowercase()
                if (!distinctMap.containsKey(lower)) {
                    distinctMap[lower] = repo
                }
            }
            prefs.edit().putStringSet(KEY_TRACKED_REPOS, distinctMap.values.toSet()).apply()
        }

    /**
     * GitHub에서 받아온 최신 활성 리포지토리 목록을 기준으로 trackedRepos를 정규화합니다.
     * - 이름 대소문자가 변경된 경우 현재 실제 이름으로 교체
     * - GitHub에서 삭제되었거나 사용자의 리포지토리가 아닌 과거 잔여 데이터는 자동 정리
     */
    fun cleanUpTrackedRepos(activeRepos: List<com.antigravity.githubnoti.data.model.GithubRepo>) {
        if (activeRepos.isEmpty()) return
        val current = trackedRepos
        val activeMap = activeRepos.associateBy { it.fullName.lowercase() }

        val cleanedSet = mutableSetOf<String>()
        for (savedRepo in current) {
            val matched = activeMap[savedRepo.lowercase()]
            if (matched != null) {
                // 현재 GitHub 상의 정식 이름(대소문자 일치)으로 등록
                cleanedSet.add(matched.fullName)
            }
        }
        // 만약 기존 trackedRepos와 달라졌다면 업데이트
        if (cleanedSet != current) {
            trackedRepos = cleanedSet
        }
    }

    /**
     * 앱 시작 시 및 백그라운드에서 신규 릴리즈 업데이트 자동 확인 활성화 여부 (기본값: true)
     */
    var isAutoUpdateCheckEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_UPDATE_CHECK, true)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_UPDATE_CHECK, value).apply()

    /**
     * 특정 리포지토리의 추적 여부를 토글합니다. 대소문자 무시하여 기존 항목을 찾고 갱신합니다.
     */
    fun setRepoTracked(repoFullName: String, isTracked: Boolean) {
        val current = trackedRepos.filterNot { it.equals(repoFullName, ignoreCase = true) }.toMutableSet()
        if (isTracked) {
            current.add(repoFullName)
        }
        trackedRepos = current
    }

    /**
     * 특정 에셋의 이전 다운로드 수를 가져옵니다. 저장된 기록이 없으면 -1을 반환합니다.
     */
    fun getPreviousDownloadCount(repoFullName: String, assetId: Long): Long {
        val key = makeAssetKey(repoFullName, assetId)
        val value = prefs.getLong(key, -1L)
        if (value != -1L) return value

        // 하위 호환성: 대소문자 변경 전 예전 키 검색 및 마이그레이션
        val legacyKey = "$PREFIX_DOWNLOAD_COUNT${repoFullName}_$assetId"
        val legacyVal = prefs.getLong(legacyKey, -1L)
        if (legacyVal != -1L) {
            prefs.edit().putLong(key, legacyVal).apply()
            return legacyVal
        }
        return -1L
    }

    /**
     * 에셋의 다운로드 수를 저장합니다.
     */
    fun saveDownloadCount(repoFullName: String, assetId: Long, count: Long) {
        val key = makeAssetKey(repoFullName, assetId)
        prefs.edit().putLong(key, count).apply()
    }

    /**
     * 리포지토리의 릴리즈 목록을 받아서 다운로드 수 증가 여부를 비교하고 감지합니다.
     *
     * @param repoFullName 리포지토리 이름 (e.g. "octocat/Hello-World")
     * @param releases 해당 리포지토리의 최신 릴리즈 목록
     * @param isInitialLoad 최초 조회인지 여부 (최초 조회 시에는 폭탄 알림 방지를 위해 현재 값만 캐싱)
     * @return 다운로드가 증가한 에셋들의 이벤트 목록
     */
    fun checkAndRecordDownloadIncreases(
        repoFullName: String,
        releases: List<GithubRelease>,
        isInitialLoad: Boolean = false
    ): List<DownloadIncreaseEvent> {
        val events = mutableListOf<DownloadIncreaseEvent>()
        val editor = prefs.edit()

        for (release in releases) {
            for (asset in release.assets) {
                val key = makeAssetKey(repoFullName, asset.id)
                val prevCount = prefs.getLong(key, -1L)

                if (prevCount == -1L) {
                    // 최초 발견된 에셋: 현재 값을 기준값으로 저장
                    editor.putLong(key, asset.downloadCount)
                } else if (asset.downloadCount > prevCount) {
                    // 다운로드 수 증가 감지!
                    if (!isInitialLoad) {
                        events.add(
                            DownloadIncreaseEvent(
                                repoFullName = repoFullName,
                                releaseTagName = release.tagName,
                                assetName = asset.name,
                                previousCount = prevCount,
                                newCount = asset.downloadCount,
                                releaseUrl = release.htmlUrl
                            )
                        )
                    }
                    editor.putLong(key, asset.downloadCount)
                }
            }
        }

        editor.apply()
        return events
    }

    /**
     * 리포지토리의 최신 릴리즈가 이전 확인 시점보다 새로 발행되었는지 확인합니다.
     *
     * @param repoFullName 리포지토리 전체 명칭
     * @param releases 해당 리포지토리의 릴리즈 목록
     * @param isInitialLoad 최초 조회 여부
     * @return 새로 발행된 릴리즈 이벤트 (새 릴리즈가 없으면 null)
     */
    fun checkAndRecordNewReleases(
        repoFullName: String,
        releases: List<GithubRelease>,
        isInitialLoad: Boolean = false
    ): NewReleaseEvent? {
        val latestRelease = releases.firstOrNull() ?: return null
        val normalizedKey = "$PREFIX_LATEST_RELEASE_ID${repoFullName.lowercase()}"
        val legacyKey = "$PREFIX_LATEST_RELEASE_ID$repoFullName"
        var prevReleaseId = prefs.getLong(normalizedKey, -1L)
        if (prevReleaseId == -1L && legacyKey != normalizedKey) {
            prevReleaseId = prefs.getLong(legacyKey, -1L)
        }

        return if (prevReleaseId == -1L) {
            // 최초 기록: 현재 최신 릴리즈 ID만 저장하고 알림은 생략
            prefs.edit().putLong(normalizedKey, latestRelease.id).apply()
            null
        } else if (latestRelease.id != prevReleaseId) {
            // 새 릴리즈 ID가 감지됨!
            prefs.edit().putLong(normalizedKey, latestRelease.id).apply()
            if (!isInitialLoad) {
                NewReleaseEvent(
                    repoFullName = repoFullName,
                    releaseTagName = latestRelease.tagName,
                    releaseName = latestRelease.name ?: latestRelease.tagName,
                    releaseUrl = latestRelease.htmlUrl
                )
            } else {
                null
            }
        } else {
            null
        }
    }

    /**
     * 테스트용: 특정 에셋의 다운로드 수를 이전 값으로 낮추거나 임의로 조작하여
     * 사용자가 "알림 테스트" 버튼을 눌렀을 때 실제 알림이 동작하는지 즉시 검증할 수 있도록 지원합니다.
     */
    fun simulateDownloadIncrease(
        repoFullName: String,
        release: GithubRelease
    ): DownloadIncreaseEvent? {
        val asset = release.assets.firstOrNull() ?: return null
        val key = makeAssetKey(repoFullName, asset.id)
        val currentCount = prefs.getLong(key, asset.downloadCount)
        val newCount = currentCount + 1

        prefs.edit().putLong(key, newCount).apply()

        return DownloadIncreaseEvent(
            repoFullName = repoFullName,
            releaseTagName = release.tagName,
            assetName = asset.name,
            previousCount = currentCount,
            newCount = newCount,
            releaseUrl = release.htmlUrl
        )
    }

    private fun makeAssetKey(repoFullName: String, assetId: Long): String {
        return "$PREFIX_DOWNLOAD_COUNT${repoFullName.lowercase()}_$assetId"
    }
}
