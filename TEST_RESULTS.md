# Test results

Date: 2026-09-28. Branch `main`. Build `0.1.1-debug`.

## Review round (0.1.0 -> 0.1.1)

Three independent read-only reviews (crash/lifecycle on real phones, system services and
blocking logic, launcher UX) produced 29 findings. All were fixed except the two marked open.

| # | Finding | Fix |
|---|---|---|
| 1 | RoleManager default-launcher dialog closed at once on Android 10+ (started with `startActivity`, no calling package) | Activity result launcher in onboarding, settings and permissions; verified: dialog shows and sets Hush |
| 2 | Onboarding and Minimal theme flashed on every cold start; NavHost rebuilt its graph when settings loaded | Settings are null until DataStore answered; start destination decided once; verified: home visible at 600 ms, no flash |
| 3 | App list reloaded on the main thread on every package event | Callbacks only signal; reload on IO via `mapLatest` |
| 4 | Wallpaper decoded full size on the main thread; large photos crash at draw | Decoded once on IO with `inSampleSize` to screen size, 55 % scrim |
| 5 | Back on home finished the launcher on Android 8 to 11 | `BackHandler` on home |
| 6 | Corrupt DataStore crashed the home app in a loop | `ReplaceFileCorruptionHandler` + IOException fallback |
| 7 | `stateNotNeeded` dropped instance state | Removed |
| 8 | Dialer, Settings, Clock, keyboards could be blocked (lock-out, killed calls) | `ProtectedPackages` denylist in repository, service, context menu and schedule picker; verified |
| 9 | 1.5 s debounce skipped enforcement when a blocked app was reopened quickly | Always go home; only the notice is debounced; `@Volatile` fields |
| 10 | Schedule starting while the app was open was never enforced | Service re-evaluates the foreground app on every block state change; verified with Gmail |
| 11 | Short video walk ran unthrottled and concurrently on every content change | Settings cached in a StateFlow, 500 ms throttle, mutex, node reads guarded |
| 12 | Media, call, alarm and system notifications could be cancelled; log before cancel | Category and media-session checks, protected packages skipped, cancel before capture |
| 13 | Captured notification text went to cloud auto-backup | `allowBackup="false"` |
| 14 | Unhandled exceptions in service coroutines killed the process | `CoroutineExceptionHandler` in both services |
| 15 | Reminder marked as sent although the notification could not be shown | Mark only after a successful `notify` |
| 16 | "Neu setzen" stacked blocks; "Aufheben" left the app blocked | Active blocks of the package are replaced |
| 17 | Full-day usage scan per limited package every 15 min; non-Gboard keyboards counted as apps | One query per worker run; IME packages from `InputMethodManager` |
| 18 | Drawer and context menu launched blocked apps; no "gesperrt" marker in the drawer | Same refuse-and-report path everywhere; marker added; verified |
| 19 | Favourites overflowed off screen with many entries or large fonts | Favourites column scrolls only when it overflows |
| 20 | Gesture hint had no way forward | Hint is tappable and opens the permissions screen |
| 21 | Keyboard auto-open ignored the setting on first open | Setting is null until loaded |
| 22 | Cancelling the wallpaper picker deleted the wallpaper | Null result is a no-op |
| 23 | Blocked notice never expired | 20 s, cleared automatically; verified |
| 24 | Weekday buttons in the schedule editor showed no selected state; start == end unexplained | Same ●/○ marker as the rule editor; "ganzer Tag" caption |
| 25 | Copy said Mr. Nook only talks in Cozy Mode, but he talks in settings in both modes (as wanted) | Copy corrected |
| 26 | 12 h clock had no AM/PM; charging line lagged a minute; FULL shown as "Lädt" | `h:mm a`; live battery flow; "Voll" |
| 27 | "Deinstallieren" offered for system apps | Hidden for system apps |
| 28 | Drawer showed "Keine Apps gefunden" for a moment on every open | Empty state gated on first load |
| 29 | Facebook short video markers were a placeholder | Only a selected Reels tab counts |

Open, not fixed: favourites reorder by drag (buttons only), English strings.

## Design review round (0.1.1 -> 0.1.2)

Two independent design reviews, one per mode, against sourced criteria (NN/g heuristic 8 and
visual hierarchy, Material spacing and 48 dp targets, Android system bars and insets, WCAG
1.4.3 and 1.4.11, NN/g dark mode, JUX 2025 rounded-aesthetic warmth, Velasco et al. 2015
typeface roundness, colour-temperature research). Full screenshot sets in
`docs/screenshots/review-minimal/` and `review-cozy/`; `v2-*` files show the result.

Verdict on "cut off at top and bottom": no layout overlap. `statusBarsPadding` and
`navigationBarsPadding` are applied on every screen; the emulator did not render status bar
icons, so the inset read as an empty band. Later captures use SystemUI demo mode.

Applied (30 findings, deduplicated):

