package com.nzrbits.hush.feature.updates

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** Receives PackageInstaller session results; starts the system dialog when Android needs one. */
@AndroidEntryPoint
class UpdateReceiver : BroadcastReceiver() {

    @Inject lateinit var repository: UpdateRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != ACTION_RESULT) return
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        val message = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE)
        if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
            @Suppress("DEPRECATION")
            val confirm = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
            confirm?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)?.let { runCatching { context.startActivity(it) } }
        }
        repository.onInstallResult(status, message)
    }

    companion object {
        const val ACTION_RESULT = "com.nzrbits.hush.UPDATE_INSTALL_RESULT"
    }
}
