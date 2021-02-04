package org.mtransit.parser.mt.data

import org.mtransit.parser.Constants
import org.mtransit.parser.DefaultAgencyTools
import org.mtransit.parser.MTLog
import org.mtransit.parser.db.SQLUtils

data class MTrip(
    val routeId: Long,
    var headsignId: Int = 0, // >= 0
    var headsignType: Int = HEADSIGN_TYPE_STRING, // 0=string, 1=direction, 2=inbound, 3=stopId, 4=descent-only
    var headsignValue: String = Constants.EMPTY
) : Comparable<MTrip> {

    constructor(
        routeId: Long
    ) : this(
        routeId,
        0,
        HEADSIGN_TYPE_STRING,
        Constants.EMPTY
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

    @Suppress("unused")
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

    @Suppress("unused")
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

    @Suppress("unused")
    fun setHeadsignDescentOnly(): MTrip {
        headsignType = HEADSIGN_TYPE_DESCENT_ONLY
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

    fun toFile(): String {
        val sb = StringBuilder() //
        sb.append(id) // ID
        sb.append(Constants.COLUMN_SEPARATOR) //
        if (DefaultAgencyTools.EXPORT_DESCENT_ONLY) {
            sb.append(headsignType) // HEADSIGN TYPE
            sb.append(Constants.COLUMN_SEPARATOR) //
            sb.append(SQLUtils.quotes(SQLUtils.escape(headsignValue))) // HEADSIGN STRING
            sb.append(Constants.COLUMN_SEPARATOR) //
        } else {
            if (headsignType == HEADSIGN_TYPE_DESCENT_ONLY) {
                sb.append(HEADSIGN_TYPE_STRING) // HEADSIGN TYPE
                sb.append(Constants.COLUMN_SEPARATOR) //
                sb.append(SQLUtils.quotes(SQLUtils.escape("Drop Off Only"))) // HEADSIGN STRING
                sb.append(Constants.COLUMN_SEPARATOR) //
            } else {
                sb.append(headsignType) // HEADSIGN TYPE
                sb.append(Constants.COLUMN_SEPARATOR) //
                sb.append(SQLUtils.quotes(SQLUtils.escape(headsignValue))) // HEADSIGN STRING
                sb.append(Constants.COLUMN_SEPARATOR) //
            }
        }
        sb.append(routeId) // ROUTE ID
        return sb.toString()
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
        const val HEADSIGN_TYPE_DESCENT_ONLY = 4

        private const val ZERO = "0"

        @JvmStatic
        fun getNewId(routeId: Long, headsignId: Int): Long {
            return "$routeId$ZERO$headsignId".toLong()
        }

        @Suppress("unused")
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
            if (mTripHeadsignToMerge == null || mTripHeadsignToMerge.isEmpty()) {
                return mTripHeadsign
            }
            if (mTripHeadsign == null || mTripHeadsign.isEmpty()) {
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