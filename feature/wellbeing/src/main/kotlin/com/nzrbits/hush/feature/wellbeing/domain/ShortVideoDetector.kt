package com.nzrbits.hush.feature.wellbeing.domain

import com.nzrbits.hush.core.common.model.ShortVideoPlatform

/**
 * Heuristics that recognise a short-video surface from accessibility view ids and texts.
 *
 * These markers are taken from the current app versions and WILL break when the apps change
 * their layouts. That is a known limitation, documented in PERMISSIONS.md and in the UI.
 * The detector is conservative: it needs at least one strong id marker, or a text marker
 * combined with a weak id marker, so normal feeds are not blocked by accident.
 */
object ShortVideoDetector {
    data class Markers(val strongIds: Set<String>, val weakIds: Set<String>, val texts: Set<String>)

    val markers: Map<ShortVideoPlatform, Markers> = mapOf(
        ShortVideoPlatform.YOUTUBE_SHORTS to Markers(
            strongIds = setOf(
                "com.google.android.youtube:id/reel_recycler",
                "com.google.android.youtube:id/reel_player_page_container",
                "com.google.android.youtube:id/reel_watch_player",
                "com.google.android.youtube:id/reel_progress_bar",
            ),
            weakIds = setOf("com.google.android.youtube:id/reel_player_underlay"),
            texts = setOf("Shorts"),
        ),
        ShortVideoPlatform.INSTAGRAM_REELS to Markers(
            strongIds = setOf(
                "com.instagram.android:id/clips_viewer_view_pager",
                "com.instagram.android:id/clips_swipe_refresh_container",
                "com.instagram.android:id/clips_video_container",
            ),
            weakIds = setOf("com.instagram.android:id/clips_tab"),
            texts = setOf("Reels"),
        ),
        // Facebook obfuscates view ids, so only a selected "Reels" tab or a Reels player label counts.
        ShortVideoPlatform.FACEBOOK_REELS to Markers(
            strongIds = emptySet(),
            weakIds = emptySet(),
            texts = setOf("Reels", "Reel abspielen", "Reels ansehen"),
        ),
        ShortVideoPlatform.SNAPCHAT_SPOTLIGHT to Markers(
            strongIds = setOf(
                "com.snapchat.android:id/spotlight_view_pager",
                "com.snapchat.android:id/spotlight_player",
            ),
            weakIds = emptySet(),
            texts = setOf("Spotlight"),
        ),
    )

    fun platformFor(packageName: String): ShortVideoPlatform? =
        ShortVideoPlatform.entries.firstOrNull { it.packageName == packageName }

    /**
     * @param viewIds all resource ids found in the current window
     * @param texts visible texts and content descriptions of the current window
     * @param selectedTabTexts texts of nodes that are selected (a selected "Reels" tab is a strong signal)
     */
    fun isShortVideoSurface(
        platform: ShortVideoPlatform,
        viewIds: Set<String>,
        texts: Set<String>,
        selectedTabTexts: Set<String>,
    ): Boolean {
        val m = markers[platform] ?: return false
        if (viewIds.any { it in m.strongIds }) return true
        val hasText = texts.any { t -> m.texts.any { t.equals(it, ignoreCase = true) } }
        val selected = selectedTabTexts.any { t -> m.texts.any { t.equals(it, ignoreCase = true) } }
        if (selected) return true
        return hasText && viewIds.any { it in m.weakIds }
    }
}
