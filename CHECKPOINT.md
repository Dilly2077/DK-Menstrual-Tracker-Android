# Averelle v0.10 checkpoint

Updated 17 September 2026.

## Implemented in this pass

- Fixed Android system status-bar and navigation-bar icon contrast so the time, battery, Wi-Fi and system controls remain visible when Averelle uses a theme that differs from the phone theme.
- Added a fully local appointment-summary PDF export. The PDF summarises tracked cycle dates, bleeding duration, cycle timing, prediction range, symptoms, high-pain days and spotting for sharing with a healthcare professional.
- Free-text notes and the optional private intimacy marker are deliberately excluded from the PDF.
- Added an offline Health library to the lower part of Today, with concise NHS-based information on usual period patterns, heavy periods, period pain and condoms/STI protection.
- Health content is bundled in the app, carries a visible review date and does not require INTERNET permission, analytics or a live content feed.
- Bumped the Android build to versionName 0.10.0 / versionCode 10.
- Kept applicationId `com.dkcycle.app` and the existing development signing identity unchanged for in-place updates.

## Validation target

GitHub Actions must pass unit tests, Android compilation and APK signing-certificate verification before the v0.10 APK is distributed.

## Medical-content boundary

The health library is educational. The PDF is a factual summary of user-entered tracking data and clearly states that it is not a diagnosis or clinical record. Predictions remain estimates and are not intended for contraception or diagnosis.
