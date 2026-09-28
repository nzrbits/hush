package com.nzrbits.hush.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.nzrbits.hush.core.common.HushConfig
import com.nzrbits.hush.core.common.model.AppearanceSettings
import com.nzrbits.hush.core.common.model.CozyPalette
import com.nzrbits.hush.core.common.model.DateFormatChoice
import com.nzrbits.hush.core.common.model.FontFamilyChoice
import com.nzrbits.hush.core.common.model.FontScale
import com.nzrbits.hush.core.common.model.GestureAction
import com.nzrbits.hush.core.common.model.GestureSettings
import com.nzrbits.hush.core.common.model.HomeSettings
import com.nzrbits.hush.core.common.model.HushSettings
import com.nzrbits.hush.core.common.model.PixelScene
import com.nzrbits.hush.core.common.model.SceneDensity
import com.nzrbits.hush.core.common.model.ShortVideoPlatform
import com.nzrbits.hush.core.common.model.ThemeMode
import com.nzrbits.hush.core.common.model.TimeFormatChoice
import com.nzrbits.hush.core.common.model.WellbeingSettings
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/** A corrupt or unreadable file falls back to defaults instead of crashing the home app in a loop. */
private val Context.hushPreferences: DataStore<Preferences> by preferencesDataStore(
    name = "${HushConfig.STORAGE_NAMESPACE}.settings",
    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
)

/**
 * All user settings. Enum values are stored by name and fall back to defaults when unknown,
 * so removing an enum constant later never crashes the app on start.
 */
@Singleton
class SettingsRepository @Inject constructor(@ApplicationContext private val context: Context) {

    private object Keys {
        val showPhone = booleanPreferencesKey("home.showPhone")
        val showCamera = booleanPreferencesKey("home.showCamera")
        val showAlarm = booleanPreferencesKey("home.showAlarm")
        val timeFormat = stringPreferencesKey("home.timeFormat")
        val dateFormat = stringPreferencesKey("home.dateFormat")
        val charging = booleanPreferencesKey("home.chargingAnimation")
        val autoKeyboard = booleanPreferencesKey("home.autoKeyboard")

        val themeMode = stringPreferencesKey("appearance.themeMode")
        val cozyPalette = stringPreferencesKey("appearance.cozyPalette")
        val fontScale = stringPreferencesKey("appearance.fontScale")
        val fontFamily = stringPreferencesKey("appearance.fontFamily")
        val wallpaper = stringPreferencesKey("appearance.wallpaperUri")
        val showMascot = booleanPreferencesKey("appearance.showMascot")
        val showMascotSettings = booleanPreferencesKey("appearance.showMascotInSettings")
        val scene = stringPreferencesKey("appearance.scene")
        val sceneDensity = stringPreferencesKey("appearance.sceneDensity")
        val reduceMotion = booleanPreferencesKey("appearance.reduceMotion")

        val swipeUp = stringPreferencesKey("gestures.swipeUp")
        val swipeDown = stringPreferencesKey("gestures.swipeDown")
        val doubleTap = stringPreferencesKey("gestures.doubleTap")

        val shortVideo = booleanPreferencesKey("wellbeing.shortVideo")
        val shortVideoPlatforms = stringSetPreferencesKey("wellbeing.shortVideoPlatforms")
        val notificationFilter = booleanPreferencesKey("wellbeing.notificationFilter")
        val limitReminders = booleanPreferencesKey("wellbeing.limitReminders")

        val onboardingDone = booleanPreferencesKey("onboardingDone")
    }

    val settings: Flow<HushSettings> = context.hushPreferences.data
        .catch { e -> if (e is IOException) emit(emptyPreferences()) else throw e }
        .map { p -> p.toSettings() }

    suspend fun current(): HushSettings = settings.first()

    private inline fun <reified E : Enum<E>> Preferences.enum(key: Preferences.Key<String>, default: E): E =
        this[key]?.let { name -> enumValues<E>().firstOrNull { it.name == name } } ?: default

