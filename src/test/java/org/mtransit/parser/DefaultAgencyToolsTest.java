package org.mtransit.parser;

import org.junit.Test;
import org.mtransit.parser.DefaultAgencyTools.Period;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GCalendarDatesExceptionType;

import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@SuppressWarnings("ConstantConditions")
public class DefaultAgencyToolsTest {

	private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyyMMdd", Locale.ENGLISH);
	private static final Calendar c = Calendar.getInstance();

	@Test
	public void testFindDayServiceIdsPeriod_0Day() {
		// Arrange
		List<GCalendar> gCalendars = Arrays.asList(
				new GCalendar("SEPT19-304Shop-Weekday-01", 1, 1, 1, 1, 1, 0, 0, 2019_12_19, 2019_12_19),
				new GCalendar("SEPT19-d1930Hoc-Weekday-05", 1, 1, 1, 1, 1, 0, 0, 2019_12_19, 2019_12_19),
				new GCalendar("SEPT19-s1900SuR-Saturday-01", 0, 0, 0, 0, 0, 1, 0, 2019_12_21, 2019_12_21),
				new GCalendar("SEPT19-sLINE1-Saturday-01", 0, 0, 0, 0, 0, 1, 0, 2019_12_21, 2019_12_21),
				new GCalendar("SEPT19 - WOTRSA - Saturday - 01", 0, 0, 0, 0, 0, 1, 0, 2019_12_21, 2019_12_21),
				new GCalendar("SEPT19 - SEPSA19 - Saturday - 11", 0, 0, 0, 0, 0, 1, 0, 2019_12_21, 2019_12_21),
				new GCalendar("JAN20-uLINE1-Sunday-01", 0, 0, 0, 0, 0, 0, 1, 2019_12_22, 2020_04_12)
		);
		Period p = new Period();
		p.todayStringInt = 2019120;
		// Act
		DefaultAgencyTools.findDayServiceIdsPeriod(gCalendars, null, p);
		// Assert
		assertNull(p.startDate);
		assertNull(p.endDate);
	}

	@Test
	public void testFindDayServiceIdsPeriod_1Day() {
		// Arrange
		List<GCalendar> gCalendars = Arrays.asList(
				new GCalendar("SEPT19-305Shop-Weekday-01", 1, 1, 1, 1, 1, 0, 0, 2019_12_20, 2019_12_20),
				new GCalendar("SEPT19-s1900SuR-Saturday-01", 0, 0, 0, 0, 0, 1, 0, 2019_12_21, 2019_12_21),
				new GCalendar("SEPT19-sLINE1-Saturday-01", 0, 0, 0, 0, 0, 1, 0, 2019_12_21, 2019_12_21),
				new GCalendar("SEPT19 - WOTRSA - Saturday - 01", 0, 0, 0, 0, 0, 1, 0, 2019_12_21, 2019_12_21),
				new GCalendar("SEPT19 - SEPSA19 - Saturday - 11", 0, 0, 0, 0, 0, 1, 0, 2019_12_21, 2019_12_21),
				new GCalendar("JAN20-uLINE1-Sunday-01", 0, 0, 0, 0, 0, 0, 1, 2019_12_22, 2020_04_12)
		);
		Period p = new Period();
		p.todayStringInt = 2019_12_21;
		// Act
		DefaultAgencyTools.findDayServiceIdsPeriod(gCalendars, null, p);
		// Assert
		assertNotNull(p.startDate);
		assertEquals(2019_12_21, p.startDate.intValue());
		assertNotNull(p.endDate);
		assertEquals(2019_12_21, p.endDate.intValue());
		assertEquals(1, p.endDate - p.startDate + 1);
	}

