package app.cascade.launcher.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.cascade.launcher.Actions
import app.cascade.launcher.LauncherViewModel
import app.cascade.launcher.Screen
import app.cascade.launcher.data.AppEntry
import app.cascade.launcher.notifications.AppNotifications
import app.cascade.launcher.notifications.NotificationStore
import kotlinx.coroutines.launch

@Composable
fun LauncherRoot(viewModel: LauncherViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val screen by viewModel.screen.collectAsStateWithLifecycle()
    val apps by viewModel.visibleApps.collectAsStateWithLifecycle()
    val favorites by viewModel.favorites.collectAsStateWithLifecycle()
    val favoriteKeys by viewModel.favoriteKeySet.collectAsStateWithLifecycle()
    val hiddenApps by viewModel.hiddenApps.collectAsStateWithLifecycle()
    val icons by viewModel.icons.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val focusSearch by viewModel.focusSearch.collectAsStateWithLifecycle()
    val notifications by NotificationStore.state.collectAsStateWithLifecycle()

    val drawerList = rememberLazyListState()
    var sheetApp by remember { mutableStateOf<AppEntry?>(null) }

    val launch: (AppEntry) -> Unit = { app ->
        if (Actions.launch(context, app)) viewModel.resetAfterLaunch()
    }
    val openNotification: (AppEntry, AppNotifications) -> Unit = { app, n ->
        if (Actions.openNotification(context, app, n)) viewModel.resetAfterLaunch()
    }

    // Home swallows back (a launcher has nowhere to go back to); other screens return home.
    BackHandler { if (screen != Screen.Home) viewModel.show(Screen.Home) }

    val letters = remember(apps) { apps.map { it.section }.distinct() }
    val firstIndexOfLetter = remember(apps) {
        HashMap<Char, Int>().apply { apps.forEachIndexed { i, app -> putIfAbsent(app.section, i) } }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(
                if (settings.dimWallpaper) {
                    Brush.verticalGradient(listOf(Color(0x33000000), Color(0x66000000), Color(0xAA000000)))
                } else {
                    Brush.verticalGradient(listOf(Color.Transparent, Color(0x55000000)))
                }
            )
            .systemBarsPadding()
            .imePadding()
    ) {
        AnimatedContent(
            targetState = screen,
            transitionSpec = {
                (fadeIn() + slideInVertically { it / 12 }) togetherWith fadeOut()
            },
            label = "screen",
        ) { target ->
            when (target) {
                Screen.Home -> HomeScreen(
                    favorites = favorites,
                    icons = icons,
                    notifications = notifications,
                    settings = settings,
                    onLaunch = launch,
                    onOpenNotification = openNotification,
                    onLongPressApp = { sheetApp = it },
                    onOpenDrawer = { focus ->
                        scope.launch { drawerList.scrollToItem(0) }
                        viewModel.show(Screen.Drawer, focusSearch = focus)
                    },
                    onOpenSettings = { viewModel.show(Screen.Settings) },
                )

                Screen.Drawer -> DrawerScreen(
                    apps = apps,
                    searchResults = searchResults,
                    query = query,
                    focusSearch = focusSearch,
                    onFocusConsumed = viewModel::searchFocused,
                    listState = drawerList,
                    icons = icons,
                    notifications = notifications,
                    settings = settings,
                    onQueryChange = viewModel::setQuery,
                    onLaunch = launch,
                    onOpenNotification = openNotification,
                    onLongPressApp = { sheetApp = it },
                    onWebSearch = { q -> if (Actions.webSearch(context, q)) viewModel.resetAfterLaunch() },
                    onOpenSettings = { viewModel.show(Screen.Settings) },
                    onClose = { viewModel.show(Screen.Home) },
                )

                Screen.Settings -> SettingsScreen(
                    settings = settings,
                    hiddenApps = hiddenApps,
                    icons = icons,
                    onChange = viewModel::updateSettings,
                    onUnhide = viewModel::unhide,
                    onBack = { viewModel.show(Screen.Home) },
                )
            }
        }

        // The scroller sits above both home and drawer, so one drag can start on the home
        // screen and keep going through the full list.
        if (screen != Screen.Settings && query.isBlank()) {
            AlphabetScroller(
                letters = letters,
                onActiveChange = { active ->
                    if (active && screen == Screen.Home) viewModel.show(Screen.Drawer)
                },
                onLetter = { letter ->
                    firstIndexOfLetter[letter]?.let { scope.launch { drawerList.scrollToItem(it) } }
                },
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(top = 72.dp, bottom = 96.dp),
            )
        }
    }

    sheetApp?.let { app ->
        AppSheet(
            app = app,
            icon = icons[app.key],
            isFavorite = app.key in favoriteKeys,
            favoriteIndex = favorites.indexOfFirst { it.key == app.key },
            favoriteCount = favorites.size,
            viewModel = viewModel,
            onDismiss = { sheetApp = null },
            onLaunched = { viewModel.resetAfterLaunch() },
        )
    }
}
