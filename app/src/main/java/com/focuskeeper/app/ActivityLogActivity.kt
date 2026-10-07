package com.focuskeeper.app

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class ActivityLogActivity : AppCompatActivity() {

    private val entries = ArrayList<String>()
    private val adapter = Adapter()
    private lateinit var empty: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_log)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        empty = findViewById(R.id.logEmpty)
        val recycler = findViewById<RecyclerView>(R.id.logRecycler)
        recycler.layoutManager = LinearLayoutManager(this)
        recycler.adapter = adapter

        findViewById<Button>(R.id.clearLogBtn).setOnClickListener {
            ActivityLog.clear(this)
            refresh()
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
        entries.clear()
        entries.addAll(ActivityLog.all(this))
        adapter.notifyDataSetChanged()
        empty.visibility = if (entries.isEmpty()) View.VISIBLE else View.GONE
    }

    private inner class Adapter : RecyclerView.Adapter<Adapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val text: TextView = v.findViewById(R.id.logText)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_log, parent, false)
            return VH(v)
        }

        override fun getItemCount() = entries.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.text.text = entries[position]
        }
    }
}
