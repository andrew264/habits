package com.andrew264.habits.ui.common.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ToggleOff
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.andrew264.habits.ui.theme.Dimens

@Composable
fun FeatureDisabledContent(
    modifier: Modifier = Modifier,
    icon: ImageVector = Icons.Outlined.ToggleOff,
    title: String,
    description: String,
    buttonText: String,
    onEnableClicked: () -> Unit
) {
    val view = LocalView.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(Dimens.PaddingLarge))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(Dimens.PaddingSmall))
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(Dimens.PaddingExtraLarge))
        Button(onClick = {
            onEnableClicked()
            view.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY)
        }) {
            Text(buttonText)
        }
    }
}

@Preview
@Composable
fun FeatureDisabledContentPreview() {
    FeatureDisabledContent(
        title = "Feature Disabled",
        description = "This feature is currently disabled. Please enable it in settings.",
        buttonText = "Enable Feature",
        onEnableClicked = {}
    )
}

