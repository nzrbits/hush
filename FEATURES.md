# Features

Status legend:
- **Implemented and tested**: unit test and/or verified on the Android 15 emulator, see `TEST_RESULTS.md`
- **Implemented, not tested**: code complete, no end-to-end verification yet
- **Partial**: works with a documented gap
- **Android limited**: works only as far as the platform allows, stated in the UI
- **Open**: not built

## Source inventory (Phase 1 result)

| Function | Source | Reusable | Adaptation | Target module |
|---|---|---|---|---|
| Launcher, drawer, blocking, schedules, limits, screen time, notification filter, short video, gestures, settings | Hush (new) | New implementation | Android APIs | `feature/*`, `core/system` |
| Colour tokens (cream, sand, sage, terracotta, dark variant) | Mr. Nook `styles.css`, Melinda `styles.css` | Yes, values copied | CSS vars to `HushColors` roles | `core/designsystem` |
| Nunito font | Both (Google Fonts / self-hosted woff2) | Yes | Bundled as variable TTF (OFL) | `core/designsystem` |
| Radius 22 / 14 px, soft shadows | Both | Yes | `HushShapes` | `core/designsystem` |
| Mr. Nook sprite sheets idle/talk/cheer/sleep (256 px frames) | Mr. Nook `public/img/nook-*.png`, `scripts/mascot.mjs` | Yes, PNGs copied | Compose Canvas frame animation with the web timings | `feature/cozy` |
| Daypart sayings, stable per hour | Mr. Nook `SAYINGS`, `saying()` | Mechanism yes, texts no | New German lines for a launcher; same seed formula | `feature/cozy` |
| Dry, short tone ("Ist raus. Jetzt sind die dran.") | Melinda copy | Tone only | All Hush copy written in that register | all UI |
| Deadline / holiday logic, listening badges, streaks, invites, credit, Pingen, AI | Melinda / Mr. Nook | No | Out of scope by decision 2026-09-28 (light inspiration only) | – |
| Mr. Nook app deep links (`#/home`, package `dev.workers.mrnook`) | Mr. Nook | Not used | – | – |

Melinda and Mr. Nook keep running as standalone web apps. Nothing in Hush depends on them
being installed.

## Launcher

| Feature | Status | Notes |
|---|---|---|
| HOME intent, selectable as default launcher | Implemented and tested | RoleManager dialog on Q+ via an activity result launcher (a plain startActivity closes the dialog at once), system home settings before; verified with the system chooser and the role dialog |
| List all launchable apps incl. work profile | Implemented and tested | `LauncherApps` per `UserManager.userProfiles`; work profile flagged, not tested with a real work profile |
| Launch apps | Implemented and tested | `startMainActivity`, fallback launch intent |
| React to install / uninstall / label change | Implemented, not tested | `LauncherApps.Callback` on main looper; no package was installed during the emulator run |
| Reboot behaviour | Implemented, not tested | BootReceiver re-enqueues worker; blocks use absolute times; process death verified with `am force-stop` |
| Persistent launcher settings | Implemented and tested | DataStore + Room, survive process death |
| Home: clock, date, charging line, favourites list, quick actions | Implemented and tested | 24 h / 12 h / system, three date formats |
| Drawer: alphabetical, search, A–Z index, auto keyboard | Implemented and tested | Search ranks prefix > word start > substring, normalised index built once per list change |
| Hidden apps, custom names, folders | Implemented and tested (hide, rename); folders implemented, not tested | Folder groups appear as sections in the drawer |
| Context menu (10 actions) | Implemented and tested | Uninstall opens the system dialog |
| Favourites reorder | Implemented, not tested | Up / down buttons in settings |

## Digital Wellbeing

