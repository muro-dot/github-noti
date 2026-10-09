package com.antigravity.githubnoti.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.antigravity.githubnoti.R
import com.antigravity.githubnoti.data.model.AppUpdateInfo
import com.antigravity.githubnoti.data.model.DownloadIncreaseEvent
import com.antigravity.githubnoti.data.model.NewReleaseEvent

/**
 * 릴리즈 다운로드 증가, 신규 릴리즈 출시, 앱 자체 업데이트 시 안드로이드 시스템 푸시 알림을 생성하고 표시하는 헬퍼 클래스입니다.
 *
 * Android 8.0(Oreo, API 26) 이상의 알림 채널 요구사항과
 * Android 13(Tiramisu, API 33) 이상의 알림 권한 체크를 안전하게 지원합니다.
 */
class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "github_release_download_channel"
        private const val CHANNEL_NAME = "GitHub 릴리즈 다운로드 알림"
        private const val CHANNEL_DESCRIPTION = "추적 중인 리포지토리의 릴리즈 다운로드 수가 증가했을 때 알림을 보냅니다."

        const val CHANNEL_ID_NEW_RELEASE = "github_new_release_channel"
        private const val CHANNEL_NAME_NEW_RELEASE = "GitHub 신규 릴리즈 알림"
        private const val CHANNEL_DESCRIPTION_NEW_RELEASE = "추적 중인 리포지토리에 새로운 릴리즈가 출시되었을 때 알림을 보냅니다."

        const val CHANNEL_ID_APP_UPDATE = "github_noti_app_update_channel"
        private const val CHANNEL_NAME_APP_UPDATE = "GitHub Noti 앱 업데이트 알림"
        private const val CHANNEL_DESCRIPTION_APP_UPDATE = "GitHub Noti 앱의 최신 버전 업데이트를 안내합니다."
    }

    init {
        createNotificationChannels()
    }

    /**
     * 알림 채널들을 시스템에 등록합니다.
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            val downloadChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
            }

            val releaseChannel = NotificationChannel(
                CHANNEL_ID_NEW_RELEASE,
                CHANNEL_NAME_NEW_RELEASE,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION_NEW_RELEASE
                enableVibration(true)
            }

            val updateChannel = NotificationChannel(
                CHANNEL_ID_APP_UPDATE,
                CHANNEL_NAME_APP_UPDATE,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = CHANNEL_DESCRIPTION_APP_UPDATE
            }

            manager.createNotificationChannel(downloadChannel)
            manager.createNotificationChannel(releaseChannel)
            manager.createNotificationChannel(updateChannel)
        }
    }

    /**
     * 다운로드 증가 이벤트를 수신하여 사용자에게 시스템 알림을 표시합니다.
     * 알림을 터치하면 기본 웹 브라우저를 통해 해당 릴리즈 웹페이지로 바로 이동합니다.
     *
     * @param event 다운로드가 증가한 에셋 및 저장소 정보
     */
    fun showDownloadIncreaseNotification(event: DownloadIncreaseEvent) {
        // 알림 클릭 시 기본 브라우저로 이동하는 인텐트 설정
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(event.releaseUrl)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            event.hashCode(),
            browserIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "🎉 [${event.repoFullName}] 신규 다운로드 감지!"
        val message = "'${event.assetName}'\n" +
                "다운로드 수: ${event.previousCount}회 ➔ ${event.newCount}회 (+${event.increaseAmount})"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(title)
            .setContentText("'${event.assetName}' 다운로드 +${event.increaseAmount}회 (총 ${event.newCount}회)")
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            if (notificationManager.areNotificationsEnabled()) {
                val notificationId = (event.repoFullName + event.assetName).hashCode()
                notificationManager.notify(notificationId, notification)
            }
        } catch (e: SecurityException) {
            // Android 13 이상에서 알림 권한이 거부된 경우의 안전한 예외 방어
            e.printStackTrace()
        }
    }

    /**
     * 추적 중인 리포지토리에 새 릴리즈가 등록되었을 때 알림을 표시합니다.
     */
    fun showNewReleaseNotification(event: NewReleaseEvent) {
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(event.releaseUrl)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            event.hashCode(),
            browserIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "🚀 [${event.repoFullName}] 신규 릴리즈 출시!"
        val message = "버전: ${event.releaseTagName}\n제목: ${event.releaseName}"

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_NEW_RELEASE)
            .setSmallIcon(android.R.drawable.stat_notify_more)
            .setContentTitle(title)
            .setContentText("${event.releaseTagName} - ${event.releaseName}")
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            if (notificationManager.areNotificationsEnabled()) {
                val notificationId = (event.repoFullName + event.releaseTagName).hashCode()
                notificationManager.notify(notificationId, notification)
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    /**
     * GitHub Noti 앱 자체의 새 버전이 배포되었을 때 업데이트 알림을 표시합니다.
     */
    fun showAppUpdateNotification(info: AppUpdateInfo) {
        val targetUrl = info.downloadUrl ?: info.releasePageUrl ?: return
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            info.hashCode(),
            browserIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "📢 GitHub Noti 앱 업데이트 가능 (v${info.latestVersion})"
        val message = "새 버전(v${info.latestVersion})이 배포되었습니다.\n현재 버전: v${info.currentVersion}\n터치하여 새 버전을 다운로드하세요."

        val notification = NotificationCompat.Builder(context, CHANNEL_ID_APP_UPDATE)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(title)
            .setContentText("새 버전 v${info.latestVersion}으로 업데이트할 수 있습니다.")
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            if (notificationManager.areNotificationsEnabled()) {
                notificationManager.notify(9999, notification)
            }
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }
}
