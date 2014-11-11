package org.mtransit.parser.gtfs.data;

// https://developers.google.com/transit/gtfs/reference#agency_fields
public class GAgency {

	public static final String FILENAME = "agency.txt";

	public static final String AGENCY_ID = "agency_id";
	public String agency_id;
	public static final String AGENCY_NAME = "agency_name";
	public String agency_name;
	public static final String AGENCY_URL = "agency_url";
	public String agency_url;
	public static final String AGENCY_TIMEZONE = "agency_timezone";
	public String agency_timezone;

	public static final String AGENCY_LANG = "agency_lang";
	public String agency_lang;
	public static final String AGENCY_PHONE = "agency_phone";
	public String agency_phone;
	public static final String AGENCY_FARE_URL = "agency_fare_url";
	public String agency_fare_url;

	public GAgency(String agency_id, String agency_name, String agency_url, String agency_timezone) {
		this.agency_id = agency_id;
		this.agency_name = agency_name;
		this.agency_url = agency_url;
		this.agency_timezone = agency_timezone;
	}
}
