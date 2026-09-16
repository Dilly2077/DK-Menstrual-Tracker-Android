# Lunara V0.2 checkpoint

Prepared 16 September 2026.

## Changes from V0.1

- Visible placeholder brand changed from DKCycle to Lunara while retaining `com.dkcycle.app` and the original `dkcycle` SharedPreferences file for upgrade/data continuity.
- Added a proper adaptive launcher icon with a mauve background, crescent motif and themed-icon support on Android 13+.
- Added an in-app Home-screen pin request on first launch and from Settings. The launcher still requires user approval.
- Added Partner Pass sharing and import. The owner shares only bleeding/spotting dates and flow intensity from the last 24 months; symptoms, mood, pain, temperature and notes are excluded.
- Added a read-only partner overview and calendar.
- Partner Pass remains offline and snapshot-based; there is still no Android `INTERNET` permission or backend.
- Existing DKCycle V0.1 JSON backups remain import-compatible.
- Added Partner Pass privacy tests.
- Bumped app version to 0.2.0 / versionCode 2.

## Important limitation

Partner Pass is not equivalent to Flo's live account pairing. A sent offline snapshot cannot be remotely revoked and does not auto-refresh. Live/revocable partner sync requires an opt-in encrypted network service and should be engineered separately.

## Next priorities

1. Validate the V0.2 clean CI build and generated APK.
2. Consider live end-to-end encrypted partner sync as an optional V0.3 feature.
3. Add notification/reminder scheduling locally.
4. Add temperature and cervical-mucus logging UI.
5. Add cycle-history editing and clearer period-start correction.
6. Add optional biometric/app lock and encrypted-at-rest storage.
7. Broaden accessibility and UI test coverage.
8. Create a signed release APK/AAB after signing-key handling is defined.
