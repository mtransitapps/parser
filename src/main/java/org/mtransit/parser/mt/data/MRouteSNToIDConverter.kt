package org.mtransit.parser.mt.data

import org.mtransit.parser.MTLog
import java.util.regex.Matcher
import java.util.regex.Pattern

object MRouteSNToIDConverter {

    private val RSN = Pattern.compile("(^([A-Z]*)([0-9]+)([A-Z]*)$)", Pattern.CASE_INSENSITIVE)

    const val PREVIOUS: Long = 1_000_000L

    const val NEXT: Long = 10_000L

    @JvmOverloads
    @JvmStatic
    fun convert(
        rsn: String,
        nextCharsToLong: ((String) -> Long?)? = null,
        previousCharsToLong: ((String) -> Long?)? = null,
    ): Long {
        if (rsn.isBlank()) {
            throw MTLog.Fatal("Unexpected route short name '$rsn' to convert to route ID!")
        }
        val matcher: Matcher = RSN.matcher(rsn)
        if (!matcher.find()) {
            throw MTLog.Fatal("Unexpected route short name '$rsn' can not be parsed by regex!")
        }
        val previousChars: String = matcher.group(2).uppercase()
        val digits: Long = matcher.group(3).toLong()
        val nextChars: String = matcher.group(4).uppercase()
        if (digits !in 0..1000L) {
            throw MTLog.Fatal("Unexpected route short name digits '$digits' in short name '$rsn' to convert to route ID!")
        }
        var routeId: Long = digits
        routeId += when (nextChars) {
            "" -> 0L * NEXT
            "A" -> 1L * NEXT
            "B" -> 2L * NEXT
            "C" -> 3L * NEXT
            "D" -> 4L * NEXT
            "E" -> 5L * NEXT
            "F" -> 6L * NEXT
            "N" -> 14L * NEXT
            "R" -> 18L * NEXT
            "T" -> 20L * NEXT
            "X" -> 24L * NEXT
            else -> {
                nextCharsToLong?.invoke(nextChars)
                    ?: throw MTLog.Fatal("Unexpected next characters '$nextChars' in short name '$rsn'!")
            }
        }
        routeId += when (previousChars) {
            "" -> 0L * PREVIOUS
            "A" -> 1L * PREVIOUS
            "N" -> 14L * PREVIOUS
            "R" -> 18L * PREVIOUS
            "T" -> 20L * PREVIOUS
            "X" -> 24L * PREVIOUS
            "Z" -> 26L * PREVIOUS
            else -> {
                previousCharsToLong?.invoke(previousChars)
                    ?: throw MTLog.Fatal("Unexpected previous characters '$previousChars' in short name '$rsn'!")
            }
        }
        return routeId
    }
}