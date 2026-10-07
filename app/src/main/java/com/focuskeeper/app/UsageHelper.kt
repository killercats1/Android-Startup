package com.focuskeeper.app

import android.app.AppOpsManager
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Process
import android.provider.Settings

/** Helpers around UsageStatsManager: permission checks, foreground app, per-app totals. */
object UsageHelper {

    /** True if the user has granted "Usage access" to this app in system Settings. */
    fun hasUsageAccess(ctx: Context): Boolean {
        val appOps = ctx.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            ctx.packageName
        )
        return if (mode == AppOpsManager.MODE_DEFAULT) {
            ctx.checkCallingOrSelfPermission(android.Manifest.permission.PACKAGE_USAGE_STATS) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED
        } else {
            mode == AppOpsManager.MODE_ALLOWED
        }
    }

    /** Package name of the app currently in the foreground, or null if unknown. */
    fun currentForegroundPackage(ctx: Context): String? {
        val usm = ctx.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return null
        val now = System.currentTimeMillis()
        val events = usm.queryEvents(now - 10_000, now)
        val e = UsageEvents.Event()
        var last: String? = null
        while (events.hasNextEvent()) {
            events.getNextEvent(e)
            if (e.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND) {
                last = e.packageName
            }
        }
        return last
    }

    /** Foreground time (ms) per package since local midnight today. */
    fun todayUsageMillis(ctx: Context): Map<String, Long> {
        val usm = ctx.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return emptyMap()
        val cal = java.util.Calendar.getInstance().apply {
            set(java.util.Calendar.HOUR_OF_DAY, 0)
            set(java.util.Calendar.MINUTE, 0)
            set(java.util.Calendar.SECOND, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val start = cal.timeInMillis
        val now = System.currentTimeMillis()
        val stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, start, now)
            ?: return emptyMap()
        val map = HashMap<String, Long>()
        for (s in stats) {
            if (s.totalTimeInForeground > 0) {
                map[s.packageName] = (map[s.packageName] ?: 0L) + s.totalTimeInForeground
            }
        }
        return map
    }

    fun usageAccessSettingsIntent() =
        android.content.Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
}
