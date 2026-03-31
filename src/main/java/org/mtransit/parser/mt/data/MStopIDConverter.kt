package org.mtransit.parser.mt.data

import org.mtransit.commons.Letters
import org.mtransit.commons.RegexUtils.BEGINNING
import org.mtransit.commons.RegexUtils.DIGIT_CAR
import org.mtransit.commons.RegexUtils.END
import org.mtransit.commons.RegexUtils.any
import org.mtransit.commons.RegexUtils.atLeastOne
import org.mtransit.commons.RegexUtils.group
import java.util.regex.Pattern

@Suppress("MemberVisibilityCanBePrivate", "unused")
object MStopIDConverter {

    private val STOP_ID = Pattern.compile(
        group(BEGINNING + group(any("[A-Z]")) + group(atLeastOne(DIGIT_CAR)) + group(any("[A-Z]")) + END), Pattern.CASE_INSENSITIVE
    )

    const val PREVIOUS = 10_000_000
    const val NEXT = 100_000
    const val MAX_DIGIT = 10_000

    @Throws(RuntimeException::class)
    @JvmOverloads
    @JvmStatic
    fun convert(
        stopIdS: String,
        notSupported: ((stopId: String) -> Int?)? = null,
        nextCharsToInt: ((nextChars: String) -> Int?)? = null,
        previousCharsToInt: ((previousChars: String) -> Int?)? = null,
    ): Int {
        if (stopIdS.isBlank()) {
            return notSupported?.invoke(stopIdS)
                ?: throw RuntimeException("Unexpected stop ID '$stopIdS' to convert to stop ID integer!")
        }
        if (stopIdS.length == 1 && stopIdS[0].isLetter()) {
            endsWithLetter(stopIdS)?.let { return it }
        }
        val matcher = STOP_ID.matcher(stopIdS)
        if (!matcher.find()) {
            return notSupported?.invoke(stopIdS)
                ?: throw RuntimeException("Unexpected stop ID '$stopIdS' can not be parsed by regex!")
        }
        val previousChars = matcher.group(2).uppercase()
        val digits = matcher.group(3).toInt()
        val nextChars = matcher.group(4).uppercase()
        if (digits !in 0..MAX_DIGIT) {
            return notSupported?.invoke(stopIdS)
                ?: throw RuntimeException("Unexpected stop ID digits '$digits' in stop ID '$stopIdS' to convert to stop ID integer!")
        }
        var stopId = digits
        stopId += endsWithLetter(nextChars) ?: run {
            nextCharsToInt?.invoke(nextChars)
                ?: notSupported?.invoke(stopIdS)
                ?: throw RuntimeException("Unexpected next characters '$nextChars' in stop ID '$stopIdS'!")
        }
        stopId += startsWithLetter(previousChars) ?: run {
            previousCharsToInt?.invoke(previousChars)
                ?: notSupported?.invoke(stopIdS)
                ?: throw RuntimeException("Unexpected previous characters '$previousChars' in stop ID '$stopIdS'!")
        }
        return stopId
    }

    @JvmStatic
    fun startsWithLetter(previousChars: String) = when (previousChars) {
        "" -> startsWith(Letters.NONE_)
        "A" -> startsWith(Letters.A)
        "B" -> startsWith(Letters.B)
        "C" -> startsWith(Letters.C)
        "D" -> startsWith(Letters.D)
        "E" -> startsWith(Letters.E)
        "F" -> startsWith(Letters.F)
        "G" -> startsWith(Letters.G)
        "H" -> startsWith(Letters.H)
        "I" -> startsWith(Letters.I)
        "J" -> startsWith(Letters.J)
        "K" -> startsWith(Letters.K)
        "L" -> startsWith(Letters.L)
        "M" -> startsWith(Letters.M)
        "N" -> startsWith(Letters.N)
        "O" -> startsWith(Letters.O)
        "P" -> startsWith(Letters.P)
        "Q" -> startsWith(Letters.Q)
        "R" -> startsWith(Letters.R)
        "S" -> startsWith(Letters.S)
        "T" -> startsWith(Letters.T)
        "U" -> startsWith(Letters.U)
        "V" -> startsWith(Letters.V)
        "W" -> startsWith(Letters.W)
        "X" -> startsWith(Letters.X)
        "Y" -> startsWith(Letters.Y)
        "Z" -> startsWith(Letters.Z)
        else -> null
    }

    @JvmStatic
    fun endsWithLetter(nextChars: String) = when (nextChars) {
        "" -> endsWith(Letters.NONE_)
        "A" -> endsWith(Letters.A)
        "B" -> endsWith(Letters.B)
        "C" -> endsWith(Letters.C)
        "D" -> endsWith(Letters.D)
        "E" -> endsWith(Letters.E)
        "F" -> endsWith(Letters.F)
        "G" -> endsWith(Letters.G)
        "H" -> endsWith(Letters.H)
        "I" -> endsWith(Letters.I)
        "J" -> endsWith(Letters.J)
        "K" -> endsWith(Letters.K)
        "L" -> endsWith(Letters.L)
        "M" -> endsWith(Letters.M)
        "N" -> endsWith(Letters.N)
        "O" -> endsWith(Letters.O)
        "P" -> endsWith(Letters.P)
        "Q" -> endsWith(Letters.Q)
        "R" -> endsWith(Letters.R)
        "S" -> endsWith(Letters.S)
        "T" -> endsWith(Letters.T)
        "U" -> endsWith(Letters.U)
        "V" -> endsWith(Letters.V)
        "W" -> endsWith(Letters.W)
        "X" -> endsWith(Letters.X)
        "Y" -> endsWith(Letters.Y)
        "Z" -> endsWith(Letters.Z)
        else -> null
    }

    @JvmStatic
    fun startsWith(digit: Int) = digit * PREVIOUS

    @JvmStatic
    fun endsWith(digit: Int) = digit * NEXT

    @JvmStatic
    fun other(digit: Int) = Letters.OTHER_MIN_ + digit
}
