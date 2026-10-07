package com.focuskeeper.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** Re-arm the focus monitor after a reboot if a focus session was left active. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED && Prefs.isFocusActive(context)) {
            FocusMonitorService.start(context)
        }
    }
}
