package org.mtransit.parser.gtfs.data

import org.mtransit.parser.MTLog

// https://gtfs.org/reference/static/#tripstxt
enum class GDirectionId(val id: Int) {

    OUTBOUND(0),
    INBOUND(1),

    NEW_1(19),
    NEW_2(29),

    NONE(9);

    fun originalId(): Int? {
        return when (this) {
            OUTBOUND -> OUTBOUND.id
            INBOUND -> INBOUND.id
            else -> null // NONE or custom
        }
    }

    companion object {
        fun parse(id: Int): GDirectionId {
            return when (id) {
                OUTBOUND.id -> OUTBOUND
                INBOUND.id -> INBOUND
                NEW_1.id -> NEW_1
                NEW_2.id -> NEW_2
                NONE.id -> NONE // default
                else -> throw MTLog.Fatal("Unexpected direction ID '$id' to parse!")
            }
        }

        @JvmStatic
        fun parse(id: Int?): GDirectionId {
            return id?.let { parse(it) } ?: NONE
        }
    }
}