| Area | Change |
|---|---|
| Cozy typography | Nunito was never active: the font default was "System". New default "Wie Modus" resolves to Nunito in Cozy, system sans in Minimal |
| Weekday toggles | "●M / o" wrapping replaced by seven 40 dp circles with 4 dp gap, accent fill when selected; the duplicate Mo–Fr / Wochenende / Täglich chips removed |
| Slider | 24 dp disc thumb, visible inactive track, no stop indicator |
| Component outlines | New `lineStrong` role (3:1) for text fields, unselected chips, switch borders and secondary buttons; `line` stays for dividers |
| Switches | One `hushSwitchColors()` incl. disabled states; no Material grey leaks |
| Material roles | secondaryContainer, tertiaryContainer, surfaceTint mapped to warm tokens (TimePicker) |
| Minimal text | Running text #EDEDED, clock stays #FFFFFF (`emphasis`) |
| Type scale | Line height 1.45; date and bubble 16 sp; home uses 64 / 22 / 16; screen-time hero 40 sp |
| Left edge | 24 dp everywhere (screens, home, drawer, app rows) |
| Home | Date and charging on one line ("· Lädt 80 %", "Voll" at 100 %); quick actions 48 dp tall, 24 dp apart; wellbeing link uses the same style; bottom padding 32 dp |
| Package names | Never shown: context sheet shows "Eigentlich: <name>" only when renamed, platform rows lost their subtitles, missing apps read "<Name> (nicht installiert)" |
| Primary buttons | "Neu" moved into the title row on schedules, limits and rules; white block only for terminal actions |
| Block screen | 7-day bars only when a day has at least a minute; bars rounded (Cozy) with a baseline |
| Short video | Platform rows appear only after the master switch is on |
| Context sheet | Insets before padding, scrollable, 24 dp bottom clearance |
| Scene | Light Cozy particles at 60 % alpha; densities 10 / 18 / 30 |
| Mascot | 84 dp on home, 64 dp elsewhere, 12 dp above; tail drawn as a path without a seam |
| Drawer | Letter index 44 dp wide |
| Settings root | Same labels and subtitles as the wellbeing hub |

Not applied, by decision: Mr. Nook stays in settings in Minimal Mode (wanted); the six
wellbeing rows stay in the settings root (brief). Open: Cozy Light still shows a black
frame for the first frames of a cold start because the window background is black.

## Build

```
./gradlew :app:assembleDebug    -> BUILD SUCCESSFUL
./gradlew :app:assembleRelease  -> BUILD SUCCESSFUL (unsigned, minified)
./gradlew test                  -> BUILD SUCCESSFUL, 57 tests, 0 failures
```

APK: `app/build/outputs/apk/debug/app-debug.apk` (about 13 MB)

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
| 0.1.1: cold start shows the finished home screen at 600 ms, no onboarding or theme flash | Pass | `24-coldstart-600ms.png` |
| 0.1.1: "Telefon" context menu shows "App blockieren" disabled with reason | Pass | dump |
| 0.1.1: blocked Chrome shows "gesperrt" in the drawer; tap from drawer and from home is refused with notice | Pass | dump |
| 0.1.1: blocked notice disappears after 20 s | Pass | dump count 0 after 22 s |
| 0.1.1: schedule starting while Gmail is open closes Gmail within 8 s of the minute | Pass | `topResumedActivity` Hush at 22:51:08, schedule start 22:51:00 |
| 0.1.1: RoleManager dialog "Set Hush as your default home app?" opens from settings and sets Hush | Pass | `25-role-dialog.png`, home = Hush afterwards |
| 0.1.1: test notification still captured and removed | Pass | `dumpsys notification` count 0 |
| No crash during the whole run | Pass | `logcat | grep -c "FATAL EXCEPTION"` = 0 after the fix |

## Bugs found and fixed during verification

1. `LauncherApps.registerCallback` was called on the IO dispatcher and crashed the process
   ("Can't create handler inside thread"). Fixed by passing a main-looper `Handler`.
2. Accessibility state was read from `Settings.Secure` only, which returned null on the
   emulator after re-enabling. Now `AccessibilityManager.getEnabledAccessibilityServiceList`
   is checked first.

## Not verified

- Anything on a physical phone: OEM launchers (Samsung, Xiaomi), Android 8 to 14, real
  keyboards, real notifications with sound.
- Short video detection against the live YouTube / Instagram / Facebook / Snapchat apps
  (YouTube did not open on the emulator). Detector logic is unit tested.
- Usage limit notification end to end (needs 15+ minutes of real usage). Decision logic
  is unit tested.
- Real work profile, real reboot (only process death), package install during runtime,
  wallpaper picker, folder UI, favourites reorder, Cozy dark palette on screen.
- Battery impact over days. Design: no foreground service, scene capped at 30 fps and
  paused when not resumed, one 15-minute worker.
