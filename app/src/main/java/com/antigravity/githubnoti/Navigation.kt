package com.antigravity.githubnoti

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import androidx.lifecycle.viewmodel.compose.viewModel
import com.antigravity.githubnoti.ui.main.MainScreen
import com.antigravity.githubnoti.ui.main.MainScreenViewModel

@Composable
fun MainNavigation(viewModel: MainScreenViewModel = viewModel()) {
  val backStack = rememberNavBackStack(Main)

  NavDisplay(
    backStack = backStack,
    onBack = { backStack.removeLastOrNull() },
    entryProvider =
      entryProvider {
        entry<Main> {
          MainScreen(viewModel = viewModel)
        }
      },
  )
}
