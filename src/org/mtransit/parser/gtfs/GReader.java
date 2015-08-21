package org.mtransit.parser.gtfs;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;
import org.apache.commons.lang3.StringUtils;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GAgency;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GCalendarDatesExceptionType;
import org.mtransit.parser.gtfs.data.GFrequency;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GStopTime;
import org.mtransit.parser.gtfs.data.GTrip;

public class GReader {

	private static final String SLASH = "/";

	private static final String POINT = ".";

	public static final Charset UTF8 = Charset.forName("UTF-8");

	private interface LineProcessor {
		void processLine(HashMap<String, String> line);
	}

	public static GSpec readGtfsZipFile(String gtfsFile, final GAgencyTools agencyTools, boolean calendarsOnly) {
		System.out.printf("\nReading GTFS file '%s'...", gtfsFile);
		long start = System.currentTimeMillis();
		final GSpec gSpec = new GSpec();
		ZipInputStream zip = null;
		InputStreamReader isr = null;
		BufferedReader reader = null;
		try {
			zip = new ZipInputStream(new FileInputStream(gtfsFile));
			isr = new InputStreamReader(zip, UTF8);
			reader = new BufferedReader(isr);
			for (ZipEntry entry = zip.getNextEntry(); entry != null; entry = zip.getNextEntry()) {
				if (entry.isDirectory()) {
					continue;
				}
				String filename = entry.getName();
				while (filename.contains(SLASH)) { // remove directory from file name
					filename = filename.substring(filename.indexOf(SLASH) + 1);
				}
				if (filename.equals(GAgency.FILENAME)) { // AGENCY
					if (calendarsOnly) {
						continue;
					}
					readCsv(filename, reader, null, null, new LineProcessor() {
						@Override
						public void processLine(HashMap<String, String> line) {
							processAgency(agencyTools, gSpec, line);
						}
					});
				} else if (filename.equals(GCalendarDate.FILENAME)) { // CALENDAR DATES
					readCsv(filename, reader, null, null, new LineProcessor() {
						@Override
						public void processLine(HashMap<String, String> line) {
							processCalendarDate(agencyTools, gSpec, line);
						}
					});
				} else if (filename.equals(GCalendar.FILENAME)) { // CALENDAR
					readCsv(filename, reader, null, null, new LineProcessor() {
						@Override
						public void processLine(HashMap<String, String> line) {
							processCalendar(agencyTools, gSpec, line);
						}
					});
				} else if (filename.equals(GRoute.FILENAME)) { // ROUTE
					if (calendarsOnly) {
						continue;
					}
					readCsv(filename, reader, null, null, new LineProcessor() {
						@Override
						public void processLine(HashMap<String, String> line) {
							processRoute(agencyTools, gSpec, line);
						}
					});
				} else if (filename.equals(GStop.FILENAME)) { // STOP
					if (calendarsOnly) {
						continue;
					}
					readCsv(filename, reader, null, null, new LineProcessor() {
						@Override
						public void processLine(HashMap<String, String> line) {
							processStop(agencyTools, gSpec, line);
						}
					});
				} else if (filename.equals(GTrip.FILENAME)) { // TRIP
					if (calendarsOnly) {
						continue;
					}
					readCsv(filename, reader, null, null, new LineProcessor() {
						@Override
						public void processLine(HashMap<String, String> line) {
							processTrip(agencyTools, gSpec, line);
						}
					});
				} else if (filename.equals(GStopTime.FILENAME)) { // STOP TIME
					if (calendarsOnly) {
						continue;
					}
					readCsv(filename, reader, null, null, new LineProcessor() {
						@Override
						public void processLine(HashMap<String, String> line) {
							processStopTime(agencyTools, gSpec, line);
						}
					});
				} else if (filename.equals(GFrequency.FILENAME)) { // FREQUENCY
					if (calendarsOnly) {
						continue;
					}
					readCsv(filename, reader, null, null, new LineProcessor() {
						@Override
						public void processLine(HashMap<String, String> line) {
							processFrequency(agencyTools, gSpec, line);
						}
					});
				} else {
					System.out.printf("\nFile not used: %s", filename);
				}
			}
		} catch (IOException ioe) {
			System.out.printf("\nI/O Error while reading GTFS file!\n");
			ioe.printStackTrace();
			System.exit(-1);
		} finally {
			if (reader != null) {
				try {
					reader.close();
				} catch (IOException e) {
				}
			}
			if (isr != null) {
				try {
					isr.close();
				} catch (IOException e) {
				}
			}
			if (zip != null) {
				try {
					zip.close();
				} catch (IOException e) {
				}
			}
		}
		System.out.printf("\nReading GTFS file '%1$s'... DONE in %2$s.", gtfsFile, Utils.getPrettyDuration(System.currentTimeMillis() - start));
		gSpec.print(calendarsOnly);
		return gSpec;
	}

