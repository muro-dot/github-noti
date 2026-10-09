package com.antigravity.githubnoti.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SystemUpdate
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.antigravity.githubnoti.BuildConfig
import com.antigravity.githubnoti.R
import com.antigravity.githubnoti.util.LocaleHelper

/**
 * 깃허브 계정명, 백그라운드 모니터링 주기, 언어 및 앱 업데이트를 설정할 수 있는 다이얼로그입니다.
 * 줄간격을 컴팩트하게 줄이고 확인 주기 및 언어를 드롭다운 메뉴로 선택할 수 있도록 구성되었습니다.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsDialog(
    currentUsername: String,
    currentInterval: Long,
    currentLanguage: String = LocaleHelper.LANGUAGE_SYSTEM,
    isAutoUpdateEnabled: Boolean = true,
    isCheckingUpdate: Boolean = false,
    updateStatusMessage: String? = null,
    onDismiss: () -> Unit,
    onCheckUpdateNow: () -> Unit = {},
    onSave: (username: String, intervalMinutes: Long, autoUpdateCheck: Boolean, language: String) -> Unit
) {
    var username by remember { mutableStateOf(currentUsername) }
    var selectedInterval by remember { mutableLongStateOf(currentInterval) }
    var selectedLanguage by remember { mutableStateOf(currentLanguage) }
    var autoUpdateCheck by remember { mutableStateOf(isAutoUpdateEnabled) }

    var intervalExpanded by remember { mutableStateOf(false) }
    var languageExpanded by remember { mutableStateOf(false) }

    val intervalOptions = listOf(
        15L to stringResource(R.string.interval_15m),
        30L to stringResource(R.string.interval_30m),
        60L to stringResource(R.string.interval_1h),
        180L to stringResource(R.string.interval_3h),
        360L to stringResource(R.string.interval_6h),
        720L to stringResource(R.string.interval_12h),
        1440L to stringResource(R.string.interval_24h)
    )

    val languageOptions = listOf(
        LocaleHelper.LANGUAGE_SYSTEM to stringResource(R.string.language_system),
        LocaleHelper.LANGUAGE_KOREAN to stringResource(R.string.language_korean),
        LocaleHelper.LANGUAGE_ENGLISH to stringResource(R.string.language_english)
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.dialog_settings_title),
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
                    label = { Text(stringResource(R.string.settings_username_label)) },
                    leadingIcon = {
                        Icon(Icons.Default.Person, contentDescription = null)
                    },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 2. 백그라운드 확인 주기 선택 (드롭다운)
                ExposedDropdownMenuBox(
                    expanded = intervalExpanded,
                    onExpandedChange = { intervalExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = intervalOptions.firstOrNull { it.first == selectedInterval }?.second ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.settings_interval_section)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = intervalExpanded) },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = intervalExpanded,
                        onDismissRequest = { intervalExpanded = false }
                    ) {
                        intervalOptions.forEach { (interval, label) ->
                            DropdownMenuItem(
                                text = { Text(label, style = MaterialTheme.typography.bodyMedium) },
                                onClick = {
                                    selectedInterval = interval
                                    intervalExpanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 3. 언어 설정 (드롭다운)
                ExposedDropdownMenuBox(
                    expanded = languageExpanded,
                    onExpandedChange = { languageExpanded = it },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = languageOptions.firstOrNull { it.first == selectedLanguage }?.second ?: "",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text(stringResource(R.string.settings_language_section)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Language,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = languageExpanded) },
                        modifier = Modifier
                            .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = languageExpanded,
                        onDismissRequest = { languageExpanded = false }
                    ) {
                        languageOptions.forEach { (code, label) ->
                            DropdownMenuItem(
                                text = { Text(label, style = MaterialTheme.typography.bodyMedium) },
                                onClick = {
                                    selectedLanguage = code
                                    languageExpanded = false
                                },
                                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 4. 앱 최신 릴리즈 자동 업데이트 확인 설정
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.SystemUpdate,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.settings_app_update_section, BuildConfig.VERSION_NAME),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.settings_auto_update_label),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Switch(
                        checked = autoUpdateCheck,
                        onCheckedChange = { autoUpdateCheck = it }
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))

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
                        Text(stringResource(R.string.settings_checking_update))
                    } else {
                        Text(stringResource(R.string.settings_btn_check_update))
                    }
                }

                if (!updateStatusMessage.isNullOrBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = updateStatusMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(R.string.settings_battery_notice),
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
                    onSave(username.trim(), selectedInterval, autoUpdateCheck, selectedLanguage)
                    onDismiss()
                }
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
