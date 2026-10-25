package com.smartalarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * Re-schedules a persisted alarm after a device reboot. Without this, an
 * alarm set for tomorrow silently disappears when the phone restarts.
 */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent?) {
        val action = intent?.action ?: return
        if (action != Intent.ACTION_BOOT_COMPLETED &&
            action != "android.intent.action.QUICKBOOT_POWERON"
        ) return
        try {
            val appCtx = ctx.applicationContext
            // Reschedules all enabled alarms so none are lost after reboot
            AlarmScheduler.rescheduleAll(appCtx)
        } catch (_: Exception) {
            // Never crash the boot sequence; user can re-set from the app.
        }
    }
}
