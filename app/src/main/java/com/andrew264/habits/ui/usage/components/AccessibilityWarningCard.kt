package com.andrew264.habits.ui.usage.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import com.andrew264.habits.ui.theme.Dimens

@Composable
fun AccessibilityWarningCard(onOpenAccessibilitySettings: () -> Unit) {
    val view = LocalView.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                onOpenAccessibilitySettings()
                view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
            },
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
                contentDescription = "Warning",
                tint = MaterialTheme.colorScheme.onErrorContainer
            )
            Text(
                text = "Usage tracking service is not running. Tap here to fix it.",
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