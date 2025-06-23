package com.andrew264.habits.presentation

import android.text.format.DateFormat
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.andrew264.habits.service.UserPresenceService
import com.andrew264.habits.ui.theme.HabitsTheme
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetBedtimeScreen(
    modifier: Modifier = Modifier,
    onSetBedtime: (hour: Int, minute: Int) -> Unit,
    onClearBedtime: () -> Unit,
    onSetWakeUpTime: (hour: Int, minute: Int) -> Unit,
    onClearWakeUpTime: () -> Unit
) {
    val currentBedtimeHour by UserPresenceService.manualBedtimeHour.collectAsState()
    val currentBedtimeMinute by UserPresenceService.manualBedtimeMinute.collectAsState()
    val currentWakeUpHour by UserPresenceService.manualWakeUpHour.collectAsState()
    val currentWakeUpMinute by UserPresenceService.manualWakeUpMinute.collectAsState()

    var showBedtimePicker by remember { mutableStateOf(false) }
    var showWakeUpTimePicker by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val isSystem24Hour = remember(context) { DateFormat.is24HourFormat(context) }

    val bedtimePickerState = rememberTimePickerState(
        initialHour = currentBedtimeHour ?: 22, // Default to 10 PM
        initialMinute = currentBedtimeMinute ?: 0,
        is24Hour = isSystem24Hour
    )
    val wakeUpTimePickerState = rememberTimePickerState(
        initialHour = currentWakeUpHour ?: 6, // Default to 6 AM
        initialMinute = currentWakeUpMinute ?: 0,
        is24Hour = isSystem24Hour
    )

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val formatter = remember {
        if (isSystem24Hour) SimpleDateFormat("HH:mm", Locale.getDefault())
        else SimpleDateFormat("hh:mm a", Locale.getDefault())
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Configure Heuristic Sleep Schedule",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center
            )
            Text(
                text = "This schedule helps the 'Heuristics' mode estimate your sleep if the screen is off during these times.",
                style = MaterialTheme.typography.bodyMedium,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            TimeSettingCard(
                title = "Bedtime",
                icon = Icons.Outlined.Bedtime,
                currentTimeHour = currentBedtimeHour,
                currentTimeMinute = currentBedtimeMinute,
                defaultTimeInfo = "Defaults to ~10 PM",
                formatter = formatter,
                onSetTimeClick = { showBedtimePicker = true },
                onClearTimeClick = {
                    onClearBedtime()
                    // Reset picker state to default
                    bedtimePickerState.hour = 22
                    bedtimePickerState.minute = 0
                    scope.launch {
                        snackbarHostState.showSnackbar("Custom bedtime cleared.")
                    }
                }
            )

            TimeSettingCard(
                title = "Wake-up Time",
                icon = Icons.Outlined.WbSunny,
                currentTimeHour = currentWakeUpHour,
                currentTimeMinute = currentWakeUpMinute,
                defaultTimeInfo = "Defaults to ~6 AM or 8hrs after bedtime",
                formatter = formatter,
                onSetTimeClick = { showWakeUpTimePicker = true },
                onClearTimeClick = {
                    onClearWakeUpTime()
                    // Reset picker state
                    wakeUpTimePickerState.hour = 6
                    wakeUpTimePickerState.minute = 0
                    scope.launch {
                        snackbarHostState.showSnackbar("Custom wake-up time cleared.")
                    }
                }
            )

            SleepDurationInfo(
                bedtimeHour = currentBedtimeHour,
                bedtimeMinute = currentBedtimeMinute,
                wakeUpHour = currentWakeUpHour,
                wakeUpMinute = currentWakeUpMinute
            )

            // Bedtime Picker Dialog
            if (showBedtimePicker) {
                TimePickerDialog(
                    title = { TimePickerDialogDefaults.Title(displayMode = TimePickerDisplayMode.Picker) },
                    onDismissRequest = { showBedtimePicker = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                onSetBedtime(bedtimePickerState.hour, bedtimePickerState.minute)
                                showBedtimePicker = false
                                val cal = Calendar.getInstance().apply {
                                    set(Calendar.HOUR_OF_DAY, bedtimePickerState.hour)
                                    set(Calendar.MINUTE, bedtimePickerState.minute)
                                }
                                scope.launch {
                                    snackbarHostState.showSnackbar("Bedtime set to: ${formatter.format(cal.time)}")
                                }
                            }
                        ) { Text("OK") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showBedtimePicker = false }) { Text("Cancel") }
                    }
                ) {
                    TimePicker(state = bedtimePickerState)
                }
            }

            // Wake-up Time Picker Dialog
            if (showWakeUpTimePicker) {
                TimePickerDialog(
                    title = { TimePickerDialogDefaults.Title(displayMode = TimePickerDisplayMode.Picker) },
                    onDismissRequest = { showWakeUpTimePicker = false },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                onSetWakeUpTime(wakeUpTimePickerState.hour, wakeUpTimePickerState.minute)
                                showWakeUpTimePicker = false
                                val cal = Calendar.getInstance().apply {
                                    set(Calendar.HOUR_OF_DAY, wakeUpTimePickerState.hour)
                                    set(Calendar.MINUTE, wakeUpTimePickerState.minute)
                                }
                                scope.launch {
                                    snackbarHostState.showSnackbar("Wake-up time set to: ${formatter.format(cal.time)}")
                                }
                            }
                        ) { Text("OK") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showWakeUpTimePicker = false }) { Text("Cancel") }
                    }
                ) {
                    TimePicker(state = wakeUpTimePickerState)
                }
            }
        }
    }
}

