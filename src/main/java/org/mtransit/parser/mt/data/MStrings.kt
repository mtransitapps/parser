package org.mtransit.parser.mt.data

import androidx.collection.SparseArrayCompat
import androidx.collection.mutableScatterMapOf
import org.mtransit.commons.FeatureFlags
import org.mtransit.parser.MTLog

object MStrings {

    private val idIntToId = SparseArrayCompat<String>()
    private val idToIdInt = mutableScatterMapOf<String, Int>()

    private val incrementLock = Any()
    private var increment = 0

    init {
        // STATIC STRINGS: (frequently used)
        add("/")
        add("-")
        add("&")
        add("@")
    }

    private const val SPACE = " "

    @JvmStatic
    fun addAll(lastStrings: List<MString>?) {
        lastStrings?.forEach { add(it) }
    }

    fun add(string: MString) {
        synchronized(incrementLock) {
            idIntToId.put(string.id, string.string)
            idToIdInt[string.string] = string.id
            increment = maxOf(increment, string.id)
        }
    }

    fun add(string: String): Int {
        synchronized(incrementLock) {
            increment++ // move to next
            val newstring = MString(increment, string)
            add(newstring)
            return newstring.id
        }
    }

    @Suppress("unused")
    @JvmStatic
    fun getString(id: Int) =
        idIntToId[id] ?: throw MTLog.Fatal("Unexpected string integer $id!")

    @JvmStatic
    fun getInt(string: String): Int =
        idToIdInt[string]
            ?: synchronized(incrementLock) {
                return idToIdInt[string] ?: add(string)
            }

    @JvmStatic
    fun count() = idIntToId.size()

    @JvmStatic
    fun getAll() = buildList {
        idToIdInt.forEach { id, idInt ->
            add(MString(idInt, id))
        }
    }.sorted()

    @JvmStatic
    fun convert(strings: String): String {
        if (!FeatureFlags.F_EXPORT_STRINGS) return strings
        if (strings.isEmpty()) return strings
        return strings
            .split(SPACE)
            .map { getInt(it) }
            .joinToString(separator = SPACE)
    }
}

fun String.toStringIds() = MStrings.convert(this)