| Feature | Status | Notes |
|---|---|---|
| Block app 1 h–30 d, slider, slide to confirm | Implemented and tested | Log-scale slider with snapping |
| Block persists, survives restart | Implemented and tested | Absolute epoch end time |
| Block enforced when opened elsewhere | Android limited, tested | Needs accessibility service; verified Chrome closed within 1 s of `am start`. Without the service the block is shown only inside Hush (home, drawer and context menu all refuse), and the UI says so |
| Block takes effect while the app is already open | Implemented and tested | Service re-evaluates the foreground app on every block state change (minute ticker, new block, schedule start); verified with a schedule starting while Clock was open |
| Protected apps cannot be blocked | Implemented and tested | Dialer, SMS, Settings, clock, installer, keyboards, system UI; see PERMISSIONS.md |
| Blocked notice on home expires after 20 s | Implemented and tested | |
| Screen time today / 7 days / per app / most used | Implemented and tested | Usage Access; DST-safe day boundaries |
| Schedules: days, start / end, overnight, several apps, on / off, edit | Implemented and tested | Engine unit tested incl. overnight and DST; UI create + reopen verified |
| Usage reminders 80 % / 100 % once per day | Implemented, logic tested | Decision unit tested; notification posting not verified end-to-end (needs real usage time) |
| Reminder vs warning vs block clearly separated | Implemented and tested | Different channels, copy says "keine Sperre" |
| Short video brake (YouTube, Instagram, Facebook, Snapchat) | Android limited, detector unit tested | View-id heuristics; not verified against the live apps (YouTube did not open on the emulator). Facebook obfuscates ids, only the selected Reels tab counts. Global on / off and per platform |

## Notifications

| Feature | Status | Notes |
|---|---|---|
| Listener, rule evaluation, local log, cancel | Implemented and tested | Test notification captured, removed from the shade, listed with app, title, text, time |
| Rules: block list / allow list, time window, weekdays, on / off | Implemented and tested | Filter unit tested incl. overnight window |
| Delete one / delete all | Implemented and tested | |
| Skips ongoing, foreground-service, group-summary, media, call, alarm, navigation and protected-package notifications | Implemented, not tested | Cancel happens before the log entry, so nothing is logged that stayed in the shade |
| Sound may already have played | Android limited | Stated on the log screen and in the FAQ |

## Cozy / design

| Feature | Status | Notes |
|---|---|---|
| Minimal Mode | Implemented and tested | |
| Cozy Mode light / dark / auto | Implemented and tested (light); dark palette unit tested | Same screens, only tokens change; switching keeps all data |
| Mr. Nook with speech bubble on home and in settings | Implemented and tested | Sleep sprite while a schedule runs, talk in settings |
| Sayings per daypart, stable per hour | Implemented and tested | |
| Pixel scenes: leaves, snow, rain, fireflies, stars, petals; three densities | Implemented and tested (leaves) | Others share the same engine; 30 fps cap, paused when not resumed, off with reduced motion |
| Font size (4), font (3), wallpaper, reduced motion | Implemented, not tested (wallpaper) | Wallpaper via `OpenDocument` with persisted URI permission, decoded once on IO sampled to screen size, drawn under a 55 % scrim |

## Gestures and settings

| Feature | Status | Notes |
|---|---|---|
| Swipe up / down / double tap, each assignable | Implemented and tested | Lock and shade verified via accessibility logs |
| Long press app / free space | Implemented and tested | |
| Settings: all sections from the brief | Implemented and tested | Wellbeing, Homescreen, Darstellung, Gesten, Cozy, Mehr |
| Permissions screen with explanations | Implemented and tested | Refreshes on resume |
| Onboarding | Implemented and tested | |
| Default launcher management, leave launcher | Implemented and tested | |
| Account | Open | No account system exists in Hush by design |

## Known gaps

- Folder UI is functional but has no drag and drop; membership is set through the context menu.
- Favourites reorder uses buttons, not drag.
- No English strings yet; the UI is German only.
- Short video detection markers will need updates when the apps change.
- Release build is unsigned; no keystore in the repo.
