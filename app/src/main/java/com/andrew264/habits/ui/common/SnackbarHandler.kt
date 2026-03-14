package com.andrew264.habits.ui.common

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import com.andrew264.habits.util.SnackbarManager

@Composable
fun SnackbarHandler(
    snackbarManager: SnackbarManager,
    snackbarHostState: SnackbarHostState
) {
    val context = LocalContext.current
    LaunchedEffect(snackbarManager, snackbarHostState) {
        snackbarManager.commands.collect { command ->
            val result = snackbarHostState.showSnackbar(
                message = command.message.resolve(context),
                actionLabel = command.actionLabel?.resolve(context),
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                command.onAction?.invoke()
            } else {
                command.onDismiss?.invoke()
            }
        }
    }
}