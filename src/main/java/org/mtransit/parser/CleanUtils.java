package org.mtransit.parser;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.parser.db.SQLUtils;

import java.util.Locale;
import java.util.regex.Pattern;

@Deprecated
public final class CleanUtils {

	private CleanUtils() {
	}

	public static final Pattern CLEAN_EN_DASHES = org.mtransit.commons.CleanUtils.CLEAN_EN_DASHES;
	public static final String CLEAN_EN_DASHES_REPLACEMENT = org.mtransit.commons.CleanUtils.CLEAN_EN_DASHES_REPLACEMENT;
	public static final Pattern CLEAN_DASHES = org.mtransit.commons.CleanUtils.CLEAN_DASHES;
	public static final String CLEAN_DASHES_REPLACEMENT = org.mtransit.commons.CleanUtils.CLEAN_EN_DASHES_REPLACEMENT;

	public static final Pattern CLEAN_PARENTHESIS1 = org.mtransit.commons.CleanUtils.CLEAN_PARENTHESIS1;
	public static final String CLEAN_PARENTHESIS1_REPLACEMENT = org.mtransit.commons.CleanUtils.CLEAN_PARENTHESIS1_REPLACEMENT;
	@Deprecated
	public static final Pattern CLEAN_PARENTHESE1 = org.mtransit.commons.CleanUtils.CLEAN_PARENTHESE1;
	@Deprecated
	public static final String CLEAN_PARENTHESE1_REPLACEMENT = org.mtransit.commons.CleanUtils.CLEAN_PARENTHESE1_REPLACEMENT;

	public static final Pattern CLEAN_PARENTHESIS2 = org.mtransit.commons.CleanUtils.CLEAN_PARENTHESIS2;
	public static final String CLEAN_PARENTHESIS2_REPLACEMENT = org.mtransit.commons.CleanUtils.CLEAN_PARENTHESIS2_REPLACEMENT;
	@Deprecated
	public static final Pattern CLEAN_PARENTHESE2 = org.mtransit.commons.CleanUtils.CLEAN_PARENTHESE2;
	@Deprecated
	public static final String CLEAN_PARENTHESE2_REPLACEMENT = org.mtransit.commons.CleanUtils.CLEAN_PARENTHESE2_REPLACEMENT;

	@NotNull
	public static String escape(@NotNull String string) {
		return SQLUtils.escape(string);
	}

	@NotNull
	public static String quotes(@NotNull String string) {
		return SQLUtils.quotes(string);
	}

	@Deprecated
	@NotNull
	public static String cleanLabel(@NotNull String label) {
		return org.mtransit.commons.CleanUtils.cleanLabel(label);
	}

	public static final Pattern[] SPACE_CHARS = org.mtransit.commons.CleanUtils.SPACE_CHARS;

	public static final Pattern[] SPACE_ST = org.mtransit.commons.CleanUtils.SPACE_ST;

	@NotNull
	public static Pattern cleanWords(@Nullable String... words) {
		return org.mtransit.commons.CleanUtils.cleanWords(words);
	}

	@NotNull
	public static Pattern cleanWordsFR(@Nullable String... words) {
		return org.mtransit.commons.CleanUtils.cleanWordsFR(words);
	}

	@NotNull
	public static Pattern cleanWords(int flags, @Nullable String... words) {
		return org.mtransit.commons.CleanUtils.cleanWords(flags, words);
	}

	@NotNull
	public static String cleanWordsReplacement(@Nullable String replacement) {
		return org.mtransit.commons.CleanUtils.cleanWordsReplacement(replacement);
	}

	@NotNull
	public static Pattern cleanWordsPlural(@Nullable String... words) {
		return org.mtransit.commons.CleanUtils.cleanWordsPlural(words);
	}

	@NotNull
	public static Pattern cleanWordsPluralFR(@Nullable String... words) {
		return org.mtransit.commons.CleanUtils.cleanWordsPluralFR(words);
	}

	@NotNull
	public static Pattern cleanWordsPlural(int flags, @Nullable String... words) {
		return org.mtransit.commons.CleanUtils.cleanWordsPlural(flags, words);
	}

	@NotNull
	public static String cleanWordsReplacementPlural(@Nullable String replacement) {
		return org.mtransit.commons.CleanUtils.cleanWordsReplacementPlural(replacement);
	}

