package org.mtransit.parser.mt.data

import org.mtransit.parser.MTLog

enum class MInboundType(val id: String) {

    NONE(""),
    INBOUND("1"),
    OUTBOUND("0");

    override fun toString(): String {
        return id
    }

    @Suppress("unused")
    fun intValue(): Int {
        return when (id) {
            INBOUND.id -> 1
            OUTBOUND.id -> 0
            else -> throw MTLog.Fatal("Unknown inbound type '%s'!", id)
        }
    }

    companion object {
        @JvmStatic
        fun parse(id: String?): MInboundType {
            return when {
                INBOUND.id == id -> INBOUND
                OUTBOUND.id == id -> OUTBOUND
                else -> NONE   // default
            }
        }
    }
}