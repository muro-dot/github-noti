package com.antigravity.githubnoti

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.antigravity.githubnoti.data.local.PreferenceManager
import com.antigravity.githubnoti.theme.GitHubNotiTheme
import com.antigravity.githubnoti.util.LocaleHelper

class MainActivity : ComponentActivity() {

  override fun attachBaseContext(newBase: Context) {
    val prefs = PreferenceManager(newBase)
    super.attachBaseContext(LocaleHelper.wrapContext(newBase, prefs.appLanguage))
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()
    setContent {
      GitHubNotiTheme { Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { MainNavigation() } }
    }
  }
}