	@Test
	public void testFindDayServiceIdsPeriod_ManyDays() {
		// Arrange
		List<GCalendar> gCalendars = Arrays.asList(
				new GCalendar("SEPT19-301Shop-Weekday-02", 1, 1, 1, 1, 1, 0, 0, 2019_12_16, 2019_12_16),
				new GCalendar("SEPT19-dLINE1-Weekday-02", 1, 1, 1, 1, 1, 0, 0, 2019_12_16, 2019_12_20),
				new GCalendar("SEPT19-dLINE1-Weekday-02-0000100", 0, 0, 0, 0, 1, 0, 0, 2019_12_16, 2019_12_20),
				new GCalendar("SEPT19-SEPDA19-Weekday-02", 1, 1, 1, 1, 1, 0, 0, 2019_12_16, 2019_12_20),
				new GCalendar("SEPT19-WOTRDA-Weekday-01", 1, 1, 1, 1, 1, 0, 0, 2019_12_16, 2019_12_20),
				new GCalendar("SEPT19-302Shop-Weekday-01", 1, 1, 1, 1, 1, 0, 0, 2019_12_17, 2019_12_17),
				new GCalendar("SEPT19-303Shop-Weekday-01", 1, 1, 1, 1, 1, 0, 0, 2019_12_18, 2019_12_18),
				new GCalendar("SEPT19-304Shop-Weekday-01", 1, 1, 1, 1, 1, 0, 0, 2019_12_19, 2019_12_19),
				new GCalendar("SEPT19-d1930Hoc-Weekday-05", 1, 1, 1, 1, 1, 0, 0, 2019_12_19, 2019_12_19),
				new GCalendar("SEPT19-305Shop-Weekday-01", 1, 1, 1, 1, 1, 0, 0, 2019_12_20, 2019_12_20)
		);
		Period p = new Period();
		p.todayStringInt = 2019_12_16;
		// Act
		DefaultAgencyTools.findDayServiceIdsPeriod(gCalendars, null, p);
		// Assert
		assertNotNull(p.startDate);
		assertEquals(2019_12_16, p.startDate.intValue());
		assertNotNull(p.endDate);
		assertEquals(2019_12_20, p.endDate.intValue());
		assertEquals(5, p.endDate - p.startDate + 1);
	}

	@Test
	public void testFindDayServiceIdsPeriod_ManyDays_included() {
		// Arrange
		List<GCalendar> gCalendars = Arrays.asList(
				new GCalendar("SEPT19-301Shop-Weekday-02", 1, 1, 1, 1, 1, 0, 0, 2019_12_16, 2019_12_16),
				new GCalendar("SEPT19-dLINE1-Weekday-02", 1, 1, 1, 1, 1, 0, 0, 2019_12_16, 2019_12_20),
				new GCalendar("SEPT19-dLINE1-Weekday-02-0000100", 0, 0, 0, 0, 1, 0, 0, 2019_12_16, 2019_12_20),
				new GCalendar("SEPT19-SEPDA19-Weekday-02", 1, 1, 1, 1, 1, 0, 0, 2019_12_16, 2019_12_20),
				new GCalendar("SEPT19-WOTRDA-Weekday-01", 1, 1, 1, 1, 1, 0, 0, 2019_12_16, 2019_12_20),
				// No service starting or ending on 2019_12_17 but included in other services
				new GCalendar("SEPT19-303Shop-Weekday-01", 1, 1, 1, 1, 1, 0, 0, 2019_12_18, 2019_12_18),
				new GCalendar("SEPT19-304Shop-Weekday-01", 1, 1, 1, 1, 1, 0, 0, 2019_12_19, 2019_12_19),
				new GCalendar("SEPT19-d1930Hoc-Weekday-05", 1, 1, 1, 1, 1, 0, 0, 2019_12_19, 2019_12_19),
				new GCalendar("SEPT19-305Shop-Weekday-01", 1, 1, 1, 1, 1, 0, 0, 2019_12_20, 2019_12_20)
		);
		Period p = new Period();
		p.todayStringInt = 2019_12_17;
		// Act
		DefaultAgencyTools.findDayServiceIdsPeriod(gCalendars, null, p);
		// Assert
		assertNotNull(p.startDate);
		assertEquals(2019_12_16, p.startDate.intValue());
		assertNotNull(p.endDate);
		assertEquals(2019_12_20, p.endDate.intValue());
		assertEquals(5, p.endDate - p.startDate + 1);
	}

