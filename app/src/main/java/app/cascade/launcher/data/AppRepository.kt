package app.cascade.launcher.data

import android.content.Context
import android.content.pm.LauncherActivityInfo
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.graphics.drawable.Drawable
import android.os.Process
import android.os.UserManager
import java.text.Collator

/** Reads launchable apps and their shortcuts from the system. */
class AppRepository(private val context: Context) {
    private val launcherApps = context.getSystemService(LauncherApps::class.java)
    private val userManager = context.getSystemService(UserManager::class.java)

    /** Raw activity infos from the last [loadApps] call, kept so icons can be loaded lazily. */
    private val activityInfos = HashMap<String, LauncherActivityInfo>()

    fun loadApps(renames: Map<String, String>): List<AppEntry> {
        val me = Process.myUserHandle()
        val infos = HashMap<String, LauncherActivityInfo>()
        val apps = userManager.userProfiles.flatMap { user ->
            val serial = userManager.getSerialNumberForUser(user)
            val activities = try {
                launcherApps.getActivityList(null, user)
            } catch (e: SecurityException) {
                emptyList()
            }
            activities
                .filter { it.componentName.packageName != context.packageName }
                .map { info ->
                    val key = "${info.componentName.flattenToString()}#$serial"
                    infos[key] = info
                    val original = info.label?.toString()?.trim().orEmpty()
                        .ifEmpty { info.componentName.packageName }
                    AppEntry(
                        key = key,
                        component = info.componentName,
                        user = user,
                        originalLabel = original,
                        label = renames[key] ?: original,
                        isWorkProfile = user != me,
                    )
                }
        }
        synchronized(activityInfos) {
            activityInfos.clear()
            activityInfos.putAll(infos)
        }
        val collator = Collator.getInstance().apply { strength = Collator.PRIMARY }
        return apps.sortedWith { a, b ->
            // '#' apps (digits, symbols, other scripts) go last, like the scroller.
            val bySection = (a.section == '#').compareTo(b.section == '#')
            if (bySection != 0) bySection else collator.compare(a.label, b.label)
        }
    }

    fun loadIcon(key: String, densityDpi: Int): Drawable? {
        val info = synchronized(activityInfos) { activityInfos[key] } ?: return null
        return try {
            info.getBadgedIcon(densityDpi)
        } catch (e: Exception) {
            null
        }
    }

    fun hasShortcutPermission(): Boolean = try {
        launcherApps.hasShortcutHostPermission()
    } catch (e: Exception) {
        false
    }

    /** App shortcuts (long-press actions) — only available while we are the default launcher. */
    fun shortcutsFor(app: AppEntry): List<ShortcutInfo> {
        if (!hasShortcutPermission()) return emptyList()
        val query = LauncherApps.ShortcutQuery()
            .setPackage(app.packageName)
            .setActivity(app.component)
            .setQueryFlags(
                LauncherApps.ShortcutQuery.FLAG_MATCH_DYNAMIC or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_MANIFEST or
                    LauncherApps.ShortcutQuery.FLAG_MATCH_PINNED
            )
        return try {
            launcherApps.getShortcuts(query, app.user).orEmpty()
                .filter { it.isEnabled }
                .distinctBy { it.id }
                .sortedWith(compareBy({ !it.isDeclaredInManifest }, { it.rank }))
                .take(MAX_SHORTCUTS)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun shortcutIcon(shortcut: ShortcutInfo, densityDpi: Int): Drawable? = try {
        launcherApps.getShortcutIconDrawable(shortcut, densityDpi)
    } catch (e: Exception) {
        null
    }

    companion object {
        const val MAX_SHORTCUTS = 6
    }
}
