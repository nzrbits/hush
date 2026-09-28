# Build

## Requirements

| Tool | Version used | Notes |
|---|---|---|
| JDK | 17 (Homebrew `openjdk@17`) | JDK 21+ works too; 26 is not supported by Gradle 8.14 |
| Android SDK | platform 35, build-tools 35.0.0 | `sdkmanager "platforms;android-35" "build-tools;35.0.0"` |
| Gradle | 8.14.2 via wrapper | `./gradlew`, no local Gradle needed |
| AGP | 8.10.1 | see `gradle/libs.versions.toml` |
| Kotlin | 2.1.21 | Compose compiler plugin bundled |

`local.properties` must point to the SDK:

```
sdk.dir=/opt/homebrew/share/android-commandlinetools
```

## Commands

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@17

# Debug APK
./gradlew :app:assembleDebug
# -> app/build/outputs/apk/debug/app-debug.apk

# Unit tests (all modules, JVM + Robolectric)
./gradlew test

# Release APK (unsigned, minified)
./gradlew :app:assembleRelease
```

The first build downloads about 600 MB of dependencies and takes several minutes.
Later builds take one to two minutes.

## Install on a device or emulator

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
# optional: make it the home app without touching the UI
adb shell cmd package set-home-activity com.nzrbits.hush.debug/com.nzrbits.hush.MainActivity
```

The debug build uses the application id `com.nzrbits.hush.debug`, so it installs next to a
release build.

## Emulator used for verification

```bash
sdkmanager "system-images;android-35;google_apis;arm64-v8a" "emulator"
avdmanager create avd -n hush35 -k "system-images;android-35;google_apis;arm64-v8a" -d pixel_6
emulator -avd hush35 -no-window -no-audio -no-boot-anim -gpu swiftshader_indirect
```

Granting the special permissions from the shell, useful for tests:

```bash
P=com.nzrbits.hush.debug
adb shell appops set $P android:get_usage_stats allow
adb shell cmd notification allow_listener $P/com.nzrbits.hush.feature.notifications.service.HushNotificationListener
adb shell settings put secure enabled_accessibility_services $P/com.nzrbits.hush.feature.wellbeing.service.HushAccessibilityService
adb shell settings put secure accessibility_enabled 1
adb shell pm grant $P android.permission.POST_NOTIFICATIONS
```

Note: `adb shell am force-stop` disables the accessibility service, as Android does for any
force-stopped app. Re-run the `settings put` line afterwards.

## Renaming the product

The name lives in two places: `HushConfig.APP_NAME` (`core/common`) and `app_name` in
`app/src/main/res/values/strings.xml`. Database and preference files use
`HushConfig.STORAGE_NAMESPACE`, so a rename does not migrate user data. The Gradle module
names and the package `com.nzrbits.hush` are not user visible.