	@Test
	public void testParseCalendars_ManyDays_included() {
		// Arrange
		List<GCalendar> gCalendars = Arrays.asList(
				new GCalendar("SEPT19-301Shop-Weekday-02", 1, 1, 1, 1, 1, 0, 0, 2019_12_16, 2019_12_16),
				new GCalendar("SEPT19-dLINE1-Weekday-02", 1, 1, 1, 1, 1, 0, 0, 2019_12_16, 2019_12_20),
				new GCalendar("SEPT19-dLINE1-Weekday-02-0000100", 0, 0, 0, 0, 1, 0, 0, 2019_12_16, 2019_12_20),
				new GCalendar("SEPT19-SEPDA19-Weekday-02", 1, 1, 1, 1, 1, 0, 0, 2019_12_16, 2019_12_20),
				new GCalendar("SEPT19-WOTRDA-Weekday-01", 1, 1, 1, 1, 1, 0, 0, 2019_12_16, 2019_12_20),
				// No service starting or ending on 2019_12_17 but included in other services
				new GCalendar("SEPT19-303Shop-Weekday-01", 1, 1, 1, 1, 1, 0, 0, 2019_12_18, 2019_12_18),
				new GCalendar("SEPT19-304Shop-Weekday-01", 1, 1, 1, 1, 1, 0, 0, 2019_12_19, 2019_12_19),
				new GCalendar("SEPT19-d1930Hoc-Weekday-05", 1, 1, 1, 1, 1, 0, 0, 2019_12_19, 2019_12_19),
				new GCalendar("SEPT19-305Shop-Weekday-01", 1, 1, 1, 1, 1, 0, 0, 2019_12_20, 2019_12_20),
				new GCalendar("SEPT19-s1900SuR-Saturday-01", 0, 0, 0, 0, 0, 1, 0, 2019_12_21, 2019_12_21),
				new GCalendar("SEPT19-sLINE1-Saturday-01", 0, 0, 0, 0, 0, 1, 0, 2019_12_21, 2019_12_21),
				new GCalendar("SEPT19 - WOTRSA - Saturday - 01", 0, 0, 0, 0, 0, 1, 0, 2019_12_21, 2019_12_21),
				new GCalendar("SEPT19 - SEPSA19 - Saturday - 11", 0, 0, 0, 0, 0, 1, 0, 2019_12_21, 2019_12_21),
				new GCalendar("JAN20-uLINE1-Sunday-01", 0, 0, 0, 0, 0, 0, 1, 2019_12_22, 2020_04_12)
		);
		Period p = new Period();
		p.todayStringInt = 2019_12_17;
		boolean lookBackward = true;
		// Act
		DefaultAgencyTools.parseCalendars(gCalendars, null, DATE_FORMAT, c, p, lookBackward);
		// Assert
		assertNotNull(p.startDate);
		assertEquals(2019_12_16, p.startDate.intValue());
		assertNotNull(p.endDate);
		assertEquals(2019_12_21, p.endDate.intValue());
	}

	@Test
	public void testFindDayServiceIdsPeriod_PreviousDayHowService() {
		// Arrange
		List<GCalendar> gCalendars = Arrays.asList(
				new GCalendar("SEPT19-304Shop-Weekday-01", 1, 1, 1, 1, 1, 0, 0, 2019_12_19, 2019_12_19),
				new GCalendar("SEPT19-d1930Hoc-Weekday-05", 1, 1, 1, 1, 1, 0, 0, 2019_12_19, 2019_12_19),
				new GCalendar("SEPT19-s1900SuR-Saturday-01", 0, 0, 0, 0, 0, 1, 0, 2019_12_21, 2019_12_21),
				new GCalendar("SEPT19-sLINE1-Saturday-01", 0, 0, 0, 0, 0, 1, 0, 2019_12_21, 2019_12_21),
				new GCalendar("SEPT19 - WOTRSA - Saturday - 01", 0, 0, 0, 0, 0, 1, 0, 2019_12_21, 2019_12_21),
				new GCalendar("SEPT19 - SEPSA19 - Saturday - 11", 0, 0, 0, 0, 0, 1, 0, 2019_12_21, 2019_12_21),
				new GCalendar("JAN20-uLINE1-Sunday-01", 0, 0, 0, 0, 0, 0, 1, 2019_12_22, 2020_04_12)
		);
		Period p = new Period();
		p.todayStringInt = 2019_12_22;
		// Act
		DefaultAgencyTools.findDayServiceIdsPeriod(gCalendars, null, p);
		// Assert
		assertNotNull(p.startDate);
		assertEquals(2019_12_22, p.startDate.intValue());
		assertNotNull(p.endDate);
		assertEquals(2020_04_12, p.endDate.intValue());
	}

	@Test
	public void testParseCalendars_PreviousDayHowService() {
		// Arrange
		List<GCalendar> gCalendars = Arrays.asList(
				new GCalendar("SEPT19 - SEPSA19 - Saturday - 11", 0, 0, 0, 0, 0, 1, 0, 2019_12_21, 2019_12_21),
				new GCalendar("JAN20-uLINE1-Sunday-01", 0, 0, 0, 0, 0, 0, 1, 2019_12_22, 2020_04_12)
		);

		Period p = new Period();
		p.todayStringInt = 2019_12_22;
		boolean lookBackward = true;
		// Act
		DefaultAgencyTools.parseCalendars(gCalendars, null, DATE_FORMAT, c, p, lookBackward);
		// Assert
		assertNotNull(p.startDate);
		assertEquals(2019_12_22, p.startDate.intValue());
		assertNotNull(p.endDate);
		assertEquals(2020_04_12, p.endDate.intValue());
	}

