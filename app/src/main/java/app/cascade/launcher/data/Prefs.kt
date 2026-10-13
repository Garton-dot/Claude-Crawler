package app.cascade.launcher.data

import android.content.Context

data class LauncherSettings(
    val showIcons: Boolean = true,
    val showClock: Boolean = true,
    val notificationPreviews: Boolean = true,
    val swipeDownForNotifications: Boolean = true,
    val autoOpenKeyboard: Boolean = false,
    val dimWallpaper: Boolean = true,
    val dynamicColor: Boolean = true,
)

/** Everything the launcher remembers, in one SharedPreferences file. */
class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("cascade", Context.MODE_PRIVATE)

    var favorites: List<String>
        get() = sp.getString(KEY_FAVORITES, "").orEmpty().split('\n').filter { it.isNotEmpty() }
        set(value) = sp.edit().putString(KEY_FAVORITES, value.joinToString("\n")).apply()

    var hidden: Set<String>
        get() = sp.getStringSet(KEY_HIDDEN, emptySet()).orEmpty().toSet()
        set(value) = sp.edit().putStringSet(KEY_HIDDEN, value).apply()

    val renames: Map<String, String>
        get() = sp.all.mapNotNull { (k, v) ->
            if (k.startsWith(RENAME_PREFIX) && v is String) k.removePrefix(RENAME_PREFIX) to v else null
        }.toMap()

    fun setRename(key: String, label: String?) {
        sp.edit().apply {
            if (label.isNullOrBlank()) remove(RENAME_PREFIX + key) else putString(RENAME_PREFIX + key, label.trim())
        }.apply()
    }

    var settings: LauncherSettings
        get() = LauncherSettings(
            showIcons = sp.getBoolean("showIcons", true),
            showClock = sp.getBoolean("showClock", true),
            notificationPreviews = sp.getBoolean("notificationPreviews", true),
            swipeDownForNotifications = sp.getBoolean("swipeDownForNotifications", true),
            autoOpenKeyboard = sp.getBoolean("autoOpenKeyboard", false),
            dimWallpaper = sp.getBoolean("dimWallpaper", true),
            dynamicColor = sp.getBoolean("dynamicColor", true),
        )
        set(value) = sp.edit()
            .putBoolean("showIcons", value.showIcons)
            .putBoolean("showClock", value.showClock)
            .putBoolean("notificationPreviews", value.notificationPreviews)
            .putBoolean("swipeDownForNotifications", value.swipeDownForNotifications)
            .putBoolean("autoOpenKeyboard", value.autoOpenKeyboard)
            .putBoolean("dimWallpaper", value.dimWallpaper)
            .putBoolean("dynamicColor", value.dynamicColor)
            .apply()

    private companion object {
        const val KEY_FAVORITES = "favorites"
        const val KEY_HIDDEN = "hidden"
        const val RENAME_PREFIX = "rename:"
    }
}
