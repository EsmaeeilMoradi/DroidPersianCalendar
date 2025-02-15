package com.byagowi.persiancalendar.ui.preferences.locationathan.athan

import android.app.Activity
import androidx.activity.result.ActivityResultLauncher
import androidx.appcompat.app.AlertDialog
import androidx.core.content.edit
import com.byagowi.persiancalendar.PREF_ATHAN_NAME
import com.byagowi.persiancalendar.PREF_ATHAN_URI
import com.byagowi.persiancalendar.R
import com.byagowi.persiancalendar.utils.appPrefs
import com.byagowi.persiancalendar.utils.getRawUri
import com.byagowi.persiancalendar.utils.logException

fun showAthanSelectDialog(activity: Activity, pickRingtone: ActivityResultLauncher<Unit>) {
    val items = listOf(
        R.string.default_athan to R.raw.special,
        R.string.abdulbasit to R.raw.abdulbasit,
        R.string.moazzenzadeh to R.raw.moazzenzadeh,
        R.string.entezar to R.raw.entezar
    ).map { (stringId, rawId) ->
        stringId to {
            val prefs = activity.appPrefs
            prefs.edit {
                putString(PREF_ATHAN_URI, activity.resources.getRawUri(rawId))
                putString(PREF_ATHAN_NAME, activity.getString(stringId))
            }
        }
    } + listOf(
        R.string.more to {
            runCatching { pickRingtone.launch(Unit) }.onFailure(logException).getOrNull()
        }
    )
    AlertDialog.Builder(activity)
        .setTitle(R.string.custom_athan)
        .setItems(items.map { activity.getString(it.first) }.toTypedArray()) { dialog, which ->
            items[which].second()
            dialog.dismiss()
        }
        .setNegativeButton(R.string.cancel, null)
        .show()
}

