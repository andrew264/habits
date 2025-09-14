package com.andrew264.habits.ui.usage.whitelist.components

import androidx.compose.animation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.outlined.FindInPage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import com.andrew264.habits.ui.common.haptics.HapticInteractionEffect

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun WhitelistTopAppBar(
    searchText: String,
    onSearchTextChanged: (String) -> Unit,
    showSystemApps: Boolean,
    onToggleShowSystemApps: () -> Unit,
    onNavigateUp: () -> Unit,
    scrollBehavior: TopAppBarScrollBehavior
) {
    var isSearchActive by rememberSaveable { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    val topAppBarColors = TopAppBarDefaults.topAppBarColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer
    )

    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            focusRequester.requestFocus()
        }
    }

    AnimatedContent(
        targetState = isSearchActive,
        transitionSpec = {
            if (targetState) {
                fadeIn() togetherWith fadeOut()
            } else {
                fadeIn() togetherWith fadeOut()
            }
        },
        label = "TopAppBarAnimation"
    ) { searchActive ->
        if (searchActive) {
            TopAppBar(
                title = {
                    TextField(
                        value = searchText,
                        onValueChange = onSearchTextChanged,
                        placeholder = { Text("Search...") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = {
                        isSearchActive = false
                        onSearchTextChanged("")
                    }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Exit search")
                    }
                },
                actions = {
                    OverflowMenu(
                        showSystemApps = showSystemApps,
                        onToggleShowSystemApps = onToggleShowSystemApps
                    )
                },
                colors = topAppBarColors
            )
        } else {
            LargeTopAppBar(
                title = { Text("Manage Whitelist") },
                navigationIcon = {
                    IconButton(onClick = onNavigateUp) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val searchInteractionSource = remember { MutableInteractionSource() }
                    HapticInteractionEffect(searchInteractionSource)
                    IconButton(onClick = { isSearchActive = true }, interactionSource = searchInteractionSource, colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)) {
                        Icon(Icons.Outlined.FindInPage, contentDescription = "Search apps")
                    }
                    OverflowMenu(
                        showSystemApps = showSystemApps,
                        onToggleShowSystemApps = onToggleShowSystemApps
                    )
                },
                scrollBehavior = scrollBehavior,
                colors = topAppBarColors
            )
        }
    }
}

@Composable
private fun OverflowMenu(
    showSystemApps: Boolean,
    onToggleShowSystemApps: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    Box {
        val interactionSource = remember { MutableInteractionSource() }
        HapticInteractionEffect(interactionSource)
        IconButton(onClick = { showMenu = true }, interactionSource = interactionSource, colors = IconButtonDefaults.iconButtonColors(contentColor = MaterialTheme.colorScheme.primary)) {
            Icon(Icons.Default.MoreVert, contentDescription = "More options")
        }
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false }
        ) {
            DropdownMenuItem(
                text = {
                    Text(if (showSystemApps) "Hide system" else "Show system")
                },
                onClick = {
                    onToggleShowSystemApps()
                    showMenu = false
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(name = "Whitelist Top App Bar - Normal")
@Composable
private fun WhitelistTopAppBarPreview() {
    var searchText by remember { mutableStateOf("") }
    var showSystem by remember { mutableStateOf(false) }
    WhitelistTopAppBar(
        searchText = searchText,
        onSearchTextChanged = { searchText = it },
        showSystemApps = showSystem,
        onToggleShowSystemApps = { showSystem = !showSystem },
        onNavigateUp = {},
        scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    )
}