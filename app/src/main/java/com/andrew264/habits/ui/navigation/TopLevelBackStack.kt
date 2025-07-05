package com.andrew264.habits.ui.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList

class TopLevelBackStack(startKey: TopLevelRoute) {

    private var topLevelStacks: LinkedHashMap<TopLevelRoute, SnapshotStateList<AppRoute>> =
        linkedMapOf(
            startKey to mutableStateListOf(startKey)
        )

    var currentTopLevelRoute by mutableStateOf(startKey)
        private set

    val backStack: SnapshotStateList<AppRoute> = mutableStateListOf(startKey)

    private fun updateBackStack() {
        backStack.apply {
            clear()
            addAll(topLevelStacks.flatMap { it.value })
        }
    }

    fun switchTopLevel(key: TopLevelRoute) {
        if (topLevelStacks[key] == null) {
            topLevelStacks[key] = mutableStateListOf(key)
        }

        topLevelStacks.remove(key)?.let {
            topLevelStacks[key] = it
        }

        currentTopLevelRoute = key
        updateBackStack()
    }

    fun add(key: AppRoute) {
        topLevelStacks[currentTopLevelRoute]?.add(key)
        updateBackStack()
    }

    fun removeLast() {
        val currentStack = topLevelStacks[currentTopLevelRoute]
        if (currentStack != null && currentStack.size > 1) {
            currentStack.removeAt(currentStack.lastIndex) // TODO: if we do minSdk 35, we can change this to currentStack.removeLast()
        } else if (topLevelStacks.size > 1) {
            topLevelStacks.remove(currentTopLevelRoute)
            currentTopLevelRoute = topLevelStacks.keys.last()
        }
        updateBackStack()
    }
}