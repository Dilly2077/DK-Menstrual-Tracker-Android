# Lunara V0.3 checkpoint

Updated 16 September 2026.

## Implemented in this pass

- Fixed the confusing blank-day logging state: an empty day is no longer treated as a meaningful log.
- Calendar is now a quick-log surface. Tap a past/today date and use the 🩸 bleeding marker without opening the detailed form.
- Detailed Log remains available for flow level, symptoms, mood, pain and notes.
- Added an optional private 💗 marker, disabled by default and controlled in Settings.
- Added predicted-start indicator (✦) to the calendar.
- Added Compose back handling so opening Detailed Log or Partner and using Android back returns within Lunara instead of immediately exiting.
- Replaced the crescent launcher artwork with a four-petal cycle mark and retained adaptive/themed-icon support.
- Version bumped to 0.3.0 / versionCode 3.
- Development signing remains pinned to the same Lunara development keystore introduced for V0.2, so V0.3+ test builds should update in place from the stable-signed V0.2 build.
- Removed the one-off Partner Pass UI and its requirement for existing logs.

## Partner status

The requested Partner experience is now defined as persistent/live pairing rather than snapshots. A secure backend is still required for two phones to keep an ongoing shared read-only view in sync. Lunara does not fall back to an unencrypted public relay. The intended shared dataset excludes detailed notes, symptoms, mood, pain, temperature and the private heart marker.

## Next engineering priority

1. Connect/provision the secure sync backend and finish live pairing + revocation.
2. Add background sync/retry and visible last-synced state.
3. Add UI/instrumentation tests for calendar quick logging and back navigation.
4. Continue accessibility and release-signing work.
