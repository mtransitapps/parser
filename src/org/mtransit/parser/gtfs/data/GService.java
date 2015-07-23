package org.mtransit.parser.gtfs.data;

import org.mtransit.parser.Constants;

// https://developers.google.com/transit/gtfs/reference#calendar_dates_service_id_field
public class GService {

	public static final String SERVICE_ID = "service_id";
	private String service_id;

	public GService(String service_id) {
		this.service_id = service_id;
	}

	@Override
	public String toString() {
		return new StringBuilder() //
				.append(Constants.STRING_DELIMITER).append(service_id).append(Constants.STRING_DELIMITER) //
				.toString();
	}

}
