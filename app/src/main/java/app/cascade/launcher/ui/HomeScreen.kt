package app.cascade.launcher.ui

import android.text.format.DateFormat
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cascade.launcher.Actions
import app.cascade.launcher.data.AppEntry
import app.cascade.launcher.data.LauncherSettings
import app.cascade.launcher.notifications.AppNotifications
import kotlinx.coroutines.delay
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun HomeScreen(
    favorites: List<AppEntry>,
    icons: Map<String, ImageBitmap>,
    notifications: Map<String, AppNotifications>,
    settings: LauncherSettings,
    onLaunch: (AppEntry) -> Unit,
    onOpenNotification: (AppEntry, AppNotifications) -> Unit,
    onLongPressApp: (AppEntry) -> Unit,
    onOpenDrawer: (focusSearch: Boolean) -> Unit,
    onOpenSettings: () -> Unit,
) {
    val context = LocalContext.current
    val thresholdPx = with(LocalDensity.current) { 72.dp.toPx() }
    val currentSettings by rememberUpdatedState(settings)
    val currentOnOpenDrawer by rememberUpdatedState(onOpenDrawer)

    Column(
        Modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onDragEnd = {
                        when {
                            total < -thresholdPx -> currentOnOpenDrawer(currentSettings.autoOpenKeyboard)
                            total > thresholdPx && currentSettings.swipeDownForNotifications ->
                                Actions.expandNotifications(context)
                        }
                    },
                    onVerticalDrag = { _, dy -> total += dy },
                )
            }
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {},
                onLongClick = onOpenSettings,
            )
            .padding(start = 20.dp, end = 44.dp, top = 24.dp, bottom = 16.dp),
    ) {
        if (settings.showClock) Clock()

        Spacer(Modifier.weight(1f))

        if (favorites.isEmpty()) {
            Text(
                "Swipe up or slide along the letters on the right to see all apps.\n\n" +
                    "Long-press any app to add it here.\n\nLong-press empty space for settings.",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 15.sp,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                favorites.forEach { app ->
                    val notification = notifications[app.packageName]
                    AppRow(
                        app = app,
                        icon = icons[app.key],
                        showIcon = settings.showIcons,
                        notification = notification,
                        showPreview = settings.notificationPreviews,
                        large = true,
                        onClick = { onLaunch(app) },
                        onLongClick = { onLongPressApp(app) },
                        onNotificationClick = { notification?.let { onOpenNotification(app, it) } },
                    )
                }
            }
        }

        Spacer(Modifier.height(28.dp))
        SearchPill(onClick = { onOpenDrawer(true) })
    }
}

@Composable
private fun Clock() {
    val context = LocalContext.current
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = System.currentTimeMillis()
            delay(60_000 - now % 60_000)
        }
    }
    val date = Date(now)
    Column(Modifier.padding(horizontal = 12.dp, vertical = 16.dp)) {
        Text(
            DateFormat.getTimeFormat(context).format(date),
            color = Color.White,
            fontSize = 56.sp,
            fontWeight = FontWeight.Light,
            modifier = Modifier.clickable { Actions.openAlarms(context) },
        )
        Text(
            DateFormat.format(DateFormat.getBestDateTimePattern(Locale.getDefault(), "EEEEMMMMd"), date).toString(),
            color = Color.White.copy(alpha = 0.8f),
            fontSize = 16.sp,
            modifier = Modifier.clickable { Actions.openCalendar(context) },
        )
    }
}

@Composable
private fun SearchPill(onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clip(MaterialShapes.pill)
            .background(Color.White.copy(alpha = 0.12f))
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text("Search apps and the web", color = Color.White.copy(alpha = 0.75f), fontSize = 15.sp)
    }
}