	private static final CSVFormat CSV_FORMAT = CSVFormat.RFC4180.withIgnoreSurroundingSpaces();

	private static final CSVFormat CSV_FORMAT_NO_QUOTE = CSV_FORMAT.withQuote(null);

	private static void readCsv(String filename, BufferedReader reader, String filterStartWith, String filterContains, LineProcessor lineProcessor)
			throws IOException {
		System.out.printf("\nReading file '%s'...", filename);
		String line;
		String[] lineColumns;
		line = reader.readLine();
		if (line.charAt(0) == '\uFEFF') { // remove 1st empty char
			System.out.printf("\nRemove 1st empty car");
			line = String.copyValueOf(line.toCharArray(), 1, line.length() - 1);
		}
		CSVRecord recordColumns = CSVParser.parse(line, CSV_FORMAT).getRecords().get(0);
		lineColumns = new String[recordColumns.size()];
		for (int i = 0; i < recordColumns.size(); i++) {
			lineColumns[i] = recordColumns.get(i);
		}
		String[] columnNames = lineColumns;
		if (columnNames == null || columnNames.length == 0) {
			return;
		}
		List<CSVRecord> records;
		HashMap<String, String> map;
		int l = 0; // LOG
		boolean noQuote;
		while ((line = reader.readLine()) != null) {
			try {
				if (filterStartWith != null && !line.startsWith(filterStartWith)) {
					continue;
				}
				if (filterContains != null && !line.contains(filterContains)) {
					continue;
				}
				try {
					records = CSVParser.parse(line, CSV_FORMAT).getRecords();
					noQuote = false;
				} catch (Exception e) {
					records = CSVParser.parse(line, CSV_FORMAT_NO_QUOTE).getRecords();
					noQuote = true;
				}
				if (records == null || records.size() == 0) {
					continue; // empty line
				}
				recordColumns = records.get(0);
				lineColumns = new String[recordColumns.size()];
				for (int i = 0; i < recordColumns.size(); i++) {
					lineColumns[i] = noQuote ? recordColumns.get(i).replaceAll("\"", "") : recordColumns.get(i);
				}
				if (columnNames.length != lineColumns.length && columnNames.length != (lineColumns.length + 1)) {
					System.out.printf("\nFile '%s' line invalid: %s columns instead of %s: %s", filename, lineColumns.length, columnNames.length, line);
					continue;
				}
				map = new HashMap<String, String>();
				for (int ci = 0; ci < lineColumns.length; ++ci) {
					map.put(columnNames[ci], lineColumns[ci]);
				}
				if (lineProcessor != null) {
					lineProcessor.processLine(map);
				}
			} catch (Exception e) {
				System.out.printf("\nError while processing line: %s\n", line);
				e.printStackTrace();
				System.exit(-1);
			}
		}
		System.out.printf("\nFile '%s' read (lines: %s).", filename, l);
	}

