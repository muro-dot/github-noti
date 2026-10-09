package com.antigravity.githubnoti.worker

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.antigravity.githubnoti.data.api.GithubApiClient
import com.antigravity.githubnoti.data.local.PreferenceManager
import com.antigravity.githubnoti.notification.NotificationHelper

/**
 * 백그라운드 환경에서 주기적으로 GitHub 릴리즈 다운로드 수를 조회하고,
 * 증가가 감지되면 시스템 푸시 알림을 띄우는 WorkManager CoroutineWorker입니다.
 *
 * 앱이 닫혀있거나 기기가 대기 모드에 있어도 정해진 주기(기본 15분)마다 안전하게 확인합니다.
 */
class ReleaseNotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    private val preferenceManager = PreferenceManager(appContext)
    private val apiClient = GithubApiClient()
    private val notificationHelper = NotificationHelper(appContext)

    override suspend fun doWork(): Result {
        val trackedRepos = preferenceManager.trackedRepos
        val token = preferenceManager.githubToken.ifBlank { null }

        // 추적할 리포지토리가 설정되어 있지 않으면 조용히 성공 처리
        if (trackedRepos.isEmpty()) {
            return Result.success()
        }

        var anyFailures = false

        for (repoFullName in trackedRepos) {
            val releasesResult = apiClient.fetchRepositoryReleases(repoFullName, token)
            releasesResult.fold(
                onSuccess = { releases ->
                    // 1. 신규 릴리즈 감지
                    val newReleaseEvent = preferenceManager.checkAndRecordNewReleases(
                        repoFullName = repoFullName,
                        releases = releases,
                        isInitialLoad = false
                    )
                    if (newReleaseEvent != null) {
                        notificationHelper.showNewReleaseNotification(newReleaseEvent)
                    }

                    // 2. 다운로드 증가 이벤트 감지
                    val increaseEvents = preferenceManager.checkAndRecordDownloadIncreases(
                        repoFullName = repoFullName,
                        releases = releases,
                        isInitialLoad = false
                    )

                    // 다운로드 증가 이벤트마다 시스템 알림 팝업 발송
                    for (event in increaseEvents) {
                        notificationHelper.showDownloadIncreaseNotification(event)
                    }
                },
                onFailure = {
                    anyFailures = true
                }
            )
        }

        // 3. 앱 자체의 신규 릴리즈 업데이트 확인 (설정 활성화 시)
        if (preferenceManager.isAutoUpdateCheckEnabled) {
            val appReleaseResult = apiClient.fetchLatestRelease("muro-dot/github-noti", token)
            appReleaseResult.getOrNull()?.let { appRelease ->
                val currentVersion = com.antigravity.githubnoti.BuildConfig.VERSION_NAME
                if (com.antigravity.githubnoti.util.VersionComparator.isNewer(currentVersion, appRelease.tagName)) {
                    val apkAsset = appRelease.assets.firstOrNull { it.name.endsWith(".apk") }
                    val updateInfo = com.antigravity.githubnoti.data.model.AppUpdateInfo(
                        hasUpdate = true,
                        latestVersion = appRelease.tagName,
                        currentVersion = currentVersion,
                        releaseNotes = appRelease.body,
                        downloadUrl = apkAsset?.browserDownloadUrl,
                        releasePageUrl = appRelease.htmlUrl
                    )
                    notificationHelper.showAppUpdateNotification(updateInfo)
                }
            }
        }

        return if (anyFailures) {
            // 네트워크 오류 등으로 일부 실패 시 다음 주기에 다시 시도
            Result.retry()
        } else {
            Result.success()
        }
    }
}
