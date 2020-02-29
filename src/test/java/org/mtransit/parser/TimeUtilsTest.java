package org.mtransit.parser;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class TimeUtilsTest {

	@Test
	public void testConvertTimeToString_0() {
		// Arrange
		// Act
		String result = TimeUtils.convertTimeToString(0);
		// Assert
		assertEquals("000000", result);
	}

	@Test
	public void testConvertTimeToString_12345() {
		// Arrange
		// Act
		String result = TimeUtils.convertTimeToString(12345);
		// Assert
		assertEquals("012345", result);
	}

	@Test
	public void testConvertTimeToString_123456() {
		// Arrange
		// Act
		String result = TimeUtils.convertTimeToString(123456);
		// Assert
		assertEquals("123456", result);
	}
}