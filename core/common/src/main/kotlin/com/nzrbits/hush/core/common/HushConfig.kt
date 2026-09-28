package com.nzrbits.hush.core.common

/**
 * Central product configuration. The product name is only defined here and in
 * `app/src/main/res/values/strings.xml` (`app_name`), so a rename touches two places.
 */
object HushConfig {
    /** Display name used in UI copy, notifications and dialogs. */
    const val APP_NAME: String = "Hush"

    /** Name of the mascot that speaks in Cozy Mode and in settings. Taken from Mr. Nook. */
    const val MASCOT_NAME: String = "Mr. Nook"

    /** Applied to preference and database file names so a rename never migrates user data. */
    const val STORAGE_NAMESPACE: String = "hush"

    /** Minimum and maximum duration for a single manual app block. */
    const val MIN_BLOCK_MINUTES: Long = 60L
    const val MAX_BLOCK_MINUTES: Long = 30L * 24L * 60L

    /** Support and legal placeholders. Replace before a public release. */
    const val SUPPORT_EMAIL: String = "post@nzrbits.dev"
    const val PRIVACY_URL: String = "https://nzrbits.github.io/hush/privacy"
}
