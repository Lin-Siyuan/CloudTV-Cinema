# Akai CloudTV518 - Live Cinema OS & Android TV App

Ultra-lean, resilient Android TV cinema app engineered specifically for 1GB RAM / 32-bit (armeabi-v7a) AOSP smart TVs such as the **Akai CloudTV518** (4-Core Cortex-A53, Mali-G31 MP2 GPU, 128MB Dalvik heap cap).

## Features
- **32-Bit (armeabi-v7a) Architecture**: Strict NDK ABI filtering and 128MB Dalvik heap compliance.
- **Hardware VPU Decoding**: Filtered for Mali-G31 MP2 hardware decoders (`OMX.mali.h264.hw` and `OMX.mali.hevc.hw`), automatically rejecting unaccelerated 4K AV1 traps.
- **Bounded Memory Buffer**: Media3 ExoPlayer configured with `DefaultLoadControl` buffer durations capped at 10s-20s (~14.2MB PSS ceiling).
- **Multi-Source Failover Orchestrator**: Sub-800ms automatic mirror failover with circuit breakers and upstream HTTP Referer/Origin injection.
- **D-Pad Spatial Navigation**: Custom `CinemaFocusCard` with 1.08x hardware layer scale and #00E5FF neon focus indicators.
- **Web TV Simulator & Chaos Lab**: Standalone interactive HTML app (`index.html`) simulating the full TV environment with procedural 60FPS video rendering, T9 search, subtitle cycling, and fault injection.

## Project Structure
```
CloudTV_Project/
├── .github/workflows/build-apk.yml     # Automated cloud APK builder
├── app/
│   ├── build.gradle.kts                # 32-bit armeabi-v7a build configuration
│   ├── proguard-rules.pro              # Optimized minification rules
│   └── src/main/
│       ├── AndroidManifest.xml         # Leanback TV declarations & permissions
│       ├── java/com/tv/stream/
│       │   ├── MainActivity.kt         # TV UI controller & player lifecycle
│       │   ├── data/providers/         # UniversalAddonExtractor.kt
│       │   ├── domain/orchestrator/    # ResilientStreamOrchestrator.kt
│       │   ├── playback/               # CloudTvPlayer.kt (Media3 ExoPlayer)
│       │   └── ui/components/          # CinemaFocusCard.kt
│       └── res/                        # TV banners, launcher icons, layout
├── gradle/wrapper/                     # Gradle 8.4 wrapper & JAR
├── build.gradle.kts                    # Root build script
├── settings.gradle.kts                 # Project settings & repositories
├── index.html                          # Interactive Web TV Simulator & Chaos Lab
└── README.md
```

## Automatic Cloud APK Compilation
Every push to `main` triggers the GitHub Action `.github/workflows/build-apk.yml`, which compiles `app-release.apk` using Gradle 8.4 and JDK 17, and publishes it as a downloadable artifact.

## Sideloading to Akai CloudTV518
1. Download `app-release.apk` from the latest GitHub Actions run artifact.
2. Copy the APK to a FAT32 USB flash drive.
3. Plug the USB drive into your Akai TV.
4. Open the TV's **File Manager** / **Media Center**, locate the APK, and install.
