# Test results

Date: 2026-09-28. Branch `initial-launcher`. Build `0.1.0-debug`.

## Build

```
./gradlew :app:assembleDebug    -> BUILD SUCCESSFUL
./gradlew :app:assembleRelease  -> BUILD SUCCESSFUL (unsigned, minified)
./gradlew test                  -> BUILD SUCCESSFUL, 57 tests, 0 failures
```

APK: `app/build/outputs/apk/debug/app-debug.apk` (12.9 MB)

## Unit tests (JVM, Robolectric for Room)

| Suite | Module | Tests | Covers |
|---|---|---|---|
| BlockingEngineTest | feature/wellbeing | 10 | manual block window, expiry, per package, schedule weekday/time, disabled, overnight window (evening + next morning), manual wins over schedule, DST end |
| ShortVideoDetectorTest | feature/wellbeing | 6 | strong id, home feed not blocked, selected tab, weak id + text, Facebook needs selected tab, platform lookup |
| LimitDecisionTest | feature/wellbeing | 4 | none, warn once, remind once, remind independent of warn |
| NotificationFilterTest | feature/notifications | 6 | block list, allow list, overnight window, disabled, weekdays, first match wins |
| UsageAggregatorTest | core/system | 6 | pairs, clipped at window start, still open at end, orphan pause, duplicate resume, ordering |
| HushDatabaseTest | core/database | 5 | favourite order + reorder, active block query + purge, folder delete clears membership, log purge + clear, reminder day persisted |
| MappersTest | core/database | 5 | days mask, packages csv, schedule round trip, rule round trip, unknown enum fallback |
| ThemeTest | core/designsystem | 3 | Minimal ignores system dark, Cozy palette choice, duration snapping |
| SearchTest / AppKeyTest / DurationsTest / ClockTest | core/common | 9 | normalisation, ranking, index letter, key round trip, formats, DST day length |
| SayingsTest | feature/cozy | 3 | daypart boundaries, stable per hour, every context non-empty |

## Emulator verification

Device: AVD `hush35`, Pixel 6 profile, Android 15 (API 35), google_apis arm64, headless.
Driven with `adb input`, `uiautomator dump` and `logcat`. Screenshots in `docs/screenshots/`.

| Scenario | Result | Evidence |
|---|---|---|
| Install, appears in "Select a Home app", chosen with Always | Pass | `02-home-*.png`, `topResumedActivity=…MainActivity` |
| Onboarding renders with Mr. Nook and bubble | Pass | `01-onboarding.png` |
| Drawer lists installed apps alphabetically with A–Z index, keyboard auto-open | Pass | `03-drawer.png` |
| Search "gma" returns Gmail | Pass | `05-drawer-search.png` |
| Tap app launches it (Chrome) | Pass | `Displayed com.android.chrome/…FirstRunActivity` |
| Long press opens context menu with all 10 actions | Pass | `04-context-menu.png` |
| Add favourites (Clock, Chrome, Gmail), shown on home | Pass | `06-home-minimal-favorites.png` |
| Rename Calendar, hide Contacts | Pass | `21-drawer-renamed-hidden.png` |
| Block Chrome: slider to 19 h, slide to confirm, favourite shows "gesperrt" | Pass | `07-block-app.png`, `08-home-blocked-fav.png` |
| Launch blocked favourite from home is refused with notice | Pass | `09-blocked-notice.png` |
| Accessibility on: `am start` Chrome is closed by the service, notice shown | Pass | Chrome displayed then Hush resumed within 4 s, `10-enforced-block.png` |
| Usage Access on: hub shows today 4 min with per-app lines; active block listed with "Aufheben" | Pass | `15-wellbeing-hub.png` |
| Process death (`am force-stop`) keeps favourites, block, Cozy Mode | Pass | dump after restart |
| Cozy Mode switch, Mr. Nook with evening saying | Pass | `14-home-cozy-leaves.png` |
| Pixel scene "Fallende Blätter" animates | Pass | `14-home-cozy-leaves.png`, `14b-…png` differ |
| Notification filter: rule (allow list, no apps), test notification captured, removed from shade, in log | Pass | `18-notification-log.png`, `dumpsys notification` count 0 |
| Gesture swipe down opens shade | Pass | `HushBridge: global action 'notifications' -> true` |
| Gesture double tap locks screen | Pass | `HushBridge: global action 'lock' -> true`, `mWakefulness=Asleep` |
| Schedule "Fokuszeit" Mo–Fr 09:00–12:00 with two apps, saved, reopened | Pass | `20-schedules.png` |
| Accessibility state shown correctly in hub after fix | Pass | warning card gone |
| No crash during the whole run | Pass | `logcat | grep -c "FATAL EXCEPTION"` = 0 after the fix |

## Bugs found and fixed during verification

1. `LauncherApps.registerCallback` was called on the IO dispatcher and crashed the process
   ("Can't create handler inside thread"). Fixed by passing a main-looper `Handler`.
2. Accessibility state was read from `Settings.Secure` only, which returned null on the
   emulator after re-enabling. Now `AccessibilityManager.getEnabledAccessibilityServiceList`
   is checked first.

## Not verified

- Short video detection against the live YouTube / Instagram / Facebook / Snapchat apps
  (YouTube did not open on the emulator). Detector logic is unit tested.
- Usage limit notification end to end (needs 15+ minutes of real usage). Decision logic
  is unit tested.
- Real work profile, real reboot (only process death), package install during runtime,
  wallpaper picker, folder UI, favourites reorder, Cozy dark palette on screen.
- Battery impact over days. Design: no foreground service, scene capped at 30 fps and
  paused when not resumed, one 15-minute worker.
