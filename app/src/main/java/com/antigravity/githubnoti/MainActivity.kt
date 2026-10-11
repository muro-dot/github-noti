package com.antigravity.githubnoti

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.antigravity.githubnoti.data.local.PreferenceManager
import com.antigravity.githubnoti.theme.GitHubNotiTheme
import com.antigravity.githubnoti.ui.main.MainScreenViewModel
import com.antigravity.githubnoti.util.LocaleHelper

class MainActivity : ComponentActivity() {

    companion object {
        const val ACTION_NOTIFICATION_CLICK = "com.antigravity.githubnoti.ACTION_NOTIFICATION_CLICK"
        const val EXTRA_REFRESH = "extra_refresh"
        const val EXTRA_REPO_NAME = "extra_repo_name"
    }

    private val mainViewModel: MainScreenViewModel by viewModels()
    private var lastRefreshTimestamp: Long = 0L

    override fun attachBaseContext(newBase: Context) {
        val prefs = PreferenceManager(newBase)
        super.attachBaseContext(LocaleHelper.wrapContext(newBase, prefs.appLanguage))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 앱 실행 시 최신 데이터 새로고침
        mainViewModel.fetchRepositories()
        lastRefreshTimestamp = System.currentTimeMillis()

        handleIntent(intent)

        enableEdgeToEdge()
        setContent {
            GitHubNotiTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainNavigation(viewModel = mainViewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    override fun onResume() {
        super.onResume()
        // 포그라운드로 복귀했을 때 마지막 조회 후 10초 이상 지났다면 자동으로 최신 정보 새로고침
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastRefreshTimestamp > 10_000L) {
            lastRefreshTimestamp = currentTime
            mainViewModel.fetchRepositories()
        }
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val shouldRefresh = intent.getBooleanExtra(EXTRA_REFRESH, false) ||
                intent.action == ACTION_NOTIFICATION_CLICK

        if (shouldRefresh) {
            lastRefreshTimestamp = System.currentTimeMillis()
            mainViewModel.fetchRepositories()
        }
    }
}

