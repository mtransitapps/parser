package org.mtransit.parser.mt.data

@Deprecated("use MDirectionCardinalType instead")
enum class MDirectionType(val id: String) {

    NONE(MDirectionCardinalType.NONE.id),
    EAST(MDirectionCardinalType.EAST.id),
    WEST(MDirectionCardinalType.WEST.id),
    NORTH(MDirectionCardinalType.NORTH.id),
    SOUTH(MDirectionCardinalType.SOUTH.id);

    override fun toString() = MDirectionCardinalType.parse(id).toString()

    fun intValue() = MDirectionCardinalType.parse(id).intValue()

    companion object {

        private const val WEST_FR = "O"

        @JvmStatic
        fun parse(id: String?): MDirectionType {
            return when {
                MDirectionCardinalType.EAST.id == id -> EAST
                MDirectionCardinalType.WEST.id == id || WEST_FR == id -> WEST
                MDirectionCardinalType.NORTH.id == id -> NORTH
                MDirectionCardinalType.SOUTH.id == id -> SOUTH
                else -> NONE // default
            }
        }
    }
}