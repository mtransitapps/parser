package org.mtransit.parser;

import org.junit.Test;

import java.util.Collections;
import java.util.Locale;
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

	@Test
	public void testCleanWords_1() {
		// Arrange
		// Act
		Pattern result = CleanUtils.cleanWords("word");
		// Assert
		assertNotNull(result);
		assertEquals("((^|\\W)(word)(\\W|$))", result.pattern());
	}

	@Test
	public void testCleanWords_Many() {
		// Arrange
		// Act
		Pattern result = CleanUtils.cleanWords("word1", "word2");
		// Assert
		assertNotNull(result);
		assertEquals("((^|\\W)(word1|word2)(\\W|$))", result.pattern());
	}

	@Test
	public void testCleanWordsPlural_Many() {
		// Arrange
		// Act
		Pattern result = CleanUtils.cleanWordsPlural("word1", "word2");
		// Assert
		assertNotNull(result);
		assertEquals("((^|\\W)((word1|word2)([s]?))(\\W|$))", result.pattern());
	}

	@Test
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

	@Test
	public void testToLowerCaseUpperCaseWordsAccent() {
		// Arrange
		String string = "STATION HONORÉ-BEAUGRAND";
		Locale locale = Locale.FRENCH;
		// Act
		String result = CleanUtils.toLowerCaseUpperCaseWords(locale, string);
		// Assert
		assertEquals("station honoré-beaugrand", result);
	}

	@Test
	public void testToLowerCaseUpperCaseWordsKept() {
		// Arrange
		String string = "STATION BERRY-UQAM";
		Locale locale = Locale.FRENCH;
		// Act
		String result = CleanUtils.toLowerCaseUpperCaseWords(locale, string, "UQAM");
		// Assert
		assertEquals("station berry-UQAM", result);
	}

	@Test
	public void testToLowerCaseUpperCaseWordsNotUpperCase() {
		// Arrange
		String string = "UdeS";
		Locale locale = Locale.FRENCH;
		// Act
		String result = CleanUtils.toLowerCaseUpperCaseWords(locale, string);
		// Assert
		assertEquals("UdeS", result);
	}

	@Test
	public void testRemovePointsSimple() {
		// Arrange
		String string = "Mt. Paul";
		// Act
		String result = CleanUtils.removePointsI(string);
		// Assert
		assertEquals("Mt Paul", result);
	}

	@Test
	public void testRemovePointsEllipsis() {
		// Arrange
		String string = "Mt... Paul...";
		// Act
		String result = CleanUtils.removePointsI(string);
		// Assert
		assertEquals("Mt… Paul…", result);
	}

	@Test
	public void testRemovePointsEndsWithPoint() {
		// Arrange
		String string = "Mt Paul (A).";
		// Act
		String result = CleanUtils.removePointsI(string);
		// Assert
		assertEquals("Mt Paul (A)", result);
	}

	@Test
	public void testRemovePointsEndsWithPoints() {
		// Arrange
		String string = "Mt Paul (A).. ";
		// Act
		String result = CleanUtils.removePointsI(string);
		// Assert
		assertEquals("Mt Paul (A)", result);
	}

	@Test
	public void testRemovePointsOverlap() {
		// Arrange
		String string = "Ft. Wm. Rd. & Intercity";
		// Act
		String result = CleanUtils.removePointsI(string);
		// Assert
		assertEquals("Ft Wm Rd & Intercity", result);
	}

	@Test
	public void testRemovePointsBeforeDash() {
		// Arrange
		String string = "T C-Montmorency - Terminus La Cimenterie";
		// Act
		String result = CleanUtils.removePointsI(string);
		// Assert
		assertEquals("T C-Montmorency - Terminus La Cimenterie", result);
	}

	@Test
	public void testRemovePointsSingleLetters() {
		// Arrange
		String string = "Cundles at J.C. Massie Way";
		// Act
		String result = CleanUtils.removePointsI(string);
		// Assert
		assertEquals("Cundles at JC Massie Way", result);
	}

	@Test
	public void testRemovePoints4SingleLettersOnly() {
		// Arrange
		String string = "U.Q.A.M.";
		// Act
		String result = CleanUtils.removePointsI(string);
		// Assert
		assertEquals("UQAM", result);
	}

	@Test
	public void testRemovePoints3SingleLettersOnly() {
		// Arrange
		String string = "R.T.L.";
		// Act
		String result = CleanUtils.removePointsI(string);
		// Assert
		assertEquals("RTL", result);
	}

	@Test
	public void testRemovePointsPoints() {
		// Arrange
		String string = "Prés.-Kennedy / Florent";
		// Act
		String result = CleanUtils.removePointsI(string);
		// Assert
		assertEquals("Prés-Kennedy / Florent", result);
	}

	@Test
	public void testCleanNumbersNo() {
		// Arrange
		String string = "RR 25 & No 5 Side Rd Ind (CW)";
		// Act
		String result = CleanUtils.cleanNumbers(string);
		// Assert
		assertEquals("RR 25 & #5 Side Rd Ind (CW)", result);
	}

	@Test
	public void testCleanNumbersNoPoint() {
		// Arrange
		String string = "RR 25 & No. 5 Side Rd Ind (CW)";
		// Act
		String result = CleanUtils.cleanNumbers(string);
		// Assert
		assertEquals("RR 25 & #5 Side Rd Ind (CW)", result);
	}
}