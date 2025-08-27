package org.mtransit.parser.mt.data

import org.mtransit.parser.Constants

@Suppress("unused")
@Deprecated("use MDirection instead") // TBD once all agency parsers migrated (or 0-code #JSON)
data class MTrip(
    val routeId: Long,
    var headsignId: Int = 0, // >= 0
    var headsignType: Int = HEADSIGN_TYPE_STRING, // 0=string, 1=direction, 2=inbound, 3=stopId, 4=descent-only
    var headsignValue: String = Constants.EMPTY,
) {

    fun toDirection() = MDirection(
        routeId = routeId,
        headsignId = headsignId,
        headsignType = headsignType,
        headsignValue = headsignValue,
    )

    companion object {

        const val HEADSIGN_TYPE_STRING = 0
        const val HEADSIGN_TYPE_DIRECTION = 1
        const val HEADSIGN_TYPE_INBOUND = 2
        const val HEADSIGN_TYPE_STOP_ID = 3
        const val HEADSIGN_TYPE_NO_PICKUP = 4

        @Deprecated("use MDirection.mergeHeadsignValue() instead", ReplaceWith("MDirection.mergeHeadsignValue(mDirectionHeadsign, mDirectionHeadsignToMerge)"))
        @JvmStatic
        fun mergeHeadsignValue(
            mDirectionHeadsign: String?,
            mDirectionHeadsignToMerge: String?
        ): String? {
            return MDirection.mergeHeadsignValue(mDirectionHeadsign, mDirectionHeadsignToMerge)
        }

        @Suppress("DEPRECATION")
        fun fromDirection(direction: MDirection) = MTrip(
            routeId = direction.routeId,
            headsignId = direction.headsignId,
            headsignType = direction.headsignType,
            headsignValue = direction.headsignValue,
        )
    }
}