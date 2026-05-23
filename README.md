<div align="center">

![TorrentSearch icon](https://github.com/prajwalch/TorrentSearch/blob/main/fastlane/metadata/android/en-US/images/icon.png)
<br/>
[![Latest release](https://img.shields.io/github/v/release/prajwalch/TorrentSearch?style=for-the-badge&color=green)](https://github.com/prajwalch/TorrentSearch/releases)
[![F-Droid](https://img.shields.io/f-droid/v/com.prajwalch.torrentsearch?style=for-the-badge&color=blue)](https://f-droid.org/packages/com.prajwalch.torrentsearch)
[![IzzyOnDroid](https://img.shields.io/endpoint?style=for-the-badge&color=skyblue&url=https://apt.izzysoft.de/fdroid/api/v1/shield/com.prajwalch.torrentsearch)](https://apt.izzysoft.de/fdroid/index/apk/com.prajwalch.torrentsearch)
[![Downloads](https://img.shields.io/github/downloads/prajwalch/TorrentSearch/total?style=for-the-badge&color=lightgreen)](https://github.com/prajwalch/TorrentSearch/releases)
[![Translation status](https://img.shields.io/weblate/progress/torrentsearch?style=for-the-badge)](https://hosted.weblate.org/engage/torrentsearch/)

# TorrentSearch

TorrentSearch is an Android app for searching torrents across multiple providers simultaneously,
with fast search speed, detailed results, category filters, and a full set of torrent actions.
</div>

## Download

> [!NOTE]
> Android 7.1+ is required

> Nightly builds are available in the artifacts section of the GitHub
> Actions [workflow](https://github.com/prajwalch/TorrentSearch/actions) runs.

[<img src="https://f-droid.org/badge/get-it-on.png" alt="Get it on F-Droid" height="80">](https://f-droid.org/packages/com.prajwalch.torrentsearch)
[<img src="https://gitlab.com/IzzyOnDroid/repo/-/raw/master/assets/IzzyOnDroid.png" alt="Get it on IzzyOnDroid" height="80">](https://apt.izzysoft.de/fdroid/index/apk/com.prajwalch.torrentsearch)
[<img src="https://github.com/machiav3lli/oandbackupx/blob/034b226cea5c1b30eb4f6a6f313e4dadcbb0ece4/badge_github.png" alt="Get it on GitHub" height="80">](https://github.com/prajwalch/TorrentSearch/releases/latest/)
[<img src="https://github.com/ImranR98/Obtainium/blob/main/assets/graphics/badge_obtainium.png" alt="Get it on Obtainium" height="55">](https://apps.obtainium.imranr.dev/redirect?r=obtainium://add/https://github.com/prajwalch/TorrentSearch/)

## Features

### Search

- Query all providers simultaneously, with per-provider enable/disable toggles
- Search by category: `Anime`, `Apps`, `Books`, `Games`, `Movies`, `Series`, and more
- Results stream in progressively as each provider responds
- Sort by `torrent name`, `seeders`, `peers`, `file size`, or `date`
- Filter out dead or already-viewed torrents
- Filter results by name, provider or category

### Detailed Results

Each result includes:

- Torrent name
- File size
- Seeders and peers
- Upload date
- Category
- NSFW indicator
- Provider name

### Torrent Actions

- **Open magnet link** in an external torrent client
- **Download `.torrent` file** to local storage
- **View torrent details** directly inside the app or open the full page in your browser
- **Copy or share** the magnet link or details page URL

### Torrent Details

- **Native details screen** — view torrent details inside the app without a browser or WebView; can
  be disabled to open the page directly in your default browser instead
- Media poster with automatic NSFW image blurring (can be disabled)
- Screenshot previews
- Full description with inline image support

### Browse

- Explore **top** and **latest** torrents from your enabled providers
- Torrents stream in progressively as each provider responds
- Filter by category and sort order — changing either instantly refreshes results
- Filter out dead or already-viewed torrents
- Filter results by torrent name or provider

### Bookmarks

- Save torrents for later
- Export bookmarks for backup or migration
- Import previously exported bookmarks

### Safe Mode

When enabled, Safe Mode automatically:

- Disables unsafe and NSFW providers
- Hides NSFW categories
- Hides NSFW search results

### Integrations

Connect your own indexer via the [Torznab API](https://torznab.github.io/spec-1.3-draft/torznab/Specification-v1.3.html#torznab-api-specification):

- [Jackett](https://github.com/Jackett/Jackett)
- [Prowlarr](https://github.com/Prowlarr/Prowlarr)
- Other *arr services

See the [wiki](https://github.com/prajwalch/TorrentSearch/wiki) for setup instructions.

### Material 3 Design

- Clean, easy-to-use interface
- Adapts to your wallpaper and system theme, with full light/dark mode support

## Screenshots

<img width="23%" src="https://github.com/prajwalch/TorrentSearch/blob/main/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot_1.jpg" alt="Search results"> <img width="23%" src="https://github.com/prajwalch/TorrentSearch/blob/main/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot_2.jpg" alt="Search bar"> <img width="23%" src="https://github.com/prajwalch/TorrentSearch/blob/main/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot_3.jpg" alt="Torrent actions"> <img width="23%" src="https://github.com/prajwalch/TorrentSearch/blob/main/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot_4.jpg" alt="Torrent client not found">
<br>
<img width="23%" src="https://github.com/prajwalch/TorrentSearch/blob/main/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot_5.jpg" alt="Bookmarks"> <img width="23%" src="https://github.com/prajwalch/TorrentSearch/blob/main/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot_6.jpg" alt="Settings"> <img width="23%" src="https://github.com/prajwalch/TorrentSearch/blob/main/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot_7.jpg" alt="Search providers">
<img width="23%" src="https://github.com/prajwalch/TorrentSearch/blob/main/fastlane/metadata/android/en-US/images/phoneScreenshots/screenshot_8.jpg" alt="Dark theme">

## Building from Source

The easiest way is to open the project in [Android Studio](https://developer.android.com/studio) — 
it handles all setup and lets you run the app in a single click.

### Command Line

**Requirements:**

- JDK 17+ with `JAVA_HOME` set ([Adoptium](https://adoptium.net/) recommended)
- Android SDK — version depends on project configuration. If you have Android Studio installed, it
  has already downloaded, set up, and configured the SDK location for you. Otherwise, install it
  manually and set `ANDROID_HOME`, or add `sdk.dir` to `local.properties` in the project root.

```sh
git clone https://github.com/prajwalch/TorrentSearch.git
cd TorrentSearch
./gradlew assembleDebug
```

Output: `app/build/outputs/apk/debug/`

## Contributing

Bug fixes, new providers, UI improvements, and translations are all welcome.
Read [CONTRIBUTING.md](https://github.com/prajwalch/TorrentSearch/blob/main/CONTRIBUTING.md) before
opening a pull request.

### Translation

Translations are managed on [Weblate](https://hosted.weblate.org/projects/torrentsearch/) — 
no local setup needed, contribute directly from your browser.

[![Translation status](https://hosted.weblate.org/widget/torrentsearch/multi-auto.svg)](https://hosted.weblate.org/engage/torrentsearch/)

## Contributors

<a href="https://github.com/prajwalch/TorrentSearch/graphs/contributors">
  <img src="https://contrib.rocks/image?repo=prajwalch/TorrentSearch" alt="TorrentSearch Contributors"/>
</a>

## Tech Stack and Open Source Libraries

- **Language:** [Kotlin](https://kotlinlang.org/)
- **UI:** [Jetpack Compose](https://developer.android.com/compose), [Material 3](https://m3.material.io/)
- **Architecture:** [Modern App Architecture](https://developer.android.com/topic/architecture)
- **Async:** [Coroutines](https://kotlinlang.org/docs/coroutines-overview.html), [Flow](https://kotlinlang.org/docs/flow.html)
- **Networking:** [Ktor](https://ktor.io/)
- **Storage:** [Room](https://developer.android.com/training/data-storage/room), [Jetpack DataStore](https://developer.android.com/topic/libraries/architecture/datastore)
- **Dependency injection:** [Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
- **Image loading:** [Coil](https://coil-kt.github.io/coil/)
- **HTML parsing**: [Jsoup](https://github.com/jhy/jsoup)
- **Immutable collections**: [Kotlinx immutable collections](https://github.com/Kotlin/kotlinx.collections.immutable)
- **Markdown handling**: [ComposeMarkdown](https://github.com/jeziellago/compose-markdown), [FlexMark (Markdown to HTML)](https://github.com/vsch/flexmark-java)
- **Scrollbar**: [LazyColumnScrollbar](https://github.com/nanihadesuka/LazyColumnScrollbar)
- **Image Blurring**: [BlurTransformation](https://github.com/T8RIN/BlurTransformation)

## Acknowledgements

- [IconKitchen](https://icon.kitchen/) — app icon
- [Metrolist](https://github.com/MetrolistGroup/Metrolist) and [Canta](https://github.com/samolego/Canta) — referenced for architecture and implementation
  patterns during development

## Disclaimer

TorrentSearch **does not host, store, or distribute any torrent files or copyrighted content**.
It searches publicly accessible third-party sources and displays the results. The developer is not
responsible for how those results are accessed or used.

Users are responsible for complying with their local laws and regulations.