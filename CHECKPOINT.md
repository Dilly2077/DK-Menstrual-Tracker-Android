# Lunara V0.2 checkpoint

Prepared 16 September 2026.

## Changes from V0.1

- Visible placeholder brand changed from DKCycle to Lunara while retaining `com.dkcycle.app` and the original `dkcycle` SharedPreferences file for data-format continuity.
- Added a proper adaptive launcher icon with a mauve background, crescent motif and themed-icon support on Android 13+.
- Added an in-app Home-screen pin request on first launch and from Settings. The launcher still requires user approval.
- Added Partner Pass sharing and import. The owner shares only bleeding/spotting dates and flow intensity from the last 24 months; symptoms, mood, pain, temperature and notes are excluded.
- Added a read-only partner overview and calendar.
- Partner Pass remains offline and snapshot-based; there is still no Android `INTERNET` permission or backend.
- Existing DKCycle V0.1 JSON backups remain import-compatible.
- Added Partner Pass privacy tests.
- Bumped app version to 0.2.0 / versionCode 2.
- Added a stable development-only signing key so GitHub-built V0.2+ APKs can update one another during sideloaded testing.

## V0.1 migration caveat

V0.1 used GitHub runner-generated debug signing, so its certificate differs from the stable V0.2 development certificate. Android therefore cannot perform an in-place V0.1 → V0.2 update. Export data from V0.1, uninstall DKCycle, install V0.2, then import the backup. V0.2 onward will retain the same development certificate unless deliberately changed.

## Important Partner Pass limitation

Partner Pass is not equivalent to Flo's live account pairing. A sent offline snapshot cannot be remotely revoked and does not auto-refresh. Live/revocable partner sync requires an opt-in encrypted network service and should be engineered separately.

## Next priorities

1. Validate the stable-signed V0.2 clean CI build and certificate.
2. Consider live end-to-end encrypted partner sync as an optional V0.3 feature.
3. Add notification/reminder scheduling locally.
4. Add temperature and cervical-mucus logging UI.
5. Add cycle-history editing and clearer period-start correction.
6. Add optional biometric/app lock and encrypted-at-rest storage.
7. Broaden accessibility and UI test coverage.
8. Create a separate production signing key and signed release APK/AAB when production distribution is ready.
