package com.soft.koyar.data.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Log entry for incoming and outgoing Bluetooth SPP messages.
 */
data class LogMessage(
    val timestamp: Long = System.currentTimeMillis(),
    val text: String,
    val isIncoming: Boolean
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(timestamp))
}
