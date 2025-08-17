package org.mtransit.parser.mt.data

import org.mtransit.parser.Constants
import org.mtransit.parser.MTLog
import org.mtransit.parser.db.SQLUtils.quotesEscape

@Suppress("unused")
data class MDirection(
    val routeId: Long,
    var headsignId: Int = 0, // >= 0 (almost = direction ID)
    var headsignType: Int = HEADSIGN_TYPE_STRING, // 0=string, 1=direction, 2=inbound, 3=stopId, 4=descent-only
    var headsignValue: String = Constants.EMPTY,
) : Comparable<MDirection> {

    constructor(
        routeId: Long
    ) : this(
        routeId = routeId,
        headsignId = 0,
        headsignType = HEADSIGN_TYPE_STRING,
        headsignValue = Constants.EMPTY,
    )

    constructor(
        routeId: Long,
        headsignString: String,
        headsignId: Int
    ) : this(routeId) {
        setHeadsignString(headsignString, headsignId)
    }

    constructor(
        routeId: Long,
        direction: MDirectionCardinalType
    ) : this(routeId) {
        setHeadsignDirection(direction)
    }

    constructor(
        routeId: Long,
        inbound: MDirectionInboundType
    ) : this(routeId) {
        setHeadsignInbound(inbound)
    }

    constructor(
        routeId: Long,
        stop: MStop
    ) : this(routeId) {
        setHeadsignStop(stop)
    }

    private var _id: Long = -1
    val id: Long
        get() {
            if (_id < 0) {
                _id = getNewId(routeId, headsignId)
            }
            return _id
        }

    fun setHeadsignStringNotEmpty(
        headsignString: String,
        headsignId: Int
    ): MDirection {
        if (headsignString.isEmpty() || headsignId < 0) {
            throw MTLog.Fatal("Invalid direction head sign string '$headsignString' or ID '$headsignId' ($this)!")
        }
        return setHeadsignString(headsignString, headsignId)
    }

    fun setHeadsignString(
        headsignString: String,
        headsignId: Int
    ): MDirection {
        headsignType = HEADSIGN_TYPE_STRING
        headsignValue = headsignString
        if (headsignId < 0) {
            throw MTLog.Fatal("Invalid direction head-sign for '$headsignId'!")
        }
        this.headsignId = headsignId
        _id = -1 // reset
        return this
    }

    fun setHeadsignDirection(direction: MDirectionCardinalType): MDirection {
        headsignType = HEADSIGN_TYPE_DIRECTION
        headsignValue = direction.id
        headsignId = direction.intValue()
        _id = -1 // reset
        return this
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun setHeadsignInbound(inbound: MDirectionInboundType): MDirection {
        headsignType = HEADSIGN_TYPE_INBOUND
        headsignValue = inbound.id
        headsignId = inbound.id.toInt()
        _id = -1 // reset
        return this
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun setHeadsignStop(stop: MStop): MDirection {
        headsignType = HEADSIGN_TYPE_STOP_ID
        headsignValue = stop.id.toString()
        if (stop.id < 0) {
            throw MTLog.Fatal("Invalid direction head-sign for '$stop'!")
        }
        headsignId = stop.id
        _id = -1 // reset
        return this
    }

    fun setHeadsignDescentOnly(): MDirection {
        headsignType = HEADSIGN_TYPE_NO_PICKUP
        headsignValue = Constants.EMPTY // null;
        headsignId = 0
        _id = -1 // reset
        return this
    }

    fun equalsExceptHeadsignValue(obj: MDirection?): Boolean {
        return when {
            obj !is MDirection -> {
                false
            }

            obj.headsignType != headsignType -> {
                false
            }

            else -> !(obj.routeId != 0L && obj.routeId != routeId)
        }
    }

    @Suppress("SameReturnValue")
    fun mergeHeadsignValue(mDirectionToMerge: MDirection?): Boolean {
        return when {
            mDirectionToMerge == null || mDirectionToMerge.headsignValue.isEmpty() -> {
                MTLog.log("$routeId: mergeHeadsignValue() > no direction heading value to merge > $headsignValue.")
                true
            }

            headsignValue.isEmpty() -> {
                headsignValue = mDirectionToMerge.headsignValue
                MTLog.log("$routeId: mergeHeadsignValue() > no current headsign value > $headsignValue.")
                true
            }

            mDirectionToMerge.headsignValue.contains(headsignValue) -> {
                headsignValue = mDirectionToMerge.headsignValue
                true
            }

            headsignValue.contains(mDirectionToMerge.headsignValue) -> {
                true
            }

            headsignValue > mDirectionToMerge.headsignValue -> {
                headsignValue =
                    mDirectionToMerge.headsignValue + SLASH + headsignValue
                MTLog.log("$routeId: mergeHeadsignValue() > merge 2 headsign value > $headsignValue.")
                true
            }

            else -> {
                headsignValue =
                    headsignValue + SLASH + mDirectionToMerge.headsignValue
                MTLog.log("$routeId: mergeHeadsignValue() > merge 2 headsign value > $headsignValue.")
                true
            }
        }
    }

    @Suppress("SameReturnValue")
    fun toFile() = buildString {
        append(id) // ID
        append(Constants.COLUMN_SEPARATOR) //
        append(headsignType) // HEADSIGN TYPE
        append(Constants.COLUMN_SEPARATOR) //
        append(headsignValue.quotesEscape()) // HEADSIGN STRING
        append(Constants.COLUMN_SEPARATOR) //
        append(routeId) // ROUTE ID
    }

    override fun compareTo(other: MDirection): Int {
        // sort by direction's route id => direction id
        return if (routeId != other.routeId) {
            routeId.compareTo(other.routeId)
        } else {
            headsignValue.compareTo(other.headsignValue)
        }
    }

    companion object {

        const val HEADSIGN_TYPE_STRING = 0
        const val HEADSIGN_TYPE_DIRECTION = 1
        const val HEADSIGN_TYPE_INBOUND = 2
        const val HEADSIGN_TYPE_STOP_ID = 3
        const val HEADSIGN_TYPE_NO_PICKUP = 4

        private const val ZERO = "0"

        @JvmStatic
        fun getNewId(routeId: Long, headsignId: Int): Long {
            return "$routeId$ZERO$headsignId".toLong()
        }

        @JvmStatic
        fun mergeEmpty(
            mDirection: MDirection,
            mDirectionToMerge: MDirection
        ): Boolean {
            return when {
                mDirection.headsignValue.isEmpty() -> {
                    mDirection.setHeadsignString(mDirectionToMerge.headsignValue, mDirection.headsignId)
                    true // merged
                }

                mDirectionToMerge.headsignValue.isEmpty() -> {
                    mDirection.setHeadsignString(mDirection.headsignValue, mDirection.headsignId)
                    true // merged
                }

                else -> {
                    false // not merged
                }
            }
        }

        private const val SLASH = " / "

        @JvmStatic
        fun mergeHeadsignValue(
            mDirectionHeadsign: String?,
            mDirectionHeadsignToMerge: String?
        ): String? {
            if (mDirectionHeadsignToMerge.isNullOrEmpty()) {
                return mDirectionHeadsign
            }
            if (mDirectionHeadsign.isNullOrEmpty()) {
                return mDirectionHeadsignToMerge
            }
            if (mDirectionHeadsignToMerge.contains(mDirectionHeadsign)) {
                return mDirectionHeadsignToMerge
            }
            if (mDirectionHeadsign.contains(mDirectionHeadsignToMerge)) {
                return mDirectionHeadsign
            }
            return if (mDirectionHeadsign > mDirectionHeadsignToMerge) {
                mDirectionHeadsignToMerge + SLASH + mDirectionHeadsign
            } else {
                mDirectionHeadsign + SLASH + mDirectionHeadsignToMerge
            }
        }
    }
}