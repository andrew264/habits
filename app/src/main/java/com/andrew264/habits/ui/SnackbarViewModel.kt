package com.andrew264.habits.ui

import androidx.lifecycle.ViewModel
import com.andrew264.habits.util.SnackbarManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

@HiltViewModel
class SnackbarViewModel @Inject constructor(
    val snackbarManager: SnackbarManager
) : ViewModel()