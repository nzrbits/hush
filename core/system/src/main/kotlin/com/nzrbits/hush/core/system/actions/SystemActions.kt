package com.nzrbits.hush.core.system.actions

import android.content.Context
import android.content.Intent
import android.provider.AlarmClock
import android.provider.MediaStore
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** Quick actions from the home screen. Each one degrades to "false" when no app can handle it. */
@Singleton
class SystemActions @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun openDialer(): Boolean = start(Intent(Intent.ACTION_DIAL))

    fun openCamera(): Boolean =
        start(Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA)) || start(Intent(MediaStore.ACTION_IMAGE_CAPTURE))

    fun openAlarms(): Boolean = start(Intent(AlarmClock.ACTION_SHOW_ALARMS))

    fun openSystemHomeSettings(): Boolean = start(Intent(android.provider.Settings.ACTION_HOME_SETTINGS))

    private fun start(intent: Intent): Boolean {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        if (intent.resolveActivity(context.packageManager) == null) return false
        return runCatching { context.startActivity(intent); true }.getOrDefault(false)
    }
}
