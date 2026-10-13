package app.cascade.launcher.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cascade.launcher.data.AppEntry
import app.cascade.launcher.notifications.AppNotifications

/**
 * One app in a list. [large] is the home-screen favorites style; the drawer uses the compact one.
 * [sectionLetter] is drawn in a gutter on the first app of each letter in the drawer.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppRow(
    app: AppEntry,
    icon: ImageBitmap?,
    showIcon: Boolean,
    notification: AppNotifications?,
    showPreview: Boolean,
    large: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onNotificationClick: () -> Unit,
    modifier: Modifier = Modifier,
    sectionLetter: Char? = null,
    showSectionGutter: Boolean = false,
) {
    val iconSize: Dp = if (large) 44.dp else 36.dp
    Row(
        modifier
            .fillMaxWidth()
            .clip(MaterialShapes.row)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
            .padding(horizontal = 12.dp, vertical = if (large) 9.dp else 7.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showSectionGutter) {
            Text(
                text = sectionLetter?.toString().orEmpty(),
                color = Accent,
                fontSize = 15.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.width(22.dp),
            )
        }
        if (showIcon) {
            AppIcon(app, icon, iconSize)
            Spacer(Modifier.width(14.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.Center) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = app.label,
                    color = Color.White,
                    fontSize = if (large) 20.sp else 17.sp,
                    fontWeight = if (large) FontWeight.Medium else FontWeight.Normal,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (notification != null) {
                    Spacer(Modifier.width(8.dp))
                    Box(
                        Modifier
                            .size(7.dp)
                            .background(Accent, CircleShape)
                    )
                    if (notification.count > 1) {
                        Text(
                            text = notification.count.toString(),
                            color = Accent,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(start = 4.dp),
                        )
                    }
                }
            }
            val preview = notification?.let { listOfNotNull(it.title, it.text).joinToString(": ") }
            if (showPreview && !preview.isNullOrBlank()) {
                Text(
                    text = preview,
                    color = Color.White.copy(alpha = 0.7f),
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .clip(MaterialShapes.row)
                        .combinedClickable(onClick = onNotificationClick, onLongClick = onLongClick),
                )
            }
        }
    }
}

@Composable
fun AppIcon(app: AppEntry, icon: ImageBitmap?, size: Dp) {
    if (icon != null) {
        Image(bitmap = icon, contentDescription = null, modifier = Modifier.size(size))
    } else {
        // Placeholder until the real icon has loaded.
        Box(
            Modifier
                .size(size)
                .background(Color.White.copy(alpha = 0.15f), CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(app.label.take(1).uppercase(), color = Color.White, fontSize = (size.value * 0.4f).sp)
        }
    }
}
