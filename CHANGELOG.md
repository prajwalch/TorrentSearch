# Unreleased

- Fixed `UIndex` to correctly extract seeders and peers from details page (6dd36cb1)

# v0.5.0

### What's new

- Added an in-app torrent details screen
- Added support for browsing latest and top torrents
- Added a viewed indicator for torrents, by [@armanmaurya](https://github.com/armanmaurya)
- Improved category support for providers
- Extended `.torrent` file download support
- Improved filtering torrents by name
- UI enhancements across the whole app
- Added [NikoBT](https://nikobt.to) as an Anime provider (disabled by default)
- Added Japanese translation by [@Sakuya_San](https://github.com/Sakuya_San)
- Added Italian translation by [@elenaferr0](https://github.com/elenaferr0)
- In the search screen, an ongoing search can now be cancelled via `⋮ > Stop Search`
- The enabled/total provider count (e.g., "3 of 24 enabled") is now displayed as a subtitle in the
  "Search providers" screen's top bar
- `TokyoToshokan` now provides a file download link

### In-app torrent details screen

The new opt-in details screen lets you view torrent details directly inside the app.
It displays all the essential torrent information and description, complete with image loading
support. Extended capabilities include a media poster (shown only when a poster URL is available)
with automatic NSFW image blurring and screenshot previews. This feature is disabled by default,
but can be easily enabled from the settings screen. A dedicated option to toggle automatic NSFW
image blurring is also available.

### Browse screen

Explore the latest and top torrents through a brand-new Browse screen, accessible via a dedicated
button on the Home screen. Category chips are shared across both Search and Browse — with one key
difference: selecting a category in Browse fetches new torrents, while in Search it filters
existing results client-side. Like Search, Browse queries only your enabled providers.

### Viewed indicator for torrents

[@armanmaurya](https://github.com/armanmaurya) introduced a new feature that marks torrents as
viewed and dims them after you tap on them, making it easier to track what you've already seen. 
A `Hide viewed` filter on the Search and Browse screens lets you hide viewed torrents manually.
Note that it hides viewed torrents at the time of activation, so toggle it off and on to hide newly
viewed ones. To clear your viewed history, use "Clear viewed torrents" in settings.

### Extended `.torrent` file download support

TorrentSearch now uses [itorrents.net](https://itorrents.net) as a fallback source for downloading
`.torrent` files when a provider doesn't return a download link. As a result, the
`Download torrent file` action is now displayed for all torrents but the file availability is not
guaranteed, and a message will be shown if the file cannot be found.

### Improved filtering torrents by name

Filtering torrents by name now matches each word in your query individually, so torrents matching
any word is included rather than requiring an exact phrase match. This feature applies to all
Search, Bookmarks and Browse screen.

Examples:

`primer 1080` matches:

- **Primer**.2004.**1080p**.BluRay
- **Primer**.2004.DVDRip
- Interstellar.**1080p**.BluRay

`breaking bad 720` matches:

- **Breaking**.**Bad**.S01.**720p**
- **Breaking**.**Bad**.S02.1080p
- Inception.**720p**.WEB-DL

### Improved category support for providers

Previously, providers were limited to declaring a single supported category. This caused two
problems: providers supporting many categories, like `Nyaa`, were restricted to just one
(e.g. `Anime`), while providers with partial support, like `InternetArchive`, were forced to
declare `All` even though they don't cover everything.

Providers now declare all their supported categories, improving provider selection during search.
`Nyaa`, for example, now supports all its categories instead of just `Anime`. Providers like
`InternetArchive`, which were previously forced to `All`, now accurately reflect only the categories
they actually support. The "Search providers" screen now shows all supported categories for each
provider.

### What's changed

- Improved the "No results found" message with a clearer description, a solution hint, and a direct
  navigation button to the providers screen
- Updated `LimeTorrents` provider URL to [limetorrents.fun](https://limetorrents.fun)
- Improved the efficiency of large search result accumulation.
- Improved date handling for torrents to ensure a consistent, social-media-style upload date format.
  This also fixes the previously broken "Sort by date" feature for torrent lists.
- Improved HTML scraping speed for the `XXXClub`, `TheRarBg`, and `MyPornClub` providers
- Updated `Yts` provider URL to [https://yts.bz](https://yts.bz)
- The `Other` category is no longer marked as NSFW
- Torrents are no longer marked as NSFW when no category is available
- The `Default category` setting is now deprecated. It remains in the app temporarily but is
non-functional except for home screen and will be removed in a future version.

### What's fixed

- "Enable all" and "Disable all" on the Search Providers screen now respect active filters
  Previously, these actions ignored any active category filter and affected all providers regardless
- Fixed an issue where enabling a provider immediately after a fresh app install would
  inadvertently disable other providers
- Fixed an issue that caused some torrents to have an empty magnet URI, by
  [@armanmaurya](https://github.com/armanmaurya)
- Bookmarking torrents with duplicate names is now allowed.
  Previously, it would silently fail if another bookmark with the same name already existed.
- Added a fix that automatically trims a trailing space (e.g., "ubuntu ") when accepting a keyboard
  suggestion ensuring clean queries, correct results, and accurate search history entries
- Fixed `SubsPlease` provider to correctly extract show names with episode numbers
- Fixed `AniRena` provider for the updated website layout
- Fixed `TorrentDatabase` provider for the updated website layout
- Fixed `UIndex` provider, which was returning truncated torrent names

# v0.4.9

## Important news

The following search providers are currently broken. They remain in the app for now but may be
removed in a future release if the issue persists.

- [Eztv](https://eztvx.to) (Cloudflare protected)
- [TorrentDownloads](https://torrentdownloads.pro) (Cloudflare protected)
- [BitSearch](https://bitsearch.to) (down)

### What's new

- Added support for downloading `.torrent` files locally.

> [!NOTE]
> Only a few search providers (see below) currently support file downloading, but there is already a
> plan to extend support to other providers as well (see #86).

- Added support for `android.intent.action.SEARCH` to improve integration with search-based
  launchers (see #84).
- Added category filter for search results.
- Added three new actions to the providers filter:
    - **Select all**: Selects all providers.
    - **Deselect all**: Deselects all providers.
    - **Invert selection**: Inverts the current selection — selected providers become deselected and
      deselected become selected.
- Category chips are now displayed in the expanded search bar, making it easier to change a category
  and initiate a search while the search bar is in the expanded state.
- New UI for search results filter.
- Other minor UI enhancements across the app.

### What's changed

- The delete button for suggestions is no longer shown in the expanded search bar.

### What's fixed

- Fixed `UIndex` provider due to recent changes to their website.
- Fixed an issue that caused `SubsPlease` provider to return empty results.

### Search providers that support file downloading

- AniRena
- AnimeTosho
- MyPornClub
- Nyaa
- Sukebei
- TheRarBg
- XXXClub
- XXXTracker
- External Torznab search providers, if they are able to return file link

# v0.4.8

### What's new

- Added a crash handler for application.
  ___When the app crashes, a new screen is shown with a button to export logs and restart
  application.___
- Added an option in the settings screen to export Torrent Search logs to file.
- Added support to manually check Torznab search provider connection.
- Added support to view failed search providers in the search screen.
- Added category filter chips in the search providers screen.

### What's changed

- Search results having a different category than the selected one are now hidden.

  ___This applies only when you use a category other than `All`.___

- Renamed `dmhy` search provider to `Dmhy`.

### What's fixed

- Fixed a regression which was introduced in [v0.4.7](#v047).
- Fixed an issue which caused the application to crash when trying to sort results by file size.
- When refreshing search results, ongoing search is now correctly canceled before performing a new
  search preventing unnecessary resources usage.
- Torznab search provider now returns search results with a correct category instead of using `All`.
- Fixed Torznab implementation to recover crashes when parsing search provider capabilities.
- Fixed an issue which caused Torznab search provider to return empty results.
- The default category is now used when triggering a search by sharing a text from other apps to
  Torrent Search as well as triggering search from the search history screen.
- Search providers are now shown in correct alphabetical order.

# v0.4.7

> [!IMPORTANT]
>
> An issue has been found in this release. Please download
> new [v0.4.8](https://github.com/prajwalch/TorrentSearch/releases/tag/v0.4.8) version.

### What's new

- Added a crash handler for application.
  ___When the app crashes, a new screen is shown with a button to export logs and restart
  application.___
- Added an option in the settings screen to export Torrent Search logs to file.
- Added support to manually check Torznab search provider connection.
- Added support to view failed search providers in the search screen.
- Added category filter chips in the search providers screen.

### What's changed

- Search results having a different category than the selected one are now hidden.

  ___This applies only when you use a category other than `All`.___

- Renamed `dmhy` search provider to `Dmhy`.

### What's fixed

- Fixed an issue which caused the application to crash when trying to sort results by file size.
- When refreshing search results, ongoing search is now correctly canceled before performing a new
  search preventing unnecessary resources usage.
- Torznab search provider now returns search results with a correct category instead of using `All`.
- Fixed Torznab implementation to recover crashes when parsing search provider capabilities.
- Fixed an issue which caused Torznab search provider to return empty results.
- The default category is now used when triggering a search by sharing a text from other apps to
  Torrent Search as well as triggering search from the search history screen.
- Search providers are now shown in correct alphabetical order.

# v0.4.6

Happy new year everyone 🥳

### What's new

- Any changes to bookmarks sort options is now remembered across app restarts.
- Predictive back gesture is now enabled.
- Added support to import and export bookmarks.
- Other minor UI enhancements.

#### New languages

- Added Turkish language by [@isabeyit79](https://github.com/isabeyit79)
- Added Bengali language by [@raselkee](https://github.com/raselkee)

### New search providers

- [SubsPlease](https://subsplease.org/) (disabled by default)
- [InternetArchive](https://archive.org/) (disabled by default)
- [XXXTracker](https://xxxtor.com/) (disabled by default)
- [TorrentDatabase](https://developify.ca/) (disabled by default)
- [AniRena](https://anirena.com/) (disabled by default)
- [TorrentDownload](https://torrentdownload.info/) (disabled by default)
- [FileMood](https://filemood.com/) (disabled by default)
- [dmhy](https://share.dmhy.org/) (disabled by default)

### What's changed

- `Safe status` field/option is now removed from Torznab config.

### What's fixed

- Fixed torrent upload date formatting for a bunch of search providers.
- Dialogs such as `Torrent client not found`, `Delete all bookmarks?`, dropdown menus such as sort
  menu, dark theme setting menu and other UI elements are now fixed to remain open when device is
  rotated or when changing theme from system setting.
- `TokyoToshokan` search provider invalid URL is now fixed.

# v0.4.4

### What's new

- The home screen search bar now filters search histories as you start to type.
- Added support for deleting bookmarks using right-to-left swipe gesture.
- Added support for triggering search from the search history screen.
- Added support for copying query from the search history screen by long pressing the list item.
- Added a delete all confirmation step for bookmarks and search history screen to prevent any
  accidental deletions.
- Added a pull-to-refresh support for search results. Please note that when you do the refresh,
  filter query and sort options will not be affected only the search results will update.
- Added a button to retry when no results are found.
- "Bookmarked" message is now displayed when a torrent is bookmarked.
- Added a separate `?` button to view the reason behind flagging `Unsafe` to the search provider.
  In previous versions, the `Unsafe` badge used to be clickable which is now changed to unclickable
  and instead a separate button is displayed.
- Added summary (supporting text) for `Enable dynamic theme`, `Pure black` and,
  `Save search history` setting.
- Other minor UI polish and enhancements.

#### New search providers

- [`BitSearch`](https://bitsearch.to) (disabled by default)

#### New languages

- Added Russian language by [@Frumkin13](https://github.com/Frumkin13)
- Added Russian language for F-Droid by [@Frumkin13](https://github.com/Frumkin13)
- Added Chinese (Simplified Han script) language by [@YuxuanQin](https://github.com/YuxuanQin)
- Added Persian language by [@milad19s](https://github.com/milad19s)
- Added Spanish language by [@acr994](https://github.com/acr994)
- Added Arabic language by [@NENO756](https://github.com/NENO756)
- Added German language by [@feuersternX](https://github.com/feuersternX)

### What's changed

- The search history icon on the home screen is now displayed only when the `Save search history`
  setting is enabled.
- `Pure black` setting is now always displayed. In previous versions, it used to be shown only on
  dark theme.
- Search provider filter chips are now displayed in a multi-row instead of using a single row.
- Search provider filter chips will now be disabled until search completes.
- Search provider filter chips will now be available only when more than one search provider is
  used.
- Increased `Knaben` search provider results limit from `50` to `300`.
- Updated `Yts` search provider URL to [https://yts.lt](https://yts.lt).
- Increased `Yts` search provider results limit from `20` to `50`.

### what's fixed

- Search screen "Search results" text field now correctly places the cursor at the end when you hide
  and re-open it.
- Fixed `Knaben` search provider for returning empty results.
- A proper error message is now displayed when empty text is shared to TorrentSearch.

# v0.4.3

### What's new

- Revamped UI
- New search results screen with actions to search, sort and filter search results
- Added support to search bookmarks
- Added an option to enable or disable share integration which was added
  on [v0.4.2](https://github.com/prajwalch/TorrentSearch/releases/tag/v0.4.2)
    - When enabled, TorrentSearch will be visible on application chooser when you select text and
      hit share.
- Added an option to enable or disable quick search which was added
  on [v0.4.2](https://github.com/prajwalch/TorrentSearch/releases/tag/v0.4.2)
    - When enabled, TorrentSearch will be visible on text selection menu (like Copy or Share).
- Added Ukrainian translation by [@nykula](https://github.com/nykula)
- Added Ukrainian translation for F-Droid metadata by [@nykula](https://github.com/nykula)
- Added Persian (Old) translation by [@milad19s](https://github.com/milad19s)
- Added Polish translation by [@kojakowski](https://github.com/kojakowski)

### What's changed

- `Hide results with zero seeders` setting is now a filter option with a name `Dead torrents` which
  can show or hide dead torrents

### What's fixed

- Fixed a search bar autofocus issue on older Android versions (7.1 - 8.1)
- Fixed crash on Android 7
- Fixed an issue where the keyboard used to obscure the content like torrent list and scroll to top
  button

# v0.4.2

### What's new

- Improved search speed
- Added support
  for [Torznab API](https://torznab.github.io/spec-1.3-draft/torznab/Specification-v1.3.html) (
  read [wiki](https://github.com/prajwalch/TorrentSearch/wiki) to learn more)
- Search can now be triggered by selecting text and choosing or sharing with TorrentSearch
- Recent or ongoing search can now be cleared or canceled using back button/gesture
- Added an option to change the app language (Android 13+ only)
- Revamp torrent list item UI
- Revamp torrent client not found dialog UI
- Improved internet connectivity check using Android-provided API
- Some other minor UI improvements

#### New languages

- Added Portuguese (Brazil) translation by [@Cauã](https://github.com/Cauã)
- Added Malayalam translation by [@leywino](https://github.com/leywino)

### What's changed

- `AnimeTosho` is now enabled by default
- `Hide results with zero seeders` now applies only before search. For example: if enabled, then
  disabled after a search, it won't take effect until a new search is performed
- Disabling a search provider no longer hides results already fetched from that provider

### What's fixed

- Fixed an issue where NSFW and unsafe search providers weren't disabled when `Enable NSFW mode` was
  turned off while only they were enabled

# v0.4.1

This is a maintenance release.

> [!NOTE]
>
> If you have `v0.4.0` installed, you don't need to uninstall previous version just download this
> newer version and update it.

Please refer to [v0.4.0](#v040) for release
note.

Downgraded agp to `v8.11.1` from `v8.12.0` for F-Droid inclusion.

# v0.4.0

> [!WARNING]
> **Breaking changes:** Please remove the previous version before installing this one.

### What's new

- Splash screen background now follows the system setting
- Added a button to delete all bookmarks on the bookmarks screen
- `Default category` setting now has a dedicated screen
- `Search providers` setting now has a dedicated screen
    - Search provider URL is now displayed
        - Click the URL to open it
    - Search provider category is now displayed
    - Search provider safety status (only `Unsafe`) is now displayed
        - Click the badge to see the reason for the unsafe status
    - Added a button to enable all search providers
    - Added a button to disable all search providers
    - Added a button to reset search providers setting to default
- `NSFW` and `Unsafe` search providers are now automatically disabled when the `Enable NSFW mode`
  setting is turned off
- Added option to change default sort criteria and order
    - Only applies to search screen
- Added option to show/hide search history in search bar
- Added new search history screen for managing search history
- Added new `About` section on the settings screen
- Minor UI improvements on both search and settings screen

#### New search providers

- [Sukebei](https://sukebai.nyaa.si) (disabled by default)
- [TokyoToshokan](https://tokyotosho.info) (enabled by default)
- [TorrentDownloads](https://torrentdownloads.pro) (enabled by default)
- [UIndex](https://uindex.org) (enabled by default)
- [XXXClub](https://xxxclub.to) (disabled by default)

### What's changed

- Removed use of `clients3.google.com` for checking internet connection
- Moved `Default category` setting to `Search` section
- Replaced `Clear search history` setting with `Manage search history`
- Renamed `Pause search history` setting to `Save search history`

### What's fixed

- Fixed an issue where scroll-to-top button remained visible during search
- Fixed an issue causing the app to enter a corrupted state when going back to home screen using
  back button or gesture

# v0.3.0

> [!IMPORTANT]
>
> This version introduces changes to how settings are stored; therefore, it is recommended to
> completely uninstall the previous version before installing this one.

### What's new

- New app icon
- Added search history
- Added sort functionality
- Added support to bookmark torrent
- Added scroll to top button
- Added scrollbar
- Results count is now displayed
- Added pure black (AMOLED) theme support
- Added setting to change default category
- Added setting to hide results with zero seeders
- Added setting to set maximum number of results to be shown
- Added setting to pause search history
- Added setting to clear search history
- Minor UI enhancements for both the search and settings screen

### What's changed

- Renamed `Enable NSFW search` setting to `Enable NSFW mode` and moved to `General` section

### What's fixed

- Fixed an issue where the application chooser, when sharing a magnet link or description page URL,
  failed to display all applications and users from platforms like Facebook Messenger, WhatsApp,
  etc.
- Fixed an issue where rapidly changing the category during a search would display mismatched
  results

# v0.2.0

### What's new

- Improved search bar UI
- Added new placeholder text for empty search: `Nothing here yet...Start searching...`
    - Shown on first launch
    - Shown when all providers are disabled after the search
- Added new `Other` category (marked as NSFW)
- Added support to copy/share magnet link
- Added support to open torrent description page in default browser
- Added support to copy/share torrent description page URL
- Category information is now displayed in the search results list
- NSFW information is now displayed in the search results list

#### New settings screen

- Added option to enable/disable dynamic theme
- Added option to choose theme mode
- Added option to enable/disable NSFW search:
    - Automatically hides categories marked as NSFW
    - Automatically hides the torrents of categories marked as NSFW
    - Automatically hides the torrents without a category (`TorrentsCsv` does not return a category,
      so its results will be hidden)
- Added option to enable/disable individual providers

#### Torrent client not found dialog

- Improved UI
- Removed `Flud` link (Non-FOSS app)
- `LibreTorrent` is now the recommended client
- Added more FOSS torrent clients to choose from:
    - `Aria2App`
    - `Gopeed`
    - `PikaTorrent`
    - `TorrServe`

#### New search providers

- [AnimeTosho](https://animetosho.org) (disabled by default)
- [Knaben](https://knaben.org) (enabled by default)
- [LimeTorrents](https://limetorrents.lol) (disabled by default)
- [MyPornClub](https://myporn.club) (disabled by default)

### What's changed

- `Porn` category is now marked NSFW
- `Eztv` is now enabled by default
- `NyaaSi` is now enabled by default
- `ThePirateBay` is now disabled by default
- `TheRarBg` is now disabled by default
- `TorrentsCsv` is now enabled by default
- `Yts` is now enabled by default
- Search results now shows provider name in simple form instead of url

### What's fixed

- Fixed `Nyaa` search results by explicitly setting the category to `Anime` to avoid mixed
  categories

### Internal

- Upgraded `agp (Android Gradle Plugin)` to `v8.11.1`

Big thanks to [@IzzySoft](https://github.com/IzzySoft), [@vdbhb59](https://github.com/vdbhb59),
[@leywino](https://github.com/leywino), and [@shuvashish76](https://github.com/shuvashish76) for
contribution, guides, tips and recommendation.

# v0.1.3

Added [nyaa.si](https://nyaa.si) provider.

# v0.1.2

Fixed UI freeze during search.

# v0.1.1

Fixed app crash due to unhandled network related exceptions.

# v0.1.0

- Added search with category filtering
- Added support to download torrent (external torrent client required)
