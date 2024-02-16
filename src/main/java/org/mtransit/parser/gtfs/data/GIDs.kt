package org.mtransit.parser.gtfs.data

import androidx.collection.SparseArrayCompat
import androidx.collection.mutableScatterMapOf
import org.mtransit.parser.Constants.EMPTY
import org.mtransit.parser.MTLog

object GIDs {

    private const val USE_HASHCODE = false // hashcode Collision is real
    // "11667511__MCOB-DO:123:0:Weekday:2:23SEP:41054:12345".hashCode() == "11612895__MCOB-DO:123:0:Weekday:1:23SEP:31011:12345".hashCode()

    private var increment = 0

    private val intToString = SparseArrayCompat<String>()
    private val stringToInt = mutableScatterMapOf<String, Int>()

    @JvmStatic
    fun getString(integer: Int): String {
        return intToString[integer] ?: throw MTLog.Fatal("Unexpected GID integer $integer!")
    }

    @JvmStatic
    fun getInt(string: String): Int {
        if (USE_HASHCODE) {
            return add(string)
        }
        return stringToInt[string] ?: add(string)
    }

    private fun add(newString: String): Int {
        if (USE_HASHCODE) {
            return newString.hashCode()
                .also {
                    intToString.put(it, newString)
                }
        }
        val newInteger = increment
        intToString.put(newInteger, newString)
        stringToInt[newString] = newInteger
        increment++ // ready for next call
        return newInteger
    }

    @JvmStatic
    fun count(): Int {
        if (USE_HASHCODE) {
            return intToString.size()
        }
        return increment
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