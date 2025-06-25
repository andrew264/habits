package com.andrew264.habits.state

enum class UserPresenceState {
    /** High confidence: User is interacting with the device. */
    AWAKE,

    /** High confidence: Sleep confirmed by Sleep API or long inactivity at night, or forced by schedule. */
    SLEEPING,

    /** The service is stopped or has insufficient data to make a determination. */
    UNKNOWN
}