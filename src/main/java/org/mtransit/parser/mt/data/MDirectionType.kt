package org.mtransit.parser.mt.data

import org.mtransit.parser.MTLog

enum class MDirectionType(val id: String) {

    NONE(""),
    EAST("E"),
    WEST("W"),
    NORTH("N"),
    SOUTH("S");

    override fun toString(): String {
        return id
    }

    fun intValue(): Int {
        return when (id) {
            EAST.id -> 1
            WEST.id -> 2
            NORTH.id -> 3
            SOUTH.id -> 4
            else -> throw MTLog.Fatal("Unknown direction type '$id'!")
        }
    }

    companion object {

        private const val WEST_FR = "O"

        @JvmStatic
        fun parse(id: String?): MDirectionType {
            return when {
                EAST.id == id -> EAST
                WEST.id == id || WEST_FR == id -> WEST
                NORTH.id == id -> NORTH
                SOUTH.id == id -> SOUTH
                else -> NONE // default
            }
        }
    }
}