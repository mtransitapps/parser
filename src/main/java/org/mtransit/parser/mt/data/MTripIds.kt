package org.mtransit.parser.mt.data

import androidx.collection.SparseArrayCompat
import androidx.collection.mutableScatterMapOf
import org.mtransit.commons.FeatureFlags
import org.mtransit.parser.MTLog
import org.mtransit.parser.db.SQLUtils.quotesEscapeId

object MTripIds {

    private val incrementLock = Any()

    private var increment = 0
    private val idIntToId = SparseArrayCompat<String>()
    private val idToIdInt = mutableScatterMapOf<String, Int>()

    @JvmStatic
    fun addAll(lastTripIds: List<MTripId>?) {
        lastTripIds?.forEach { add(it) }
    }

    fun add(tripId: MTripId) {
        synchronized(incrementLock) {
            idIntToId.put(tripId.tripIdInt, tripId.tripId)
            idToIdInt[tripId.tripId] = tripId.tripIdInt
            increment = maxOf(increment, tripId.tripIdInt)
        }
    }

    fun add(tripId: String): Int {
        synchronized(incrementLock) {
            increment++ // move to next
            val newTripId = MTripId(increment, tripId)
            add(newTripId)
            return newTripId.tripIdInt
        }
    }

    @JvmStatic
    fun prune(usedTripIds: Set<String>): Int {
        var removedCount = 0
        synchronized(incrementLock) {
            val iterator = idToIdInt.asMutableMap().entries.iterator()
            while (iterator.hasNext()) {
                val (tripId,tripIdInt) = iterator.next()
                if (tripId !in usedTripIds) {
                    idIntToId.remove(tripIdInt)
                    iterator.remove()
                    removedCount++
                }
            }
        }
        return removedCount
    }

    @Suppress("unused")
    @JvmStatic
    fun remove(tripId: MTripId) {
        synchronized(incrementLock) {
            idIntToId.remove(tripId.tripIdInt)
            idToIdInt.remove(tripId.tripId)
        }
    }

    @Suppress("unused")
    @JvmStatic
    fun getId(tripIdInt: Int) =
        idIntToId[tripIdInt] ?: throw MTLog.Fatal("Unexpected Trip ID integer $tripIdInt!")

    @JvmStatic
    fun getInt(tripId: String): Int =
        idToIdInt[tripId]
            ?: synchronized(incrementLock) {
                return idToIdInt[tripId] ?: add(tripId)
            }

    @JvmStatic
    fun count() = idIntToId.size()

    @JvmStatic
    fun getAll() = buildList {
        idToIdInt.forEach { id, idInt ->
            add(MTripId(idInt, id))
        }
    }

    @JvmStatic
    fun getAllSorted() = getAll().sorted()

    @JvmStatic
    fun convert(tripId: String) =
        if (FeatureFlags.F_EXPORT_TRIP_ID_INTS) {
            getInt(tripId).toString()
        } else {
            tripId.quotesEscapeId()
        }
}
