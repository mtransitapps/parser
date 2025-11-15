package org.mtransit.parser.mt.data

import androidx.collection.SparseArrayCompat
import androidx.collection.mutableScatterMapOf
import org.mtransit.parser.MTLog

object MServiceIds {

    private var increment = 0
    private val intToString = SparseArrayCompat<String>()
    private val stringToInt = mutableScatterMapOf<String, Int>()

    @JvmStatic
    fun addAll(lastServiceIds: List<MServiceId>?) {
        lastServiceIds?.forEach { add(it) }
    }

    fun add(serviceId: MServiceId) {
        intToString.put(serviceId.serviceIdInt, serviceId.serviceId)
        stringToInt[serviceId.serviceId] = serviceId.serviceIdInt
        increment = maxOf(increment, serviceId.serviceIdInt)
    }

    fun add(serviceId: String): Int {
        increment++ // move to next
        val newServiceId = MServiceId(serviceId, increment)
        add(newServiceId)
        return newServiceId.serviceIdInt
    }

    @Suppress("unused")
    @JvmStatic
    fun getId(serviceIdInt: Int): String {
        return intToString[serviceIdInt] ?: throw MTLog.Fatal("Unexpected Service ID integer $serviceIdInt!")
    }

    @JvmStatic
    fun getInt(serviceId: String): Int {
        return stringToInt[serviceId] ?: add(serviceId)
    }

    @JvmStatic
    fun getAll() = buildList {
        stringToInt.forEach { id, idInt ->
            add(MServiceId(id, idInt))
        }
    }
}
