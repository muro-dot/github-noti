package com.antigravity.githubnoti.util

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.antigravity.githubnoti.R
import java.io.File

/**
 * 인앱 최신 APK 다운로드 및 다운로드 완료 시 자동 패키지 설치를 총괄하는 유틸리티입니다.
 */
object AppUpdateInstaller {

    /**
     * 최신 버전 APK를 백그라운드에서 다운로드하고,
     * 다운로드가 완료되면 자동으로 안드로이드 시스템 패키지 설치 화면을 호출합니다.
     *
     * @param context 안드로이드 컨텍스트
     * @param downloadUrl 다운로드할 APK 파일 URL (null이거나 유효하지 않으면 릴리즈 웹페이지로 브라우저 연결)
     * @param releasePageUrl 웹 브라우저 대체 이동 URL
     * @param versionTag 다운로드할 버전 태그 (파일명 구분용, 예: "v1.1.2")
     */
    fun startDownloadAndInstall(
        context: Context,
        downloadUrl: String?,
        releasePageUrl: String?,
        versionTag: String
    ) {
        if (downloadUrl.isNullOrBlank() || !downloadUrl.endsWith(".apk", ignoreCase = true)) {
            // 직접 다운로드할 APK 에셋이 없는 경우 웹 브라우저로 안전하게 폴백
            val targetUrl = releasePageUrl ?: downloadUrl
            if (!targetUrl.isNullOrBlank()) {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(targetUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            }
            return
        }

        val appContext = context.applicationContext
        val fileName = "github-noti-${versionTag.removePrefix("v")}.apk"
        val downloadDir = appContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
        val destinationFile = File(downloadDir, fileName)

        // 이전에 다운로드된 동일 이름의 파일이 있다면 삭제
        if (destinationFile.exists()) {
            destinationFile.delete()
        }

        val downloadManager = appContext.getSystemService(Context.DOWNLOAD_SERVICE) as? DownloadManager
        if (downloadManager == null) {
            Toast.makeText(appContext, appContext.getString(R.string.installer_download_manager_error), Toast.LENGTH_SHORT).show()
            return
        }

        val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
            setTitle(appContext.getString(R.string.installer_download_title, versionTag))
            setDescription(appContext.getString(R.string.installer_download_desc))
            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            setDestinationUri(Uri.fromFile(destinationFile))
            setMimeType("application/vnd.android.package-archive")
        }

        val downloadId = downloadManager.enqueue(request)

        Toast.makeText(
            appContext,
            appContext.getString(R.string.installer_download_started),
            Toast.LENGTH_LONG
        ).show()

        // 다운로드 완료 이벤트를 수신할 BroadcastReceiver 등록
        val onCompleteReceiver = object : BroadcastReceiver() {
            override fun onReceive(recvContext: Context, intent: Intent) {
                val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (id == downloadId) {
                    try {
                        appContext.unregisterReceiver(this)
                    } catch (e: Exception) {
                        // 이미 해제된 경우 안전하게 무시
                    }
                    installApk(appContext, destinationFile)
                }
            }
        }

        val intentFilter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        ContextCompat.registerReceiver(
            appContext,
            onCompleteReceiver,
            intentFilter,
            ContextCompat.RECEIVER_EXPORTED
        )
    }

    /**
     * 다운로드된 APK 파일을 FileProvider를 통해 안전하게 시스템 패키지 설치 관리자에 전달합니다.
     */
    fun installApk(context: Context, apkFile: File) {
        if (!apkFile.exists()) {
            Toast.makeText(context, context.getString(R.string.installer_apk_not_found), Toast.LENGTH_SHORT).show()
            return
        }

        // Android 8.0(API 26) 이상에서 알 수 없는 출처 앱 설치 권한 체크
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            if (!context.packageManager.canRequestPackageInstalls()) {
                Toast.makeText(
                    context,
                    context.getString(R.string.installer_unknown_source_permission),
                    Toast.LENGTH_LONG
                ).show()

                val permissionIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                    data = Uri.parse("package:${context.packageName}")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(permissionIntent)
                // 권한 설정 후 바로 설치도 시도할 수 있도록 인텐트 전달 유지
            }
        }

        try {
            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                apkFile
            )

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(contentUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(installIntent)
        } catch (e: Exception) {
            e.printStackTrace()
            Toast.makeText(
                context,
                context.getString(R.string.installer_launch_error, e.localizedMessage ?: ""),
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
