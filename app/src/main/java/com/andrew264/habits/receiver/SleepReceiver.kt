package com.andrew264.habits.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.andrew264.habits.service.UserPresenceService
import com.google.android.gms.location.SleepClassifyEvent
import com.google.android.gms.location.SleepSegmentEvent

class SleepReceiver : BroadcastReceiver() {
    companion object {
        const val ACTION_PROCESS_SLEEP_EVENTS = "com.andrew264.habits.ACTION_PROCESS_SLEEP_EVENTS"
        private const val TAG = "SleepReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d(TAG, "Sleep event received: ${intent.action}")

        // For SleepSegmentEvents
        if (SleepSegmentEvent.hasEvents(intent)) {
            val sleepSegmentEvents = SleepSegmentEvent.extractEvents(intent)
            Log.d(TAG, "Received ${sleepSegmentEvents.size} SleepSegmentEvents")
            // Forward to service for processing
            val serviceIntent = Intent(context, UserPresenceService::class.java).apply {
                action = UserPresenceService.ACTION_PROCESS_SLEEP_SEGMENT_EVENTS
                putParcelableArrayListExtra(UserPresenceService.EXTRA_SLEEP_SEGMENTS, ArrayList(sleepSegmentEvents))
            }
            context.startService(serviceIntent) // Use startService, service will decide if it needs to be foreground
        }

        // For SleepClassifyEvents (optional, SleepSegmentEvent is usually better for AWAKE/SLEEPING)
        if (SleepClassifyEvent.hasEvents(intent)) {
            val sleepClassifyEvents = SleepClassifyEvent.extractEvents(intent)
            Log.d(TAG, "Received ${sleepClassifyEvents.size} SleepClassifyEvents")
            // Forward to service for processing
            val serviceIntent = Intent(context, UserPresenceService::class.java).apply {
                action = UserPresenceService.ACTION_PROCESS_SLEEP_CLASSIFY_EVENTS
                putParcelableArrayListExtra(UserPresenceService.EXTRA_SLEEP_CLASSIFY_EVENTS, ArrayList(sleepClassifyEvents))
            }
            context.startService(serviceIntent)
        }
    }
}