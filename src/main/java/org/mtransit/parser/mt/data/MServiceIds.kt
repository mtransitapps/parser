package org.mtransit.parser.mt.data

import androidx.collection.SparseArrayCompat
import androidx.collection.mutableScatterMapOf
import org.mtransit.parser.MTLog

object MServiceIds {

    private var increment = 0
    private val idIntToId = SparseArrayCompat<String>()
    private val idToIdInt = mutableScatterMapOf<String, Int>()

    @JvmStatic
    fun addAll(lastServiceIds: List<MServiceId>?) {
        lastServiceIds?.forEach { add(it) }
    }

    fun add(serviceId: MServiceId) {
        idIntToId.put(serviceId.serviceIdInt, serviceId.serviceId)
        idToIdInt[serviceId.serviceId] = serviceId.serviceIdInt
        increment = maxOf(increment, serviceId.serviceIdInt)
    }

    fun add(serviceId: String): Int {
        increment++ // move to next
        val newServiceId = MServiceId(increment, serviceId)
        add(newServiceId)
        return newServiceId.serviceIdInt
    }

    @Suppress("unused")
    @JvmStatic
    fun getString(serviceIdInt: Int): String {
        return idIntToId[serviceIdInt] ?: throw MTLog.Fatal("Unexpected Service ID integer $serviceIdInt!")
    }

    @JvmStatic
    fun getInt(serviceId: String): Int {
        return idToIdInt[serviceId] ?: add(serviceId)
    }

    @JvmStatic
    fun getAll() = buildList {
        idToIdInt.forEach { id, idInt ->
            add(MServiceId(idInt, id))
        }
    }.sorted()
}
