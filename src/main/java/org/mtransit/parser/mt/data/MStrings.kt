package org.mtransit.parser.mt.data

import androidx.collection.SparseArrayCompat
import androidx.collection.mutableScatterMapOf
import org.mtransit.commons.GTFSCommons
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

    @JvmStatic
    fun addAll(lastStrings: List<MString>?) {
        synchronized(incrementLock) {
            lastStrings?.forEach { addSynchronized(it) }
        }
    }

    private fun addSynchronized(string: MString) {
        idIntToId.put(string.id, string.string)
        idToIdInt[string.string] = string.id
        increment = maxOf(increment, string.id)
    }

    fun add(string: String): Int {
        synchronized(incrementLock) {
            increment++ // move to next
            return MString(increment, string)
                .apply { addSynchronized(this) }
                .id
        }
    }

    @Suppress("unused")
    @JvmStatic
    fun getString(id: Int) =
        idIntToId[id] ?: throw MTLog.Fatal("Unexpected string integer $id!")

    @JvmStatic
    fun getInt(string: String): Int =
        idToIdInt[string]
            ?: synchronized(incrementLock) { idToIdInt[string] ?: add(string) }

    @JvmStatic
    fun count() = idIntToId.size()

    @JvmStatic
    fun getAll() = buildList {
        idToIdInt.forEach { id, idInt ->
            add(MString(idInt, id))
        }
    }.sorted()

    @JvmStatic
    fun convert(strings: String, enabled: Boolean): String {
        if (!enabled) return strings
        if (strings.isEmpty()) return strings
        return strings
            .split(GTFSCommons.STRINGS_SEPARATOR)
            .map { getInt(it) }
            .joinToString(GTFSCommons.STRINGS_SEPARATOR)
    }
}

fun String.toStringIds(enabled: Boolean) = MStrings.convert(this, enabled)