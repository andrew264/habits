package com.andrew264.habits.ui.water.components

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.andrew264.habits.R
import com.andrew264.habits.ui.common.haptics.HapticInteractionEffect
import com.andrew264.habits.ui.theme.Dimens
import com.andrew264.habits.ui.theme.HabitsTheme
import com.andrew264.habits.ui.water.WaterScreenUiState

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun ProgressSection(
    uiState: WaterScreenUiState,
    onEditTarget: () -> Unit,
    modifier: Modifier = Modifier
) {
    val animatedProgress by animateFloatAsState(
        targetValue = uiState.progress,
        label = stringResource(R.string.water_progress_animation),
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
                text = stringResource(id = R.string.water_input_section_ml, uiState.todaysIntakeMl),
                style = MaterialTheme.typography.displayLargeEmphasized
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingExtraSmall)
            ) {
                Text(
                    text = stringResource(id = R.string.water_progress_of_ml, uiState.settings.waterDailyTargetMl),
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
            uiState = WaterScreenUiState(todaysIntakeMl = 1500, progress = 0.6f),
            onEditTarget = {}
        )
    }
}