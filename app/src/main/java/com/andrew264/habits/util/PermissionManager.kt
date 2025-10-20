package com.andrew264.habits.util

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PermissionManager @Inject constructor() {
    private val _requests = MutableSharedFlow<List<String>>()
    val requests: Flow<List<String>> = _requests.asSharedFlow()

    private val _results = MutableSharedFlow<Map<String, Boolean>>()
    val results: Flow<Map<String, Boolean>> = _results.asSharedFlow()

    suspend fun request(vararg permissions: String) {
        _requests.emit(permissions.toList())
    }

    suspend fun onResult(results: Map<String, Boolean>) {
        _results.emit(results)
    }
}