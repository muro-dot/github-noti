package com.antigravity.githubnoti.ui.main

import android.app.Activity
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.antigravity.githubnoti.R
import com.antigravity.githubnoti.ui.components.NotificationPermissionBanner
import com.antigravity.githubnoti.ui.components.RepoCard
import com.antigravity.githubnoti.ui.components.SettingsDialog
import com.antigravity.githubnoti.util.LocaleHelper

/**
 * 깃허브 공개 리포지토리 목록과 릴리즈 다운로드 현황을 확인하고,
 * 다운로드 증가 알림을 관리하는 메인 화면 컴포저블입니다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    onItemClick: ((Any) -> Unit)? = null,
    modifier: Modifier = Modifier,
    viewModel: MainScreenViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current

    val okLabel = stringResource(R.string.action_ok)

    val increaseEvent = uiState.recentIncreaseEvent
    if (increaseEvent != null) {
        val message = stringResource(
            R.string.snackbar_download_increase,
            increaseEvent.repoFullName,
            increaseEvent.assetName,
            increaseEvent.increaseAmount,
            increaseEvent.previousCount,
            increaseEvent.newCount
        )
        LaunchedEffect(increaseEvent) {
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = okLabel,
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed || result == SnackbarResult.Dismissed) {
                viewModel.dismissRecentEvent()
            }
        }
    }

    val newReleaseEvent = uiState.recentNewReleaseEvent
    if (newReleaseEvent != null) {
        val message = stringResource(
            R.string.snackbar_new_release,
            newReleaseEvent.repoFullName,
            newReleaseEvent.releaseTagName,
            newReleaseEvent.releaseName
        )
        LaunchedEffect(newReleaseEvent) {
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = okLabel,
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed || result == SnackbarResult.Dismissed) {
                viewModel.dismissRecentNewReleaseEvent()
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = stringResource(R.string.app_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (uiState.username.isNotBlank()) {
                            Text(
                                text = stringResource(R.string.user_public_repos, uiState.username),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    // 수동 새로고침
                    IconButton(
                        onClick = { viewModel.fetchRepositories() },
                        enabled = !uiState.isLoading
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.action_refresh))
                        }
                    }

                    // 설정 (사용자명, 토큰, 모니터링 주기, 언어)
                    IconButton(onClick = { viewModel.openSettings() }) {
                        Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.action_settings))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
        ) {
            // Android 13+ 알림 권한 유도 배너
            NotificationPermissionBanner()

            Spacer(modifier = Modifier.height(6.dp))

            // 통계 대시보드 카드
            if (uiState.repos.isNotEmpty()) {
                SummaryDashboardCard(
                    totalRepos = uiState.totalReposCount,
                    trackedRepos = uiState.trackedReposCount,
                    totalDownloads = uiState.totalDownloadCountSum
                )
                Spacer(modifier = Modifier.height(10.dp))
            }

            // 정렬 필터 칩 가로 스크롤
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SortType.values().forEach { sort ->
                    FilterChip(
                        selected = uiState.sortType == sort,
                        onClick = { viewModel.onSortTypeChange(sort) },
                        label = { Text(stringResource(sort.titleRes)) },
                        leadingIcon = if (uiState.sortType == sort) {
                            { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                        } else null
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // 메인 콘텐츠 영역: 로딩, 에러, 빈 목록, 리스트
            when {
                uiState.isLoading && uiState.repos.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.loading_repos),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                uiState.errorMessage != null && uiState.repos.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = uiState.errorMessage ?: stringResource(R.string.error_occurred),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            Button(onClick = { viewModel.openSettings() }) {
                                Text(stringResource(R.string.btn_open_settings))
                            }
                        }
                    }
                }

                uiState.username.isBlank() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.PersonSearch,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(56.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.empty_user_title),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.empty_user_desc),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(onClick = { viewModel.openSettings() }) {
                                Icon(Icons.Default.Settings, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(stringResource(R.string.btn_setup_user))
                            }
                        }
                    }
                }

                uiState.filteredRepos.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.empty_repos),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp)
                    ) {
                        items(
                            items = uiState.filteredRepos,
                            key = { it.repo.id }
                        ) { item ->
                            RepoCard(
                                item = item,
                                onToggleTrack = { viewModel.toggleRepoTracking(item.repo.fullName) },
                                onToggleExpand = { viewModel.toggleRepoExpand(item.repo.fullName) },
                                onSimulateTestIncrease = { viewModel.simulateDownloadIncrease(item.repo.fullName) }
                            )
                        }
                    }
                }
            }
        }
    }

    // 설정 대화상자
    if (uiState.isSettingsOpen) {
        SettingsDialog(
            currentUsername = uiState.username,
            currentInterval = uiState.monitorInterval,
            currentLanguage = uiState.appLanguage,
            isAutoUpdateEnabled = uiState.isAutoUpdateCheckEnabled,
            isCheckingUpdate = uiState.isCheckingAppUpdate,
            updateStatusMessage = uiState.updateCheckMessage,
            onDismiss = { viewModel.closeSettings() },
            onCheckUpdateNow = { viewModel.checkAppUpdate(isManual = true) },
            onSave = { user, interval, autoUpdate, language ->
                val languageChanged = uiState.appLanguage != language
                viewModel.saveSettings(user, interval, autoUpdate, language)
                if (languageChanged && context is Activity) {
                    LocaleHelper.setAppLanguage(context, language)
                }
            }
        )
    }

    // 앱 자체 신규 릴리즈 업데이트 알림 대화상자
    uiState.appUpdateInfo?.let { updateInfo ->
        AlertDialog(
            onDismissRequest = { viewModel.dismissAppUpdateInfo() },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.app_update_dialog_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = stringResource(R.string.app_update_dialog_msg, updateInfo.latestVersion),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.app_update_version_diff, updateInfo.currentVersion, updateInfo.latestVersion),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (!updateInfo.releaseNotes.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = stringResource(R.string.app_update_release_notes),
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant,
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 160.dp)
                        ) {
                            Text(
                                text = updateInfo.releaseNotes,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        com.antigravity.githubnoti.util.AppUpdateInstaller.startDownloadAndInstall(
                            context = context,
                            downloadUrl = updateInfo.downloadUrl,
                            releasePageUrl = updateInfo.releasePageUrl,
                            versionTag = updateInfo.latestVersion
                        )
                        viewModel.dismissAppUpdateInfo()
                    }
                ) {
                    Text(stringResource(R.string.app_update_btn_install))
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissAppUpdateInfo() }) {
                    Text(stringResource(R.string.action_later))
                }
            }
        )
    }
}

/**
 * 상단 통계 요약 대시보드 카드
 */
@Composable
private fun SummaryDashboardCard(
    totalRepos: Int,
    trackedRepos: Int,
    totalDownloads: Long
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            SummaryItem(
                label = stringResource(R.string.summary_public_repos),
                value = stringResource(R.string.count_unit_repos, totalRepos),
                icon = Icons.Default.FolderOpen
            )
            VerticalDivider(modifier = Modifier.height(30.dp), color = MaterialTheme.colorScheme.outlineVariant)
            SummaryItem(
                label = stringResource(R.string.summary_tracked_repos),
                value = stringResource(R.string.count_unit_repos, trackedRepos),
                icon = Icons.Default.NotificationsActive
            )
            VerticalDivider(modifier = Modifier.height(30.dp), color = MaterialTheme.colorScheme.outlineVariant)
            SummaryItem(
                label = stringResource(R.string.summary_total_downloads),
                value = stringResource(R.string.count_unit_downloads, totalDownloads),
                icon = Icons.Default.Download
            )
        }
    }
}

@Composable
private fun SummaryItem(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
