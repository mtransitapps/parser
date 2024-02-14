package org.mtransit.parser.gtfs.data

import androidx.collection.mutableScatterMapOf
import org.mtransit.parser.Constants.EMPTY
import org.mtransit.parser.MTLog

object GIDs {

    private val integerToString = mutableScatterMapOf<Int, String>()

    @JvmStatic
    fun getString(integer: Int): String {
        return integerToString[integer] ?: throw MTLog.Fatal("Unexpected GID integer $integer!")
    }

    @JvmStatic
    fun getInt(string: String): Int {
        return add(string)
    }

    private fun add(newString: String): Int {
        return newString.hashCode()
            .also { integerToString[it] = newString }
    }

    @JvmStatic
    fun count(): Int {
        return integerToString.size
    }

    @Suppress("unused")
    @JvmStatic
    fun toStringPlus(integer: Int?): String {
        return integer?.let { if (it != -1) getString(it) else EMPTY } ?: EMPTY
    }

    @Suppress("unused")
    @JvmStatic
    fun toStringPlus(integer: Pair<Int?, Int?>?): String {
        return integer?.let { toStringPlus(it.first) + ", " + toStringPlus(it.second) } ?: EMPTY
    }

    @Suppress("unused")
    @JvmStatic
    fun toStringPlus(integers: Iterable<Int?>?): String {
        return integers?.joinToString { toStringPlus(it) } ?: EMPTY
    }

    @Suppress("unused")
    @JvmStatic
    fun toStringPlusP1(integers: Iterable<org.mtransit.parser.Pair<Int, Int>>): String {
        return integers.joinToString { "[1:" + toStringPlus(it.first) + "|2:" + it.second + "]" }
    }
}