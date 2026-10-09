package com.antigravity.githubnoti.ui.main

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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.antigravity.githubnoti.ui.components.NotificationPermissionBanner
import com.antigravity.githubnoti.ui.components.RepoCard
import com.antigravity.githubnoti.ui.components.SettingsDialog

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

    LaunchedEffect(uiState.recentIncreaseEvent) {
        uiState.recentIncreaseEvent?.let { event ->
            val message = "🎉 [${event.repoFullName}] '${event.assetName}' 다운로드 +${event.increaseAmount}회! (${event.previousCount}회 ➔ ${event.newCount}회)"
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = "확인",
                duration = SnackbarDuration.Long
            )
            if (result == SnackbarResult.ActionPerformed || result == SnackbarResult.Dismissed) {
                viewModel.dismissRecentEvent()
            }
        }
    }

    LaunchedEffect(uiState.recentNewReleaseEvent) {
        uiState.recentNewReleaseEvent?.let { event ->
            val message = "🚀 [${event.repoFullName}] 신규 릴리즈 '${event.releaseTagName}' (${event.releaseName}) 출시!"
            val result = snackbarHostState.showSnackbar(
                message = message,
                actionLabel = "확인",
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
                            text = "GitHub 알림 매니저",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (uiState.username.isNotBlank()) {
                            Text(
                                text = "@${uiState.username} 의 공개 리포지토리",
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
                            Icon(Icons.Default.Refresh, contentDescription = "새로고침")
                        }
                    }

                    // 설정 (사용자명, 토큰, 모니터링 주기)
                    IconButton(onClick = { viewModel.openSettings() }) {
                        Icon(Icons.Default.Settings, contentDescription = "설정")
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
                        label = { Text(sort.displayName) },
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
                                text = "GitHub 리포지토리 및 릴리즈 다운로드 정보를 불러오는 중...",
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
                                text = uiState.errorMessage ?: "오류가 발생했습니다.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            Button(onClick = { viewModel.openSettings() }) {
                                Text("계정 및 토큰 설정 열기")
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
                                text = "모니터링할 GitHub 계정을 설정해주세요",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "설정 버튼을 눌러 GitHub 아이디를 입력하시면\n공개 저장소와 릴리즈 다운로드 통계를 자동으로 추적합니다.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(20.dp))
                            Button(onClick = { viewModel.openSettings() }) {
                                Icon(Icons.Default.Settings, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("GitHub 계정 설정하기")
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
                            text = "공개 리포지토리가 없습니다.",
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

    val context = LocalContext.current

    // 설정 대화상자
    if (uiState.isSettingsOpen) {
        SettingsDialog(
            currentUsername = uiState.username,
            currentInterval = uiState.monitorInterval,
            isAutoUpdateEnabled = uiState.isAutoUpdateCheckEnabled,
            isCheckingUpdate = uiState.isCheckingAppUpdate,
            updateStatusMessage = uiState.updateCheckMessage,
            onDismiss = { viewModel.closeSettings() },
            onCheckUpdateNow = { viewModel.checkAppUpdate(isManual = true) },
            onSave = { user, interval, autoUpdate ->
                viewModel.saveSettings(user, interval, autoUpdate)
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
                        text = "새로운 업데이트 알림",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            text = {
                Column {
                    Text(
                        text = "GitHub Noti의 새로운 버전(${updateInfo.latestVersion})이 배포되었습니다!",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "현재 버전: v${updateInfo.currentVersion} ➔ 최신 버전: ${updateInfo.latestVersion}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.primary
                    )

                    if (!updateInfo.releaseNotes.isNullOrBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "릴리즈 변경 사항:",
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
                    Text("지금 업데이트 (다운로드 후 자동 설치)")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissAppUpdateInfo() }) {
                    Text("나중에 하기")
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
                label = "공개 저장소",
                value = "${totalRepos}개",
                icon = Icons.Default.FolderOpen
            )
            VerticalDivider(modifier = Modifier.height(30.dp), color = MaterialTheme.colorScheme.outlineVariant)
            SummaryItem(
                label = "알림 추적 중",
                value = "${trackedRepos}개",
                icon = Icons.Default.NotificationsActive
            )
            VerticalDivider(modifier = Modifier.height(30.dp), color = MaterialTheme.colorScheme.outlineVariant)
            SummaryItem(
                label = "총 다운로드",
                value = "${totalDownloads}회",
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
