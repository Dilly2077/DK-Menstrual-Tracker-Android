# DKCycle V0.1 checkpoint

Prepared 16 September 2026.

## Implemented

- Native Kotlin + Jetpack Compose Android app.
- Entire data model stored locally in SharedPreferences as JSON.
- Bleeding, symptoms, mood, pain and notes logging.
- Weighted recent-cycle prediction engine with variability/confidence reporting.
- Today, Calendar, Log, Insights, Learn and Settings screens.
- JSON export/import.
- No Android INTERNET permission.
- Unit tests for 28-day prediction and spotting exclusion.
- GitHub Actions workflow that installs Android API 37, tests, builds and uploads a debug APK.

## Next engineering priorities

1. Run the GitHub Actions build and fix any CI/compiler issue revealed by a clean Android environment.
2. Add a proper adaptive launcher icon and polished onboarding.
3. Add notification/reminder scheduling locally.
4. Add temperature and cervical-mucus fields to the log UI (the model already contains temperature storage).
5. Add cycle-history editing and a clearer period-start correction workflow.
6. Add optional local app lock / biometric gate.
7. Add encrypted-at-rest storage before treating the app as production-ready for sensitive health data.
8. Add accessibility testing and broader unit/UI test coverage.
9. Build a signed release APK/AAB only after signing-key handling is defined.
