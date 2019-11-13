package org.mtransit.parser.gtfs.data;

// https://developers.google.com/transit/gtfs/reference#agency_fields
public class GAgency {

	public static final String FILENAME = "agency.txt";

	public static final String AGENCY_ID = "agency_id";
	private String agency_id;
	public static final String AGENCY_TIMEZONE = "agency_timezone";
	private String agency_timezone;

	public GAgency(String agency_id, String agency_timezone) {
		this.agency_id = agency_id;
		this.agency_timezone = agency_timezone;
	}

	@Override
	public int hashCode() {
		return (this.agency_id == null ? 0 : this.agency_id.hashCode()) + (this.agency_timezone == null ? 0 : this.agency_timezone.hashCode());
	}

	public String getAgencyId() {
		return agency_id;
	}

	public String getAgencyTimezone() {
		return agency_timezone;
	}
}
