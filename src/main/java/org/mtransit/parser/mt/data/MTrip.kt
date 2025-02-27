package org.mtransit.parser.mt.data

import org.mtransit.parser.Constants
import org.mtransit.parser.MTLog
import org.mtransit.parser.db.SQLUtils.quotesEscape

@Suppress("unused")
data class MTrip(
    val routeId: Long,
    var headsignId: Int = 0, // >= 0
    var headsignType: Int = HEADSIGN_TYPE_STRING, // 0=string, 1=direction, 2=inbound, 3=stopId, 4=descent-only
    var headsignValue: String = Constants.EMPTY,
) : Comparable<MTrip> {

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
        direction: MDirectionType
    ) : this(routeId) {
        setHeadsignDirection(direction)
    }

    constructor(
        routeId: Long,
        inbound: MInboundType
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
    ): MTrip {
        if (headsignString.isEmpty() || headsignId < 0) {
            throw MTLog.Fatal("Invalid trip head sign string '$headsignString' or ID '$headsignId' ($this)!")
        }
        return setHeadsignString(headsignString, headsignId)
    }

    fun setHeadsignString(
        headsignString: String,
        headsignId: Int
    ): MTrip {
        headsignType = HEADSIGN_TYPE_STRING
        headsignValue = headsignString
        if (headsignId < 0) {
            throw MTLog.Fatal("Invalid trip head-sign for '$headsignId'!")
        }
        this.headsignId = headsignId
        _id = -1 // reset
        return this
    }

    fun setHeadsignDirection(direction: MDirectionType): MTrip {
        headsignType = HEADSIGN_TYPE_DIRECTION
        headsignValue = direction.id
        headsignId = direction.intValue()
        _id = -1 // reset
        return this
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun setHeadsignInbound(inbound: MInboundType): MTrip {
        headsignType = HEADSIGN_TYPE_INBOUND
        headsignValue = inbound.id
        headsignId = inbound.id.toInt()
        _id = -1 // reset
        return this
    }

    @Suppress("MemberVisibilityCanBePrivate")
    fun setHeadsignStop(stop: MStop): MTrip {
        headsignType = HEADSIGN_TYPE_STOP_ID
        headsignValue = stop.id.toString()
        if (stop.id < 0) {
            throw MTLog.Fatal("Invalid trip head-sign for '$stop'!")
        }
        headsignId = stop.id
        _id = -1 // reset
        return this
    }

    fun setHeadsignDescentOnly(): MTrip {
        headsignType = HEADSIGN_TYPE_NO_PICKUP
        headsignValue = Constants.EMPTY // null;
        headsignId = 0
        _id = -1 // reset
        return this
    }

    fun equalsExceptHeadsignValue(obj: MTrip?): Boolean {
        return when {
            obj !is MTrip -> {
                false
            }

            obj.headsignType != headsignType -> {
                false
            }

            else -> !(obj.routeId != 0L && obj.routeId != routeId)
        }
    }

    @Suppress("SameReturnValue")
    fun mergeHeadsignValue(mTripToMerge: MTrip?): Boolean {
        return when {
            mTripToMerge == null || mTripToMerge.headsignValue.isEmpty() -> {
                MTLog.log("$routeId: mergeHeadsignValue() > no trip heading value to merge > $headsignValue.")
                true
            }

            headsignValue.isEmpty() -> {
                headsignValue = mTripToMerge.headsignValue
                MTLog.log("$routeId: mergeHeadsignValue() > no current headsign value > $headsignValue.")
                true
            }

            mTripToMerge.headsignValue.contains(headsignValue) -> {
                headsignValue = mTripToMerge.headsignValue
                true
            }

            headsignValue.contains(mTripToMerge.headsignValue) -> {
                true
            }

            headsignValue > mTripToMerge.headsignValue -> {
                headsignValue =
                    mTripToMerge.headsignValue + SLASH + headsignValue
                MTLog.log("$routeId: mergeHeadsignValue() > merge 2 headsign value > $headsignValue.")
                true
            }

            else -> {
                headsignValue =
                    headsignValue + SLASH + mTripToMerge.headsignValue
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

    override fun compareTo(other: MTrip): Int {
        // sort by trip route id => trip id
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
            mTrip: MTrip,
            mTripToMerge: MTrip
        ): Boolean {
            return when {
                mTrip.headsignValue.isEmpty() -> {
                    mTrip.setHeadsignString(mTripToMerge.headsignValue, mTrip.headsignId)
                    true // merged
                }

                mTripToMerge.headsignValue.isEmpty() -> {
                    mTrip.setHeadsignString(mTrip.headsignValue, mTrip.headsignId)
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
            mTripHeadsign: String?,
            mTripHeadsignToMerge: String?
        ): String? {
            if (mTripHeadsignToMerge.isNullOrEmpty()) {
                return mTripHeadsign
            }
            if (mTripHeadsign.isNullOrEmpty()) {
                return mTripHeadsignToMerge
            }
            if (mTripHeadsignToMerge.contains(mTripHeadsign)) {
                return mTripHeadsignToMerge
            }
            if (mTripHeadsign.contains(mTripHeadsignToMerge)) {
                return mTripHeadsign
            }
            return if (mTripHeadsign > mTripHeadsignToMerge) {
                mTripHeadsignToMerge + SLASH + mTripHeadsign
            } else {
                mTripHeadsign + SLASH + mTripHeadsignToMerge
            }
        }
    }
}