package org.mtransit.parser.mt.data

import androidx.collection.SparseArrayCompat
import androidx.collection.mutableScatterMapOf
import org.mtransit.commons.FeatureFlags
import org.mtransit.parser.MTLog
import org.mtransit.parser.db.SQLUtils.escapeId
import org.mtransit.parser.gtfs.GAgencyTools

object MServiceIds {

    private val incrementLock = Any()

    private var increment = 0
    private val idIntToId = SparseArrayCompat<String>()
    private val idToIdInt = mutableScatterMapOf<String, Int>()

    @JvmStatic
    fun addAll(lastServiceIds: List<MServiceId>?) {
        lastServiceIds?.forEach { add(it) }
    }

    fun add(serviceId: MServiceId) {
        synchronized(incrementLock) {
            idIntToId.put(serviceId.serviceIdInt, serviceId.serviceId)
            idToIdInt[serviceId.serviceId] = serviceId.serviceIdInt
            increment = maxOf(increment, serviceId.serviceIdInt)
        }
    }

    fun add(serviceId: String): Int {
        synchronized(incrementLock) {
            increment++ // move to next
            val newServiceId = MServiceId(increment, serviceId)
            add(newServiceId)
            return newServiceId.serviceIdInt
        }
    }

    @Suppress("unused")
    @JvmStatic
    fun getId(serviceIdInt: Int) =
        idIntToId[serviceIdInt] ?: throw MTLog.Fatal("Unexpected Service ID integer $serviceIdInt!")

    @JvmStatic
    fun getInt(serviceId: String): Int =
        idToIdInt[serviceId]
            ?: synchronized(incrementLock) {
                return idToIdInt[serviceId] ?: add(serviceId)
            }

    @JvmStatic
    fun count() = idIntToId.size()

    @JvmStatic
    fun getAll() = buildList {
        idToIdInt.forEach { id, idInt ->
            add(MServiceId(idInt, id))
        }
    }.sorted()

    @JvmStatic
    fun containsAllIdInts(idInts: Iterable<Int>)
        = idInts.all { idIntToId.containsKey(it) }

    @JvmStatic
    fun convert(agencyTools: GAgencyTools, serviceId: String) =
        if (FeatureFlags.F_EXPORT_SERVICE_ID_INTS) {
            getInt(agencyTools.cleanServiceId(serviceId)).toString()
        } else {
            agencyTools.cleanServiceId(serviceId).escapeId()
        }
}

fun String.convertServiceId(agencyTools: GAgencyTools) = MServiceIds.convert(agencyTools, this)
