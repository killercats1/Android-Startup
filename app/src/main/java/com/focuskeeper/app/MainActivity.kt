package com.focuskeeper.app

import android.Manifest
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.material.materialswitch.MaterialSwitch

class MainActivity : AppCompatActivity() {

    private lateinit var focusSwitch: MaterialSwitch
    private lateinit var permUsage: TextView
    private lateinit var permOverlay: TextView
    private lateinit var grantUsageBtn: Button
    private lateinit var grantOverlayBtn: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        focusSwitch = findViewById(R.id.focusSwitch)
        permUsage = findViewById(R.id.permUsageStatus)
        permOverlay = findViewById(R.id.permOverlayStatus)
        grantUsageBtn = findViewById(R.id.grantUsageBtn)
        grantOverlayBtn = findViewById(R.id.grantOverlayBtn)

        focusSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked == Prefs.isFocusActive(this)) return@setOnCheckedChangeListener
            if (isChecked) startFocus() else stopFocus()
        }

        grantUsageBtn.setOnClickListener {
            startActivity(UsageHelper.usageAccessSettingsIntent())
        }
        grantOverlayBtn.setOnClickListener {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        }

        findViewById<Button>(R.id.blockedAppsBtn).setOnClickListener {
            startActivity(Intent(this, AppListActivity::class.java))
        }
        findViewById<Button>(R.id.activityLogBtn).setOnClickListener {
            startActivity(Intent(this, ActivityLogActivity::class.java))
        }
        findViewById<Button>(R.id.usageBtn).setOnClickListener {
            startActivity(Intent(this, UsageStatsActivity::class.java))
        }

        requestNotificationPermissionIfNeeded()
    }

    override fun onResume() {
        super.onResume()
        refreshState()
    }

    private fun refreshState() {
        focusSwitch.isChecked = Prefs.isFocusActive(this)

        val hasUsage = UsageHelper.hasUsageAccess(this)
        permUsage.text = getString(
            R.string.perm_usage_status,
            getString(if (hasUsage) R.string.granted else R.string.not_granted)
        )
        grantUsageBtn.isEnabled = !hasUsage

        val hasOverlay = Settings.canDrawOverlays(this)
        permOverlay.text = getString(
            R.string.perm_overlay_status,
            getString(if (hasOverlay) R.string.granted else R.string.not_granted)
        )
        grantOverlayBtn.isEnabled = !hasOverlay
    }

    private fun startFocus() {
        if (!UsageHelper.hasUsageAccess(this) || !Settings.canDrawOverlays(this)) {
            focusSwitch.isChecked = false
            android.widget.Toast.makeText(
                this, R.string.need_permissions, android.widget.Toast.LENGTH_LONG
            ).show()
            return
        }
        Prefs.setFocusActive(this, true)
        FocusMonitorService.start(this)
        ActivityLog.add(this, "Focus session started")
    }

    private fun stopFocus() {
        Prefs.setFocusActive(this, false)
        FocusMonitorService.stop(this)
        ActivityLog.add(this, "Focus session stopped")
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) !=
                android.content.pm.PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101
                )
            }
        }
    }
}
