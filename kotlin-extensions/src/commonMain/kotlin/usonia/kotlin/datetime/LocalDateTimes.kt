package usonia.kotlin.datetime

import kotlinx.datetime.LocalDateTime

/**
 * Format a local date/time as a readable string.
 *
 * example:
 *    July 10th at 4:01
 *
 * Note: Uses 24-hour time format
 * Note: This should be removed once KotlinX-datetime gets proper formatting.
 */
val LocalDateTime.timestapSentanceFormat: String get() {
    val minuteFormat = when {
        minute < 10 -> "0${minute}"
        else -> minute
    }
    val suffix = when(dayOfMonth) {
        1, 21, 31 -> "st"
        2, 22 -> "nd"
        3, 23 -> "rd"
        else -> "th"
    }

    return "$month $dayOfMonth${suffix} at ${hour}:$minuteFormat"
}

val LocalDateTime.minuteOfDay: Int get() = (hour * 60) + minute
