package com.nzrbits.hush

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.nzrbits.hush.feature.wellbeing.service.UsageLimitWorker

/**
 * After a reboot or an update, re-enqueue the periodic worker. Blocks and schedules need no
 * action: they are evaluated from stored absolute times whenever an app comes to the foreground.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED || intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            UsageLimitWorker.schedule(context.applicationContext)
        }
    }
}
