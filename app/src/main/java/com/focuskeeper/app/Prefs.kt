package com.focuskeeper.app

import android.content.Context

/** Small SharedPreferences wrapper for the blocked-app set and focus state. */
object Prefs {
    private const val FILE = "focuskeeper_prefs"
    private const val KEY_BLOCKED = "blocked_packages"
    private const val KEY_FOCUS = "focus_active"

    private fun sp(ctx: Context) =
        ctx.applicationContext.getSharedPreferences(FILE, Context.MODE_PRIVATE)

    fun getBlocked(ctx: Context): MutableSet<String> =
        HashSet(sp(ctx).getStringSet(KEY_BLOCKED, emptySet()) ?: emptySet())

    fun isBlocked(ctx: Context, pkg: String): Boolean =
        sp(ctx).getStringSet(KEY_BLOCKED, emptySet())?.contains(pkg) == true

    fun setBlocked(ctx: Context, pkg: String, blocked: Boolean) {
        val set = getBlocked(ctx)
        if (blocked) set.add(pkg) else set.remove(pkg)
        sp(ctx).edit().putStringSet(KEY_BLOCKED, set).apply()
    }

    fun isFocusActive(ctx: Context): Boolean =
        sp(ctx).getBoolean(KEY_FOCUS, false)

    fun setFocusActive(ctx: Context, active: Boolean) {
        sp(ctx).edit().putBoolean(KEY_FOCUS, active).apply()
    }
}
