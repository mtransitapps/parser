package org.mtransit.parser.gtfs.data

import org.mtransit.parser.Constants
import org.mtransit.parser.MTLog

object GIDs {

    private var increment = 0

    private val integerToString = hashMapOf<Int, String>()
    private val stringToInteger = hashMapOf<String, Int>()

    @JvmStatic
    fun getString(integer: Int): String {
        return integerToString[integer] ?: throw MTLog.Fatal("Unexpected GID integer $integer!")
    }

    @JvmStatic
    fun getInt(string: String): Int {
        return stringToInteger[string] ?: add(string)
    }

    private fun add(newString: String): Int {
        val newInteger = increment
        integerToString[newInteger] = newString
        stringToInteger[newString] = newInteger
        increment++ // ready for next call
        return newInteger
    }

    @JvmStatic
    fun count(): Int {
        return increment
    }

    @Suppress("unused")
    @JvmStatic
    fun toStringPlus(integer: Int?): String {
        return integer?.let { getString(it) } ?: Constants.EMPTY
    }

    @Suppress("unused")
    @JvmStatic
    fun toStringPlus(integer: Pair<Int?, Int?>?): String {
        return integer?.let { toStringPlus(it.first) + ", " + toStringPlus(it.second) } ?: Constants.EMPTY
    }

    @Suppress("unused")
    @JvmStatic
    fun toStringPlus(integers: Iterable<Int>): String {
        return integers.joinToString { getString(it) }
    }

    @Suppress("unused")
    @JvmStatic
    fun toStringPlusP1(integers: Iterable<org.mtransit.parser.Pair<Int, Int>>): String {
        return integers.joinToString { "[1:" + getString(it.first) + "|2:" + it.second + "]" }
    }
}