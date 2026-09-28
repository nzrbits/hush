package com.nzrbits.hush.core.common.model

/**
 * Identity of a launchable app. Work profile apps share the package name with the personal
 * profile, so the user serial number is part of the key.
 */
data class AppKey(val packageName: String, val userSerial: Long) {
    /** Stable string form used as a database key and in preferences. */
    val id: String get() = "$packageName@$userSerial"

    companion object {
        const val PERSONAL_USER: Long = 0L
        fun parse(id: String): AppKey {
            val at = id.lastIndexOf('@')
            if (at < 0) return AppKey(id, PERSONAL_USER)
            return AppKey(id.substring(0, at), id.substring(at + 1).toLongOrNull() ?: PERSONAL_USER)
        }
    }
}

/** An app as reported by the system, before user customisation is applied. */
data class InstalledApp(
    val key: AppKey,
    val label: String,
    val activityClassName: String,
    val isWorkProfile: Boolean,
    val isSystemApp: Boolean,
)

/** User customisation stored by Hush for one app. All fields are optional. */
data class AppCustomization(
    val key: AppKey,
    val customLabel: String? = null,
    val hidden: Boolean = false,
    val favoriteOrder: Int? = null,
    val folderId: Long? = null,
)

/** Combined view used by the drawer and the home screen. */
data class LauncherApp(
    val key: AppKey,
    val originalLabel: String,
    val customLabel: String?,
    val hidden: Boolean,
    val favoriteOrder: Int?,
    val folderId: Long?,
    val isWorkProfile: Boolean,
    val isSystemApp: Boolean,
    val activityClassName: String,
) {
    val displayLabel: String get() = customLabel?.takeIf { it.isNotBlank() } ?: originalLabel
    val isFavorite: Boolean get() = favoriteOrder != null
    val packageName: String get() = key.packageName
}

data class AppFolder(
    val id: Long,
    val name: String,
    val sortOrder: Int,
)
