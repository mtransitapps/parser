package org.mtransit.parser.mt.data

import org.mtransit.parser.MTLog

enum class MDirectionInboundType(val id: String) {

    NONE(""),
    INBOUND("1"),
    OUTBOUND("0");

    override fun toString(): String {
        return id
    }

    @Suppress("unused")
    fun intValue(): Int {
        return when (this) {
            INBOUND -> 1
            OUTBOUND -> 0
            else -> throw MTLog.Fatal("Unknown inbound type '%s'!", id)
        }
    }

    companion object {
        @JvmStatic
        fun parse(id: String?): MDirectionInboundType {
            return when(id) {
                INBOUND.id -> INBOUND
                OUTBOUND.id -> OUTBOUND
                else -> NONE   // default
            }
        }
    }
}