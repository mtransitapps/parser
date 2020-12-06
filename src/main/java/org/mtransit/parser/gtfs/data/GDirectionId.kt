package org.mtransit.parser.gtfs.data

import org.mtransit.parser.MTLog

// https://developers.google.com/transit/gtfs/reference#tripstxt
// https://gtfs.org/reference/static/#tripstxt
enum class GDirectionId(private val id: Int) {

    OUTBOUND(0),
    INBOUND(1),

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