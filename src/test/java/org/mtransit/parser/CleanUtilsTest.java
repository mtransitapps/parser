package org.mtransit.parser;

import org.junit.Test;

import java.util.Collections;
import java.util.regex.Pattern;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class CleanUtilsTest {

	@Test(expected = RuntimeException.class)
	public void testCleanWords_Null() {
		// Arrange
		// Act
		CleanUtils.cleanWords((String[]) null);
		// Assert
	}

	@Test(expected = RuntimeException.class)
	public void testCleanWords_Empty() {
		// Arrange
		// Act
		CleanUtils.cleanWords(Collections.<String>emptyList().toArray(new String[0]));
		// Assert
	}

	@Test()
	public void testCleanWords_1() {
		// Arrange
		// Act
		Pattern result = CleanUtils.cleanWords("word");
		// Assert
		assertNotNull(result);
		assertEquals("((^|\\W)(word)(\\W|$))", result.pattern());
	}

	@Test()
	public void testCleanWords_Many() {
		// Arrange
		// Act
		Pattern result = CleanUtils.cleanWords("word1", "word2");
		// Assert
		assertNotNull(result);
		assertEquals("((^|\\W)(word1|word2)(\\W|$))", result.pattern());
	}

	@Test()
	public void testCleanWordsPlural_Many() {
		// Arrange
		// Act
		Pattern result = CleanUtils.cleanWordsPlural("word1", "word2");
		// Assert
		assertNotNull(result);
		assertEquals("((^|\\W)((word1|word2)([s]?))(\\W|$))", result.pattern());
	}

	@Test()
	public void testCleanWordsPlural_cleaning() {
		// Arrange
		String string = "This is multiple word1s and word2s. And also single word1 and word2.";
		// Act
		Pattern pattern = CleanUtils.cleanWordsPlural("word1", "word2");
		String replacement = CleanUtils.cleanWordsReplacementPlural("Wrd");
		String result = pattern.matcher(string).replaceAll(replacement);
		// Assert
		assertEquals("This is multiple Wrds and Wrds. And also single Wrd and Wrd.", result);
	}
}