	public static final Pattern SAINT = org.mtransit.commons.CleanUtils.SAINT;
	public static final String SAINT_REPLACEMENT = org.mtransit.commons.CleanUtils.SAINT_REPLACEMENT;

	public static final Pattern CLEAN_AT = org.mtransit.commons.CleanUtils.CLEAN_AT;
	public static final String CLEAN_AT_REPLACEMENT = org.mtransit.commons.CleanUtils.CLEAN_AT_REPLACEMENT;

	public static final Pattern CLEAN_AND = org.mtransit.commons.CleanUtils.CLEAN_AND;
	public static final String CLEAN_AND_REPLACEMENT = org.mtransit.commons.CleanUtils.CLEAN_AND_REPLACEMENT;

	public static final Pattern CLEAN_ET = org.mtransit.commons.CleanUtils.CLEAN_ET;
	public static final String CLEAN_ET_REPLACEMENT = org.mtransit.commons.CleanUtils.CLEAN_ET_REPLACEMENT;

	public static final String SPACE = org.mtransit.commons.CleanUtils.SPACE;
	public static final char SPACE_CHAR = org.mtransit.commons.CleanUtils.SPACE_CHAR;

	public static final String SLASH_SPACE = org.mtransit.commons.CleanUtils.SLASH_SPACE;

	@NotNull
	public static String cleanLabelFR(@NotNull String label) {
		return org.mtransit.commons.CleanUtils.cleanLabelFR(label);
	}

	@NotNull
	public static String cleanSlashes(@NotNull String string) {
		return org.mtransit.commons.CleanUtils.cleanSlashes(string);
	}

	@Deprecated
	@NotNull
	public static String removePoints(@NotNull String capitalizedString) {
		return org.mtransit.commons.CleanUtils.removePoints(capitalizedString);
	}

	@NotNull
	public static String keepToFR(@NotNull String string) {
		return org.mtransit.commons.CleanUtils.keepToFR(string);
	}

	@NotNull
	public static String keepTo(@NotNull String string) {
		return org.mtransit.commons.CleanUtils.keepTo(string);
	}

	@NotNull
	public static String keepVia(@NotNull String string) {
		return org.mtransit.commons.CleanUtils.keepVia(string);
	}

	@NotNull
	public static String removeVia(@NotNull String string) {
		return org.mtransit.commons.CleanUtils.removeVia(string);
	}

	@Deprecated
	@NotNull
	public static String keepToAndRevoveVia(@NotNull String string) {
		return org.mtransit.commons.CleanUtils.keepToAndRevoveVia(string);
	}

	@NotNull
	public static String keepToAndRemoveVia(@NotNull String string) {
		return org.mtransit.commons.CleanUtils.keepToAndRemoveVia(string);
	}

	@NotNull
	public static String fixMcXCase(@NotNull String string) { // Mccowan -> McCowan
		return org.mtransit.commons.CleanUtils.fixMcXCase(string);
	}

	@NotNull
	public static String toLowerCaseUpperCaseWords(@NotNull Locale locale, @NotNull String string, @NotNull String... ignoreWords) {
		return org.mtransit.commons.CleanUtils.toLowerCaseUpperCaseWords(locale, string, ignoreWords);
	}

	@NotNull
	public static String cleanNumbers(@NotNull String string) {
		return org.mtransit.commons.CleanUtils.cleanNumbers(string);
	}

	@NotNull
	public static String cleanBounds(@NotNull String string) {
		return org.mtransit.commons.CleanUtils.cleanBounds(string);
	}

	@NotNull
	public static String cleanBounds(@NotNull Locale locale, @NotNull String string) {
		return org.mtransit.commons.CleanUtils.cleanBounds(locale, string);
	}

	@NotNull
	public static String cleanMergedID(@NotNull String mergedId) {
		return org.mtransit.commons.CleanUtils.cleanMergedID(mergedId);
	}

	public static final Pattern POINT = org.mtransit.commons.CleanUtils.POINT;
	public static final String POINT_REPLACEMENT = org.mtransit.commons.CleanUtils.POINT_REPLACEMENT;

	@NotNull
	public static String cleanStreetTypes(@NotNull String string) {
		return org.mtransit.commons.CleanUtils.cleanStreetTypes(string);
	}

	@NotNull
	public static String cleanStreetTypesFRCA(@NotNull String string) {
		return org.mtransit.commons.CleanUtils.cleanStreetTypesFRCA(string);
	}
}
