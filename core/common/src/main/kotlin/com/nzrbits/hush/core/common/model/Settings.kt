package com.nzrbits.hush.core.common.model

enum class ThemeMode { MINIMAL, COZY }

enum class CozyPalette { AUTO, LIGHT, DARK }

enum class FontScale(val factor: Float, val label: String) {
    SMALL(0.9f, "Klein"),
    MEDIUM(1.0f, "Mittel"),
    LARGE(1.15f, "Groß"),
    EXTRA_LARGE(1.3f, "Extra groß"),
}

/** DEFAULT follows the mode: system sans in Minimal, Nunito in Cozy. */
enum class FontFamilyChoice(val label: String) {
    DEFAULT("Wie Modus"),
    SYSTEM("System"),
    NUNITO("Nunito"),
    MONO("Monospace"),
}

enum class TimeFormatChoice { SYSTEM, H24, H12 }

enum class DateFormatChoice { LONG, SHORT, NUMERIC }

/** Animated pixel background scenes. NONE keeps the plain background. */
enum class PixelScene(val label: String) {
    NONE("Aus"),
    LEAVES("Fallende Blätter"),
    SNOW("Schneewehen"),
    RAIN("Leichter Regen"),
    FIREFLIES("Glühwürmchen"),
    STARS("Sternenhimmel"),
    PETALS("Blütenblätter"),
}

enum class SceneDensity(val particles: Int, val label: String) {
    SPARSE(10, "Wenig"),
    NORMAL(18, "Normal"),
    DENSE(30, "Viel"),
}

enum class GestureAction(val label: String) {
    NONE("Nichts"),
    OPEN_DRAWER("App-Suche öffnen"),
    OPEN_NOTIFICATIONS("Benachrichtigungen öffnen"),
    LOCK_SCREEN("Bildschirm sperren"),
    OPEN_SETTINGS("Einstellungen öffnen"),
    OPEN_WELLBEING("Fokus & Bildschirmzeit öffnen"),
    OPEN_NOTIFICATION_LOG("Gefilterte Meldungen öffnen"),
}

data class GestureSettings(
    val swipeUp: GestureAction = GestureAction.OPEN_DRAWER,
    val swipeDown: GestureAction = GestureAction.OPEN_NOTIFICATIONS,
    val doubleTap: GestureAction = GestureAction.LOCK_SCREEN,
)

/** Bottom-left shortcut slots. A slot holds a package name or one of the built-in tokens. */
object Shortcuts {
    const val PHONE = "hush:phone"
    const val CAMERA = "hush:camera"
    const val ALARM = "hush:alarm"
    const val SLOTS = 3
    val default: List<String> = listOf(PHONE, CAMERA)
    fun builtInLabel(token: String): String? = when (token) {
        PHONE -> "Telefon"
        CAMERA -> "Kamera"
        ALARM -> "Wecker"
        else -> null
    }
}

data class HomeSettings(
    val shortcuts: List<String> = Shortcuts.default,
    val timeFormat: TimeFormatChoice = TimeFormatChoice.SYSTEM,
    val dateFormat: DateFormatChoice = DateFormatChoice.LONG,
    val showChargingAnimation: Boolean = true,
    val autoKeyboardInDrawer: Boolean = true,
    /** Hide Android's navigation bar (3-button or gesture pill); a swipe from the bottom edge shows it briefly. */
    val hideNavigationBar: Boolean = true,
)

data class AppearanceSettings(
    val themeMode: ThemeMode = ThemeMode.MINIMAL,
    val cozyPalette: CozyPalette = CozyPalette.AUTO,
    val fontScale: FontScale = FontScale.MEDIUM,
    val fontFamily: FontFamilyChoice = FontFamilyChoice.DEFAULT,
    val wallpaperUri: String? = null,
    val showMascot: Boolean = true,
    val showMascotInSettings: Boolean = true,
    val scene: PixelScene = PixelScene.NONE,
    val sceneDensity: SceneDensity = SceneDensity.NORMAL,
    val reduceMotion: Boolean = false,
)

data class WellbeingSettings(
    val shortVideoBlockingEnabled: Boolean = false,
    val shortVideoPlatforms: Set<ShortVideoPlatform> = ShortVideoPlatform.entries.toSet(),
    val notificationFilterEnabled: Boolean = false,
    val usageLimitRemindersEnabled: Boolean = true,
)

/**
 * In-app updates from GitHub releases. Off by default: with [autoCheck] off the app never
 * opens a network connection. [autoInstall] lets the background worker download and install
 * silently once Android allows it (Hush is its own installer of record, same signature).
 */
data class UpdateSettings(
    val autoCheck: Boolean = false,
    val autoInstall: Boolean = false,
    val lastCheckMillis: Long = 0L,
    val skippedVersion: String? = null,
)

data class HushSettings(
    val home: HomeSettings = HomeSettings(),
    val appearance: AppearanceSettings = AppearanceSettings(),
    val gestures: GestureSettings = GestureSettings(),
    val wellbeing: WellbeingSettings = WellbeingSettings(),
    val updates: UpdateSettings = UpdateSettings(),
    val onboardingDone: Boolean = false,
)
