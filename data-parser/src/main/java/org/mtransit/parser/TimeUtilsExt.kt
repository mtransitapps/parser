package org.mtransit.parser

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.nanoseconds

fun formatSimpleDuration(durationInMs: Long) = buildString {
    val negative = durationInMs < 0
    abs(durationInMs).milliseconds.toComponents { days, hours, minutes, seconds, nanoseconds ->
        days.takeIf { it > 0 }?.let { append(it).append("d ") }
        hours.takeIf { it > 0 }?.let { append(it).append("h ") }
        minutes.takeIf { it > 0 }?.let { append(it).append("m ") }
        seconds.takeIf { it > 0 }?.let { append(it).append("s ") }
        nanoseconds.takeIf { it > 0 }?.nanoseconds?.inWholeMilliseconds?.let { append(it).append("ms ") }
    }
    if (negative) insert(0, "-")
}.trim()

private val shortDateTimeFormatter = DateTimeFormatter.ofPattern("yyyy/MM/dd HH:mm:ss z", Locale.ENGLISH)
    .withZone(ZoneId.systemDefault())

fun formatShortDateTime(date: Date): String = formatShortDateTime(date.toInstant())

fun formatShortDateTime(date: Instant): String =
    shortDateTimeFormatter.format(date)
