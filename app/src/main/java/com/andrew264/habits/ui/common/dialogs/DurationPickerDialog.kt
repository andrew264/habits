package com.andrew264.habits.ui.common.dialogs

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.andrew264.habits.ui.common.components.NumberPicker
import com.andrew264.habits.ui.theme.Dimens
import com.andrew264.habits.ui.theme.HabitsTheme
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import java.util.Locale

@OptIn(ExperimentalTextApi::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun DurationPickerDialog(
    title: String,
    description: String,
    initialTotalMinutes: Int,
    onDismissRequest: () -> Unit,
    onConfirm: (totalMinutes: Int) -> Unit
) {
    val initialHours = initialTotalMinutes / 60
    val initialMinutes = initialTotalMinutes % 60
    val roundedInitialMinutes = (initialMinutes / 5) * 5

    var selectedHour by rememberSaveable { mutableIntStateOf(initialHours) }
    var selectedMinute by rememberSaveable { mutableIntStateOf(roundedInitialMinutes) }


    var isInitialized by remember { mutableStateOf(false) }

    val view = LocalView.current

    val hoursItems = remember { (0..23).map { it.toString() } }
    val minutesItems = remember { (0..55 step 5).map { String.format(Locale.getDefault(), "%02d", it) } }

    val hoursState = rememberLazyListState()
    val minutesState = rememberLazyListState()

    val itemHeight = 48.dp

    val textMeasurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val textStyle = MaterialTheme.typography.headlineLarge
    val pickerWidth = remember(textStyle, density, textMeasurer) {
        val width = textMeasurer.measure("00", style = textStyle).size.width
        with(density) {
            width.toDp() + 32.dp
        }
    }

    LaunchedEffect(hoursState, minutesState) {
        snapshotFlow { hoursState.layoutInfo.totalItemsCount > 0 && minutesState.layoutInfo.totalItemsCount > 0 }
            .filter { it }
            .first()

        hoursState.scrollToItem(initialHours)

        val initialMinuteIndex = roundedInitialMinutes / 5
        val middleIndex = (Int.MAX_VALUE / 2) - ((Int.MAX_VALUE / 2) % minutesItems.size) + initialMinuteIndex
        minutesState.scrollToItem(middleIndex)

        // Delay to allow scroll to finish before enabling value changes
        kotlinx.coroutines.delay(100)
        isInitialized = true
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            shape = MaterialTheme.shapes.extraLargeIncreased,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(Dimens.PaddingExtraLarge),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = title, style = MaterialTheme.typography.headlineSmall)
                Spacer(Modifier.height(Dimens.PaddingSmall))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(Dimens.PaddingLarge))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NumberPicker(
                            modifier = Modifier.width(pickerWidth),
                            items = hoursItems,
                            state = hoursState,
                            loop = false,
                            onValueChange = {
                                if (isInitialized) {
                                    selectedHour = it.toIntOrNull() ?: 0
                                }
                            },
                            itemHeight = itemHeight
                        )
                        Text(
                            text = "hr",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Row(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        NumberPicker(
                            modifier = Modifier.width(pickerWidth),
                            items = minutesItems,
                            state = minutesState,
                            loop = true,
                            onValueChange = {
                                if (isInitialized) {
                                    selectedMinute = it.toIntOrNull() ?: 0
                                }
                            },
                            itemHeight = itemHeight
                        )
                        Text(
                            text = "min",
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(Modifier.height(Dimens.PaddingExtraLarge))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = {
                        onDismissRequest()
                        view.performHapticFeedback(HapticFeedbackConstants.REJECT)
                    }) {
                        Text("Cancel")
                    }
                    Spacer(Modifier.width(Dimens.PaddingSmall))
                    TextButton(onClick = {
                        val totalMinutes = selectedHour * 60 + selectedMinute
                        onConfirm(totalMinutes)
                        view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    }) {
                        Text("OK")
                    }
                }
            }
        }
    }
}

@Preview
@Composable
private fun DurationPickerDialogPreview() {
    HabitsTheme {
        DurationPickerDialog(
            title = "Set daily limit",
            description = "This app limit for Chrome will reset at midnight",
            initialTotalMinutes = 90,
            onDismissRequest = {},
            onConfirm = {}
        )
    }
}