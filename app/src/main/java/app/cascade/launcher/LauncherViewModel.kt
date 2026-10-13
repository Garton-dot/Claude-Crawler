package app.cascade.launcher

import android.app.Application
import android.content.pm.LauncherApps
import android.content.pm.ShortcutInfo
import android.os.UserHandle
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.cascade.launcher.data.AppEntry
import app.cascade.launcher.data.AppRepository
import app.cascade.launcher.data.LauncherSettings
import app.cascade.launcher.data.Prefs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Screen { Home, Drawer, Settings }

class LauncherViewModel(app: Application) : AndroidViewModel(app) {
    private val prefs = Prefs(app)
    val repository = AppRepository(app)
    private val densityDpi = app.resources.displayMetrics.densityDpi
    private val iconSizePx = (48 * app.resources.displayMetrics.density).toInt()

    private val allApps = MutableStateFlow<List<AppEntry>>(emptyList())
    private val favoriteKeys = MutableStateFlow(prefs.favorites)
    private val hiddenKeys = MutableStateFlow(prefs.hidden)

    private val _icons = MutableStateFlow<Map<String, ImageBitmap>>(emptyMap())
    val icons: StateFlow<Map<String, ImageBitmap>> = _icons.asStateFlow()

    private val _settings = MutableStateFlow(prefs.settings)
    val settings: StateFlow<LauncherSettings> = _settings.asStateFlow()

    private val _screen = MutableStateFlow(Screen.Home)
    val screen: StateFlow<Screen> = _screen.asStateFlow()

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    /** Set when the drawer should grab the keyboard as it opens; cleared once it has. */
    private val _focusSearch = MutableStateFlow(false)
    val focusSearch: StateFlow<Boolean> = _focusSearch.asStateFlow()