    private fun Preferences.toSettings(): HushSettings = HushSettings(
        home = HomeSettings(
            showPhone = this[Keys.showPhone] ?: true,
            showCamera = this[Keys.showCamera] ?: true,
            showAlarm = this[Keys.showAlarm] ?: false,
            timeFormat = enum(Keys.timeFormat, TimeFormatChoice.SYSTEM),
            dateFormat = enum(Keys.dateFormat, DateFormatChoice.LONG),
            showChargingAnimation = this[Keys.charging] ?: true,
            autoKeyboardInDrawer = this[Keys.autoKeyboard] ?: true,
        ),
        appearance = AppearanceSettings(
            themeMode = enum(Keys.themeMode, ThemeMode.MINIMAL),
            cozyPalette = enum(Keys.cozyPalette, CozyPalette.AUTO),
            fontScale = enum(Keys.fontScale, FontScale.MEDIUM),
            fontFamily = enum(Keys.fontFamily, FontFamilyChoice.DEFAULT),
            wallpaperUri = this[Keys.wallpaper],
            showMascot = this[Keys.showMascot] ?: true,
            showMascotInSettings = this[Keys.showMascotSettings] ?: true,
            scene = enum(Keys.scene, PixelScene.NONE),
            sceneDensity = enum(Keys.sceneDensity, SceneDensity.NORMAL),
            reduceMotion = this[Keys.reduceMotion] ?: false,
        ),
        gestures = GestureSettings(
            swipeUp = enum(Keys.swipeUp, GestureAction.OPEN_DRAWER),
            swipeDown = enum(Keys.swipeDown, GestureAction.OPEN_NOTIFICATIONS),
            doubleTap = enum(Keys.doubleTap, GestureAction.LOCK_SCREEN),
        ),
        wellbeing = WellbeingSettings(
            shortVideoBlockingEnabled = this[Keys.shortVideo] ?: false,
            shortVideoPlatforms = this[Keys.shortVideoPlatforms]
                ?.mapNotNull { name -> ShortVideoPlatform.entries.firstOrNull { it.name == name } }?.toSet()
                ?: ShortVideoPlatform.entries.toSet(),
            notificationFilterEnabled = this[Keys.notificationFilter] ?: false,
            usageLimitRemindersEnabled = this[Keys.limitReminders] ?: true,
        ),
        onboardingDone = this[Keys.onboardingDone] ?: false,
    )

    suspend fun updateHome(transform: (HomeSettings) -> HomeSettings) {
        context.hushPreferences.edit { p ->
            val h = transform(p.toSettings().home)
            p[Keys.showPhone] = h.showPhone
            p[Keys.showCamera] = h.showCamera
            p[Keys.showAlarm] = h.showAlarm
            p[Keys.timeFormat] = h.timeFormat.name
            p[Keys.dateFormat] = h.dateFormat.name
            p[Keys.charging] = h.showChargingAnimation
            p[Keys.autoKeyboard] = h.autoKeyboardInDrawer
        }
    }

    suspend fun updateAppearance(transform: (AppearanceSettings) -> AppearanceSettings) {
        context.hushPreferences.edit { p ->
            val a = transform(p.toSettings().appearance)
            p[Keys.themeMode] = a.themeMode.name
            p[Keys.cozyPalette] = a.cozyPalette.name
            p[Keys.fontScale] = a.fontScale.name
            p[Keys.fontFamily] = a.fontFamily.name
            val wallpaper = a.wallpaperUri
            if (wallpaper == null) p.remove(Keys.wallpaper) else p[Keys.wallpaper] = wallpaper
            p[Keys.showMascot] = a.showMascot
            p[Keys.showMascotSettings] = a.showMascotInSettings
            p[Keys.scene] = a.scene.name
            p[Keys.sceneDensity] = a.sceneDensity.name
            p[Keys.reduceMotion] = a.reduceMotion
        }
    }

    suspend fun updateGestures(transform: (GestureSettings) -> GestureSettings) {
        context.hushPreferences.edit { p ->
            val g = transform(p.toSettings().gestures)
            p[Keys.swipeUp] = g.swipeUp.name
            p[Keys.swipeDown] = g.swipeDown.name
            p[Keys.doubleTap] = g.doubleTap.name
        }
    }

    suspend fun updateWellbeing(transform: (WellbeingSettings) -> WellbeingSettings) {
        context.hushPreferences.edit { p ->
            val w = transform(p.toSettings().wellbeing)
            p[Keys.shortVideo] = w.shortVideoBlockingEnabled
            p[Keys.shortVideoPlatforms] = w.shortVideoPlatforms.map { it.name }.toSet()
            p[Keys.notificationFilter] = w.notificationFilterEnabled
            p[Keys.limitReminders] = w.usageLimitRemindersEnabled
        }
    }

    suspend fun setOnboardingDone(done: Boolean) {
        context.hushPreferences.edit { it[Keys.onboardingDone] = done }
    }
}
