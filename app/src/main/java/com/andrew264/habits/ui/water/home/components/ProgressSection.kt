package com.andrew264.habits.ui.water.home.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.andrew264.habits.ui.common.haptics.HapticInteractionEffect
import com.andrew264.habits.ui.theme.Dimens
import com.andrew264.habits.ui.theme.HabitsTheme
import com.andrew264.habits.ui.water.home.WaterHomeUiState

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ProgressSection(
    uiState: WaterHomeUiState,
    onEditTarget: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = uiState.progress,
        label = "WaterProgressAnimation",
        animationSpec = WavyProgressIndicatorDefaults.ProgressAnimationSpec
    )
    val interactionSource = remember { MutableInteractionSource() }
    HapticInteractionEffect(interactionSource)
    val strokeWidth = 8.dp
    val stroke = with(LocalDensity.current) {
        remember { Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round) }
    }

    BoxWithConstraints(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        val indicatorSize = min(maxWidth, maxHeight)

        CircularWavyProgressIndicator(
            progress = { animatedProgress },
            modifier = Modifier
                .size(indicatorSize)
                .padding(Dimens.PaddingLarge),
            stroke = stroke,
            trackStroke = stroke,
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
        Column(
            modifier = Modifier
                .clip(CircleShape)
                .clickable(
                    interactionSource = interactionSource,
                    indication = null,
                    onClick = onEditTarget
                )
                .padding(Dimens.PaddingLarge),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "${uiState.todaysIntakeMl} ml",
                style = MaterialTheme.typography.displayLargeEmphasized
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingExtraSmall)
            ) {
                Text(
                    text = "of ${uiState.settings.waterDailyTargetMl} ml",
                    style = MaterialTheme.typography.titleLargeEmphasized,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Preview
@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
internal fun ProgressSectionPreview() {
    HabitsTheme {
        ProgressSection(
            uiState = WaterHomeUiState(todaysIntakeMl = 1500, progress = 0.6f),
            onEditTarget = {}
        )
    }
}