# Kotlin Multiplatform Migration Plan

Tracks the incremental move towards a shared KMP module, per
[issue #105](https://github.com/prajwalch/TorrentSearch/issues/105).

## Checklist

- [x] Introduce `:shared` KMP module scaffold (android + jvm targets)
- [ ] Move domain models (`domain/model`) to `:shared` commonMain
- [ ] Move provider gateway/manager abstractions (`domain/SearchProvidersGateway`,
      `SearchProvidersManager`) to `:shared`, keeping Android-specific
      implementations behind platform APIs
- [ ] Move networking code (`network/NetworkClient`, `Dns`, `DohProviders`,
      `ConnectivityChecker`) to `:shared`, using `expect`/`actual` where
      platform APIs are required
- [ ] Move Torznab client/parsing code (`torznab/`) to `:shared`
- [ ] Move search providers (`providers/`) to `:shared` where portable
- [ ] Add unit tests for migrated code in `:shared` commonTest
- [ ] Re-evaluate scope: Compose Multiplatform UI migration and additional
      targets (desktop/iOS) as follow-on work

## Notes

- Keep Android-only integrations (storage, system share/open magnet,
  clients) behind thin platform APIs (`expect`/`actual`).
- Migrate incrementally, one area at a time, validating build/tests after
  each step.
