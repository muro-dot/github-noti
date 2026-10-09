package com.antigravity.githubnoti.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

import androidx.compose.material.icons.filled.SystemUpdate
import com.antigravity.githubnoti.BuildConfig

/**
 * 깃허브 계정명, Personal Access Token, 백그라운드 모니터링 주기 및 앱 업데이트를 설정할 수 있는 다이얼로그입니다.
 */
@Composable
fun SettingsDialog(
    currentUsername: String,
    currentToken: String,
    currentInterval: Long,
    isAutoUpdateEnabled: Boolean = true,
    isCheckingUpdate: Boolean = false,
    updateStatusMessage: String? = null,
    onDismiss: () -> Unit,
    onCheckUpdateNow: () -> Unit = {},
    onSave: (username: String, token: String, intervalMinutes: Long, autoUpdateCheck: Boolean) -> Unit
) {
    var username by remember { mutableStateOf(currentUsername) }
    var token by remember { mutableStateOf(currentToken) }
    var selectedInterval by remember { mutableLongStateOf(currentInterval) }
    var autoUpdateCheck by remember { mutableStateOf(isAutoUpdateEnabled) }

    val intervalOptions = listOf(
        15L to "15분 (권장 최소값)",
        30L to "30분",
        60L to "1시간",
        180L to "3시간"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "모니터링 & 계정 설정",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // 1. 사용자명 입력
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("GitHub 사용자명/조직명") },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null)
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 2. Personal Access Token 입력 (선택)
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text("GitHub Token (선택 사항)") },
                    leadingIcon = {
                        Icon(Icons.Default.Key, contentDescription = null)
                    },
                    supportingText = {
                        Text(
                            text = "미입력 시 시간당 60회, 토큰 입력 시 시간당 5,000회까지 호출 가능합니다."
                        )
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 3. 백그라운드 확인 주기 선택
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "백그라운드 확인 주기",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                intervalOptions.forEach { (interval, label) ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedInterval == interval,
                            onClick = { selectedInterval = interval }
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = label, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 4. 앱 자체 최신 릴리즈 자동 업데이트 확인 설정
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "앱 최신 버전 (현재 v${BuildConfig.VERSION_NAME})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "시작 시 신규 버전 자동 확인",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked = autoUpdateCheck,
                        onCheckedChange = { autoUpdateCheck = it }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                OutlinedButton(
                    onClick = onCheckUpdateNow,
                    enabled = !isCheckingUpdate,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isCheckingUpdate) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("최신 릴리즈 확인 중...")
                    } else {
                        Text("지금 최신 릴리즈 확인")
                    }
                }

                if (!updateStatusMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = updateStatusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "안드로이드 OS 배터리 최적화 정책에 의해 실제 백그라운드 동작 시각은 약간의 오차가 있을 수 있습니다.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(username.trim(), token.trim(), selectedInterval, autoUpdateCheck)
                    onDismiss()
                }
            ) {
                Text("저장")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("취소")
            }
        }
    )
}
