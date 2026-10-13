package app.cascade.launcher.notifications

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** What the launcher shows next to an app: a dot, a count, and a one-line preview. */
data class AppNotifications(
    val count: Int,
    val title: String?,
    val text: String?,
    val key: String,
    val contentIntent: android.app.PendingIntent?,
    val autoCancel: Boolean,
)

object NotificationStore {
    private val _state = MutableStateFlow<Map<String, AppNotifications>>(emptyMap())
    val state: StateFlow<Map<String, AppNotifications>> = _state.asStateFlow()

    @Volatile
    internal var listener: NotificationListener? = null

    internal fun publish(value: Map<String, AppNotifications>) {
        _state.value = value
    }

    fun cancel(key: String) {
        try {
            listener?.cancelNotification(key)
        } catch (e: Exception) {
            // Listener disconnected in the meantime; nothing to cancel.
        }
    }
}

/** Optional: once the user grants notification access, feeds dots and previews to the launcher. */
class NotificationListener : NotificationListenerService() {

    override fun onListenerConnected() {
        NotificationStore.listener = this
        refresh()
    }

    override fun onListenerDisconnected() {
        NotificationStore.listener = null
        NotificationStore.publish(emptyMap())
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) = refresh()

    override fun onNotificationRemoved(sbn: StatusBarNotification?) = refresh()

    private fun refresh() {
        val active = try {
            activeNotifications
        } catch (e: Exception) {
            null
        } ?: return

        val relevant = active.filter {
            val flags = it.notification.flags
            flags and Notification.FLAG_ONGOING_EVENT == 0 && flags and Notification.FLAG_FOREGROUND_SERVICE == 0
        }
        NotificationStore.publish(
            relevant.groupBy { it.packageName }.mapValues { (_, list) ->
                val real = list.filter { it.notification.flags and Notification.FLAG_GROUP_SUMMARY == 0 }
                val counted = real.ifEmpty { list }
                val latest = counted.maxBy { it.postTime }
                val extras = latest.notification.extras
                AppNotifications(
                    count = counted.size,
                    title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString(),
                    text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString(),
                    key = latest.key,
                    contentIntent = latest.notification.contentIntent,
                    autoCancel = latest.notification.flags and Notification.FLAG_AUTO_CANCEL != 0,
                )
            }
        )
    }
}
