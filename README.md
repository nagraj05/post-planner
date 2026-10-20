<div align="center">
  <img src="composeApp/src/androidMain/res/drawable-nodpi/ic_launcher_foreground_art.png" alt="Post Planner app icon" width="120" />

  # Post Planner

  **Plan and perfect your Instagram grid before you post.**

  [![Android Release](https://github.com/nagraj05/instagram-planner/actions/workflows/android-release.yml/badge.svg)](https://github.com/nagraj05/instagram-planner/actions/workflows/android-release.yml)
  [![Latest release](https://img.shields.io/github/v/release/nagraj05/instagram-planner?display_name=tag)](https://github.com/nagraj05/instagram-planner/releases/latest)
  ![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)
  ![Kotlin](https://img.shields.io/badge/Kotlin-Multiplatform-7F52FF?logo=kotlin&logoColor=white)

  [Download APK](https://github.com/nagraj05/instagram-planner/releases/latest) · [Report a bug](https://github.com/nagraj05/instagram-planner/issues/new)
</div>

## About

Post Planner is a private, offline-first Instagram grid planner. Import photos from your phone, preview how they look together, and reorder them until the feed feels right—all without signing in to Instagram or uploading your photos to a server.

## Features

- Preview posts in a modern, packed Instagram-style grid.
- Add several photos at once from Samsung Gallery, the Android photo picker, or device folders.
- Long-press and drag a photo to reorder the grid with live movement.
- Select photos and **change, swap, archive, delete, copy, or move** them.
- Keep separate plans for multiple Instagram profiles.
- Preview each profile with a photo, display name, bio, website, follower count, and following count.
- Restore or permanently delete archived photos.
- Choose a light, dark, or system-matched theme.
- Keep profiles, photos, archive state, and grid order after closing the app.

## Privacy

Post Planner is designed to work locally:

- No Instagram login is required.
- No account credentials are collected.
- Imported images and planner data are stored in the app's private storage on your device.
- Android backups are disabled for the app.
- Deleting the app also removes its locally stored planner data.

> Post Planner is an independent planning tool and is not affiliated with, endorsed by, or connected to Instagram or Meta.

## Install the app

1. Open the [latest GitHub release](https://github.com/nagraj05/instagram-planner/releases/latest).
2. Download the `PostPlanner-*.apk` file.
3. Open the downloaded APK on your Android device.
4. If Android asks, allow installation from the browser or file manager you used.

The current Android build requires **Android 8.0 (API 26)** or newer.

## Tech stack

| Area | Technology |
| --- | --- |
| Language | Kotlin |
| UI | Compose Multiplatform and Material 3 |
| Architecture | Kotlin Multiplatform project |
| Android storage | App-private files and SharedPreferences |
| Async work | Kotlin Coroutines |
| Build | Gradle Kotlin DSL and Java 17 |
| Releases | GitHub Actions and GitHub Releases |

The repository includes Android and iOS targets. The current distributable release and device integrations are focused on Android.

## Build locally

### Requirements

- Android Studio with the Android SDK installed
- JDK 17
- An Android device or emulator running Android 8.0+

Clone the repository:

```powershell
git clone https://github.com/nagraj05/instagram-planner.git
cd instagram-planner
```

Open the project in Android Studio, allow Gradle sync to finish, select the `composeApp` run configuration, and run it on a device or emulator.

To build a debug APK from PowerShell:

```powershell
.\gradlew.bat :composeApp:assembleDebug
```

The APK will be created at:

```text
composeApp/build/outputs/apk/debug/composeApp-debug.apk
```

## Create a release

Update `versionCode` and `versionName` in `composeApp/build.gradle.kts`, commit the change, and push a version tag:

```powershell
git add composeApp/build.gradle.kts
git commit -m "Prepare v0.2.1 release"
git push origin main
git tag -a v0.2.1 -m "Post Planner v0.2.1"
git push origin v0.2.1
```

Tags beginning with `v` trigger the release workflow. GitHub Actions builds the signed APK and attaches it to a new GitHub Release.

The workflow expects these repository secrets:

- `ANDROID_KEYSTORE_BASE64`
- `ANDROID_KEYSTORE_PASSWORD`
- `ANDROID_KEY_ALIAS`
- `ANDROID_KEY_PASSWORD`

Never commit a keystore, passwords, access tokens, `local.properties`, or other local secrets.

## Project structure

```text
composeApp/
├── src/commonMain/     Shared Compose UI and planner models
├── src/androidMain/    Android activity, storage, and media integration
└── src/iosMain/        iOS entry point

.github/workflows/      Automated Android release workflow
```

## Roadmap

- Continue polishing grid interactions and accessibility.
- Add optional captions, notes, and scheduling metadata.
- Expand and validate the iOS implementation.
- Add import/export for local planner backups.

---

<div align="center">
  Made with Kotlin and Compose.
</div>
