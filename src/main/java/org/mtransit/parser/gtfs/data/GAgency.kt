package org.mtransit.parser.gtfs.data

// https://developers.google.com/transit/gtfs/reference#agency_fields
data class GAgency(
    val agencyId: Int,
    val agencyTimezone: String
) {

    constructor(
        agencyIdString: String,
        agencyTimezone: String
    ) : this(
        GIDs.getInt(agencyIdString),
        agencyTimezone
    )

    val agencyIdString: String
        get() {
            return GIDs.getString(agencyId)
        }

    companion object {
        const val FILENAME = "agency.txt"

        const val AGENCY_ID = "agency_id"
        const val AGENCY_TIMEZONE = "agency_timezone"
    }
}