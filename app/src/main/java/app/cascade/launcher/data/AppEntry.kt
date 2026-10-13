package app.cascade.launcher.data

import android.content.ComponentName
import android.os.UserHandle
import java.text.Normalizer

/** One launchable activity, for one user profile (personal or work). */
data class AppEntry(
    val key: String,
    val component: ComponentName,
    val user: UserHandle,
    val originalLabel: String,
    val label: String,
    val isWorkProfile: Boolean,
) {
    val packageName: String get() = component.packageName

    /** Label folded to plain lowercase ASCII-ish text, used for search and section letters. */
    val searchKey: String = fold(label)

    /** The letter this app is filed under in the alphabet scroller, or '#' for anything else. */
    val section: Char = searchKey.firstOrNull()?.uppercaseChar()?.takeIf { it in 'A'..'Z' } ?: '#'

    companion object {
        private val marks = "\\p{M}+".toRegex()

        fun fold(text: String): String =
            Normalizer.normalize(text, Normalizer.Form.NFD).replace(marks, "").lowercase()
    }
}
