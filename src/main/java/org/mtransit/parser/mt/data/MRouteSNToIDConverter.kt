package org.mtransit.parser.mt.data

import org.mtransit.commons.RegexUtils.BEGINNING
import org.mtransit.commons.RegexUtils.DIGIT_CAR
import org.mtransit.commons.RegexUtils.END
import org.mtransit.commons.RegexUtils.any
import org.mtransit.commons.RegexUtils.atLeastOne
import org.mtransit.commons.RegexUtils.group
import org.mtransit.parser.MTLog
import java.util.regex.Matcher
import java.util.regex.Pattern

@Suppress("MemberVisibilityCanBePrivate", "unused")
object MRouteSNToIDConverter {

    private val RSN = Pattern.compile(
        group(BEGINNING + group(any("[A-Z]")) + group(atLeastOne(DIGIT_CAR)) + group(any("[A-Z]")) + END), Pattern.CASE_INSENSITIVE
    )

    const val NONE_: Long = 0L
    const val A: Long = 1L
    const val B: Long = 2L
    const val C: Long = 3L
    const val D: Long = 4L
    const val E: Long = 5L
    const val F: Long = 6L
    const val G: Long = 7L
    const val H: Long = 8L
    const val I: Long = 9L
    const val J: Long = 10L
    const val K: Long = 11L
    const val L: Long = 12L
    const val M: Long = 13L
    const val N: Long = 14L
    const val O: Long = 15L
    const val P: Long = 16L
    const val Q: Long = 17L
    const val R: Long = 18L
    const val S: Long = 19L
    const val T: Long = 20L
    const val U: Long = 21L
    const val V: Long = 22L
    const val W: Long = 23L
    const val X: Long = 24L
    const val Y: Long = 25L
    const val Z: Long = 26L

    const val OTHER_MIN_: Long = 27L

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
            "" -> endsWith(NONE_)
            "A" -> endsWith(A)
            "B" -> endsWith(B)
            "C" -> endsWith(C)
            "D" -> endsWith(D)
            "E" -> endsWith(E)
            "F" -> endsWith(F)
            "G" -> endsWith(G)
            "H" -> endsWith(H)
            "I" -> endsWith(I)
            "J" -> endsWith(J)
            "K" -> endsWith(K)
            "L" -> endsWith(L)
            "M" -> endsWith(M)
            "N" -> endsWith(N)
            "O" -> endsWith(O)
            "P" -> endsWith(P)
            "Q" -> endsWith(Q)
            "R" -> endsWith(R)
            "S" -> endsWith(S)
            "T" -> endsWith(T)
            "U" -> endsWith(U)
            "V" -> endsWith(V)
            "W" -> endsWith(W)
            "X" -> endsWith(X)
            "Y" -> endsWith(Y)
            "Z" -> endsWith(Z)
            else -> {
                nextCharsToLong?.invoke(nextChars)
                    ?: throw MTLog.Fatal("Unexpected next characters '$nextChars' in short name '$rsn'!")
            }
        }
        routeId += when (previousChars) {
            "" -> startsWith(NONE_)
            "A" -> startsWith(A)
            "B" -> startsWith(B)
            "C" -> startsWith(C)
            "D" -> startsWith(D)
            "E" -> startsWith(E)
            "F" -> startsWith(F)
            "G" -> startsWith(G)
            "H" -> startsWith(H)
            "I" -> startsWith(I)
            "J" -> startsWith(J)
            "K" -> startsWith(K)
            "L" -> startsWith(L)
            "M" -> startsWith(M)
            "N" -> startsWith(N)
            "O" -> startsWith(O)
            "P" -> startsWith(P)
            "Q" -> startsWith(Q)
            "R" -> startsWith(R)
            "S" -> startsWith(S)
            "T" -> startsWith(T)
            "U" -> startsWith(U)
            "V" -> startsWith(V)
            "W" -> startsWith(W)
            "X" -> startsWith(X)
            "Y" -> startsWith(Y)
            "Z" -> startsWith(Z)
            else -> {
                previousCharsToLong?.invoke(previousChars)
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
        return OTHER_MIN_ + digit
    }
}