@Composable
fun TimeSettingCard(
    title: String,
    icon: ImageVector,
    currentTimeHour: Int?,
    currentTimeMinute: Int?,
    defaultTimeInfo: String,
    formatter: SimpleDateFormat,
    onSetTimeClick: () -> Unit,
    onClearTimeClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = icon, contentDescription = title, modifier = Modifier.size(32.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = title, style = MaterialTheme.typography.titleLarge)
            }
            Spacer(modifier = Modifier.height(12.dp))

            val timeIsSet = currentTimeHour != null && currentTimeMinute != null
            val displayTime: String = if (timeIsSet) {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, currentTimeHour)
                    set(Calendar.MINUTE, currentTimeMinute)
                }
                formatter.format(cal.time)
            } else {
                "Not Set"
            }

            Text(
                text = displayTime,
                style = if (timeIsSet) MaterialTheme.typography.headlineMedium else MaterialTheme.typography.headlineSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            if (!timeIsSet) {
                Text(
                    text = defaultTimeInfo,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
            ) {
                if (timeIsSet) {
                    OutlinedButton(onClick = onClearTimeClick) {
                        Text("Clear")
                    }
                }
                Button(onClick = onSetTimeClick) {
                    Text(if (timeIsSet) "Change $title" else "Set $title")
                }
            }
        }
    }
}

@Composable
fun SleepDurationInfo(
    bedtimeHour: Int?,
    bedtimeMinute: Int?,
    wakeUpHour: Int?,
    wakeUpMinute: Int?
) {
    if (bedtimeHour != null && bedtimeMinute != null && wakeUpHour != null && wakeUpMinute != null) {
        val bedtimeCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, bedtimeHour)
            set(Calendar.MINUTE, bedtimeMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val wakeUpCalendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, wakeUpHour)
            set(Calendar.MINUTE, wakeUpMinute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        // Adjust wakeUpCalendar to the next day if wake up time is earlier than bedtime (e.g. bed 11PM, wake 7AM)
        if (wakeUpCalendar.before(bedtimeCalendar)) {
            wakeUpCalendar.add(Calendar.DAY_OF_YEAR, 1)
        }

        val durationMillis = wakeUpCalendar.timeInMillis - bedtimeCalendar.timeInMillis
        val hours = TimeUnit.MILLISECONDS.toHours(durationMillis)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(durationMillis) % 60

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 8.dp).fillMaxWidth()
        ) {
            Text(
                "Estimated Sleep Window: ${hours}h ${minutes}m",
                style = MaterialTheme.typography.titleMedium
            )
            // Check if the original wake up time (without day adjustment) is "before" bedtime on the same day
            if (wakeUpHour * 60 + wakeUpMinute < bedtimeHour * 60 + bedtimeMinute) {
                Text(
                    "(Sleep window crosses midnight)",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    } else if (bedtimeHour != null || wakeUpHour != null) { // Only one is set
        Text(
            "Set both bedtime and wake-up time to see the estimated sleep window.",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
    }
    // If neither is set, don't show any message about duration.
}


@Preview(showBackground = true, heightDp = 900)
@Composable
fun SetBedtimeScreenPreview_BothSet() {
    // Manually set UserPresenceService states for preview
    LaunchedEffect(Unit) {
        UserPresenceService._manualBedtimeHour.value = 22
        UserPresenceService._manualBedtimeMinute.value = 30
        UserPresenceService._manualWakeUpHour.value = 6
        UserPresenceService._manualWakeUpMinute.value = 45
    }
    HabitsTheme {
        SetBedtimeScreen(
            onSetBedtime = { _, _ -> },
            onClearBedtime = {},
            onSetWakeUpTime = { _, _ -> },
            onClearWakeUpTime = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
fun SetBedtimeScreenPreview_NoneSet() {
    LaunchedEffect(Unit) {
        UserPresenceService._manualBedtimeHour.value = null
        UserPresenceService._manualBedtimeMinute.value = null
        UserPresenceService._manualWakeUpHour.value = null
        UserPresenceService._manualWakeUpMinute.value = null
    }
    HabitsTheme {
        SetBedtimeScreen(
            onSetBedtime = { _, _ -> },
            onClearBedtime = {},
            onSetWakeUpTime = { _, _ -> },
            onClearWakeUpTime = {}
        )
    }
}

@Preview(showBackground = true, heightDp = 900)
@Composable
fun SetBedtimeScreenPreview_BedtimeSet() {
    LaunchedEffect(Unit) {
        UserPresenceService._manualBedtimeHour.value = 23
        UserPresenceService._manualBedtimeMinute.value = 0
        UserPresenceService._manualWakeUpHour.value = null
        UserPresenceService._manualWakeUpMinute.value = null
    }
    HabitsTheme {
        SetBedtimeScreen(
            onSetBedtime = { _, _ -> },
            onClearBedtime = {},
            onSetWakeUpTime = { _, _ -> },
            onClearWakeUpTime = {}
        )
    }
}