package org.mtransit.parser.gtfs.data

enum class GDirectionType(val value: String) {
    NORTH("North"),
    SOUTH("South"),
    EAST("East"),
    WEST("West"),
    NORTH_EAST("Northeast"),
    NORTH_WEST("Northwest"),
    SOUTH_EAST("Southeast"),
    SOUTH_WEST("Southwest"),
    CLOCKWISE("Clockwise"),
    COUNTER_CLOCKWISE("Counterclockwise"),
    INBOUND("Inbound"),
    OUTBOUND("Outbound"),
    LOOP("Loop"),
    A_LOOP("A Loop"), // or "Loop A"? (not sure)
    B_LOOP("B Loop"), // or "Loop B"? (not sure)
    UNKNOWN("Unknown");

    companion object {
        fun parse(value: String?): GDirectionType {
            val valueLC = value?.lowercase()
            return entries.firstOrNull { it.value.lowercase() == valueLC } ?: UNKNOWN
        }
    }
}