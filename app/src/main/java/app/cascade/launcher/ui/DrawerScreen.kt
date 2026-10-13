package app.cascade.launcher.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cascade.launcher.data.AppEntry
import app.cascade.launcher.data.LauncherSettings
import app.cascade.launcher.notifications.AppNotifications
import kotlinx.coroutines.flow.filter

@Composable
fun DrawerScreen(
    apps: List<AppEntry>,
    searchResults: List<AppEntry>,
    query: String,
    focusSearch: Boolean,
    onFocusConsumed: () -> Unit,
    listState: LazyListState,
    icons: Map<String, ImageBitmap>,
    notifications: Map<String, AppNotifications>,
    settings: LauncherSettings,
    onQueryChange: (String) -> Unit,
    onLaunch: (AppEntry) -> Unit,
    onOpenNotification: (AppEntry, AppNotifications) -> Unit,
    onLongPressApp: (AppEntry) -> Unit,
    onWebSearch: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onClose: () -> Unit,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val focus = remember { FocusRequester() }
    val searching = query.isNotBlank()

    LaunchedEffect(focusSearch) {
        if (focusSearch) {
            focus.requestFocus()
            onFocusConsumed()
        }
    }
    // Scrolling the list puts the keyboard away.
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }.filter { it }.collect { keyboard?.hide() }
    }

    // Pulling down past the top of the list closes the drawer.
    val closeThresholdPx = with(LocalDensity.current) { 96.dp.toPx() }
    val pullToClose = remember(onClose) {
        object : NestedScrollConnection {
            var pulled = 0f
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y > 0f) {
                    pulled += available.y
                    if (pulled > closeThresholdPx) {
                        pulled = 0f
                        onClose()
                    }
                } else if (consumed.y != 0f) {
                    pulled = 0f
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                pulled = 0f
                return Velocity.Zero
            }
        }
    }

    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                placeholder = { Text("Search") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (query.isNotEmpty()) {
                        IconButton(onClick = { onQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go),
                keyboardActions = KeyboardActions(onGo = {
                    val first = searchResults.firstOrNull()
                    when {
                        first != null -> onLaunch(first)
                        query.isNotBlank() -> onWebSearch(query)
                    }
                }),
                shape = MaterialShapes.pill,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.White.copy(alpha = 0.14f),
                    unfocusedContainerColor = Color.White.copy(alpha = 0.10f),
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    cursorColor = Accent,
                ),
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focus),
            )
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Default.Settings, contentDescription = "Launcher settings", tint = Color.White.copy(alpha = 0.8f))
            }
        }

        Box(Modifier.weight(1f)) {
            if (searching) {
                LazyColumn(
                    contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = 24.dp),
                    modifier = Modifier.fillMaxSize(),
                ) {
                    itemsIndexed(searchResults, key = { _, app -> app.key }) { _, app ->
                        DrawerRow(app, icons, notifications, settings, onLaunch, onOpenNotification, onLongPressApp)
                    }
                    item(key = "web") {
                        Text(
                            "Search the web for “$query”",
                            color = Accent,
                            fontSize = 16.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(MaterialShapes.row)
                                .clickable { onWebSearch(query) }
                                .padding(horizontal = 12.dp, vertical = 14.dp),
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    contentPadding = PaddingValues(start = 8.dp, end = 44.dp, bottom = 24.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(pullToClose),
                ) {
                    itemsIndexed(apps, key = { _, app -> app.key }) { index, app ->
                        val firstOfSection = index == 0 || apps[index - 1].section != app.section
                        DrawerRow(
                            app, icons, notifications, settings, onLaunch, onOpenNotification, onLongPressApp,
                            sectionLetter = if (firstOfSection) app.section else null,
                            showSectionGutter = true,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DrawerRow(
    app: AppEntry,
    icons: Map<String, ImageBitmap>,
    notifications: Map<String, AppNotifications>,
    settings: LauncherSettings,
    onLaunch: (AppEntry) -> Unit,
    onOpenNotification: (AppEntry, AppNotifications) -> Unit,
    onLongPressApp: (AppEntry) -> Unit,
    sectionLetter: Char? = null,
    showSectionGutter: Boolean = false,
) {
    val notification = notifications[app.packageName]
    AppRow(
        app = app,
        icon = icons[app.key],
        showIcon = settings.showIcons,
        notification = notification,
        showPreview = false,
        large = false,
        onClick = { onLaunch(app) },
        onLongClick = { onLongPressApp(app) },
        onNotificationClick = { notification?.let { onOpenNotification(app, it) } },
        sectionLetter = sectionLetter,
        showSectionGutter = showSectionGutter,
    )
}
