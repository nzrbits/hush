package com.nzrbits.hush.core.common.navigation

/** All navigation destinations. Feature modules only depend on these strings, not on each other. */
object HushRoutes {
    const val HOME = "home"
    const val DRAWER = "drawer"
    const val ONBOARDING = "onboarding"

    const val WELLBEING = "wellbeing"
    const val BLOCK_APP = "wellbeing/block/{packageName}"
    const val BLOCKED = "wellbeing/blocked/{packageName}"
    const val SCHEDULES = "wellbeing/schedules"
    const val SCHEDULE_EDIT = "wellbeing/schedules/{id}"
    const val LIMITS = "wellbeing/limits"
    const val SCREEN_TIME = "wellbeing/screentime"
    const val SHORT_VIDEO = "wellbeing/shortvideo"

    const val NOTIFICATION_LOG = "notifications/log"
    const val NOTIFICATION_RULES = "notifications/rules"
    const val NOTIFICATION_RULE_EDIT = "notifications/rules/{id}"

    const val SETTINGS = "settings"
    const val SETTINGS_HOME = "settings/home"
    const val SETTINGS_FAVORITES = "settings/favorites"
    const val SETTINGS_HIDDEN = "settings/hidden"
    const val SETTINGS_APPEARANCE = "settings/appearance"
    const val SETTINGS_GESTURES = "settings/gestures"
    const val SETTINGS_COZY = "settings/cozy"
    const val SETTINGS_PERMISSIONS = "settings/permissions"
    const val SETTINGS_ABOUT = "settings/about"
    const val SETTINGS_PRIVACY = "settings/privacy"
    const val SETTINGS_FAQ = "settings/faq"

    fun blockApp(packageName: String) = "wellbeing/block/$packageName"
    fun blocked(packageName: String) = "wellbeing/blocked/$packageName"
    fun scheduleEdit(id: Long) = "wellbeing/schedules/$id"
    fun notificationRuleEdit(id: Long) = "notifications/rules/$id"
}
