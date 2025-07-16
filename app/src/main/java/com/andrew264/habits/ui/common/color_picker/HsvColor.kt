package com.andrew264.habits.ui.common.color_picker

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class HsvColor(
    val hue: Float, // 0-360
    val saturation: Float, // 0-1
    val value: Float, // 0-1
    val alpha: Float // 0-1
) : Parcelable