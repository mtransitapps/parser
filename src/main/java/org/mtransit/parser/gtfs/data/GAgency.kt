package org.mtransit.parser.gtfs.data

import org.mtransit.commons.gtfs.data.Agency
import org.mtransit.parser.Constants
import org.mtransit.parser.MTLog

// https://gtfs.org/schedule/reference/#agencytxt
// https://developers.google.com/transit/gtfs/reference#agency_fields
data class GAgency(
    val agencyIdInt: Int,
    val agencyName: String,
    val agencyUrl: String,
    val agencyTimezone: String,
    val agencyLang: String?,
    val agencyPhone: String?,
    val agencyFareUrl: String?,
    val agencyEmail: String?,
) {

    constructor(
        agencyId: String?,
        agencyName: String,
        agencyUrl: String,
        agencyTimezone: String,
        agencyLang: String?,
        agencyPhone: String?,
        agencyFareUrl: String?,
        agencyEmail: String?,
    ) : this(
        GIDs.getInt(agencyId ?: MISSING_AGENCY_ID),
        agencyName,
        agencyUrl,
        agencyTimezone,
        agencyLang,
        agencyPhone,
        agencyFareUrl,
        agencyEmail,
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

    @Deprecated(message = "Not memory efficient", replaceWith = ReplaceWith("isDifferentAgency(GIDs.getInt(otherAgencyId))"))
    @Suppress("unused")
    fun isDifferentAgency(otherAgencyId: String): Boolean = isDifferentAgency(GIDs.getInt(otherAgencyId))

    fun to() = Agency(
        _agencyId,
        agencyName,
        agencyUrl,
        agencyTimezone,
        agencyLang,
        agencyPhone,
        agencyFareUrl,
        agencyEmail,
    )

    companion object {
        const val FILENAME = "agency.txt"

        private const val AGENCY_ID = "agency_id" // Conditionally Required
        private const val AGENCY_NAME = "agency_name" // Required
        private const val AGENCY_URL = "agency_url" // Required
        private const val AGENCY_TIMEZONE = "agency_timezone" // Required

        private const val AGENCY_LANG = "agency_lang" // Optional
        private const val AGENCY_PHONE = "agency_phone" // Optional
        private const val AGENCY_FARE_URL = "agency_fare_url" // Optional
        private const val AGENCY_EMAIL = "agency_email" // Optional

        const val MISSING_AGENCY_ID = Constants.EMPTY

        @JvmStatic
        fun fromLine(line: Map<String, String>) = GAgency(
            line[AGENCY_ID],
            line[AGENCY_NAME] ?: throw MTLog.Fatal("Invalid GAgency from $line!"),
            line[AGENCY_URL] ?: throw MTLog.Fatal("Invalid GAgency from $line!"),
            line[AGENCY_TIMEZONE] ?: throw MTLog.Fatal("Invalid GAgency from $line!"),
            line[AGENCY_LANG],
            line[AGENCY_PHONE],
            line[AGENCY_FARE_URL],
            line[AGENCY_EMAIL],
        )

        @JvmStatic
        fun from(agency: Collection<Agency>) = agency.map { from(it) }

        @JvmStatic
        fun from(agency: Agency?) = agency?.let {
            GAgency(
                it.agencyId,
                it.agencyName,
                it.agencyUrl,
                it.agencyTimezone,
                it.agencyLang,
                it.agencyPhone,
                it.agencyFareUrl,
                it.agencyEmail,
            )
        }
    }
}