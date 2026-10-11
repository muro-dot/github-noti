# 작업 계획 및 결과 보고서

## 1. 문제 분석
1. **리포지토리 이름 변경(대소문자/이전 이름)에 따른 중복 알림**:
   - `muro-dot/LYWSD02_BLE_Android_App` (이전 명칭)과 `muro-dot/lywsd02-ble-android-app` (신규 명칭)이 SharedPreferences `trackedRepos`에 둘 다 보존되어 Worker가 2번 API를 호출하고 알림을 2개 발생시킴.
   - 에셋 다운로드 카운터 키도 대소문자별로 분리되어 각각 카운팅됨.
2. **알림 클릭 시 브라우저 이동**:
   - `NotificationHelper`의 알림 Intent가 브라우저 URL 이동(`ACTION_VIEW`)으로 설정되어 앱 대신 브라우저가 실행됨.
3. **앱 이동 및 실행 시 새로고침 누락**:
   - 알림 터치 또는 앱 복귀 시 UI 메모리(ViewModel)가 새로고침되지 않아 백그라운드에서 감지된 최신 카운트가 화면에 즉시 표시되지 않음.
4. **알림 후 릴리즈 다운로드 수치가 이전 기록 그대로 표시되는 현상**:
   - Worker가 SharedPreferences에 새 카운트를 저장하더라도 UI에서 갱신이 발생하지 않았거나 API 응답과의 정합성이 어긋남.

---

## 2. 작업 내용 및 수정 내역
1. **`PreferenceManager.kt`**:
   - `trackedRepos`: 대소문자 무시(case-insensitive) 정규화 적용.
   - `cleanUpTrackedRepos`: GitHub에서 조회한 최신 리포지토리 목록을 기준으로 저장된 추적 리포지토리를 정규화하고 과거/중복 이름 자동 제거.
   - `makeAssetKey`: 리포지토리명을 lowercase로 정규화하여 대소문자 변경 시에도 동일한 에셋 카운터를 공유하도록 처리 및 기존 레거시 키 마이그레이션 지원.
   - `setRepoTracked`, `getPreviousDownloadCount`: 대소문자 무시 비교 및 안전 조회.
2. **`ReleaseNotificationWorker.kt`**:
   - `trackedRepos` 순회 시 `distinctBy { it.lowercase() }`로 정규화하여 중복 조회 및 중복 알림 방지.
3. **`NotificationHelper.kt`**:
   - `showDownloadIncreaseNotification` 및 `showNewReleaseNotification`의 PendingIntent를 `MainActivity`로 변경.
   - 인텐트에 `EXTRA_REFRESH = true` 및 `EXTRA_REPO_NAME` 플래그 전달.
   - `notificationId` 생성 시 리포지토리명을 lowercase로 처리하여 중복 알림 차단.
4. **`MainActivity.kt`**:
   - `onNewIntent` 구현 및 알림 인텐트 수신 시 즉시 데이터 새로고침 트리거.
   - `onResume()` 시 포그라운드 복귀 후 일정 시간 경과 시 자동 새로고침 연동.
   - `MainNavigation`에 `mainViewModel` 전달.
5. **`Navigation.kt`**:
   - `MainNavigation`에 `viewModel` 파라미터 전달 구조 반영.
6. **`MainScreenViewModel.kt`**:
   - `fetchRepositories`: `cleanUpTrackedRepos` 호출로 저장소 목록 자동 정규화.
   - Worker가 저장한 로컬 최신 다운로드 카운트(`getPreviousDownloadCount`)와 API 수치 중 최댓값을 에셋에 반영하여 알림에 뜬 다운로드 수가 화면에도 즉각 일치하여 표시되도록 보장.
   - `toggleRepoTracking`, `toggleRepoExpand`, `simulateDownloadIncrease` 등에서 리포지토리 대소문자 무시 비교(`equals(..., ignoreCase = true)`) 적용.
7. **`GithubApiClient.kt`**:
   - 요청 헤더에 `Cache-Control: no-cache`를 추가하여 캐시로 인한 이전 데이터 응답 방지.

---

## 3. 검증 결과
- **Gradle Release 빌드**: `assembleRelease` 정상 완료 (Exit Code 0).
- **APK 생성**: `app/build/outputs/apk/release/github-noti_v1.1.5.apk` (13,224,436 bytes).
---

## 4. v1.1.6 버전업 및 릴리즈 게시 작업
- **버전 변경**: `app/build.gradle.kts`
  - `versionCode = 8`
  - `versionName = "1.1.6"`
- **빌드 검증**: `assembleRelease`
- **로컬 및 네트워크 배포**: `\\192.168.0.250\web\github-noti` 최신 v1.1.6 APK 복사
- **GitHub 릴리즈 발행**: `gh release create v1.1.6` (태그 생성, APK 에셋 첨부, 간결한 릴리즈 노트 포함)
- **완료 알림**: 딩동 사운드 재생

