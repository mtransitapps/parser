package org.mtransit.parser.mt.data

import org.mtransit.commons.Letters
import org.mtransit.commons.RegexUtils.BEGINNING
import org.mtransit.commons.RegexUtils.DIGIT_CAR
import org.mtransit.commons.RegexUtils.END
import org.mtransit.commons.RegexUtils.any
import org.mtransit.commons.RegexUtils.atLeastOne
import org.mtransit.commons.RegexUtils.group
import org.mtransit.parser.MTLog
import java.util.regex.Matcher
import java.util.regex.Pattern
import kotlin.math.absoluteValue

@Suppress("MemberVisibilityCanBePrivate", "unused")
object MRouteSNToIDConverter {

    private val RSN = Pattern.compile(
        group(BEGINNING + group(any("[A-Z]")) + group(atLeastOne(DIGIT_CAR)) + group(any("[A-Z]")) + END), Pattern.CASE_INSENSITIVE
    )

    @JvmStatic
    fun defaultConverter(routeShortName: String) = routeShortName.padStart(3).lowercase().hashCode().absoluteValue.toLong()

    // region letters

    @Deprecated(message = "Use Letters directly")
    const val NONE_ = Letters.NONE_

    @Deprecated(message = "Use Letters directly")
    const val A = Letters.A

    @Deprecated(message = "Use Letters directly")
    const val B = Letters.B

    @Deprecated(message = "Use Letters directly")
    const val C = Letters.C

    @Deprecated(message = "Use Letters directly")
    const val D = Letters.D

    @Deprecated(message = "Use Letters directly")
    const val E = Letters.E

    @Deprecated(message = "Use Letters directly")
    const val F = Letters.F

    @Deprecated(message = "Use Letters directly")
    const val G = Letters.G

    @Deprecated(message = "Use Letters directly")
    const val H = Letters.H

    @Deprecated(message = "Use Letters directly")
    const val I = Letters.I

    @Deprecated(message = "Use Letters directly")
    const val J = Letters.J

    @Deprecated(message = "Use Letters directly")
    const val K = Letters.K

    @Deprecated(message = "Use Letters directly")
    const val L = Letters.L

    @Deprecated(message = "Use Letters directly")
    const val M = Letters.M

    @Deprecated(message = "Use Letters directly")
    const val N = Letters.N

    @Deprecated(message = "Use Letters directly")
    const val O = Letters.O

    @Deprecated(message = "Use Letters directly")
    const val P = Letters.P

    @Deprecated(message = "Use Letters directly")
    const val Q = Letters.Q

    @Deprecated(message = "Use Letters directly")
    const val R = Letters.R

    @Deprecated(message = "Use Letters directly")
    const val S = Letters.S

    @Deprecated(message = "Use Letters directly")
    const val T = Letters.T

    @Deprecated(message = "Use Letters directly")
    const val U = Letters.U

    @Deprecated(message = "Use Letters directly")
    const val V = Letters.V

    @Deprecated(message = "Use Letters directly")
    const val W = Letters.W

    @Deprecated(message = "Use Letters directly")
    const val X = Letters.X

    @Deprecated(message = "Use Letters directly")
    const val Y = Letters.Y

    @Deprecated(message = "Use Letters directly")
    const val Z = Letters.Z

    @Deprecated(message = "Use Letters directly")
    const val OTHER_MIN_ = Letters.OTHER_MIN_

    // endregion

    const val PREVIOUS: Long = 1_000_000L
    const val NEXT: Long = 10_000L

    @JvmOverloads
    @JvmStatic
    fun convert(
        rsn: String,
        notSupportedToRouteId: ((rsn: String) -> Long?)? = null,
        nextCharsToLong: ((nextChars: String) -> Long?)? = null,
        previousCharsToLong: ((previousChars: String) -> Long?)? = null,
    ): Long {
        if (rsn.isBlank()) {
            return notSupportedToRouteId?.invoke(rsn)
                ?: throw MTLog.Fatal("Unexpected route short name '$rsn' to convert to route ID!")
        }
        val matcher: Matcher = RSN.matcher(rsn)
        if (!matcher.find()) {
            return notSupportedToRouteId?.invoke(rsn)
                ?: throw MTLog.Fatal("Unexpected route short name '$rsn' can not be parsed by regex!")
        }
        val previousChars: String = matcher.group(2).uppercase()
        val digits: Long = matcher.group(3).toLong()
        val nextChars: String = matcher.group(4).uppercase()
        if (digits !in 0..1000L) {
            return notSupportedToRouteId?.invoke(rsn)
                ?: throw MTLog.Fatal("Unexpected route short name digits '$digits' in short name '$rsn' to convert to route ID!")
        }
        var routeId: Long = digits
        routeId += when (nextChars) {
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
            else -> {
                nextCharsToLong?.invoke(nextChars)
                    ?: notSupportedToRouteId?.invoke(rsn)
                    ?: throw MTLog.Fatal("Unexpected next characters '$nextChars' in short name '$rsn'!")
            }
        }
        routeId += when (previousChars) {
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
            else -> {
                previousCharsToLong?.invoke(previousChars)
                    ?: notSupportedToRouteId?.invoke(rsn)
                    ?: throw MTLog.Fatal("Unexpected previous characters '$previousChars' in short name '$rsn'!")
            }
        }
        return routeId
    }

    @JvmStatic
    fun startsWith(digit: Long): Long {
        return digit * PREVIOUS
    }

    @JvmStatic
    fun endsWith(digit: Long): Long {
        return digit * NEXT
    }

    @JvmStatic
    fun other(digit: Long): Long {
        return Letters.OTHER_MIN_ + digit
    }
}