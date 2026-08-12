# Kotlin Multiplatform Migration Plan

Tracks the incremental move towards a shared KMP module, per
[issue #105](https://github.com/prajwalch/TorrentSearch/issues/105).

## Checklist

- [x] Introduce `:shared` KMP module scaffold (android + jvm targets)
- [x] AGP 9 split: convert `:app` to KMP Android library; add thin `:androidApp`
      (Application / Activities / Manifest); fold stub `:shared` into `:app`
- [x] Point `:app` `androidMain` at existing `src/main/kotlin` via
      `addStaticSourceDirectory` (resources live in `src/androidMain/res`)
- [ ] Move domain models (`domain/model`) to `:app` commonMain
- [ ] Move provider gateway/manager abstractions (`domain/SearchProvidersGateway`,
      `SearchProvidersManager`) to commonMain, keeping Android-specific
      implementations behind platform APIs
- [ ] Move networking code (`network/NetworkClient`, `Dns`, `DohProviders`,
      `ConnectivityChecker`) to commonMain, using `expect`/`actual` where
      platform APIs are required
- [ ] Move Torznab client/parsing code (`torznab/`) to commonMain
- [ ] Move search providers (`providers/`) to commonMain where portable
- [ ] Add unit tests for migrated code in `:app` commonTest
- [ ] Re-evaluate scope: Compose Multiplatform UI migration and additional
      targets (desktop/iOS) as follow-on work

## Module shape (AGP 9)

| Module | Role |
|--------|------|
| `:androidApp` | `com.android.application` — packaging, Manifest, Application, Activities |
| `:app` | `kotlin.multiplatform` + `com.android.kotlin.multiplatform.library` — UI + domain + data (androidMain today; peel into commonMain over time) |

Do **not** point `commonMain` at the entire `src/main/kotlin` tree — it still
contains Android/`R`/Compose UI code. Peel package-by-package into commonMain.

## Notes

- Keep Android-only integrations (storage, system share/open magnet,
  clients) behind thin platform APIs (`expect`/`actual`).
- Migrate incrementally, one area at a time, validating build/tests after
  each step.
- Library code uses `AppInfo` instead of `BuildConfig` (unsupported by the
  KMP Android library plugin).
