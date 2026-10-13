package app.cascade.launcher

import android.annotation.SuppressLint
import android.app.ActivityOptions
import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.widget.Toast
import app.cascade.launcher.data.AppEntry
import app.cascade.launcher.notifications.AppNotifications
import app.cascade.launcher.notifications.NotificationStore

/** Thin wrappers around the system calls the UI needs. Every one fails soft with a toast. */
object Actions {

    fun launch(context: Context, app: AppEntry): Boolean = attempt(context) {
        context.getSystemService(LauncherApps::class.java)
            .startMainActivity(app.component, app.user, null, null)
    }

    fun launchShortcut(context: Context, shortcut: ShortcutInfo): Boolean = attempt(context) {
        context.getSystemService(LauncherApps::class.java).startShortcut(shortcut, null, null)
    }

    fun openNotification(context: Context, app: AppEntry, notification: AppNotifications): Boolean {
        val pending = notification.contentIntent ?: return launch(context, app)
        return attempt(context) {
            val options = if (Build.VERSION.SDK_INT >= 34) {
                ActivityOptions.makeBasic()
                    .setPendingIntentBackgroundActivityStartMode(ActivityOptions.MODE_BACKGROUND_ACTIVITY_START_ALLOWED)
                    .toBundle()
            } else {
                null
            }
            pending.send(context, 0, null, null, null, null, options)
            if (notification.autoCancel) NotificationStore.cancel(notification.key)
        }
    }

    fun appInfo(context: Context, app: AppEntry) = attempt(context) {
        context.getSystemService(LauncherApps::class.java)
            .startAppDetailsActivity(app.component, app.user, null, null)
    }

    fun uninstall(context: Context, app: AppEntry) = attempt(context) {
        context.startActivity(
            Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.packageName}"))
                .putExtra(Intent.EXTRA_USER, app.user)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    fun webSearch(context: Context, query: String): Boolean {
        val search = Intent(Intent.ACTION_WEB_SEARCH).putExtra(SearchManager.QUERY, query)
        val fallback = Intent(Intent.ACTION_VIEW, Uri.parse("https://duckduckgo.com/?q=" + Uri.encode(query)))
        return try {
            context.startActivity(search)
            true
        } catch (e: ActivityNotFoundException) {
            attempt(context) { context.startActivity(fallback) }
        }
    }

    fun openAlarms(context: Context) = attempt(context, silent = true) {
        context.startActivity(Intent(AlarmClock.ACTION_SHOW_ALARMS))
    }

    fun openCalendar(context: Context) = attempt(context, silent = true) {
        val uri = CalendarContract.CONTENT_URI.buildUpon().appendPath("time")
            .appendPath(System.currentTimeMillis().toString()).build()
        context.startActivity(Intent(Intent.ACTION_VIEW, uri))
    }

    /** Pulls down the notification shade. Uses the same hidden API every third-party launcher uses. */
    @SuppressLint("WrongConstant")
    fun expandNotifications(context: Context) {
        try {
            val service = context.getSystemService("statusbar") ?: return
            Class.forName("android.app.StatusBarManager").getMethod("expandNotificationsPanel").invoke(service)
        } catch (e: Exception) {
            // Not supported on this device; swipe-down just does nothing.
        }
    }

    private inline fun attempt(context: Context, silent: Boolean = false, block: () -> Unit): Boolean =
        try {
            block()
            true
        } catch (e: Exception) {
            if (!silent) Toast.makeText(context, R.string.action_failed, Toast.LENGTH_SHORT).show()
            false
        }
}