	@Test
	public void test_parseCalendarDates_Split() { // #KingstonTransit #Should be 2 group of calendars
		// Arrange
		List<GCalendarDate> gCalendarDates = Arrays.asList(
				// current
				new GCalendarDate("1203", 2024_04_06, 1), // SATURDAY
				new GCalendarDate("1205", 2024_04_07, 1), // SUNDAY
				new GCalendarDate("1216", 2024_04_08, 1), // WEEKDAY
				new GCalendarDate("1214", 2024_04_09, 1), // WEEKDAY
				new GCalendarDate("1215", 2024_04_09, 1), // WEEKDAY
				new GCalendarDate("1214", 2024_04_10, 1), // WEEKDAY
				new GCalendarDate("1215", 2024_04_10, 1), // WEEKDAY
				new GCalendarDate("1214", 2024_04_11, 1), // WEEKDAY
				new GCalendarDate("1215", 2024_04_11, 1), // WEEKDAY
				new GCalendarDate("1214", 2024_04_12, 1), // WEEKDAY
				new GCalendarDate("1215", 2024_04_12, 1), // WEEKDAY
				new GCalendarDate("1203", 2024_04_13, 1), // SATURDAY
				new GCalendarDate("1205", 2024_04_14, 1), // SUNDAY
				new GCalendarDate("1214", 2024_04_15, 1), // WEEKDAY
				new GCalendarDate("1215", 2024_04_15, 1), // WEEKDAY
				new GCalendarDate("1214", 2024_04_16, 1), // WEEKDAY
				new GCalendarDate("1215", 2024_04_16, 1), // WEEKDAY
				new GCalendarDate("1214", 2024_04_17, 1), // WEEKDAY
				new GCalendarDate("1215", 2024_04_17, 1), // WEEKDAY
				new GCalendarDate("1214", 2024_04_18, 1), // WEEKDAY
				new GCalendarDate("1215", 2024_04_18, 1), // WEEKDAY
				new GCalendarDate("1214", 2024_04_19, 1), // WEEKDAY
				new GCalendarDate("1215", 2024_04_19, 1), // WEEKDAY
				new GCalendarDate("1203", 2024_04_20, 1), // SATURDAY
				new GCalendarDate("1205", 2024_04_21, 1), // SUNDAY
				new GCalendarDate("1214", 2024_04_22, 1), // WEEKDAY
				new GCalendarDate("1215", 2024_04_22, 1), // WEEKDAY
				new GCalendarDate("1214", 2024_04_23, 1), // WEEKDAY
				new GCalendarDate("1215", 2024_04_23, 1), // WEEKDAY
				new GCalendarDate("1214", 2024_04_24, 1), // WEEKDAY
				new GCalendarDate("1215", 2024_04_24, 1), // WEEKDAY
				new GCalendarDate("1169", 2024_04_25, 1), // WEEKDAY
				// next
				new GCalendarDate("1209", 2024_04_26, 1), // WEEKDAY
				new GCalendarDate("1210", 2024_04_27, 1), // SATURDAY
				new GCalendarDate("1211", 2024_04_28, 1), // SUNDAY
				new GCalendarDate("1209", 2024_04_29, 1), // WEEKDAY
				new GCalendarDate("1209", 2024_04_30, 1), // WEEKDAY
				new GCalendarDate("1209", 2024_05_01, 1), // WEEKDAY
				new GCalendarDate("1209", 2024_05_02, 1), // WEEKDAY
				new GCalendarDate("1209", 2024_05_03, 1), // WEEKDAY
				new GCalendarDate("1210", 2024_05_04, 1), // SATURDAY
				new GCalendarDate("1211", 2024_05_05, 1) // SUNDAY
		);
		// CURRENT
		Period p = new Period();
		p.todayStringInt = 2024_04_09;
		boolean lookBackward = true;
		// Act
		DefaultAgencyTools.parseCalendarDates(gCalendarDates, c, p, lookBackward);
		System.out.println("CURRENT: " + p);
		// Assert
		assertNotNull(p.startDate);
		assertEquals(2024_04_06, p.startDate.intValue());
		assertNotNull(p.endDate);
		assertEquals(2024_04_25, p.endDate.intValue());
		// NEXT
		p.todayStringInt = 2024_04_26; // p.endDate + 1;
		p.startDate = null; // reset
		p.endDate = null; // reset
		lookBackward = false;
		// Act
		DefaultAgencyTools.parseCalendarDates(gCalendarDates, c, p, lookBackward);
		System.out.println("NEXT: " + p);
		// Assert
		assertNotNull(p.startDate);
		assertEquals(2024_04_26, p.startDate.intValue());
		assertNotNull(p.endDate);
		assertEquals(2024_05_10, p.endDate.intValue());
	}

