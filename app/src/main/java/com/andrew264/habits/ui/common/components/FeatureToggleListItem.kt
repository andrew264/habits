package com.andrew264.habits.ui.common.components

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.andrew264.habits.ui.theme.Dimens
import com.andrew264.habits.ui.theme.HabitsTheme

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun FeatureToggleListItem(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val view = LocalView.current
    val interactionSource = remember { MutableInteractionSource() }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(64.dp),
        color = MaterialTheme.colorScheme.primaryContainer
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = {
                        val newChecked = !checked
                        onCheckedChange(newChecked)
                        val feedback =
                            if (newChecked) HapticFeedbackConstants.TOGGLE_ON else HapticFeedbackConstants.TOGGLE_OFF
                        view.performHapticFeedback(feedback)
                    }
                )
                .padding(Dimens.PaddingExtraLarge),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLargeEmphasized,
                color = if (enabled) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            )
            IconSwitch(
                checked = checked,
                onCheckedChange = null, // Handled by parent clickable
                enabled = enabled,
                interactionSource = interactionSource
            )
        }
    }
}

@Preview
@Composable
private fun FeatureToggleListItemPreview() {
    HabitsTheme {
        var checkedOn by remember { mutableStateOf(true) }
        FeatureToggleListItem(
            title = "Enable Feature 1",
            checked = checkedOn,
            onCheckedChange = { checkedOn = it }
        )
    }
}