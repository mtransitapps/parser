package org.mtransit.parser.gtfs.data

import org.mtransit.parser.Constants

// https://developers.google.com/transit/gtfs/reference#agency_fields
data class GAgency(
    val agencyIdInt: Int,
    val agencyTimezone: String
) {

    constructor(
        agencyId: String?,
        agencyTimezone: String
    ) : this(
        GIDs.getInt(agencyId ?: MISSING_AGENCY_ID),
        agencyTimezone
    )

    @Deprecated(message = "Not memory efficient")
    @Suppress("unused")
    val agencyId = _agencyId

    private val _agencyId: String
        get() {
            return GIDs.getString(agencyIdInt)
        }

    @Suppress("unused")
    fun isDifferentAgency(otherAgencyIdInt: Int): Boolean = agencyIdInt != otherAgencyIdInt

    @Deprecated(message = "Not memory efficient")
    @Suppress("unused")
    fun isDifferentAgency(otherAgencyId: String): Boolean = isDifferentAgency(GIDs.getInt(otherAgencyId))

    companion object {
        const val FILENAME = "agency.txt"

        const val AGENCY_ID = "agency_id"
        const val AGENCY_TIMEZONE = "agency_timezone"

        const val MISSING_AGENCY_ID = Constants.EMPTY;
    }
}