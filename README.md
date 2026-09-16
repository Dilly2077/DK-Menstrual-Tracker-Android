# Lunara Android

Lunara is the current placeholder brand for this privacy-first Android menstrual-cycle tracker. V0.2 keeps the existing `com.dkcycle.app` application ID so V0.1 installations can be upgraded without losing locally stored cycle data.

## V0.2 features

- Log bleeding intensity, symptoms, mood, pain and notes by date.
- Calendar view for logged periods, predicted periods and an estimated fertile window.
- Personalised cycle-length estimate weighted toward recent cycles.
- Prediction confidence derived from the amount and variability of logged cycle history.
- Estimated ovulation/fertile dates with explicit limitations.
- Cycle-day and likely-phase explanation with the main reproductive hormones involved.
- Insights for average cycle length, average period length, variability and common symptoms.
- Built-in cycle physiology guide.
- Local JSON export and restore, with backward-compatible DKCycle V0.1 import.
- Proper adaptive launcher icon including Android 13+ monochrome/themed icon support.
- First-launch and Settings options to request a pinned Home-screen shortcut.
- Offline Partner Pass sharing: a user can explicitly share a read-only snapshot containing bleeding dates needed for cycle/calendar estimates. Symptoms, mood, pain, temperature and notes are excluded.
- Partner Pass import provides a read-only shared overview and calendar. It is a snapshot rather than live server sync.
- No Android `INTERNET` permission, account, ads or analytics in this build.

## Partner Pass privacy model

Partner Pass deliberately avoids a backend. The code is created locally and only leaves Lunara when the user chooses an Android share destination. It includes up to 24 months of bleeding/spotting dates and flow intensity, but excludes symptoms, mood, pain, temperature and notes. Because it is a static offline snapshot, Lunara cannot remotely revoke a pass after it has been sent; the recipient can clear it locally, and refreshed data requires a newly shared pass.

A future live partner-sync version would require an opt-in encrypted relay/account architecture and should be treated as a separate privacy/security feature rather than silently adding network access.

## Build on GitHub

Every push to `main` and every manual workflow run executes `.github/workflows/android-apk.yml`.

Open **Actions → Build Lunara APK → Run workflow**. After the build completes, download the `Lunara-v0.2-debug-apk` artifact. It contains `app-debug.apk`.

## Tooling

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

## Medical limitation

Lunara is a tracking and educational tool. Date-derived predictions are estimates. It does not measure hormone concentrations, confirm ovulation, diagnose a condition, provide contraception, or replace pregnancy testing or clinical assessment.
