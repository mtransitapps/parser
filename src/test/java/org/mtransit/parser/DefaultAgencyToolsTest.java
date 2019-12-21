package org.mtransit.parser;

import org.junit.Test;
import org.mtransit.parser.DefaultAgencyTools.Period;
import org.mtransit.parser.gtfs.data.GCalendar;

import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class DefaultAgencyToolsTest {

	@Test
	public void testConvertTimeToString_0() {
		// Arrange
		// Act
		String result = DefaultAgencyTools.convertTimeToString(0);
		// Assert
		assertEquals("000000", result);
	}

	@Test
	public void testConvertTimeToString_12345() {
		// Arrange
		// Act
		String result = DefaultAgencyTools.convertTimeToString(12345);
		// Assert
		assertEquals("012345", result);
	}

	@Test
	public void testConvertTimeToString_123456() {
		// Arrange
		// Act
		String result = DefaultAgencyTools.convertTimeToString(123456);
		// Assert
		assertEquals("123456", result);
	}

	@Test
	public void testFindDayServiceIdsPeriod_0Day() {
		// Arrange
		List<GCalendar> gCalendars = Arrays.asList(
				new GCalendar("SEPT19-304Shop-Weekday-01", 1, 1, 1, 1, 1, 0, 0, 20191219, 20191219),
				new GCalendar("SEPT19-d1930Hoc-Weekday-05", 1, 1, 1, 1, 1, 0, 0, 20191219, 20191219),
				new GCalendar("SEPT19-s1900SuR-Saturday-01", 0, 0, 0, 0, 0, 1, 0, 20191221, 20191221),
				new GCalendar("SEPT19-sLINE1-Saturday-01", 0, 0, 0, 0, 0, 1, 0, 20191221, 20191221),
				new GCalendar("SEPT19 - WOTRSA - Saturday - 01", 0, 0, 0, 0, 0, 1, 0, 20191221, 20191221),
				new GCalendar("SEPT19 - SEPSA19 - Saturday - 11", 0, 0, 0, 0, 0, 1, 0, 20191221, 20191221),
				new GCalendar("JAN20-uLINE1-Sunday-01", 0, 0, 0, 0, 0, 0, 1, 20191222, 20200412)
		);
		Period p = new Period();
		p.todayStringInt = 2019120;
		// Act
		DefaultAgencyTools.findDayServiceIdsPeriod(gCalendars, p);
		// Assert
		assertNull(p.startDate);
		assertNull(p.endDate);
	}

	@Test
	public void testFindDayServiceIdsPeriod_1Day() {
		// Arrange
		List<GCalendar> gCalendars = Arrays.asList(
				new GCalendar("SEPT19-305Shop-Weekday-01", 1, 1, 1, 1, 1, 0, 0, 20191220, 20191220),
				new GCalendar("SEPT19-s1900SuR-Saturday-01", 0, 0, 0, 0, 0, 1, 0, 20191221, 20191221),
				new GCalendar("SEPT19-sLINE1-Saturday-01", 0, 0, 0, 0, 0, 1, 0, 20191221, 20191221),
				new GCalendar("SEPT19 - WOTRSA - Saturday - 01", 0, 0, 0, 0, 0, 1, 0, 20191221, 20191221),
				new GCalendar("SEPT19 - SEPSA19 - Saturday - 11", 0, 0, 0, 0, 0, 1, 0, 20191221, 20191221),
				new GCalendar("JAN20-uLINE1-Sunday-01", 0, 0, 0, 0, 0, 0, 1, 20191222, 20200412)
		);
		Period p = new Period();
		p.todayStringInt = 20191221;
		// Act
		DefaultAgencyTools.findDayServiceIdsPeriod(gCalendars, p);
		// Assert
		assertNotNull(p.startDate);
		assertEquals(20191221, p.startDate.intValue());
		assertNotNull(p.endDate);
		assertEquals(20191221, p.endDate.intValue());
		assertEquals(1, p.endDate - p.startDate + 1);
	}

	@Test
	public void testFindDayServiceIdsPeriod_ManyDays() {
		// Arrange
		List<GCalendar> gCalendars = Arrays.asList(
				new GCalendar("SEPT19-301Shop-Weekday-02", 1, 1, 1, 1, 1, 0, 0, 20191216, 20191216),
				new GCalendar("SEPT19-dLINE1-Weekday-02", 1, 1, 1, 1, 1, 0, 0, 20191216, 20191220),
				new GCalendar("SEPT19-dLINE1-Weekday-02-0000100", 0, 0, 0, 0, 1, 0, 0, 20191216, 20191220),
				new GCalendar("SEPT19-SEPDA19-Weekday-02", 1, 1, 1, 1, 1, 0, 0, 20191216, 20191220),
				new GCalendar("SEPT19-WOTRDA-Weekday-01", 1, 1, 1, 1, 1, 0, 0, 20191216, 20191220),
				new GCalendar("SEPT19-302Shop-Weekday-01", 1, 1, 1, 1, 1, 0, 0, 20191217, 20191217),
				new GCalendar("SEPT19-303Shop-Weekday-01", 1, 1, 1, 1, 1, 0, 0, 20191218, 20191218),
				new GCalendar("SEPT19-304Shop-Weekday-01", 1, 1, 1, 1, 1, 0, 0, 20191219, 20191219),
				new GCalendar("SEPT19-d1930Hoc-Weekday-05", 1, 1, 1, 1, 1, 0, 0, 20191219, 20191219),
				new GCalendar("SEPT19-305Shop-Weekday-01", 1, 1, 1, 1, 1, 0, 0, 20191220, 20191220)
		);
		Period p = new Period();
		p.todayStringInt = 20191216;
		// Act
		DefaultAgencyTools.findDayServiceIdsPeriod(gCalendars, p);
		// Assert
		assertNotNull(p.startDate);
		assertEquals(20191216, p.startDate.intValue());
		assertNotNull(p.endDate);
		assertEquals(20191220, p.endDate.intValue());
		assertEquals(5, p.endDate - p.startDate + 1);
	}

	@Test
	public void testFindDayServiceIdsPeriod_ManyDays_included() {
		// Arrange
		List<GCalendar> gCalendars = Arrays.asList(
				new GCalendar("SEPT19-301Shop-Weekday-02", 1, 1, 1, 1, 1, 0, 0, 20191216, 20191216),
				new GCalendar("SEPT19-dLINE1-Weekday-02", 1, 1, 1, 1, 1, 0, 0, 20191216, 20191220),
				new GCalendar("SEPT19-dLINE1-Weekday-02-0000100", 0, 0, 0, 0, 1, 0, 0, 20191216, 20191220),
				new GCalendar("SEPT19-SEPDA19-Weekday-02", 1, 1, 1, 1, 1, 0, 0, 20191216, 20191220),
				new GCalendar("SEPT19-WOTRDA-Weekday-01", 1, 1, 1, 1, 1, 0, 0, 20191216, 20191220),
				// No service starting or ending on 20191217 but included in other services
				new GCalendar("SEPT19-303Shop-Weekday-01", 1, 1, 1, 1, 1, 0, 0, 20191218, 20191218),
				new GCalendar("SEPT19-304Shop-Weekday-01", 1, 1, 1, 1, 1, 0, 0, 20191219, 20191219),
				new GCalendar("SEPT19-d1930Hoc-Weekday-05", 1, 1, 1, 1, 1, 0, 0, 20191219, 20191219),
				new GCalendar("SEPT19-305Shop-Weekday-01", 1, 1, 1, 1, 1, 0, 0, 20191220, 20191220)
		);
		Period p = new Period();
		p.todayStringInt = 20191217;
		// Act
		DefaultAgencyTools.findDayServiceIdsPeriod(gCalendars, p);
		// Assert
		assertNotNull(p.startDate);
		assertEquals(20191216, p.startDate.intValue());
		assertNotNull(p.endDate);
		assertEquals(20191220, p.endDate.intValue());
		assertEquals(5, p.endDate - p.startDate + 1);
	}
}