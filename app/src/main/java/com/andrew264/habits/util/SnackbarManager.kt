package com.andrew264.habits.util

import com.andrew264.habits.ui.common.SnackbarMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

data class SnackbarCommand(
    val message: SnackbarMessage,
    val actionLabel: SnackbarMessage? = null,
    val onAction: (() -> Unit)? = null,
    val onDismiss: (() -> Unit)? = null
)

@Singleton
class SnackbarManager @Inject constructor() {
    private val _commands = MutableSharedFlow<SnackbarCommand>(extraBufferCapacity = 1)
    val commands: Flow<SnackbarCommand> = _commands.asSharedFlow()

    suspend fun showMessage(command: SnackbarCommand) {
        _commands.emit(command)
    }
}