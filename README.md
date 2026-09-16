# DKCycle Android

DKCycle is a privacy-first Android menstrual-cycle tracker. V0.1 is designed to work locally without an account, ads, analytics, or network access.

## V0.1 features

- Log bleeding intensity, symptoms, mood, pain and notes by date.
- Calendar view for logged periods, predicted periods and an estimated fertile window.
- Personalised cycle-length estimate weighted toward recent cycles.
- Prediction confidence derived from the amount and variability of logged cycle history.
- Estimated ovulation/fertile dates with explicit limitations.
- Cycle-day and likely-phase explanation with the main reproductive hormones involved.
- Insights for average cycle length, average period length, variability and common symptoms.
- Built-in cycle physiology guide.
- Local JSON export and restore.
- No `INTERNET` permission in the Android manifest.
- GitHub Actions workflow that runs unit tests and produces a downloadable debug APK.

## Prediction model

Period starts are detected from runs of non-spotting bleeding. Up to the six most recent valid cycle lengths are used, with more recent cycles receiving greater weight. The fertile-window display is a calendar estimate based on an estimated ovulation date 14 days before the next predicted period; it does not confirm ovulation and must not be used as contraception.

## Build on GitHub

Every push to `main` and every manual workflow run executes `.github/workflows/android-apk.yml`.

Open **Actions → Build DKCycle APK → Run workflow**. After the build completes, download the `DKCycle-debug-apk` artifact. It contains `app-debug.apk`.

## Local build

Current project tooling:

- Android Gradle Plugin 9.4.0
- Gradle 9.6.0
- Kotlin 2.4.20
- Jetpack Compose BOM 2026.08.00
- `compileSdk` 37
- `targetSdk` 36
- JDK 17

With an Android SDK containing platform 37 installed:

```bash
gradle testDebugUnitTest
gradle assembleDebug
```

The APK is produced at `app/build/outputs/apk/debug/app-debug.apk`.

## Medical limitation

DKCycle is a tracking and educational tool. Date-derived predictions are estimates. It does not measure hormone concentrations, confirm ovulation, diagnose a condition, provide contraception, or replace pregnancy testing or clinical assessment.
