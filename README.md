# 🔔 GitHub Release Notifier (Android) 🚀

<p align="center">
  <strong>Native Android application for real-time tracking and background push notifications of GitHub release download counts</strong>
</p>

<p align="center">
  <a href="https://github.com/muro-dot/github-noti/releases/latest"><img src="https://img.shields.io/github/v/release/muro-dot/github-noti?color=blue&label=Latest%20Release" alt="Latest Release"></a>
  <a href="https://github.com/muro-dot/github-noti/releases"><img src="https://img.shields.io/github/downloads/muro-dot/github-noti/total?color=blueviolet&logo=github&label=Downloads" alt="Total Downloads"></a>
  <a href="https://github.com/muro-dot/github-noti/releases/latest/download/github-noti_v1.1.5.apk"><img src="https://img.shields.io/badge/Download-APK-success?logo=android" alt="Download APK"></a>
  <img src="https://img.shields.io/badge/License-GPL--3.0-orange" alt="License">
</p>

---

## 📖 Overview

**GitHub Release Notifier (github-noti)** is a modern native Android application built with Jetpack Compose and Kotlin Coroutines. It monitors your public GitHub repositories, tracks binary asset release download counts, and alerts you via system push notifications whenever someone downloads your software releases or when a new version is published.

---

## ✨ Key Features

| Feature | Description |
| :--- | :--- |
| 🌍 **Multi-language Support (i18n)** | Automatically displays in English if the device system language is not Korean. Includes in-app manual language switcher (System Default / 한국어 / English) in Settings. |
| 📦 **Repository & Release Tracking** | Enter any GitHub user/organization to explore all public repositories with stars, forks, and cumulative download counters visualized on sleek Material 3 cards. |
| 🔔 **Download Increase Alerts** | Detects incremental download count changes (.apk, .zip, etc.) against locally cached records and fires head-up push notifications (`🎉 [Repository] Download Increased!`). |
| 🔄 **Periodic Background Sync** | Powered by AndroidX WorkManager, periodically checking repositories in the background (15m, 30m, 1h, 3h, 6h, 12h, 24h) while strictly adhering to Android battery optimization policies. |
| 🚀 **In-App Auto Update & Install** | Automatically verifies new releases of this app, initiates in-app background download via DownloadManager, and seamlessly launches the native package installer upon completion. |
| 🧪 **Notification Test Simulation** | Built-in 'Download +1 Test' button for instant validation of notification channels, sounds, and head-up banner behavior. |
| 🔍 **Search & Multi-criteria Sort** | Fast repository filtering with real-time search, sorted by downloads (desc), stars (desc), last updated, or alphabetical order. |
| 🌐 **One-Click Browser Access** | Direct navigation buttons to open repository source pages or release download pages in your smartphone's default web browser. |

---

## 🛠️ Tech Stack & Architecture

- **Language**: Kotlin 2.3 (Toolchain Java 17)
- **UI Framework**: Jetpack Compose, Material Design 3
- **Architecture**: MVVM + Clean Architecture, Coroutines & Flow
- **Minimum SDK**: Android 7.0 (API 24)
- **Target SDK**: Android 16 (API 36)
- **Networking & Serialization**: OkHttp 4.12, Kotlinx Serialization JSON 1.8
- **Background Worker**: AndroidX WorkManager (CoroutineWorker)
- **Notifications**: Android NotificationManagerCompat (Android 13+ `POST_NOTIFICATIONS`)
- **Local Persistence**: SharedPreferences (Download counts & app preferences)

---

## 📥 Download & Install

You can download the pre-compiled APK directly from the [GitHub Releases](https://github.com/muro-dot/github-noti/releases/latest) page:

- 📦 **Latest APK**: [github-noti_v1.1.5.apk](https://github.com/muro-dot/github-noti/releases/latest/download/github-noti_v1.1.5.apk)
- Requires Android 7.0 (API 24) or higher.

---

## 🚀 Building & Testing

### Prerequisites
- Android Studio Ladybug / Meerkat or later
- Android SDK 36
- JDK 17

### Build Commands

```bash
# Build Debug APK
./gradlew assembleDebug

# Build Release APK (Generates github-noti_v1.1.5.apk)
./gradlew assembleRelease

# Run Unit Tests
./gradlew testDebugUnitTest
```

---

## 📜 Attribution & License

- License: **GNU General Public License v3.0 (GPL-3.0)**
