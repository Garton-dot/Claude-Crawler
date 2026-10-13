package app.cascade.launcher.ui

import android.content.pm.ShortcutInfo
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cascade.launcher.Actions
import app.cascade.launcher.LauncherViewModel
import app.cascade.launcher.data.AppEntry

/** Long-press menu: app shortcuts on top, then launcher actions. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSheet(
    app: AppEntry,
    icon: ImageBitmap?,
    isFavorite: Boolean,
    favoriteIndex: Int,
    favoriteCount: Int,
    viewModel: LauncherViewModel,
    onDismiss: () -> Unit,
    onLaunched: () -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var renaming by remember { mutableStateOf(false) }
    val shortcuts by produceState(emptyList<ShortcutInfo>(), app.key) { value = viewModel.shortcutsFor(app) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(bottom = 12.dp)
        ) {
            Row(Modifier.padding(horizontal = 24.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                AppIcon(app, icon, 40.dp)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(app.label, fontSize = 20.sp, fontWeight = FontWeight.Medium, color = Color.White)
                    if (app.label != app.originalLabel || app.isWorkProfile) {
                        Text(
                            listOfNotNull(
                                app.originalLabel.takeIf { it != app.label },
                                "Work profile".takeIf { app.isWorkProfile },
                            ).joinToString(" · "),
                            fontSize = 13.sp,
                            color = Color.White.copy(alpha = 0.6f),
                        )
                    }
                }
            }

            if (shortcuts.isNotEmpty()) {
                shortcuts.forEach { shortcut ->
                    val shortcutIcon by produceState<ImageBitmap?>(null, shortcut.id) {
                        value = viewModel.shortcutIcon(shortcut)
                    }
                    SheetItem(
                        text = (shortcut.shortLabel ?: shortcut.longLabel ?: shortcut.id).toString(),
                        icon = shortcutIcon,
                    ) {
                        if (Actions.launchShortcut(context, shortcut)) onLaunched()
                        onDismiss()
                    }
                }
                HorizontalDivider(Modifier.padding(vertical = 6.dp), color = Color.White.copy(alpha = 0.1f))
            }

            SheetItem(if (isFavorite) "Remove from favorites" else "Add to favorites") {
                viewModel.toggleFavorite(app)
                onDismiss()
            }
            if (isFavorite && favoriteIndex > 0) {
                SheetItem("Move up") { viewModel.moveFavorite(app, -1) }
            }
            if (isFavorite && favoriteIndex in 0 until favoriteCount - 1) {
                SheetItem("Move down") { viewModel.moveFavorite(app, +1) }
            }
            SheetItem("Rename") { renaming = true }
            SheetItem("Hide from app list") {
                viewModel.hide(app)
                onDismiss()
            }
            SheetItem("App info") {
                Actions.appInfo(context, app)
                onDismiss()
            }
            SheetItem("Uninstall") {
                Actions.uninstall(context, app)
                onDismiss()
            }
        }
    }

    if (renaming) {
        RenameDialog(
            app = app,
            onDismiss = { renaming = false },
            onRename = {
                viewModel.rename(app, it)
                renaming = false
                onDismiss()
            },
        )
    }
}

@Composable
private fun SheetItem(text: String, icon: ImageBitmap? = null, onClick: () -> Unit) {
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (icon != null) {
            Image(icon, contentDescription = null, modifier = Modifier.size(24.dp))
            Spacer(Modifier.width(16.dp))
        }
        Text(text, fontSize = 16.sp, color = Color.White)
    }
}

@Composable
private fun RenameDialog(app: AppEntry, onDismiss: () -> Unit, onRename: (String?) -> Unit) {
    var text by remember { mutableStateOf(app.label) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocus() }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                placeholder = { Text(app.originalLabel) },
                modifier = Modifier.focusRequester(focus),
            )
        },
        confirmButton = { TextButton(onClick = { onRename(text) }) { Text("Save") } },
        dismissButton = {
            Row {
                TextButton(onClick = { onRename(null) }) { Text("Reset") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        },
    )
}
