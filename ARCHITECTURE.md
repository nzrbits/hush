# Architecture

Kotlin, Jetpack Compose, Hilt, Room, DataStore, WorkManager. Clean-ish layering with
feature modules that only know the core modules and a shared route table.

## Modules

```
app                       HOME activity, navigation graph, Application, BootReceiver
core/common               HushConfig, domain models, HushClock, routes, search helpers (pure JVM)
core/designsystem         HushTheme (Minimal / Cozy), typography, components, PixelSceneBackground
core/database             Room: entities, DAOs, mappers, schema export
core/datastore            SettingsRepository (Preferences DataStore)
core/system               Android integrations: LauncherApps, UsageStats, permissions,
                          AccessibilityBridge, SystemActions, HushNotifications
feature/cozy              Mr. Nook sprites, MascotBubble, Sayings
feature/apps              Drawer, context menu sheet, favorites / hidden management
feature/home              Home screen and gestures
feature/wellbeing         Blocking engine, schedules, limits, screen time, short video,
                          HushAccessibilityService, UsageLimitWorker
feature/notifications     NotificationFilter, HushNotificationListener, log and rules UI
feature/settings          All settings screens, onboarding, permissions, FAQ, privacy
```

Dependency direction: `app -> feature/* -> core/*`. Feature modules do not depend on each
other except `home -> apps, cozy, wellbeing` (context menu, mascot, blocked notice),
`wellbeing -> cozy` (mascot on the blocked notice), `notifications -> wellbeing` (shared
app picker dialog) and `settings -> cozy`. Navigation between features goes through string
routes in `HushRoutes` and callback data classes (`HomeNavigation`, `SettingsNavigation`,
`AppActionsNavigation`), so a feature never imports another feature's screen.

## Data flow

```
LauncherApps + UserManager ──▶ InstalledAppsSource.apps (callbackFlow)
                                       │
Room app_customization ────────────────┴──▶ AppsRepository.allApps (shared, sorted)
                                             ├─ visibleApps  (drawer)
                                             └─ favorites    (home)

Room app_blocks + block_schedules + minute ticker ──▶ BlockingRepository.status
                                                     BlockingRepository.reasonFor(pkg)
                                                             ▲
HushAccessibilityService (window state changed) ─────────────┘  ──▶ GLOBAL_ACTION_HOME + reportBlocked
HomeViewModel.launch(app) ───────────────────────────────────┘  ──▶ refuse + reportBlocked

UsageStatsManager events ──▶ UsageAggregator (pure) ──▶ UsageStatsSource.today()/lastDays()
                                                            │
Room usage_limits ──▶ LimitDecision (pure) ◀────────────────┘ ──▶ HushNotifications

NotificationListenerService ──▶ NotificationRepository.evaluate(pkg)
                                 └─ NotificationFilter (pure) ──▶ capture + cancelNotification

Preferences DataStore ──▶ SettingsRepository.settings ──▶ HushTheme, HomeScreen, services
```

Pure decision logic lives in objects without Android imports so it is unit tested directly:
`BlockingEngine`, `ScheduleEvaluator`, `ShortVideoDetector`, `LimitDecision`,
`NotificationFilter`, `UsageAggregator`, `Search`, `Sayings`, `snapDuration`, `resolveColors`.

## Time handling

All domain code takes a `HushClock`. Manual blocks store absolute epoch millis, so a reboot
or a changed system clock never extends or shortens them beyond what the wall clock says.
Schedules store local wall-clock minutes and a weekday mask; `ScheduleEvaluator` resolves
them in the device zone, so DST shifts the window with the clock and an overnight window
counts on the day it started. A minute ticker drives the UI, no service polls.

## Theme system

`HushTheme(appearance)` resolves a `HushColors` role set: `Minimal` (black / white / grey),
`CozyLight` and `CozyDark` (CSS tokens from Mr. Nook and Melinda). Screens only use the roles,
never a mode check, so both modes share every screen. Typography is scaled by
`FontScale.factor`; Nunito is bundled as a variable font (OFL). Shapes are square in Minimal
and 22 / 14 dp in Cozy. `PixelSceneBackground` draws up to 44 particles on a 5 dp grid at
30 fps while the screen is resumed; scene colours are grey in Minimal and brand colours in Cozy.

## Background work

There is no foreground service. The accessibility service is the only long-running piece
and only when the user enables it. `UsageLimitWorker` runs every 15 minutes as a fallback
for limit reminders and purges expired blocks. `BootReceiver` re-enqueues it after reboot.

## Persistence

| Data | Store |
|---|---|
| Favorites, custom names, hidden, folders | Room `app_customization`, `folders` |
| Manual blocks | Room `app_blocks` |
| Schedules | Room `block_schedules` |
| Usage limits and per-day reminder markers | Room `usage_limits` |
| Notification rules and captured notifications | Room `notification_rules`, `captured_notifications` |
| Every setting (theme, scene, gestures, quick actions, flags) | Preferences DataStore `hush.settings` |

Room schema is exported to `core/database/schemas/` for future migration tests. Version 1.

## Process death and lifecycle

- Home screen state is derived from flows; nothing lives only in memory except the current
  drawer query and dialog state.
- The HOME activity uses `singleTask`, `clearTaskOnLaunch`, `excludeFromRecents` and
  `stateNotNeeded`. A HOME press on a sub screen pops back to the home route.
- Work profile apps are listed via `UserManager.userProfiles` and launched with
  `LauncherApps.startMainActivity`. The app key is `package@userSerial`.
- Missing permissions never throw: each source returns empty data and the UI shows the card.
