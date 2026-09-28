package com.nzrbits.hush.feature.wellbeing.domain

import com.google.common.truth.Truth.assertThat
import com.nzrbits.hush.core.common.model.ShortVideoPlatform
import org.junit.Test

class ShortVideoDetectorTest {
    @Test
    fun youtubeShortsStrongIdDetected() {
        val hit = ShortVideoDetector.isShortVideoSurface(
            ShortVideoPlatform.YOUTUBE_SHORTS,
            viewIds = setOf("com.google.android.youtube:id/reel_recycler"),
            texts = emptySet(), selectedTabTexts = emptySet(),
        )
        assertThat(hit).isTrue()
    }

    @Test
    fun youtubeHomeFeedNotDetected() {
        val hit = ShortVideoDetector.isShortVideoSurface(
            ShortVideoPlatform.YOUTUBE_SHORTS,
            viewIds = setOf("com.google.android.youtube:id/results", "com.google.android.youtube:id/pivot_bar"),
            texts = setOf("Startseite", "Shorts", "Abos"), selectedTabTexts = setOf("Startseite"),
        )
        assertThat(hit).isFalse()
    }

    @Test
    fun selectedShortsTabCounts() {
        val hit = ShortVideoDetector.isShortVideoSurface(
            ShortVideoPlatform.YOUTUBE_SHORTS,
            viewIds = emptySet(), texts = setOf("Shorts"), selectedTabTexts = setOf("Shorts"),
        )
        assertThat(hit).isTrue()
    }

    @Test
    fun instagramReelsTextAlonePlusWeakId() {
        val hit = ShortVideoDetector.isShortVideoSurface(
            ShortVideoPlatform.INSTAGRAM_REELS,
            viewIds = setOf("com.instagram.android:id/clips_tab"), texts = setOf("Reels"), selectedTabTexts = emptySet(),
        )
        assertThat(hit).isTrue()
    }

    @Test
    fun facebookNeedsSelectedReelsTab() {
        assertThat(
            ShortVideoDetector.isShortVideoSurface(ShortVideoPlatform.FACEBOOK_REELS, emptySet(), setOf("Reels"), emptySet()),
        ).isFalse()
        assertThat(
            ShortVideoDetector.isShortVideoSurface(ShortVideoPlatform.FACEBOOK_REELS, emptySet(), setOf("Reels"), setOf("Reels")),
        ).isTrue()
    }

    @Test
    fun platformLookup() {
        assertThat(ShortVideoDetector.platformFor("com.snapchat.android")).isEqualTo(ShortVideoPlatform.SNAPCHAT_SPOTLIGHT)
        assertThat(ShortVideoDetector.platformFor("com.whatsapp")).isNull()
    }
}
