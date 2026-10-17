# Contributing to Torrent Search

First off, thanks for taking the time to contribute! ❤️

All types of contributions are encouraged and valued. If you do know how to code and want to contribute see below and if you like the project but don't know how to code, you can support the project in other easy ways:

- Star the project
- Help to translate to other languages
- Report bugs
- Suggest new features or enhancements

  
## Setting up the project

### Prerequisites

- **Android Studio**: Install the latest version of Android Studio. You can download it from [here](https://developer.android.com/studio).
- **Git**: Install Git to clone and manage the repository.

### Steps

1. **Fork the repository**
   - Click the `Fork` button on the top right of the repository page to create your own copy.
2. **Clone the forked repository**
   ```shell
   git clone https://github.com/<your-username>/TorrentSearch
   cd TorrentSearch
   ```
3. **Open the project in Android Studio**
   - Open Android Studio and click `Open an Existing Project`.
   - Navigate to the cloned repository and open it.
   - Wait for sync to finish.
4. **Run the app**
   - Connect an Android device or start an emulator.
   - Click the play button on the top right in Android Studio to build and run the app.
  
### Development guidelines

### Build variants

The app is built from a single codebase with a `device` product flavor:

| Variant | Application id | Module path |
| --- | --- | --- |
| `mobileDebug` / `mobileStaging` / `mobileRelease` | `com.prajwalch.torrentsearch[.debug\|.staging]` | `app/src/mobile/` |
| `tvDebug` / `tvStaging` / `tvRelease` | `com.prajwalch.torrentsearch.tv[…]` | `app/src/tv/` |

```sh
./gradlew assembleMobileDebug    # handheld
./gradlew assembleTvDebug        # Android TV / Google TV
```

Everything under `app/src/main/` is shared. The two flavors own their own `AndroidManifest.xml`
(launcher intent filters, hardware feature declarations, banner) and their own UI package.
When adding a screen, decide which flavor it belongs to, and keep non-UI logic in `main` so both
benefit from it.

### Android TV notes

- Use `androidx.tv.material3` components. Do not introduce `TvLazyColumn`/`TvLazyRow` - they were
  deprecated in `tv-foundation` 1.0.0-alpha11 and **removed** in alpha12. Standard
  `LazyColumn`/`LazyRow` have carried the focus behaviour since Compose Foundation 1.7.0.
- `androidx.tv.material3` has no `TextField`. Use `TvTextInput`, which wires up IME attachment
  and the BACK trap. If you need a raw `androidx.compose.material3` primitive, wrap it in
  `TvMaterial3Bridge` or it will render with the library's default light scheme.
- Every interactive element needs a visible focus indicator, and focus must never dead-end.
- `androidx.tv.material3.ColorScheme` has no `surfaceContainer*`/`outline` roles; use
  `tvCardSurface` / `tvElevatedSurface`.
- Verify with a Google TV system image:
  `avdmanager create avd -n tvtest -k "system-images;android-36;google-tv;x86_64" -d tv_1080p`

## Code styles

- Follow [Kotlin Coding Conventions](https://kotlinlang.org/docs/coding-conventions.html)
- Use Android Studio's default formatting (Ctrl+Alt+L).
- Use meaningful name for variable, function, class, object, and so on.
- Use newline to group related code snippet.
- Group releated imports and remove unused ones.

### Commit message style

Use [Conventional Commits](https://www.conventionalcommits.org/) format as much as possible:

```
<type>: <description>
```

Common types: `feat`, `fix`, `refactor`, `chore`.

### Creating pull request

- Once you commit your changes, push your branch to your forked repository.
- Open your forked repository and click `Contribute > Open pull request`.
- Provide a clear title and description for your pull request.
- Reference any related issues (e.g., "Fixes #123").

Thank you for contributing to Torrent Search! 🚀
