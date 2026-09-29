# Permissions

Hush asks for nothing at install time. Each special permission is requested from the screen
that needs it, with a card that says what is read, what it enables and what stops working
without it. `Einstellungen > Berechtigungen` lists all of them with their current state.

Hush uses the network for exactly one thing: the optional update check against
`https://api.github.com/repos/nzrbits/hush/releases/latest`, off by default. Nothing else
leaves the device.

| Permission | Where it is granted | Used for | Without it |
|---|---|---|---|
| Home role (default launcher) | RoleManager dialog (Android 10+) or system home settings | Home button opens Hush | The previous launcher stays active; Hush still works as a normal app |
| `PACKAGE_USAGE_STATS` (Usage Access) | System settings, opened from Screen Time / Limits | Screen time today and 7 days, per-app time, usage limit reminders | No screen time, no limit reminders |
| Notification listener (Notification Access) | System settings, opened from the notification log | Notification filter: read title and text of incoming notifications, store matches locally, cancel them | No notification filter |
| Accessibility service | System settings, opened from Block / Short video / Permissions | Close blocked apps, leave short-video surfaces, lock screen and open shade by gesture | Blocks only show inside Hush; short video blocking and the two gestures do nothing |
| `POST_NOTIFICATIONS` (Android 13+) | Runtime prompt from the system app settings | Hush's own usage limit reminders | Limits are reached silently |
| `RECEIVE_BOOT_COMPLETED` | Normal permission | Re-enqueue the periodic worker after reboot | Worker resumes on next app start |
| `REQUEST_DELETE_PACKAGES` | Normal permission | "Deinstallieren" opens the system uninstall dialog | Only via system settings |
| `READ_CALENDAR` | Runtime prompt when Mr. Nook is tapped | Mr. Nook reads the next event of Google-account calendars (title, start) on the home screen; opens it in the calendar app on tap | Mr. Nook only offers to ask for it |
| `INTERNET` | Normal permission | Optional update check and APK download from GitHub releases, only while "Automatisch nach Updates suchen" is on | No update check; install releases by hand |
| `REQUEST_INSTALL_PACKAGES` ("install unknown apps") | System settings, opened from the update line | Hand the downloaded APK to PackageInstaller | Update line opens the APK for manual install |
| `QUERY_ALL_PACKAGES` | **not used** | | |

Package visibility is declared with `<queries>` intent filters for `MAIN/LAUNCHER`, `MAIN/HOME`,
camera, dialer and alarm intents (`core/system/src/main/AndroidManifest.xml`).

## Accessibility service, in detail

Service: `com.nzrbits.hush.feature.wellbeing.service.HushAccessibilityService`
Config: `feature/wellbeing/src/main/res/xml/hush_accessibility_config.xml`

Events: `TYPE_WINDOW_STATE_CHANGED` and `TYPE_WINDOW_CONTENT_CHANGED`. The XML config lists
the four short video packages; at runtime the service widens the package filter so app
blocking works for every app.

What it does, each gated by a user setting:

1. **App blocking.** On a window state change it looks up the foreground package in the
   block table and the enabled schedules. If blocked, it performs `GLOBAL_ACTION_HOME` and
   reports the event so the home screen shows why. Debounced to one action per 1.5 s.
2. **Usage limits.** On a window state change it runs the limit check for that package.
3. **Short video blocking.** For YouTube, Instagram, Facebook and Snapchat only, it walks the
   current window (max 600 nodes, depth 24), collects view ids and texts, and presses
   `GLOBAL_ACTION_BACK` when a Shorts / Reels / Spotlight marker is found. Debounced to one
   action per 1.2 s. Nothing from the tree is stored.
4. **Gestures.** Lock screen (`GLOBAL_ACTION_LOCK_SCREEN`, Android 9+) and notification shade
   (`GLOBAL_ACTION_NOTIFICATIONS`) through `AccessibilityBridge`.

Play policy: the accessibility description states all four uses in plain language. If the
app is ever published, the Play Console "AccessibilityService" declaration must repeat them.

Known behaviour: Android disables an app's accessibility service when the app is force
stopped (from settings or `adb shell am force-stop`). The user must switch it on again. The
Permissions screen shows this.

## Notification listener, in detail

Service: `com.nzrbits.hush.feature.notifications.service.HushNotificationListener`

Skipped and never touched: ongoing notifications, foreground service notifications, group
summaries, and Hush's own notifications. Matches are stored with app label, title, text and
time in the local Room database, kept for 30 days, and cancelled with `cancelNotification`.

Android limitation: the listener is called after the notification was posted, so a sound or
vibration may already have played. The UI says so.

## Protected packages

Hush never blocks, schedules or filters these, whatever the user picks
(`core/system/.../ProtectedPackages.kt`): Hush itself, `android`, system UI, Settings, the
in-call and telecom packages, the default dialer, the default SMS app, the alarm clock app,
the package installer and permission controller, and every installed keyboard. The context
menu shows "App blockieren" greyed out with the reason, the schedule picker hides them, and
the accessibility service ignores them even if a row slipped into the database.

Why: blocking the dialer would throw an incoming call to the home screen, blocking Settings
would remove the only way to switch the accessibility service off, blocking the clock would
hide the alarm dismiss screen.

## Backup

`android:allowBackup="false"`. Captured notification text and the rest of the local
database never go to a cloud backup.

## In-app updates

`feature/updates`. A WorkManager job every 6 hours (network required) and a throttled check
on app start, both no-ops unless "Automatisch nach Updates suchen" is on. The APK is
downloaded to the cache directory and committed through a `PackageInstaller` session with
`USER_ACTION_NOT_REQUIRED`. Android 12+ then installs without a dialog when Hush is the
installer of record of itself and the signing key matches; the first update after a manual
install still shows the system dialog. `UpdateReceiver` handles `PENDING_USER_ACTION`,
success and failure. "Automatisch installieren" lets the worker do the whole chain.

## Device admin

Not used. Screen lock goes through the accessibility global action instead, which needs no
device admin and cannot wipe the device.
