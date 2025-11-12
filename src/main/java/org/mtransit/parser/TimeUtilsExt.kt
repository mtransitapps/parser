package org.mtransit.parser

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
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

private val shortDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss z", Locale.ENGLISH)
    .withZone(ZoneId.systemDefault())

fun formatShortDateTime(date: Date): String = formatShortDateTime(date.toInstant())

fun formatShortDateTime(date: Instant): String =
    shortDateTimeFormatter.format(date)
