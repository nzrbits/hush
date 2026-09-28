package com.nzrbits.hush.core.designsystem

import com.google.common.truth.Truth.assertThat
import com.nzrbits.hush.core.common.model.AppearanceSettings
import com.nzrbits.hush.core.common.model.CozyPalette
import com.nzrbits.hush.core.common.model.ThemeMode
import com.nzrbits.hush.core.designsystem.components.snapDuration
import com.nzrbits.hush.core.designsystem.theme.HushPalettes
import com.nzrbits.hush.core.designsystem.theme.resolveColors
import org.junit.Test

class ThemeTest {
    @Test
    fun minimalIgnoresSystemDarkness() {
        val a = AppearanceSettings(themeMode = ThemeMode.MINIMAL)
        assertThat(resolveColors(a, systemDark = false)).isEqualTo(HushPalettes.Minimal)
        assertThat(resolveColors(a, systemDark = true)).isEqualTo(HushPalettes.Minimal)
    }

    @Test
    fun cozyFollowsPaletteChoice() {
        assertThat(resolveColors(AppearanceSettings(ThemeMode.COZY, CozyPalette.AUTO), systemDark = true)).isEqualTo(HushPalettes.CozyDark)
        assertThat(resolveColors(AppearanceSettings(ThemeMode.COZY, CozyPalette.AUTO), systemDark = false)).isEqualTo(HushPalettes.CozyLight)
        assertThat(resolveColors(AppearanceSettings(ThemeMode.COZY, CozyPalette.LIGHT), systemDark = true)).isEqualTo(HushPalettes.CozyLight)
        assertThat(resolveColors(AppearanceSettings(ThemeMode.COZY, CozyPalette.DARK), systemDark = false)).isEqualTo(HushPalettes.CozyDark)
    }

    @Test
    fun durationSnapsToSensibleSteps() {
        assertThat(snapDuration(70, 60, 43200)).isEqualTo(75)
        assertThat(snapDuration(59, 60, 43200)).isEqualTo(60)
        assertThat(snapDuration(200, 60, 43200)).isEqualTo(180)
        assertThat(snapDuration(30 * 60, 60, 43200)).isEqualTo(30 * 60)
        assertThat(snapDuration(5 * 24 * 60 + 100, 60, 43200)).isEqualTo(5 * 24 * 60)
        assertThat(snapDuration(99_999, 60, 43200)).isEqualTo(43200)
    }
}