	@Test
	public void testDirectionHeadSignsDescriptive_None() {
		// Arrange
		Map<Integer, String> directionHeadSigns = new HashMap<>();
		// Act
		boolean result = DefaultAgencyTools.directionHeadSignsDescriptiveS(directionHeadSigns);
		// Assert
		assertTrue(result);
	}

	@Test
	public void testDirectionHeadSignsDescriptive_OneBlank() {
		// Arrange
		Map<Integer, String> directionHeadSigns = new HashMap<>();
		directionHeadSigns.put(0, " ");
		// Act
		boolean result = DefaultAgencyTools.directionHeadSignsDescriptiveS(directionHeadSigns);
		// Assert
		assertFalse(result);
	}

	@Test
	public void testDirectionHeadSignsDescriptive_Distinct() {
		// Arrange
		Map<Integer, String> directionHeadSigns = new HashMap<>();
		directionHeadSigns.put(0, "head-sign 0");
		directionHeadSigns.put(1, "head-sign 1");
		// Act
		boolean result = DefaultAgencyTools.directionHeadSignsDescriptiveS(directionHeadSigns);
		// Assert
		assertTrue(result);
	}

	@Test
	public void testDirectionHeadSignsDescriptive_HasBlank() {
		// Arrange
		Map<Integer, String> directionHeadSigns = new HashMap<>();
		directionHeadSigns.put(0, " ");
		directionHeadSigns.put(1, "head-sign");
		// Act
		boolean result = DefaultAgencyTools.directionHeadSignsDescriptiveS(directionHeadSigns);
		// Assert
		assertFalse(result);
	}

	@Test
	public void testDirectionHeadSignsDescriptive_Same() {
		// Arrange
		Map<Integer, String> directionHeadSigns = new HashMap<>();
		directionHeadSigns.put(0, "head-sign");
		directionHeadSigns.put(1, "head-sign");
		// Act
		boolean result = DefaultAgencyTools.directionHeadSignsDescriptiveS(directionHeadSigns);
		// Assert
		assertFalse(result);
	}

	@Test
	public void test_findCalendarsTodayPeriod_Current_NoCurrentScheduleNextOnly() {
		List<GCalendar> gCalendars = Arrays.asList(
				new GCalendar("id0", 1, 1, 1, 1, 1, 1, 0, 2024_01_01, 2024_12_31),
				new GCalendar("id1", 0, 0, 0, 0, 0, 1, 0, 2024_01_01, 2024_12_31),
				new GCalendar("id2", 0, 0, 0, 0, 0, 0, 1, 2024_01_01, 2024_12_31)
		);
		Period p = new Period();
		p.todayStringInt = 2023_11_25;
		boolean lookBackward = true;
		List<GCalendarDate> gCalendarDates = Collections.emptyList();

		DefaultAgencyTools.findCalendarsTodayPeriod(gCalendars, gCalendarDates, DATE_FORMAT, c, p, lookBackward);

		assertNull(p.startDate);
		assertNull(p.endDate);
	}

	@Test
	public void test_findCalendarsTodayPeriod_Current_InProgress() {
		List<GCalendar> gCalendars = Arrays.asList(
				new GCalendar("id0", 1, 1, 1, 1, 1, 1, 0, 2023_01_01, 2023_12_31),
				new GCalendar("id1", 0, 0, 0, 0, 0, 1, 0, 2023_01_01, 2023_12_31),
				new GCalendar("id2", 0, 0, 0, 0, 0, 0, 1, 2023_01_01, 2023_12_31)
		);
		Period p = new Period();
		p.todayStringInt = 2023_11_25;
		boolean lookBackward = true;
		List<GCalendarDate> gCalendarDates = Collections.emptyList();

		DefaultAgencyTools.findCalendarsTodayPeriod(gCalendars, gCalendarDates, DATE_FORMAT, c, p, lookBackward);

		assertEquals(2023_01_01, p.startDate.intValue());
		assertEquals(2023_12_31, p.endDate.intValue());
	}

