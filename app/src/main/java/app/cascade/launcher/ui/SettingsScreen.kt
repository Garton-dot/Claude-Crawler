package app.cascade.launcher.ui

import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import app.cascade.launcher.data.AppEntry
import app.cascade.launcher.data.LauncherSettings

@Composable
fun SettingsScreen(
    settings: LauncherSettings,
    hiddenApps: List<AppEntry>,
    icons: Map<String, ImageBitmap>,
    onChange: ((LauncherSettings) -> LauncherSettings) -> Unit,
    onUnhide: (AppEntry) -> Unit,
    onBack: () -> Unit,
) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var notificationAccess by remember { mutableStateOf(false) }
    var isDefaultLauncher by remember { mutableStateOf(false) }

    // Re-check permissions every time we come back from the system settings pages.
    LaunchedEffect(lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            notificationAccess = context.packageName in NotificationManagerCompat.getEnabledListenerPackages(context)
            isDefaultLauncher = context.packageManager
                .resolveActivity(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)
                ?.activityInfo?.packageName == context.packageName
        }
    }

    val roleRequest = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {}
    fun requestDefaultLauncher() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val roles = context.getSystemService(RoleManager::class.java)
            if (roles.isRoleAvailable(RoleManager.ROLE_HOME) && !roles.isRoleHeld(RoleManager.ROLE_HOME)) {
                roleRequest.launch(roles.createRequestRoleIntent(RoleManager.ROLE_HOME))
                return
            }
        }
        try {
            context.startActivity(Intent(Settings.ACTION_HOME_SETTINGS))
        } catch (e: Exception) {
            context.startActivity(Intent(Settings.ACTION_SETTINGS))
        }
    }

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Text("Cascade settings", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Medium)
            }
        }

        item { Header("Setup") }
        item {
            ActionRow(
                title = "Default home app",
                summary = if (isDefaultLauncher) "Cascade is your home screen" else "Make Cascade your home screen",
                action = if (isDefaultLauncher) null else "Set",
                onClick = ::requestDefaultLauncher,
            )
        }
        item {
            ActionRow(
                title = "Notification dots & previews",
                summary = if (notificationAccess) "Access granted" else "Needs notification access (stays on your phone)",
                action = if (notificationAccess) "Manage" else "Grant",
                onClick = { context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS)) },
            )
        }

        item { Header("Look & feel") }
        item { Toggle("Show app icons", settings.showIcons) { v -> onChange { it.copy(showIcons = v) } } }
        item { Toggle("Show clock and date", settings.showClock) { v -> onChange { it.copy(showClock = v) } } }
        item { Toggle("Dim wallpaper", settings.dimWallpaper) { v -> onChange { it.copy(dimWallpaper = v) } } }
        item {
            Toggle("Notification previews on favorites", settings.notificationPreviews) { v ->
                onChange { it.copy(notificationPreviews = v) }
            }
        }

        item { Header("Gestures") }
        item {
            Toggle("Swipe down for notifications", settings.swipeDownForNotifications) { v ->
                onChange { it.copy(swipeDownForNotifications = v) }
            }
        }
        item {
            Toggle("Open keyboard on swipe up", settings.autoOpenKeyboard) { v ->
                onChange { it.copy(autoOpenKeyboard = v) }
            }
        }

        item { Header("Hidden apps") }
        if (hiddenApps.isEmpty()) {
            item {
                Text(
                    "None. Long-press an app and choose “Hide” to remove it from the list.",
                    color = Color.White.copy(alpha = 0.6f),
                    fontSize = 14.sp,
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
                )
            }
        }
        items(hiddenApps, key = { it.key }) { app ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AppIcon(app, icons[app.key], 32.dp)
                Spacer(Modifier.width(14.dp))
                Text(app.label, color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
                TextButton(onClick = { onUnhide(app) }) { Text("Unhide") }
            }
        }

        item {
            Text(
                "Cascade is free and open source. Every feature is included — no Pro tier, no ads, no tracking.",
                color = Color.White.copy(alpha = 0.5f),
                fontSize = 13.sp,
                modifier = Modifier.padding(24.dp),
            )
        }
    }
}

@Composable
private fun Header(text: String) {
    Text(
        text,
        color = Accent,
        fontSize = 14.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 4.dp),
    )
}

@Composable
private fun Toggle(title: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable { onChange(!checked) }
            .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(title, color = Color.White, fontSize = 16.sp, modifier = Modifier.weight(1f))
        Switch(checked = checked, onCheckedChange = onChange)
    }
}

@Composable
private fun ActionRow(title: String, summary: String, action: String?, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = Color.White, fontSize = 16.sp)
            Text(summary, color = Color.White.copy(alpha = 0.6f), fontSize = 13.sp)
        }
        if (action != null) TextButton(onClick = onClick) { Text(action) }
    }
}
