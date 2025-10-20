package com.andrew264.habits.ui.usage

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.andrew264.habits.R
import com.andrew264.habits.ui.common.charts.BarChart
import com.andrew264.habits.ui.common.color_picker.ColorPickerDialog
import com.andrew264.habits.ui.common.color_picker.utils.toColorOrNull
import com.andrew264.habits.ui.common.color_picker.utils.toHexCode
import com.andrew264.habits.ui.common.components.DrawableImage
import com.andrew264.habits.ui.common.components.FilterButtonGroup
import com.andrew264.habits.ui.common.components.SimpleTopAppBar
import com.andrew264.habits.ui.common.duration_picker.DurationPickerDialog
import com.andrew264.habits.ui.common.list_items.ListItemPosition
import com.andrew264.habits.ui.common.list_items.NavigationListItem
import com.andrew264.habits.ui.common.utils.FormatUtils
import com.andrew264.habits.ui.common.utils.rememberAppIcon
import com.andrew264.habits.ui.theme.Dimens
import com.andrew264.habits.ui.theme.HabitsTheme
import java.util.concurrent.TimeUnit
import kotlin.math.ceil
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun UsageDetailScreen(
    app: AppDetails,
    onSetAppColor: (packageName: String, colorHex: String) -> Unit,
    onSaveLimits: (sessionMinutes: Int?) -> Unit,
    onNavigateUp: () -> Unit
) {
    var selectedMetric by remember { mutableStateOf(UsageMetric.SCREEN_TIME) }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SimpleTopAppBar(
                title = stringResource(R.string.usage_detail_title),
                onNavigateUp = onNavigateUp,
                scrollBehavior = scrollBehavior
            )
        },
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(Dimens.PaddingLarge),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Dimens.PaddingMedium)
            ) {
                val icon = rememberAppIcon(packageName = app.packageName)
                DrawableImage(drawable = icon, contentDescription = null, modifier = Modifier.size(48.dp), mask = MaterialShapes.Sunny)
                Text(app.friendlyName, style = MaterialTheme.typography.headlineMedium)
            }

            // Metric Toggles
            FilterButtonGroup(
                options = UsageMetric.entries,
                selectedOption = selectedMetric,
                onOptionSelected = { selectedMetric = it },
                label = { Text(it.title) }
            )

            // Main Metric Display & Chart
            AnimatedContent(
                targetState = selectedMetric,
                transitionSpec = {
                    fadeIn(animationSpec = tween(220, delayMillis = 90))
                        .togetherWith(fadeOut(animationSpec = tween(90)))
                },
                label = stringResource(R.string.usage_detail_metric_display_label)
            ) { metric ->
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(Dimens.PaddingLarge)
                ) {
                    val (value, unit) = when (metric) {
                        UsageMetric.SCREEN_TIME -> FormatUtils.formatDuration(app.totalUsageMillis).split(" ")
                            .let { if (it.size > 1) it[0] to it[1] else it[0] to "" }

                        UsageMetric.TIMES_OPENED -> app.timesOpened.toString() to if (app.timesOpened == 1) stringResource(R.string.usage_detail_times_opened_unit_singular) else stringResource(
                            R.string.usage_detail_times_opened_unit_plural
                        )
                    }

                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(value, style = MaterialTheme.typography.displayMedium)
                        if (unit.isNotEmpty()) {
                            Text(
                                text = unit,
                                style = MaterialTheme.typography.headlineSmall,
                                modifier = Modifier.padding(start = Dimens.PaddingSmall, bottom = 4.dp)
                            )
                        }
                    }

                    val chartEntries = when (metric) {
                        UsageMetric.SCREEN_TIME -> app.screenTimeHistoricalData
                        UsageMetric.TIMES_OPENED -> app.timesOpenedHistoricalData
                    }

                    val topValue = when (metric) {
                        UsageMetric.SCREEN_TIME -> {
                            val maxMins = ceil((chartEntries.maxOfOrNull { it.value } ?: 0f) / 60000f)
                            (ceil(maxMins / 15f) * 15f) * 60000f // Round up to nearest 15 mins
                        }

                        UsageMetric.TIMES_OPENED -> {
                            val maxOpens = chartEntries.maxOfOrNull { it.value } ?: 0f
                            ceil(maxOpens / 5f) * 5f // Round up to nearest 5
                        }
                    }

                    val resources = LocalResources.current
                    val yAxisFormatter: (Float) -> String = {
                        when (metric) {
                            UsageMetric.SCREEN_TIME -> {
                                if (topValue <= TimeUnit.MINUTES.toMillis(5)) {
                                    resources.getString(
                                        R.string.usage_detail_seconds_formatter,
                                        (it / 1000f).roundToInt()
                                    )
                                } else {
                                    FormatUtils.formatDuration(it.toLong())
                                }
                            }

                            UsageMetric.TIMES_OPENED -> it.roundToInt().toString()
                        }
                    }

                    BarChart(
                        entries = chartEntries,
                        barColor = app.color.toColorOrNull() ?: MaterialTheme.colorScheme.primary,
                        topValue = if (topValue > 0) topValue else null,
                        yAxisLabelFormatter = yAxisFormatter,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                    )
                }
            }

            // Settings Section
            Column {
                var showSessionLimitDialog by rememberSaveable { mutableStateOf(false) }
                if (showSessionLimitDialog) {
                    DurationPickerDialog(
                        title = stringResource(R.string.usage_detail_set_session_limit_title),
                        description = stringResource(R.string.usage_detail_set_session_limit_description),
                        initialTotalMinutes = app.sessionLimitMinutes ?: 0,
                        onDismissRequest = { showSessionLimitDialog = false },
                        onConfirm = { totalMinutes ->
                            onSaveLimits(if (totalMinutes > 0) totalMinutes else null)
                            showSessionLimitDialog = false
                        }
                    )
                }

                NavigationListItem(
                    icon = Icons.Outlined.Timer,
                    title = stringResource(R.string.usage_detail_session_limit),
                    onClick = { showSessionLimitDialog = true },
                    position = ListItemPosition.TOP,
                    valueContent = {
                        Text(
                            text = if (app.sessionLimitMinutes != null) FormatUtils.formatDuration(app.sessionLimitMinutes * 60_000L) else stringResource(
                                R.string.usage_detail_not_set
                            ),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )

                var showColorDialog by rememberSaveable { mutableStateOf(false) }
                if (showColorDialog) {
                    ColorPickerDialog(
                        title = stringResource(R.string.usage_detail_choose_color_title, app.friendlyName),
                        initialColor = app.color.toColorOrNull() ?: Color.Gray,
                        showAlphaSlider = false,
                        onDismissRequest = { showColorDialog = false },
                        onConfirmation = { newColor ->
                            onSetAppColor(app.packageName, newColor.toHexCode(includeAlpha = false))
                            showColorDialog = false
                        }
                    )
                }

                NavigationListItem(
                    icon = Icons.Outlined.Palette,
                    title = stringResource(R.string.usage_detail_display_color),
                    onClick = { showColorDialog = true },
                    position = ListItemPosition.BOTTOM,
                    valueContent = {
                        Box(
                            modifier = Modifier
                                .clip(MaterialShapes.Pill.toShape())
                                .size(24.dp)
                                .background(
                                    color = app.color.toColorOrNull() ?: Color.Gray,
                                    shape = MaterialTheme.shapes.small
                                )
                        )
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun UsageDetailScreenPreview() {
    val sampleAppDetails = AppDetails(
        packageName = "com.example.app",
        friendlyName = "Sample App",
        color = "#4CAF50",
        sessionLimitMinutes = 20,
        totalUsageMillis = TimeUnit.MINUTES.toMillis(153),
        usagePercentage = 0.35f,
        averageSessionMillis = TimeUnit.MINUTES.toMillis(12),
        screenTimeHistoricalData = listOf(
            com.andrew264.habits.ui.common.charts.BarChartEntry(TimeUnit.MINUTES.toMillis(20).toFloat(), "Mon"),
            com.andrew264.habits.ui.common.charts.BarChartEntry(TimeUnit.MINUTES.toMillis(45).toFloat(), "Tue"),
            com.andrew264.habits.ui.common.charts.BarChartEntry(TimeUnit.MINUTES.toMillis(15).toFloat(), "Wed"),
            com.andrew264.habits.ui.common.charts.BarChartEntry(TimeUnit.MINUTES.toMillis(63).toFloat(), "Thu"),
            com.andrew264.habits.ui.common.charts.BarChartEntry(0f, "Fri"),
        ),
        timesOpened = 23,
        timesOpenedHistoricalData = listOf(
            com.andrew264.habits.ui.common.charts.BarChartEntry(5f, "Mon"),
            com.andrew264.habits.ui.common.charts.BarChartEntry(8f, "Tue"),
            com.andrew264.habits.ui.common.charts.BarChartEntry(3f, "Wed"),
            com.andrew264.habits.ui.common.charts.BarChartEntry(7f, "Thu"),
            com.andrew264.habits.ui.common.charts.BarChartEntry(0f, "Fri"),
        ),
        peakUsageTimeLabel = "Most used around 8 PM",
    )
    HabitsTheme {
        Surface {
            UsageDetailScreen(
                app = sampleAppDetails,
                onSetAppColor = { _, _ -> },
                onSaveLimits = { _ -> },
                onNavigateUp = {}
            )
        }
    }
}