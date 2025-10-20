package com.andrew264.habits.ui.usage.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.andrew264.habits.R
import com.andrew264.habits.ui.common.haptics.HapticInteractionEffect
import com.andrew264.habits.ui.theme.Dimens

@Composable
fun AccessibilityWarningCard(onOpenAccessibilitySettings: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    HapticInteractionEffect(interactionSource)
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onOpenAccessibilitySettings,
            ),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(Dimens.PaddingLarge),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
        ) {
            Icon(
                imageVector = Icons.Outlined.Warning,
                contentDescription = stringResource(R.string.accessibility_warning_card_warning_icon_content_description),
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = stringResource(R.string.accessibility_warning_card_service_not_running),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onErrorContainer
            )
        }
    }
}

@Preview
@Composable
fun AccessibilityWarningCardPreview() {
    AccessibilityWarningCard(onOpenAccessibilitySettings = {})
}