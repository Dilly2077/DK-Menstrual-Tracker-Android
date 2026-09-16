# DKCycle V0.1 checkpoint

Updated 16 September 2026.

## Implemented and verified

- Native Kotlin + Jetpack Compose Android app.
- Entire data model stored locally in SharedPreferences as JSON.
- Bleeding, symptoms, mood, pain and notes logging.
- Weighted recent-cycle prediction engine with variability/confidence reporting.
- Today, Calendar, Log, Insights, Learn and Settings screens.
- JSON export/import.
- No Android `INTERNET` permission.
- Unit tests for 28-day prediction and spotting exclusion.
- Stable Android API 36 build configuration.
- GitHub Actions workflow that installs Android API 36, runs unit tests, assembles the debug APK and uploads it as an artifact.
- Verified successful workflow run #6 (ID `35039535301`) on commit `acdc37990ab4fead89f9749bc13b1569fd31bdbb`.
- Verified generated APK: 18,547,563 bytes; SHA-256 `e66873e2435c0b74aa3b9a5fb5686b51c9a0c24098bf255734292e2e28505170`.

## Current dependency baseline

- Android Gradle Plugin 9.4.0
- Gradle 9.6.0
- Kotlin / Compose compiler plugin 2.4.20
- Jetpack Compose 1.11.4
- Material 3 1.4.0
- AndroidX Core KTX 1.17.0
- AndroidX Activity Compose 1.11.0
- `compileSdk` 36
- `targetSdk` 36
- JDK 17

## Next engineering priorities

1. Add a proper adaptive launcher icon and polished onboarding.
2. Add notification/reminder scheduling locally.
3. Add temperature and cervical-mucus fields to the log UI (the model already contains temperature storage).
4. Add cycle-history editing and a clearer period-start correction workflow.
5. Add optional local app lock / biometric gate.
6. Add encrypted-at-rest storage before treating the app as production-ready for sensitive health data.
7. Add accessibility testing and broader unit/UI test coverage.
8. Build a signed release APK/AAB only after signing-key handling is defined.
