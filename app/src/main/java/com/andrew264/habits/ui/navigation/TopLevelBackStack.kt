package com.andrew264.habits.ui.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList

class TopLevelBackStack(startKey: TopLevelRoute) {

    internal var topLevelStacks: LinkedHashMap<TopLevelRoute, SnapshotStateList<AppRoute>> =
        linkedMapOf(
            startKey to mutableStateListOf(startKey)
        )

    var currentTopLevelRoute by mutableStateOf(startKey)
        private set

    val backStack: SnapshotStateList<AppRoute> = mutableStateListOf(startKey)

    internal fun updateBackStack() {
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

    companion object {
        val Saver: Saver<TopLevelBackStack, *> = listSaver(
            save = { stack ->
                val savedList = mutableListOf<Any>()
                savedList.add(stack.currentTopLevelRoute)
                savedList.add(stack.topLevelStacks.size)
                stack.topLevelStacks.forEach { (route, routes) ->
                    savedList.add(route)
                    savedList.add(ArrayList(routes))
                }
                savedList
            },
            restore = { savedList ->
                val currentRoute = savedList[0] as TopLevelRoute
                val numStacks = savedList[1] as Int
                val stacks = linkedMapOf<TopLevelRoute, SnapshotStateList<AppRoute>>()
                var index = 2
                repeat(numStacks) {
                    val routeKey = savedList[index++] as TopLevelRoute
                    @Suppress("UNCHECKED_CAST")
                    val routeStack = savedList[index++] as ArrayList<AppRoute>
                    stacks[routeKey] = mutableStateListOf<AppRoute>().apply { addAll(routeStack) }
                }
                TopLevelBackStack(currentRoute).apply {
                    this.topLevelStacks = stacks
                    this.currentTopLevelRoute = currentRoute
                    this.updateBackStack()
                }
            }
        )
    }
}