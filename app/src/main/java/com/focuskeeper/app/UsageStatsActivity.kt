package com.focuskeeper.app

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.concurrent.TimeUnit

class UsageStatsActivity : AppCompatActivity() {

    data class Row(val label: String, val millis: Long)

    private val rows = ArrayList<Row>()
    private val adapter = Adapter()
    private lateinit var hint: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_usage)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        hint = findViewById(R.id.usageHint)
        val recycler = findViewById<RecyclerView>(R.id.usageRecycler)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        findViewById<Button>(R.id.usageGrantBtn).setOnClickListener {
            startActivity(UsageHelper.usageAccessSettingsIntent())
        }
    }

    override fun onResume() {
        super.onResume()
        refresh()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish(); return true
    }

    private fun refresh() {
        if (!UsageHelper.hasUsageAccess(this)) {
            hint.text = getString(R.string.usage_need_access)
            hint.visibility = View.VISIBLE
            findViewById<Button>(R.id.usageGrantBtn).visibility = View.VISIBLE
            rows.clear()
            adapter.notifyDataSetChanged()
            return
        }
        findViewById<Button>(R.id.usageGrantBtn).visibility = View.GONE
        val pm = packageManager
        val usage = UsageHelper.todayUsageMillis(this)
            .entries
            .sortedByDescending { it.value }
            .take(50)
        rows.clear()
        for (e in usage) {
            val label = try {
                pm.getApplicationLabel(pm.getApplicationInfo(e.key, 0)).toString()
            } catch (ex: PackageManager.NameNotFoundException) {
                e.key
            }
            rows.add(Row(label, e.value))
        }
        adapter.notifyDataSetChanged()
        hint.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
        if (rows.isEmpty()) hint.text = getString(R.string.usage_empty)
    }

    private fun format(millis: Long): String {
        val h = TimeUnit.MILLISECONDS.toHours(millis)
        val m = TimeUnit.MILLISECONDS.toMinutes(millis) % 60
        return if (h > 0) "${h}h ${m}m" else "${m}m"
    }

    private inner class Adapter : RecyclerView.Adapter<Adapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val name: TextView = v.findViewById(R.id.usageName)
            val time: TextView = v.findViewById(R.id.usageTime)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_usage, parent, false)
            return VH(v)
        }

        override fun getItemCount() = rows.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val r = rows[position]
            holder.name.text = r.label
            holder.time.text = format(r.millis)
        }
    }
}
