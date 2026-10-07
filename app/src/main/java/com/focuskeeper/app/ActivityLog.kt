package com.focuskeeper.app

import android.content.Context
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * A visible, on-device log of actions this app takes: focus sessions started/stopped,
 * apps blocked, uninstalls requested, block-list changes. Stored locally as JSON.
 * Nothing leaves the device.
 */
object ActivityLog {
    private const val FILE = "focuskeeper_log"
    private const val KEY = "entries"
    private const val MAX_ENTRIES = 1000

    private val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())

    private fun sp(ctx: Context) =
        ctx.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun add(ctx: Context, message: String) {
        val arr = readArray(ctx)
        val line = "${fmt.format(Date())}  —  $message"
        // newest first
        val newArr = JSONArray()
        newArr.put(line)
        var count = 1
        var i = 0
        while (i < arr.length() && count < MAX_ENTRIES) {
            newArr.put(arr.getString(i))
            count++
            i++
        }
        sp(ctx).edit().putString(KEY, newArr.toString()).apply()
    }

    fun all(ctx: Context): List<String> {
        val arr = readArray(ctx)
        val out = ArrayList<String>(arr.length())
        for (i in 0 until arr.length()) out.add(arr.getString(i))
        return out
    }

    fun clear(ctx: Context) {
        sp(ctx).edit().remove(KEY).apply()
    }

    private fun readArray(ctx: Context): JSONArray {
        val raw = sp(ctx).getString(KEY, null) ?: return JSONArray()
        return try {
            JSONArray(raw)
        } catch (e: Exception) {
            JSONArray()
        }
    }
}
