package com.andrew264.habits.state

enum class UserPresenceState {
    /** High confidence: User is interacting with the device. */
    AWAKE,

    /** High confidence: Sleep confirmed by Sleep API or long inactivity at night. */
    SLEEPING,

    /**
     * Transitional state. Screen is off during the night, but sleep is not yet confirmed.
     * The system is waiting for either a longer period of inactivity or a Sleep API event.
     */
    WINDING_DOWN,

    /** The service is stopped or has insufficient data to make a determination. */
    UNKNOWN
}