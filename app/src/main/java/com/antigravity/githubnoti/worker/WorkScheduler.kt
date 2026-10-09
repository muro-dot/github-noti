package com.antigravity.githubnoti.worker

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

/**
 * 릴리즈 다운로드 감지 백그라운드 작업(WorkManager)을 스케줄링하고 제어하는 유틸리티 클래스입니다.
 */
object WorkScheduler {

    private const val WORK_NAME_PERIODIC = "github_release_periodic_worker"

    /**
     * 주기적 백그라운드 모니터링 작업을 등록하거나 업데이트합니다.
     * 배터리 소모를 방지하기 위해 네트워크 연결 시에만 실행되도록 제약조건을 설정합니다.
     *
     * @param context 안드로이드 컨텍스트
     * @param intervalMinutes 실행 주기(분 단위, 최소 15분)
     */
    fun schedulePeriodicMonitoring(context: Context, intervalMinutes: Long = 15L) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED) // 인터넷 연결 시에만 수행
            .build()

        val periodicWorkRequest = PeriodicWorkRequestBuilder<ReleaseNotificationWorker>(
            intervalMinutes.coerceAtLeast(15L),
            TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME_PERIODIC,
            ExistingPeriodicWorkPolicy.UPDATE, // 설정이 바뀔 때 새로운 주기로 교체
            periodicWorkRequest
        )
    }

    /**
     * 사용자가 백그라운드 체크를 수동으로 즉시 1회 테스트해보고 싶을 때 실행합니다.
     */
    fun triggerImmediateCheck(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val oneTimeWorkRequest = OneTimeWorkRequestBuilder<ReleaseNotificationWorker>()
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context).enqueue(oneTimeWorkRequest)
    }

    /**
     * 백그라운드 모니터링을 취소합니다.
     */
    fun cancelMonitoring(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME_PERIODIC)
    }
}
