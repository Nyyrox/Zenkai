# Aniverse Android

A dark, Compose-first Android anime browser built around the public AniList GraphQL API.

## Included

- Home: featured hero + trending/popular media cards
- Browse: anime search, responsive media grid, pagination
- Airing & Scheduled: upcoming episodes from AniList
- Player-style details screen: episode buttons with 1–100 range paging
- AniList legal streaming-source links open through the system browser
- Dark glass-like cards, gradients, rounded surfaces, and animated page transitions
- GitHub Actions debug APK build

## Build

The repository is configured for Android Gradle Plugin 8.13.2, Gradle 8.13, JDK 17, compile/target SDK 36, and Jetpack Compose BOM 2026.08.00.

For GitHub Actions, open **Actions → Build Aniverse APK**. The workflow uploads `aniverse-debug-apk` as an artifact.

## Playback note

AniList provides anime metadata and links to legal external streaming pages; it does not provide a universal direct video-stream API. The player screen therefore uses AniList for episode/metadata and exposes the legal streaming destinations supplied by AniList rather than scraping or proxying third-party video hosts.