	@Test
	public void test_findCalendarsTodayPeriod_Current_InTheDistantPast() {
		List<GCalendar> gCalendars = Arrays.asList(
				new GCalendar("id0", 1, 1, 1, 1, 1, 1, 0, 2010_01_01, 2010_12_31),
				new GCalendar("id1", 0, 0, 0, 0, 0, 1, 0, 2010_01_01, 2010_12_31),
				new GCalendar("id2", 0, 0, 0, 0, 0, 0, 1, 2010_01_01, 2010_12_31)
		);
		Period p = new Period();
		p.todayStringInt = 2023_11_25;
		boolean lookBackward = true;
		List<GCalendarDate> gCalendarDates = Collections.emptyList();

		DefaultAgencyTools.findCalendarsTodayPeriod(gCalendars, gCalendarDates, DATE_FORMAT, c, p, lookBackward);

		assertEquals(2010_01_01, p.startDate.intValue());
		assertEquals(2010_12_31, p.endDate.intValue());
	}

	@Test
	public void test_findCalendarsTodayPeriod_Next_NoNextScheduleCurrentOnly() {
		List<GCalendar> gCalendars = Arrays.asList(
				new GCalendar("id0", 1, 1, 1, 1, 1, 1, 0, 2023_01_01, 2023_12_31),
				new GCalendar("id1", 0, 0, 0, 0, 0, 1, 0, 2023_01_01, 2023_12_31),
				new GCalendar("id2", 0, 0, 0, 0, 0, 0, 1, 2023_01_01, 2023_12_31)
		);
		Period p = new Period();
		//noinspection UnusedAssignment
		p.todayStringInt = 2023_11_25; // current
		p.todayStringInt = 2024_01_01; // current.endDate + 1 = next.todayStringInt
		boolean lookBackward = false;
		List<GCalendarDate> gCalendarDates = Collections.emptyList();

		DefaultAgencyTools.findCalendarsTodayPeriod(gCalendars, gCalendarDates, DATE_FORMAT, c, p, lookBackward);

		assertNull(p.startDate);
		assertNull(p.endDate);
	}

	@Test
	public void test_findCalendarsTodayPeriod_Next_NextOnly() {
		List<GCalendar> gCalendars = Arrays.asList(
				new GCalendar("id0", 1, 1, 1, 1, 1, 1, 0, 2024_01_01, 2024_12_31),
				new GCalendar("id1", 0, 0, 0, 0, 0, 1, 0, 2024_01_01, 2024_12_31),
				new GCalendar("id2", 0, 0, 0, 0, 0, 0, 1, 2024_01_01, 2024_12_31)
		);
		Period p = new Period();
		p.todayStringInt = 2023_11_25;
		boolean lookBackward = false;
		List<GCalendarDate> gCalendarDates = Collections.emptyList();

		DefaultAgencyTools.findCalendarsTodayPeriod(gCalendars, gCalendarDates, DATE_FORMAT, c, p, lookBackward);

		assertEquals(2024_01_01, p.startDate.intValue());
		assertEquals(2024_12_31, p.endDate.intValue());
	}

	@Test
	public void test_findCalendarsTodayPeriod_Next_NextOnly_StartRemoved() {
		List<GCalendar> gCalendars = Arrays.asList(
				new GCalendar("id0", 1, 1, 1, 1, 1, 1, 0, 2024_01_01, 2024_12_31),
				new GCalendar("id1", 0, 0, 0, 0, 0, 1, 0, 2024_01_01, 2024_12_31),
				new GCalendar("id2", 0, 0, 0, 0, 0, 0, 1, 2024_01_01, 2024_12_31)
		);
		Period p = new Period();
		p.todayStringInt = 2023_11_25;
		boolean lookBackward = false;
		List<GCalendarDate> gCalendarDates = Collections.singletonList(
				new GCalendarDate("id1", 2024_01_01, GCalendarDatesExceptionType.SERVICE_REMOVED)
		);

		DefaultAgencyTools.findCalendarsTodayPeriod(gCalendars, gCalendarDates, DATE_FORMAT, c, p, lookBackward);

		assertEquals(2024_01_01, p.startDate.intValue());
		assertEquals(2024_12_31, p.endDate.intValue());
	}
}