	private static void processStopTime(GAgencyTools agencyTools, GSpec gSpec, HashMap<String, String> line) {
		try {
			GStopTime gStopTime = new GStopTime(line.get(GStopTime.TRIP_ID), line.get(GStopTime.DEPARTURE_TIME).trim(), line.get(GStopTime.STOP_ID).trim(),
					Integer.parseInt(line.get(GStopTime.STOP_SEQUENCE).trim()), line.get(GStopTime.STOP_HEADSIGN));
			if (agencyTools.excludeStopTime(gStopTime)) {
				return;
			}
			gSpec.addStopTime(gStopTime);
		} catch (Exception e) {
			System.out.printf("\nError while parsing: '%s'!\n", line);
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private static void processFrequency(GAgencyTools agencyTools, GSpec gSpec, HashMap<String, String> line) {
		try {
			GFrequency gFrequency = new GFrequency(line.get(GFrequency.TRIP_ID), line.get(GFrequency.START_TIME), line.get(GFrequency.END_TIME),
					Integer.parseInt(line.get(GFrequency.HEADWAY_SECS)));
			gSpec.addFrequency(gFrequency);
		} catch (Exception e) {
			System.out.printf("\nError while parsing: '%s'!\n", line);
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private static void processAgency(GAgencyTools agencyTools, GSpec gSpec, HashMap<String, String> line) {
		try {
			gSpec.addAgency(new GAgency(line.get(GAgency.AGENCY_ID), line.get(GAgency.AGENCY_TIMEZONE)));
		} catch (Exception e) {
			System.out.printf("\nError while processing: '%s'!\n", line);
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private static void processCalendarDate(GAgencyTools agencyTools, GSpec gSpec, HashMap<String, String> line) {
		try {
			String serviceId = line.get(GCalendarDate.SERVICE_ID);
			String date = line.get(GCalendarDate.DATE);
			String exceptionDate = line.get(GCalendarDate.EXCEPTION_DATE);
			if (StringUtils.isEmpty(serviceId) && StringUtils.isEmpty(date) && StringUtils.isEmpty(exceptionDate)) {
				System.out.printf("\nEmpty calendar dates ignored (%s).", line);
				return;
			}
			GCalendarDate gCalendarDate = new GCalendarDate(serviceId, Integer.parseInt(date), GCalendarDatesExceptionType.parse(exceptionDate));
			if (agencyTools.excludeCalendarDate(gCalendarDate)) {
				return;
			}
			gSpec.addCalendarDate(gCalendarDate);
		} catch (Exception e) {
			System.out.printf("\nError while processing: '%s'!\n", line);
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private static final String DAY_TRUE = "1";

	private static void processCalendar(GAgencyTools agencyTools, GSpec gSpec, HashMap<String, String> line) {
		try {
			GCalendar gCalendar = new GCalendar(line.get(GCalendar.SERVICE_ID), //
					DAY_TRUE.equals(line.get(GCalendar.MONDAY)), DAY_TRUE.equals(line.get(GCalendar.TUESDAY)), //
					DAY_TRUE.equals(line.get(GCalendar.WEDNESDAY)), DAY_TRUE.equals(line.get(GCalendar.THURSDAY)), //
					DAY_TRUE.equals(line.get(GCalendar.FRIDAY)), DAY_TRUE.equals(line.get(GCalendar.SATURDAY)), //
					DAY_TRUE.equals(line.get(GCalendar.SUNDAY)), //
					Integer.parseInt(line.get(GCalendar.START_DATE)), Integer.parseInt(line.get(GCalendar.END_DATE)));
			if (agencyTools.excludeCalendar(gCalendar)) {
				return;
			}
			gSpec.addCalendar(gCalendar);
		} catch (Exception e) {
			System.out.printf("\nError while processing: %s!\n", line);
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private static void processTrip(GAgencyTools agencyTools, GSpec gSpec, HashMap<String, String> line) {
		try {
			String directionId = line.get(GTrip.DIRECTION_ID);
			GTrip gTrip = new GTrip(line.get(GTrip.ROUTE_ID), line.get(GTrip.SERVICE_ID), line.get(GTrip.TRIP_ID), StringUtils.isEmpty(directionId) ? null
					: Integer.valueOf(directionId), line.get(GTrip.TRIP_HEADSIGN), line.get(GTrip.SHAPE_ID));
			if (agencyTools.excludeTrip(gTrip)) {
				return;
			}
			gSpec.addTrip(gTrip);
		} catch (Exception e) {
			System.out.printf("\nError while processing: %s\n", line);
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private static final String PARENT_STATION_TYPE = "1";
	private static final String ENTRANCE_TYPE = "2";

	private static void processStop(GAgencyTools agencyTools, GSpec gSpec, Map<String, String> line) {
		try {
			if (PARENT_STATION_TYPE.equals(line.get(GStop.LOCATION_TYPE))) {
				return; // skip parent stations
			}
			if (ENTRANCE_TYPE.equals(line.get(GStop.LOCATION_TYPE))) {
				return; // skip entrance stations
			}
			String code = line.get(GStop.STOP_CODE);
			GStop gStop = new GStop(line.get(GStop.STOP_ID), line.get(GStop.STOP_NAME), Double.parseDouble(line.get(GStop.STOP_LAT)), Double.parseDouble(line
					.get(GStop.STOP_LON)), code == null ? null : code.trim());
			if (agencyTools.excludeStop(gStop)) {
				return;
			}
			gSpec.addStop(gStop);
		} catch (Exception e) {
			System.out.printf("\nError while parsing stop line %s!\n", line);
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private static void processRoute(GAgencyTools agencyTools, GSpec gSpec, HashMap<String, String> line) {
		try {
			String routeColor = line.get(GRoute.ROUTE_COLOR);
			GRoute gRoute = new GRoute(line.get(GRoute.AGENCY_ID), line.get(GRoute.ROUTE_ID), line.get(GRoute.ROUTE_SHORT_NAME),
					line.get(GRoute.ROUTE_LONG_NAME), line.get(GRoute.ROUTE_DESC), Integer.parseInt(line.get(GRoute.ROUTE_TYPE)), routeColor == null ? null
							: routeColor.trim());
			if (agencyTools.excludeRoute(gRoute)) {
				return;
			}
			gSpec.addRoute(gRoute);
		} catch (Exception e) {
			System.out.printf("\nError while parsing route line %s!\n", line);
			e.printStackTrace();
			System.exit(-1);
		}
	}

	private GReader() {
	}
}
