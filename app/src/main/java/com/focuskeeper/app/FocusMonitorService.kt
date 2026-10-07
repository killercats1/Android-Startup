package com.focuskeeper.app

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.provider.Settings
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.TextView
import androidx.core.app.NotificationCompat
import androidx.lifecycle.LifecycleService

/**
 * While a focus session is active, this foreground service polls the current foreground
 * app once a second. If it is on the user's block list, a full-screen "stay focused"
 * overlay is shown and the user is nudged back to the home screen. All visible; the
 * persistent notification makes the service obvious.
 */
class FocusMonitorService : LifecycleService() {

    private val handler = Handler(Looper.getMainLooper())
    private var windowManager: WindowManager? = null
    private var overlayView: View? = null
    private var lastBlockedLogged: String? = null

    private val poll = object : Runnable {
        override fun run() {
            tick()
            handler.postDelayed(this, POLL_MS)
        }
    }

    override fun onCreate() {
        super.onCreate()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        startForeground(NOTIF_ID, buildNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        handler.removeCallbacks(poll)
        handler.post(poll)
        return START_STICKY
    }

    private fun tick() {
        if (!Prefs.isFocusActive(this)) {
            stopSelfSafely()
            return
        }
        if (!UsageHelper.hasUsageAccess(this) || !Settings.canDrawOverlays(this)) {
            // Can't enforce without permissions; keep running but do nothing.
            hideOverlay()
            return
        }
        val fg = UsageHelper.currentForegroundPackage(this)
        if (fg != null && fg != packageName && Prefs.isBlocked(this, fg)) {
            if (lastBlockedLogged != fg) {
                ActivityLog.add(this, "Blocked access to ${labelFor(fg)} during focus session")
                lastBlockedLogged = fg
            }
            showOverlay(labelFor(fg))
        } else {
            lastBlockedLogged = null
            hideOverlay()
        }
    }

    private fun labelFor(pkg: String): String = try {
        val pm = packageManager
        pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString()
    } catch (e: Exception) {
        pkg
    }

    private fun showOverlay(appLabel: String) {
        if (overlayView != null) {
            overlayView?.findViewById<TextView>(R.id.blockMessage)?.text =
                getString(R.string.block_message, appLabel)
            return
        }
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        else
            @Suppress("DEPRECATION") WindowManager.LayoutParams.TYPE_PHONE

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON,
            PixelFormat.OPAQUE
        )
        params.gravity = Gravity.CENTER

        val view = LayoutInflater.from(this).inflate(R.layout.overlay_block, null)
        view.findViewById<TextView>(R.id.blockMessage).text =
            getString(R.string.block_message, appLabel)
        view.findViewById<View>(R.id.goHomeButton).setOnClickListener {
            hideOverlay()
            val home = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(home)
        }
        try {
            windowManager?.addView(view, params)
            overlayView = view
        } catch (e: Exception) {
            overlayView = null
        }
    }

    private fun hideOverlay() {
        val v = overlayView ?: return
        try {
            windowManager?.removeView(v)
        } catch (e: Exception) {
            // ignore
        }
        overlayView = null
    }

    private fun stopSelfSafely() {
        handler.removeCallbacks(poll)
        hideOverlay()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        handler.removeCallbacks(poll)
        hideOverlay()
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        super.onBind(intent)
        return null
    }

    private fun buildNotification(): Notification {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.focus_channel_name),
                NotificationManager.IMPORTANCE_LOW
            )
            nm.createNotificationChannel(channel)
        }
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.focus_notif_title))
            .setContentText(getString(R.string.focus_notif_text))
            .setSmallIcon(R.drawable.ic_focus)
            .setOngoing(true)
            .setContentIntent(openIntent)
            .build()
    }

    companion object {
        private const val POLL_MS = 1000L
        private const val NOTIF_ID = 42
        private const val CHANNEL_ID = "focus_monitor"

        fun start(ctx: Context) {
            val i = Intent(ctx, FocusMonitorService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                ctx.startForegroundService(i)
            } else {
                ctx.startService(i)
            }
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, FocusMonitorService::class.java))
        }
    }
}