    val visibleApps: StateFlow<List<AppEntry>> =
        combine(allApps, hiddenKeys) { apps, hidden -> apps.filter { it.key !in hidden } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val favorites: StateFlow<List<AppEntry>> =
        combine(allApps, favoriteKeys) { apps, keys ->
            val byKey = apps.associateBy { it.key }
            keys.mapNotNull { byKey[it] }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val hiddenApps: StateFlow<List<AppEntry>> =
        combine(allApps, hiddenKeys) { apps, hidden -> apps.filter { it.key in hidden } }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    val favoriteKeySet: StateFlow<Set<String>> =
        combine(favoriteKeys, allApps) { keys, _ -> keys.toSet() }
            .stateIn(viewModelScope, SharingStarted.Eagerly, emptySet())

    /** Search matches: prefix of the label first, then prefix of any word, then anywhere. */
    val searchResults: StateFlow<List<AppEntry>> =
        combine(visibleApps, _query) { apps, q ->
            val needle = AppEntry.fold(q.trim())
            if (needle.isEmpty()) return@combine emptyList<AppEntry>()
            apps.mapNotNull { app ->
                val hay = app.searchKey
                val rank = when {
                    hay.startsWith(needle) -> 0
                    hay.split(' ', '-', '_', '.').any { it.startsWith(needle) } -> 1
                    hay.contains(needle) -> 2
                    initials(hay).startsWith(needle) && needle.length > 1 -> 3
                    else -> return@mapNotNull null
                }
                rank to app
            }.sortedBy { it.first }.map { it.second }
        }.stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

    private var refreshJob: Job? = null

    private val packageCallback = object : LauncherApps.Callback() {
        override fun onPackageRemoved(packageName: String, user: UserHandle) = refresh(packageName)
        override fun onPackageAdded(packageName: String, user: UserHandle) = refresh(packageName)
        override fun onPackageChanged(packageName: String, user: UserHandle) = refresh(packageName)
        override fun onPackagesAvailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) =
            refresh(*packageNames)
        override fun onPackagesUnavailable(packageNames: Array<out String>, user: UserHandle, replacing: Boolean) =
            refresh(*packageNames)
    }

    init {
        app.getSystemService(LauncherApps::class.java).registerCallback(packageCallback)
        refresh()
    }

    override fun onCleared() {
        getApplication<Application>().getSystemService(LauncherApps::class.java)
            .unregisterCallback(packageCallback)
    }

    /** Reloads the app list. Icons of [changedPackages] are reloaded too; others are reused. */
    fun refresh(vararg changedPackages: String) {
        refreshJob?.cancel()
        refreshJob = viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) { repository.loadApps(prefs.renames) }
            val changed = changedPackages.toSet()
            val reusable = apps.filter { it.packageName !in changed }.mapTo(HashSet()) { it.key }
            val kept = _icons.value.filterKeys { it in reusable }
            _icons.value = kept
            allApps.value = apps

            // Load missing icons in small batches so the list fills in progressively.
            val missing = apps.filter { it.key !in kept }
            withContext(Dispatchers.IO) {
                missing.chunked(16).forEach { batch ->
                    val loaded = batch.mapNotNull { app ->
                        repository.loadIcon(app.key, densityDpi)
                            ?.toBitmap(iconSizePx, iconSizePx)
                            ?.asImageBitmap()
                            ?.let { app.key to it }
                    }
                    _icons.value = _icons.value + loaded
                }
            }
        }
    }

    fun show(screen: Screen, focusSearch: Boolean = false) {
        if (screen != Screen.Drawer) _query.value = ""
        _screen.value = screen
        _focusSearch.value = focusSearch
    }

    fun searchFocused() {
        _focusSearch.value = false
    }

    /** Called when the home button is pressed while we're already in front. */
    fun onHomePressed() = show(Screen.Home)

    /** After launching something, quietly reset so the launcher is clean when the user returns. */
    fun resetAfterLaunch() {
        viewModelScope.launch {
            delay(400)
            show(Screen.Home)
        }
    }

    fun setQuery(value: String) {
        _query.value = value
    }

    fun isFavorite(app: AppEntry) = app.key in favoriteKeys.value

    fun toggleFavorite(app: AppEntry) {
        val current = favoriteKeys.value
        favoriteKeys.value = if (app.key in current) current - app.key else current + app.key
        prefs.favorites = favoriteKeys.value
    }

    fun moveFavorite(app: AppEntry, delta: Int) {
        // Work on the favorites that still exist, so uninstalled entries don't block moves.
        val list = favorites.value.map { it.key }.toMutableList()
        val from = list.indexOf(app.key)
        val to = from + delta
        if (from < 0 || to !in list.indices) return
        list.add(to, list.removeAt(from))
        favoriteKeys.value = list
        prefs.favorites = list
    }

    fun hide(app: AppEntry) {
        hiddenKeys.value = hiddenKeys.value + app.key
        prefs.hidden = hiddenKeys.value
        if (app.key in favoriteKeys.value) toggleFavorite(app)
    }

    fun unhide(app: AppEntry) {
        hiddenKeys.value = hiddenKeys.value - app.key
        prefs.hidden = hiddenKeys.value
    }

    fun rename(app: AppEntry, label: String?) {
        prefs.setRename(app.key, label?.takeIf { it.trim() != app.originalLabel })
        refresh()
    }

    fun updateSettings(transform: (LauncherSettings) -> LauncherSettings) {
        _settings.value = transform(_settings.value)
        prefs.settings = _settings.value
    }

    suspend fun shortcutsFor(app: AppEntry): List<ShortcutInfo> =
        withContext(Dispatchers.IO) { repository.shortcutsFor(app) }

    suspend fun shortcutIcon(shortcut: ShortcutInfo): ImageBitmap? = withContext(Dispatchers.IO) {
        repository.shortcutIcon(shortcut, densityDpi)?.toBitmap(iconSizePx, iconSizePx)?.asImageBitmap()
    }

    private fun initials(text: String) =
        text.split(' ', '-', '_', '.').filter { it.isNotEmpty() }.joinToString("") { it.take(1) }
}
