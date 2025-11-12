package org.mtransit.parser

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.time.Duration.Companion.milliseconds

fun formatSimpleDuration(durationInMs: Long) = buildString {
    durationInMs.milliseconds.toComponents { days, hours, minutes, seconds, nanoseconds ->
        days.takeIf { it > 0 }?.let { append(days).append("d ") }
        hours.takeIf { it > 0 }?.let { append(hours).append("h ") }
        minutes.takeIf { it > 0 }?.let { append(minutes).append("m ") }
        seconds.takeIf { it > 0 }?.let { append(seconds).append("s ") }
        nanoseconds.takeIf { it > 0 }?.let { append(nanoseconds).append("ns ") }
    }
}

fun formatShortDateTime(dateInMs: Long) = formatShortDateTime(Date(dateInMs))

fun formatShortDateTime(date: Date): String =
    SimpleDateFormat("yyyy/MM/dd HH:mm:ss z", Locale.ENGLISH).format(date)
