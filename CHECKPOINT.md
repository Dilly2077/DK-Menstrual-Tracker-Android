# Averelle v0.8 checkpoint

Updated 16 September 2026.

## Implemented in this pass

- Renamed the user-facing app from Lunara to Averelle across the launcher label, Home screen, widgets, Settings, prompts, backup filename and Learn copy.
- Kept applicationId `com.dkcycle.app` and the existing development signing identity unchanged so v0.8 remains an in-place update for existing stable-signed builds.
- Bumped the Android build to versionName 0.8.0 / versionCode 8.
- Replaced the Learn section's raster WebP phase illustrations with resolution-independent Jetpack Compose Canvas artwork.
- Removed the Learn screen's runtime dependency on the old Follicular WebP asset, addressing the Android crash seen when opening the Follicular tab.
- Added distinct scalable illustrations for overview, menstrual, follicular, ovulation and luteal topics.
- Kept the existing cycle prediction engine, themes, widgets, logging, Insights and privacy model unchanged.

## Validation target

GitHub Actions must pass unit tests, Android compilation and APK signing-certificate verification before the v0.8 APK is distributed.

## Compatibility

The package identifier and signing certificate are intentionally unchanged. Existing v0.2+ stable-signed test installations should be able to update directly to v0.8 without uninstalling or losing local app data.
