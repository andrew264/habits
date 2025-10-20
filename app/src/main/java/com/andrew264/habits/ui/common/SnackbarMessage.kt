package com.andrew264.habits.ui.common

import android.content.Context
import androidx.annotation.StringRes

sealed class SnackbarMessage {
    data class FromString(val text: String) : SnackbarMessage()
    data class FromResource(
        @param:StringRes val resId: Int,
        val formatArgs: List<Any> = emptyList()
    ) : SnackbarMessage()

    fun resolve(context: Context): String {
        return when (this) {
            is FromString -> text
            is FromResource -> context.getString(resId, *formatArgs.toTypedArray())
        }
    }
}