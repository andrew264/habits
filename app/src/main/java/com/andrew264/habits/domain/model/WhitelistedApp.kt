package com.andrew264.habits.domain.model

data class WhitelistedApp(
    val packageName: String,
    val colorHex: String,
    val sessionLimitMinutes: Int?
)