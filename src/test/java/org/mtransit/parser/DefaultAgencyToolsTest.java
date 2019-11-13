package org.mtransit.parser;


import org.junit.Assert;
import org.junit.Test;


public class DefaultAgencyToolsTest {

	@Test
	public void testConvertTimeToString_0() {
		// Arrange
		// Act
		String result = DefaultAgencyTools.convertTimeToString(0);
		// Assert
		Assert.assertEquals("000000", result);
	}

	@Test
	public void testConvertTimeToString_12345() {
		// Arrange
		// Act
		String result = DefaultAgencyTools.convertTimeToString(12345);
		// Assert
		Assert.assertEquals("012345", result);
	}

	@Test
	public void testConvertTimeToString_123456() {
		// Arrange
		// Act
		String result = DefaultAgencyTools.convertTimeToString(123456);
		// Assert
		Assert.assertEquals("123456", result);
	}

}