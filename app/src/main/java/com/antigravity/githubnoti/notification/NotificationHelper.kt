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
import com.antigravity.githubnoti.data.model.DownloadIncreaseEvent

/**
 * 릴리즈 다운로드 증가 시 안드로이드 시스템 푸시 알림을 생성하고 표시하는 헬퍼 클래스입니다.
 *
 * Android 8.0(Oreo, API 26) 이상의 알림 채널 요구사항과
 * Android 13(Tiramisu, API 33) 이상의 알림 권한 체크를 안전하게 지원합니다.
 */
class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "github_release_download_channel"
        private const val CHANNEL_NAME = "GitHub 릴리즈 다운로드 알림"
        private const val CHANNEL_DESCRIPTION = "추적 중인 리포지토리의 릴리즈 다운로드 수가 증가했을 때 알림을 보냅니다."
    }

    init {
        createNotificationChannel()
    }

    /**
     * 알림 채널을 시스템에 등록합니다.
     * 중요도를 HIGH로 설정하여 화면 상단 헤드업 팝업과 알림음이 발생하도록 합니다.
     */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESCRIPTION
                enableVibration(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
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
}
