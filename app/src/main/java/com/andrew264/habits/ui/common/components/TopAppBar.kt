package com.andrew264.habits.ui.common.components

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.tooling.preview.Preview
import com.andrew264.habits.ui.common.haptics.HapticInteractionEffect

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun SimpleTopAppBar(
    title: String,
    onNavigateUp: (() -> Unit)? = null,
    scrollBehavior: TopAppBarScrollBehavior? = null
) {
    LargeFlexibleTopAppBar(
        title = { Text(text = title) },
        navigationIcon = {
            if (onNavigateUp != null) {
                val interactionSource = remember { MutableInteractionSource() }
                HapticInteractionEffect(interactionSource)
                IconButton(onClick = onNavigateUp, interactionSource = interactionSource) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            }
        },
        scrollBehavior = scrollBehavior,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Composable
fun SimpleTopAppBarPreview() {
    SimpleTopAppBar(title = "Habits")
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Preview
@Composable
fun SimpleTopAppBarWithNavigationPreview() {
    SimpleTopAppBar(title = "Habits", onNavigateUp = {})
}