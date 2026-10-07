package com.focuskeeper.app

import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.concurrent.Executors

class AppListActivity : AppCompatActivity() {

    data class AppItem(val pkg: String, val label: String, val icon: Drawable)

    private lateinit var recycler: RecyclerView
    private lateinit var progress: ProgressBar
    private val items = ArrayList<AppItem>()
    private val adapter = Adapter()
    private val io = Executors.newSingleThreadExecutor()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_list)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        recycler = findViewById(R.id.appRecycler)
        progress = findViewById(R.id.appProgress)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        loadApps()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish(); return true
    }

    private fun loadApps() {
        progress.visibility = View.VISIBLE
        io.execute {
            val pm = packageManager
            val launch = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            val resolved = pm.queryIntentActivities(launch, 0)
            val seen = HashSet<String>()
            val list = ArrayList<AppItem>()
            for (ri in resolved) {
                val pkg = ri.activityInfo.packageName
                if (pkg == packageName || !seen.add(pkg)) continue
                // Skip pre-installed system apps that can't be uninstalled anyway.
                val appInfo = try {
                    pm.getApplicationInfo(pkg, 0)
                } catch (e: PackageManager.NameNotFoundException) {
                    continue
                }
                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val isUpdatedSystem =
                    (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0
                if (isSystem && !isUpdatedSystem) continue
                val label = pm.getApplicationLabel(appInfo).toString()
                val icon = pm.getApplicationIcon(appInfo)
                list.add(AppItem(pkg, label, icon))
            }
            list.sortBy { it.label.lowercase() }
            runOnUiThread {
                items.clear()
                items.addAll(list)
                adapter.notifyDataSetChanged()
                progress.visibility = View.GONE
            }
        }
    }

    private fun requestUninstall(item: AppItem) {
        ActivityLog.add(this, "Requested uninstall of ${item.label} (${item.pkg})")
        val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:${item.pkg}"))
        startActivity(intent)
    }

    private inner class Adapter : RecyclerView.Adapter<Adapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val icon: ImageView = v.findViewById(R.id.appIcon)
            val name: TextView = v.findViewById(R.id.appName)
            val pkg: TextView = v.findViewById(R.id.appPkg)
            val block: com.google.android.material.materialswitch.MaterialSwitch =
                v.findViewById(R.id.appBlockSwitch)
            val uninstall: Button = v.findViewById(R.id.appUninstallBtn)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_app, parent, false)
            return VH(v)
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = items[position]
            holder.icon.setImageDrawable(item.icon)
            holder.name.text = item.label
            holder.pkg.text = item.pkg
            holder.block.setOnCheckedChangeListener(null)
            holder.block.isChecked = Prefs.isBlocked(this@AppListActivity, item.pkg)
            holder.block.setOnCheckedChangeListener { _, checked ->
                Prefs.setBlocked(this@AppListActivity, item.pkg, checked)
                ActivityLog.add(
                    this@AppListActivity,
                    (if (checked) "Added " else "Removed ") + "${item.label} " +
                        (if (checked) "to" else "from") + " block list"
                )
            }
            holder.uninstall.setOnClickListener { requestUninstall(item) }
        }
    }

    override fun onDestroy() {
        io.shutdown()
        super.onDestroy()
    }
}
