package com.andrew264.habits.ui.common

import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.andrew264.habits.util.SnackbarManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Composable
fun SnackbarHandler(
    snackbarManager: SnackbarManager,
    snackbarHostState: SnackbarHostState,
    scope: CoroutineScope = rememberCoroutineScope()
) {
    val context = LocalContext.current
    LaunchedEffect(snackbarManager, snackbarHostState, scope) {
        snackbarManager.commands.collect { command ->
            scope.launch {
                val result = snackbarHostState.showSnackbar(
                    message = command.message.resolve(context),
                    actionLabel = command.actionLabel?.resolve(context)
                )
                if (result == SnackbarResult.ActionPerformed) {
                    command.onAction?.invoke()
                } else {
                    command.onDismiss?.invoke()
                }
            }
        }
    }
}