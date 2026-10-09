# GitHub Release Notifier (안드로이드 앱) 🚀

GitHub의 공개 리포지토리 목록을 실시간으로 확인하고, 릴리즈 파일(에셋)의 다운로드 수 증가를 백그라운드 및 포그라운드에서 감지하여 푸시 알림을 띄워주는 최신 안드로이드 네이티브 앱입니다.

---

## 📱 주요 기능

1. **GitHub 공개 리포지토리 목록 조회**
   - 계정명 입력 시 해당 사용자의 모든 공개 저장소를 카드 형태로 시각화
   - 리포지토리별 스타(Stars), 포크(Forks), 주요 언어 뱃지 및 **누적 총 릴리즈 다운로드 수** 요약 제공
   - 정렬 옵션 지원 (다운로드 많은 순, 스타 많은 순, 최근 수정 순, 이름순)

2. **원클릭 웹브라우저 이동**
   - 저장소 카드 및 릴리즈 카드의 '웹에서 보기' / 브라우저 열기 버튼 클릭 시 스마트폰 기본 브라우저로 깃허브 웹페이지 즉시 연결

3. **릴리즈 다운로드 수 증가 실시간 & 백그라운드 푸시 알림**
   - 리포지토리별 개별 파일(.apk, .zip 등)의 다운로드 수를 로컬에 영속 캐싱
   - 신규 다운로드 발생 감지 시 시스템 상단 헤드업 알림 발송 (`🎉 [저장소] 다운로드 증가!`)
   - 안드로이드 표준 **WorkManager** 기반 백그라운드 주기적 모니터링 지원 (배터리 최적화 정책 준수)
   - 앱 내에서 즉시 알림 작동 여부를 검증할 수 있는 **'다운로드 +1 테스트'** 시뮬레이션 기능 탑재

4. **GitHub API 호출 한도(Rate Limit) 대응**
   - 미인증 시 시간당 60회 제한을 시간당 5,000회까지 확대할 수 있는 Personal Access Token(PAT) 설정 지원

---

## 🛠 기술 스택

- **언어**: Kotlin 2.3
- **UI 프레임워크**: Jetpack Compose (Material Design 3)
- **네트워크 & 직렬화**: OkHttp 4.12, Kotlinx Serialization JSON 1.8
- **백그라운드 작업**: AndroidX WorkManager (CoroutineWorker)
- **알림**: Android NotificationManagerCompat (Android 13+ `POST_NOTIFICATIONS` 런타임 권한 지원)
- **로컬 캐시**: SharedPreferences

---

## 📂 프로젝트 구조

```
app/src/main/java/com/antigravity/githubnoti/
├── MainActivity.kt               # 앱 진입점 및 테마 설정
├── Navigation.kt                 # 내비게이션 진입
├── data/
│   ├── api/GithubApiClient.kt   # GitHub REST API v3 통신 클라이언트
│   ├── local/PreferenceManager.kt # 이전 다운로드 수 및 설정 로컬 저장소
│   └── model/GithubModels.kt     # DTO 및 UI 상태 데이터 모델
├── notification/
│   └── NotificationHelper.kt    # 알림 채널 생성 및 푸시 알림 발송
├── worker/
│   ├── ReleaseNotificationWorker.kt # 백그라운드 주기적 다운로드 감지 워커
│   └── WorkScheduler.kt          # WorkManager 스케줄링 관리 유틸리티
└── ui/
    ├── main/MainScreen.kt        # 대시보드 및 리포지토리 메인 화면
    ├── main/MainScreenViewModel.kt # 비즈니스 로직 및 정렬/필터링
    └── components/
        ├── RepoCard.kt           # 리포지토리 & 릴리즈 다운로드 카드
        ├── SettingsDialog.kt     # 계정명/토큰/모니터링 주기 설정 팝업
        └── NotificationPermissionBanner.kt # 알림 권한 허용 배너
```

---

## 📄 라이선스

이 프로젝트는 오픈소스로 배포됩니다.
