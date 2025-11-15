package org.mtransit.parser.gtfs.data

import androidx.annotation.Discouraged
import org.mtransit.commons.gtfs.data.Agency
import org.mtransit.commons.gtfs.data.AgencyId
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
        agencyId: AgencyId,
        agencyName: String,
        agencyUrl: String,
        agencyTimezone: String,
        agencyLang: String?,
        agencyPhone: String?,
        agencyFareUrl: String?,
        agencyEmail: String?,
    ) : this(
        GIDs.getInt(agencyId),
        agencyName,
        agencyUrl,
        agencyTimezone,
        agencyLang,
        agencyPhone,
        agencyFareUrl,
        agencyEmail,
    )

    @Suppress("unused")
    @get:Discouraged(message = "Not memory efficient")
    val agencyId: AgencyId get() = _agencyId

    private val _agencyId: AgencyId
        get() = GIDs.getString(agencyIdInt)

    @Suppress("unused")
    fun isDifferentAgency(otherAgencyIdInt: Int): Boolean = agencyIdInt != otherAgencyIdInt

    @Discouraged(message = "Not memory efficient")
    @Suppress("unused")
    fun isDifferentAgency(otherAgencyId: AgencyId): Boolean = isDifferentAgency(GIDs.getInt(otherAgencyId))

    @Suppress("unused")
    fun toStringPlus(): String {
        return toString() +
                "+(agencyId:$_agencyId)"
    }

    fun to() = Agency(
        agencyId = _agencyId,
        agencyName = agencyName,
        agencyUrl = agencyUrl,
        agencyTimezone = agencyTimezone,
        agencyLang = agencyLang,
        agencyPhone = agencyPhone,
        agencyFareUrl = agencyFareUrl,
        agencyEmail = agencyEmail,
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

        @JvmStatic
        fun fromLine(line: Map<String, String>) = GAgency(
            agencyId = line[AGENCY_ID].orEmpty(),
            agencyName = line[AGENCY_NAME] ?: throw MTLog.Fatal("Invalid GAgency from $line!"),
            agencyUrl = line[AGENCY_URL] ?: throw MTLog.Fatal("Invalid GAgency from $line!"),
            agencyTimezone = line[AGENCY_TIMEZONE] ?: throw MTLog.Fatal("Invalid GAgency from $line!"),
            agencyLang = line[AGENCY_LANG],
            agencyPhone = line[AGENCY_PHONE],
            agencyFareUrl = line[AGENCY_FARE_URL],
            agencyEmail = line[AGENCY_EMAIL],
        )

        @JvmStatic
        fun from(agencies: Collection<Agency>) = agencies.mapNotNull { from(it) }

        @JvmStatic
        fun from(agency: Agency?) = agency?.let {
            GAgency(
                agencyId = it.agencyId,
                agencyName = it.agencyName,
                agencyUrl = it.agencyUrl,
                agencyTimezone = it.agencyTimezone,
                agencyLang = it.agencyLang,
                agencyPhone = it.agencyPhone,
                agencyFareUrl = it.agencyFareUrl,
                agencyEmail = it.agencyEmail,
            )
        }
    }
}