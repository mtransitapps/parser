package org.mtransit.parser;

import org.apache.commons.text.WordUtils;
import org.apache.commons.text.translate.CharSequenceTranslator;
import org.apache.commons.text.translate.LookupTranslator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.mtransit.parser.Constants.EMPTY;

@SuppressWarnings({"WeakerAccess", "unused", "RedundantSuppression"})
public final class CleanUtils {

	private CleanUtils() {
	}

	public static final Pattern CLEAN_EN_DASHES = Pattern.compile("(\\w)[\\s]*[–][\\s]*(\\w)");
	public static final String CLEAN_EN_DASHES_REPLACEMENT = "$1–$2";
	public static final Pattern CLEAN_DASHES = Pattern.compile("(\\w)[\\s]*[\\-][\\s]*(\\w)");
	public static final String CLEAN_DASHES_REPLACEMENT = "$1-$2";

	private static final String PARENTHESIS1 = "\\(";
	public static final Pattern CLEAN_PARENTHESIS1 = Pattern.compile("[" + PARENTHESIS1 + "][\\s]*(\\w)");
	public static final String CLEAN_PARENTHESIS1_REPLACEMENT = PARENTHESIS1 + "$1";
	@Deprecated
	private static final String PARENTHESE1 = PARENTHESIS1;
	@Deprecated
	public static final Pattern CLEAN_PARENTHESE1 = CLEAN_PARENTHESIS1;
	@Deprecated
	public static final String CLEAN_PARENTHESE1_REPLACEMENT = CLEAN_PARENTHESIS1_REPLACEMENT;

	private static final String PARENTHESIS2 = "\\)";
	public static final Pattern CLEAN_PARENTHESIS2 = Pattern.compile("(\\w)[\\s]*[" + PARENTHESIS2 + "]");
	public static final String CLEAN_PARENTHESIS2_REPLACEMENT = "$1" + PARENTHESIS2;
	@Deprecated
	private static final String PARENTHESE2 = PARENTHESIS2;
	@Deprecated
	public static final Pattern CLEAN_PARENTHESE2 = CLEAN_PARENTHESIS2;
	@Deprecated
	public static final String CLEAN_PARENTHESE2_REPLACEMENT = CLEAN_PARENTHESIS2_REPLACEMENT;

	private static final CharSequenceTranslator ESCAPE;

	static {
		Map<CharSequence, CharSequence> map = new HashMap<>();
		map.put("'", "''");
		map.put("_", EMPTY);
		ESCAPE = new LookupTranslator(map);
	}

	@NotNull
	public static String escape(@NotNull String string) {
		return ESCAPE.translate(string);
	}

	@NotNull
	public static String quotes(@NotNull String string) {
		return "'" + string + "'";
	}

	private static final Pattern CLEAN_SPACES = Pattern.compile("\\s+");
	private static final Pattern CLEAN_P1 = Pattern.compile("[\\s]*\\([\\s]*");
	private static final String CLEAN_P1_REPLACEMENT = " ("; // space: guaranty 1 before & 0 after
	private static final Pattern CLEAN_P2 = Pattern.compile("[\\s]*\\)[\\s]*");
	private static final String CLEAN_P2_REPLACEMENT = ") "; // space: guaranty 0 before & 1 after

	@NotNull
	public static String cleanLabel(@NotNull String label) {
		return cleanLabel(Locale.ENGLISH, label);
	}

	@NotNull
	private static String cleanLabel(@NotNull Locale locale, @NotNull String label) {
		label = CLEAN_SPACES.matcher(label).replaceAll(SPACE);
		label = CLEAN_P1.matcher(label).replaceAll(CLEAN_P1_REPLACEMENT);
		label = CLEAN_P2.matcher(label).replaceAll(CLEAN_P2_REPLACEMENT);
		if (locale == Locale.FRENCH) {
			label = WordUtils.capitalize(label, SPACE_CHAR, '-', '–', '/', '(', '.', '\'', '`');
		} else {
			label = WordUtils.capitalize(label, SPACE_CHAR, '-', '–', '/', '(', '.');
		}
		label = removePointsI(label); // after capitalize
		return label.trim();
	}

	private static final String PLACE_CHAR_DE_L = "de l'";
	private static final String PLACE_CHAR_DE_LA = "de la ";
	private static final String PLACE_CHAR_D = "d'";
	private static final String PLACE_CHAR_DE = "de ";
	private static final String PLACE_CHAR_DES = "des ";
	private static final String PLACE_CHAR_DU = "du ";
	private static final String PLACE_CHAR_LA = "la ";
	private static final String PLACE_CHAR_LE = "le ";
	private static final String PLACE_CHAR_LES = "les ";
	private static final String PLACE_CHAR_L = "l'";

	private static final Pattern[] START_WITH_CHARS = new Pattern[]{ //
			Pattern.compile("^(" + PLACE_CHAR_DE_L + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("^(" + PLACE_CHAR_DE_LA + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("^(" + PLACE_CHAR_D + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("^(" + PLACE_CHAR_DE + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("^(" + PLACE_CHAR_DES + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("^(" + PLACE_CHAR_DU + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("^(" + PLACE_CHAR_LA + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("^(" + PLACE_CHAR_LE + ")", Pattern.CASE_INSENSITIVE),//
			Pattern.compile("^(" + PLACE_CHAR_LES + ")", Pattern.CASE_INSENSITIVE),//
			Pattern.compile("^(" + PLACE_CHAR_L + ")", Pattern.CASE_INSENSITIVE) //
	};

	public static final Pattern[] SPACE_CHARS = new Pattern[]{ //
			Pattern.compile("( " + PLACE_CHAR_DE_L + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("( " + PLACE_CHAR_DE_LA + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("( " + PLACE_CHAR_D + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("( " + PLACE_CHAR_DE + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("( " + PLACE_CHAR_DES + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("( " + PLACE_CHAR_DU + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("( " + PLACE_CHAR_LA + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("( " + PLACE_CHAR_LE + ")", Pattern.CASE_INSENSITIVE),//
			Pattern.compile("( " + PLACE_CHAR_LES + ")", Pattern.CASE_INSENSITIVE),//
			Pattern.compile("( " + PLACE_CHAR_L + ")", Pattern.CASE_INSENSITIVE) //
	};

	private static final Pattern[] SLASH_CHARS = new Pattern[]{//
			Pattern.compile("(/ " + PLACE_CHAR_DE_L + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("(/ " + PLACE_CHAR_DE_LA + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("(/ " + PLACE_CHAR_D + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("(/ " + PLACE_CHAR_DE + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("(/ " + PLACE_CHAR_DES + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("(/ " + PLACE_CHAR_DU + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("(/ " + PLACE_CHAR_LA + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("(/ " + PLACE_CHAR_LE + ")", Pattern.CASE_INSENSITIVE),//
			Pattern.compile("(/ " + PLACE_CHAR_LES + ")", Pattern.CASE_INSENSITIVE),//
			Pattern.compile("(/ " + PLACE_CHAR_L + ")", Pattern.CASE_INSENSITIVE) //
	};

	private static final String PLACE_CHAR_ARRONDISSEMENT = "arrondissement ";
	private static final String PLACE_CHAR_AV = "av ";
	private static final String PLACE_CHAR_AVENUE = "avenue ";
	private static final String PLACE_CHAR_BOUL = "boul ";
	private static final String PLACE_CHAR_BOULEVARD = "boulevard ";
	private static final String PLACE_CHAR_CH = "ch ";
	private static final String PLACE_CHAR_CIVIQUE = "civique ";
	private static final String PLACE_CHAR_CROISS = "croiss ";
	private static final String PLACE_CHAR_QUARTIER = "quartier ";
	private static final String PLACE_CHAR_RTE = "rte ";
	private static final String PLACE_CHAR_RUE = "rue ";
	private static final String PLACE_CHAR_TSSE = "tsse ";

	private static final Pattern[] START_WITH_ST = new Pattern[]{ //
			Pattern.compile("^(" + PLACE_CHAR_ARRONDISSEMENT + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("^(" + PLACE_CHAR_AV + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("^(" + PLACE_CHAR_AVENUE + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("^(" + PLACE_CHAR_BOUL + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("^(" + PLACE_CHAR_BOULEVARD + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("^(" + PLACE_CHAR_CH + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("^(" + PLACE_CHAR_CIVIQUE + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("^(" + PLACE_CHAR_CROISS + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("^(" + PLACE_CHAR_QUARTIER + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("^(" + PLACE_CHAR_RTE + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("^(" + PLACE_CHAR_RUE + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("^(" + PLACE_CHAR_TSSE + ")", Pattern.CASE_INSENSITIVE) //
	};

	public static final Pattern[] SPACE_ST = new Pattern[]{ //
			Pattern.compile("( " + PLACE_CHAR_ARRONDISSEMENT + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("( " + PLACE_CHAR_AV + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("( " + PLACE_CHAR_AVENUE + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("( " + PLACE_CHAR_BOUL + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("( " + PLACE_CHAR_BOULEVARD + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("( " + PLACE_CHAR_CH + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("( " + PLACE_CHAR_CIVIQUE + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("( " + PLACE_CHAR_CROISS + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("( " + PLACE_CHAR_QUARTIER + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("( " + PLACE_CHAR_RTE + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("( " + PLACE_CHAR_RUE + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("( " + PLACE_CHAR_TSSE + ")", Pattern.CASE_INSENSITIVE) //
	};

	private static final Pattern[] SLASH_ST = new Pattern[]{ //
			Pattern.compile("(/ " + PLACE_CHAR_ARRONDISSEMENT + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("(/ " + PLACE_CHAR_AV + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("(/ " + PLACE_CHAR_AVENUE + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("(/ " + PLACE_CHAR_BOUL + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("(/ " + PLACE_CHAR_BOULEVARD + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("(/ " + PLACE_CHAR_CH + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("(/ " + PLACE_CHAR_CIVIQUE + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("(/ " + PLACE_CHAR_CROISS + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("(/ " + PLACE_CHAR_QUARTIER + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("(/ " + PLACE_CHAR_RTE + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("(/ " + PLACE_CHAR_RUE + ")", Pattern.CASE_INSENSITIVE), //
			Pattern.compile("(/ " + PLACE_CHAR_TSSE + ")", Pattern.CASE_INSENSITIVE) //
	};

	@NotNull
	public static Pattern cleanWords(@Nullable String... words) {
		return cleanWords(Pattern.CASE_INSENSITIVE, words);
	}

	@NotNull
	public static Pattern cleanWords(int flags, @Nullable String... words) {
		if (words == null || words.length <= 0) {
			throw new RuntimeException("Cannot clean empty list of words!");
		}
		StringBuilder sb = new StringBuilder("((^|\\W)(");
		boolean firstWorld = true;
		for (String word : words) {
			if (!firstWorld) {
				sb.append('|');
			}
			sb.append(word);
			firstWorld = false;
		}
		sb.append(")(\\W|$))");
		return Pattern.compile(sb.toString(), flags);
	}

	@NotNull
	public static String cleanWordsReplacement(@Nullable String replacement) {
		if (replacement == null || replacement.length() <= 0) {
			return EMPTY;
		}
		return "$2" + replacement + "$4";
	}

	@NotNull
	public static Pattern cleanWordsPlural(@Nullable String... words) {
		if (words == null || words.length <= 0) {
			throw new RuntimeException("Cannot clean empty list of words!");
		}
		StringBuilder sb = new StringBuilder("((^|\\W)((");
		boolean firstWorld = true;
		for (String word : words) {
			if (!firstWorld) {
				sb.append('|');
			}
			sb.append(word);
			firstWorld = false;
		}
		sb.append(")([s]?))(\\W|$))");
		return Pattern.compile(sb.toString(), Pattern.CASE_INSENSITIVE);
	}

	@NotNull
	public static String cleanWordsReplacementPlural(@Nullable String replacement) {
		if (replacement == null || replacement.length() <= 0) {
			return EMPTY;
		}
		return "$2" + replacement + "$5" + "$6";
	}

	public static final Pattern SAINT = Pattern.compile("(saint)", Pattern.CASE_INSENSITIVE);
	public static final String SAINT_REPLACEMENT = "St";

	public static final Pattern CLEAN_AT = cleanWords("at");
	public static final String CLEAN_AT_REPLACEMENT = cleanWordsReplacement("@");

	public static final Pattern CLEAN_AND = cleanWords("and");
	public static final String CLEAN_AND_REPLACEMENT = cleanWordsReplacement("&");

	public static final Pattern CLEAN_ET = cleanWords("et");
	public static final String CLEAN_ET_REPLACEMENT = CLEAN_AND_REPLACEMENT;

	public static final String SPACE = " ";
	public static final char SPACE_CHAR = ' ';

	public static final String SLASH_SPACE = "/ ";

	@NotNull
	public static String cleanLabelFR(@NotNull String label) {
		label = cleanSlashes(label);
		label = CLEAN_PARENTHESIS1.matcher(label).replaceAll(CLEAN_PARENTHESIS1_REPLACEMENT);
		label = CLEAN_PARENTHESIS2.matcher(label).replaceAll(CLEAN_PARENTHESIS2_REPLACEMENT);
		label = SAINT.matcher(label).replaceAll(SAINT_REPLACEMENT);
		label = removePointsI(label); // after capitalize
		label = Utils.replaceAll(label.trim(), START_WITH_ST, SPACE); // Constants.EMPTY); // SPACE);
		label = Utils.replaceAll(label, SLASH_ST, SLASH_SPACE);
		label = Utils.replaceAll(label.trim(), START_WITH_CHARS, SPACE); // , Constants.EMPTY); //
		label = Utils.replaceAll(label, SLASH_CHARS, SLASH_SPACE);
		return cleanLabel(Locale.FRENCH, label);
	}

	private static final Pattern CLEAN_SLASH = Pattern.compile("(\\S)[\\s]*[/][\\s]*(\\S)");
	private static final String CLEAN_SLASH_REPLACEMENT = "$1" + " / " + "$2";

	@NotNull
	public static String cleanSlashes(@NotNull String string) {
		return CLEAN_SLASH.matcher(string).replaceAll(CLEAN_SLASH_REPLACEMENT);
	}

	private static final Pattern _3_POINTS = Pattern.compile("(\\.\\.\\.)");
	private static final String _3_POINTS_REPLACEMENT = "…";

	private static final Pattern POINT1 = Pattern.compile("((?=(^|\\s|[A-Z]+))([\\w])\\.(?=(\\w(\\.|\\s|$)|\\W|$)))");
	private static final String POINT1_REPLACEMENT = "$3";

	private static final Pattern POINTS = Pattern.compile("((?=(^|\\s|[\\w]+))([\\w]+)\\.(?=(\\w{2,}\\.|\\W|$)))");
	private static final String POINTS_REPLACEMENT = "$3";

	private static final Pattern ENDS_WITH_POINTS = Pattern.compile("((\\.+)(\\W*)$)");
	private static final String ENDS_WITH_POINTS_REPLACEMENT = EMPTY;

	@Deprecated
	@NotNull
	public static String removePoints(@NotNull String capitalizedString) {
		return removePointsI(capitalizedString);
	}

	@NotNull
	protected static String removePointsI(@NotNull String capitalizedString) {
		capitalizedString = _3_POINTS.matcher(capitalizedString).replaceAll(_3_POINTS_REPLACEMENT);
		capitalizedString = POINT1.matcher(capitalizedString).replaceAll(POINT1_REPLACEMENT);
		capitalizedString = POINTS.matcher(capitalizedString).replaceAll(POINTS_REPLACEMENT);
		capitalizedString = ENDS_WITH_POINTS.matcher(capitalizedString).replaceAll(ENDS_WITH_POINTS_REPLACEMENT);
		return capitalizedString;
	}

	private static final Pattern STARTS_WITH_VERS = Pattern.compile("((^|^.* )vers )", Pattern.CASE_INSENSITIVE);

	@NotNull
	public static String keepToFR(@NotNull String string) {
		string = STARTS_WITH_VERS.matcher(string).replaceAll(EMPTY);
		return string;
	}

	private static final Pattern STARTS_WITH_TO = Pattern.compile("((^|^.* )to )", Pattern.CASE_INSENSITIVE);

	@NotNull
	public static String keepTo(@NotNull String string) {
		string = STARTS_WITH_TO.matcher(string).replaceAll(EMPTY);
		return string;
	}

	private static final Pattern STARTS_WITH_VIA = Pattern.compile("((^|^.* )via )", Pattern.CASE_INSENSITIVE);

	@NotNull
	public static String keepVia(@NotNull String string) {
		string = STARTS_WITH_VIA.matcher(string).replaceAll(EMPTY);
		return string;
	}

	private static final Pattern ENDS_WITH_VIA = Pattern.compile("( via .*$)", Pattern.CASE_INSENSITIVE);

	@NotNull
	public static String removeVia(@NotNull String string) {
		string = ENDS_WITH_VIA.matcher(string).replaceAll(EMPTY);
		return string;
	}

	@Deprecated
	@NotNull
	public static String keepToAndRevoveVia(@NotNull String string) {
		return keepToAndRemoveVia(string);
	}

	@NotNull
	public static String keepToAndRemoveVia(@NotNull String string) {
		string = keepTo(string);
		string = removeVia(string);
		return string;
	}

	private static final Pattern MC_ = Pattern.compile("(((mc)([a-z]))([a-z]+))", Pattern.CASE_INSENSITIVE);

	@NotNull
	public static String fixMcXCase(@NotNull String string) { // Mccowan -> McCowan
		final Matcher matcher = MC_.matcher(string);
		while (matcher.find()) {
			string = string.replaceAll(
					matcher.group(2),
					matcher.group(3) + matcher.group(4).toUpperCase()
			);
		}
		return string;
	}

	private static final String WORD_REGEX = "a-zA-ZÀ-ÿ'"; // MARY'S

	private static final Pattern WORD_NON_WORDS = Pattern.compile(
			"([^" + WORD_REGEX + "]*)([" + WORD_REGEX + "]+)([^" + WORD_REGEX + "]*)",
			Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.CANON_EQ);

	private static final String WORD_REGEX_FR = "a-zA-ZÀ-ÿ"; // d'AYLMER

	private static final Pattern WORD_NON_WORDS_FR = Pattern.compile(
			"([^" + WORD_REGEX_FR + "]*)([" + WORD_REGEX_FR + "]+)([^" + WORD_REGEX_FR + "]*)",
			Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE | Pattern.CANON_EQ);

	@NotNull
	public static String toLowerCaseUpperCaseWords(@NotNull Locale locale, @NotNull String string, @NotNull String... ignoreWords) {
		if (string.isEmpty()) {
			return string;
		}
		StringBuilder sb = new StringBuilder();
		Pattern pattern = locale == Locale.FRENCH ? WORD_NON_WORDS_FR : WORD_NON_WORDS;
		Matcher matcher = pattern.matcher(string);
		while (matcher.find()) {
			sb.append(matcher.group(1)); // before
			String word = matcher.group(2);
			if (word.length() > 1
					&& Utils.isUppercaseOnly(word, false, true)
					&& !Utils.isRomanDigits(word)
					&& !containsIgnoreCase(word, ignoreWords)) {
				sb.append(word.toLowerCase(locale));
			} else {
				sb.append(word);
			}
			sb.append(matcher.group(3)); // after
		}
		return sb.toString();
	}

	private static boolean containsIgnoreCase(@Nullable String string, @NotNull String... ignoreWords) {
		if (ignoreWords.length > 0) {
			for (String ignoreWord : ignoreWords) {
				if (ignoreWord.equalsIgnoreCase(string)) {
					return true;
				}
			}
		}
		return false;
	}

	// TODO white-space VS non-word?
	private static final Pattern FIRST = cleanWords("first");
	private static final String FIRST_REPLACEMENT = cleanWordsReplacement("1st");
	private static final Pattern SECOND = cleanWords("second");
	private static final String SECOND_REPLACEMENT = cleanWordsReplacement("2nd");
	private static final Pattern THIRD = cleanWords("third");
	private static final String THIRD_REPLACEMENT = cleanWordsReplacement("3rd");
	private static final Pattern FOURTH = cleanWords("fourth");
	private static final String FOURTH_REPLACEMENT = cleanWordsReplacement("4th");
	private static final Pattern FIFTH = cleanWords("fifth");
	private static final String FIFTH_REPLACEMENT = cleanWordsReplacement("5th");
	private static final Pattern SIXTH = cleanWords("sixth");
	private static final String SIXTH_REPLACEMENT = cleanWordsReplacement("6th");
	private static final Pattern SEVENTH = cleanWords("seventh");
	private static final String SEVENTH_REPLACEMENT = cleanWordsReplacement("7th");
	private static final Pattern EIGHTH = cleanWords("eighth");
	private static final String EIGHTH_REPLACEMENT = cleanWordsReplacement("8th");
	private static final Pattern NINTH = cleanWords("ninth");
	private static final String NINTH_REPLACEMENT = cleanWordsReplacement("9th");

	private static final Pattern NO_DIGITS = Pattern.compile("((^|\\W)(no\\.? (\\d+))(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String NO_DIGITS_REPLACEMENT = "$2#$4$5"; // #0

	@NotNull
	public static String cleanNumbers(@NotNull String string) {
		string = FIRST.matcher(string).replaceAll(FIRST_REPLACEMENT);
		string = SECOND.matcher(string).replaceAll(SECOND_REPLACEMENT);
		string = THIRD.matcher(string).replaceAll(THIRD_REPLACEMENT);
		string = FOURTH.matcher(string).replaceAll(FOURTH_REPLACEMENT);
		string = FIFTH.matcher(string).replaceAll(FIFTH_REPLACEMENT);
		string = SIXTH.matcher(string).replaceAll(SIXTH_REPLACEMENT);
		string = SEVENTH.matcher(string).replaceAll(SEVENTH_REPLACEMENT);
		string = EIGHTH.matcher(string).replaceAll(EIGHTH_REPLACEMENT);
		string = NINTH.matcher(string).replaceAll(NINTH_REPLACEMENT);
		string = NO_DIGITS.matcher(string).replaceAll(NO_DIGITS_REPLACEMENT);
		return string;
	}

	private static final Pattern EASTBOUND_ = cleanWords("eastbound", "eb");
	private static final String EASTBOUND_REPLACEMENT = cleanWordsReplacement("EB");
	private static final Pattern WESTBOUND_ = cleanWords("westbound", "wb");
	private static final String WESTBOUND_REPLACEMENT = cleanWordsReplacement("WB");
	private static final Pattern SOUTHBOUND_ = cleanWords("southbound", "sb");
	private static final String SOUTHBOUND_REPLACEMENT = cleanWordsReplacement("SB");
	private static final Pattern NORTHBOUND_ = cleanWords("northbound", "nb");
	private static final String NORTHBOUND_REPLACEMENT = cleanWordsReplacement("NB");

	private static final Pattern EAST_ = cleanWords("east");
	private static final String EAST_REPLACEMENT = cleanWordsReplacement("E");
	private static final Pattern WEST_ = cleanWords("west");
	private static final String WEST_REPLACEMENT = cleanWordsReplacement("W");
	private static final Pattern SOUTH_ = cleanWords("south");
	private static final String SOUTH_REPLACEMENT = cleanWordsReplacement("S");
	private static final Pattern NORTH_ = cleanWords("north");
	private static final String NORTH_REPLACEMENT = cleanWordsReplacement("N");

	private static final Pattern EAST_FR_ = cleanWords("est");
	private static final String EAST_FR_REPLACEMENT = cleanWordsReplacement("E");
	private static final Pattern WEST_FR_ = cleanWords("ouest");
	private static final String WEST_FR_REPLACEMENT = cleanWordsReplacement("O");
	private static final Pattern SOUTH_FR_ = cleanWords("sud");
	private static final String SOUTH_FR_REPLACEMENT = cleanWordsReplacement("S");
	private static final Pattern NORTH_FR_ = cleanWords("nord");
	private static final String NORTH_FR_REPLACEMENT = cleanWordsReplacement("N");

	@NotNull
	public static String cleanBounds(@NotNull String string) {
		return cleanBounds(Locale.ENGLISH, string);
	}

	@NotNull
	public static String cleanBounds(@NotNull Locale locale, @NotNull String string) {
		if (locale == Locale.FRENCH) {
			string = EAST_FR_.matcher(string).replaceAll(EAST_FR_REPLACEMENT);
			string = WEST_FR_.matcher(string).replaceAll(WEST_FR_REPLACEMENT);
			string = SOUTH_FR_.matcher(string).replaceAll(SOUTH_FR_REPLACEMENT);
			string = NORTH_FR_.matcher(string).replaceAll(NORTH_FR_REPLACEMENT);
		} else {
			string = EASTBOUND_.matcher(string).replaceAll(EASTBOUND_REPLACEMENT);
			string = WESTBOUND_.matcher(string).replaceAll(WESTBOUND_REPLACEMENT);
			string = SOUTHBOUND_.matcher(string).replaceAll(SOUTHBOUND_REPLACEMENT);
			string = NORTHBOUND_.matcher(string).replaceAll(NORTHBOUND_REPLACEMENT);
			string = EAST_.matcher(string).replaceAll(EAST_REPLACEMENT);
			string = WEST_.matcher(string).replaceAll(WEST_REPLACEMENT);
			string = SOUTH_.matcher(string).replaceAll(SOUTH_REPLACEMENT);
			string = NORTH_.matcher(string).replaceAll(NORTH_REPLACEMENT);
		}
		return string;
	}

	private static final Pattern ID_MERGED = Pattern.compile("(([0-9]*)_merged_([0-9]*))", Pattern.CASE_INSENSITIVE);
	private static final String ID_MERGED_REPLACEMENT = "$2";

	@NotNull
	public static String cleanMergedID(@NotNull String mergedId) {
		return ID_MERGED.matcher(mergedId).replaceAll(ID_MERGED_REPLACEMENT);
	}

	// http://www.semaphorecorp.com/cgi/abbrev.html
	private static final Pattern STREET = cleanWords("street");
	private static final String STREET_REPLACEMENT = cleanWordsReplacement("St");
	private static final Pattern AVENUE = cleanWords("avenue");
	private static final String AVENUE_REPLACEMENT = cleanWordsReplacement("Ave");
	private static final Pattern ROAD = cleanWords("road");
	private static final String ROAD_REPLACEMENT = cleanWordsReplacement("Rd");
	private static final Pattern HIGHWAY = cleanWords("highway");
	private static final String HIGHWAY_REPLACEMENT = cleanWordsReplacement("Hwy");
	private static final Pattern BOULEVARD = cleanWords("boulevard");
	private static final String BOULEVARD_REPLACEMENT = cleanWordsReplacement("Blvd");
	private static final Pattern DRIVE = cleanWords("drive");
	private static final String DRIVE_REPLACEMENT = cleanWordsReplacement("Dr");
	private static final Pattern PLACE = cleanWords("place");
	private static final String PLACE_REPLACEMENT = cleanWordsReplacement("Pl");
	private static final Pattern PLAZA = cleanWords("plaza");
	private static final String PLAZA_REPLACEMENT = cleanWordsReplacement("Plz");
	private static final Pattern LANE = cleanWords("lane");
	private static final String LANE_REPLACEMENT = cleanWordsReplacement("Ln");
	private static final Pattern CRESCENT = cleanWords("crescent");
	private static final String CRESCENT_REPLACEMENT = cleanWordsReplacement("Cr");
	private static final Pattern HEIGHTS = cleanWords("heights");
	private static final String HEIGHTS_REPLACEMENT = cleanWordsReplacement("Hts");
	private static final Pattern GROVE = cleanWords("grove");
	private static final String GROVE_REPLACEMENT = cleanWordsReplacement("Grv");
	public static final Pattern POINT = cleanWords("point");
	public static final String POINT_REPLACEMENT = cleanWordsReplacement("Pt");
	private static final Pattern POINTE = cleanWords("pointe");
	private static final String POINTE_REPLACEMENT = cleanWordsReplacement("Pte");
	private static final Pattern TERRACE = cleanWords("terrace");
	private static final String TERRACE_REPLACEMENT = cleanWordsReplacement("Ter");
	private static final Pattern MANOR = cleanWords("manor");
	private static final String MANOR_REPLACEMENT = cleanWordsReplacement("Mnr");
	private static final Pattern GREEN = cleanWords("green");
	private static final String GREEN_REPLACEMENT = cleanWordsReplacement("Grn");
	private static final Pattern VALLEY = cleanWords("valley", "vallley");
	private static final String VALLEY_REPLACEMENT = cleanWordsReplacement("Vly");
	private static final Pattern HILL = cleanWordsPlural("hill", "h ill");
	private static final String HILL_REPLACEMENT = cleanWordsReplacementPlural("Hl");
	private static final Pattern LAKE = cleanWordsPlural("lake");
	private static final String LAKE_REPLACEMENT = cleanWordsReplacementPlural("Lk");
	private static final Pattern MEADOW = cleanWordsPlural("meadow");
	private static final String MEADOW_REPLACEMENT = cleanWordsReplacementPlural("Mdw");
	private static final Pattern CIRCLE = cleanWords("circle");
	private static final String CIRCLE_REPLACEMENT = cleanWordsReplacement("Cir");
	private static final Pattern GLEN = cleanWords("glen");
	private static final String GLEN_REPLACEMENT = cleanWordsReplacement("Gln");
	private static final Pattern RIDGE = cleanWordsPlural("ridge");
	private static final String RIDGE_REPLACEMENT = cleanWordsReplacementPlural("Rdg");
	private static final Pattern GARDEN = cleanWordsPlural("garden");
	private static final String GARDEN_REPLACEMENT = cleanWordsReplacementPlural("Gdn");
	private static final Pattern CENTER = cleanWords("center", "centre");
	private static final String CENTER_REPLACEMENT = cleanWordsReplacement("Ctr");
	private static final Pattern ESTATE = cleanWordsPlural("estate");
	private static final String ESTATE_REPLACEMENT = cleanWordsReplacementPlural("Est");
	private static final Pattern LANDING = cleanWords("landing");
	private static final String LANDING_REPLACEMENT = cleanWordsReplacement("Lndg");
	private static final Pattern TRAIL = cleanWords("trail");
	private static final String TRAIL_REPLACEMENT = cleanWordsReplacement("Trl");
	private static final Pattern SPRING = cleanWordsPlural("spring");
	private static final String SPRING_REPLACEMENT = cleanWordsReplacementPlural("Spg");
	private static final Pattern VIEW = cleanWords("view");
	private static final String VIEW_REPLACEMENT = cleanWordsReplacement("Vw");
	private static final Pattern VILLAGE = cleanWords("village");
	private static final String VILLAGE_REPLACEMENT = cleanWordsReplacement("Vlg");
	private static final Pattern STATION = cleanWords("station");
	private static final String STATION_REPLACEMENT = cleanWordsReplacement("Sta");
	private static final Pattern RANCH = cleanWords("ranch");
	private static final String RANCH_REPLACEMENT = cleanWordsReplacement("Rnch");
	private static final Pattern COVE = cleanWords("cove");
	private static final String COVE_REPLACEMENT = cleanWordsReplacement("Cv");
	private static final Pattern SQUARE = cleanWords("square");
	private static final String SQUARE_REPLACEMENT = cleanWordsReplacement("Sq");
	private static final Pattern BROOK = cleanWords("brook");
	private static final String BROOK_REPLACEMENT = cleanWordsReplacement("Brk");
	private static final Pattern CREEK = cleanWords("creek");
	private static final String CREEK_REPLACEMENT = cleanWordsReplacement("Crk");
	private static final Pattern CROSSING = cleanWords("crossing");
	private static final String CROSSING_REPLACEMENT = cleanWordsReplacement("Xing");
	private static final Pattern CLIFF = cleanWordsPlural("cliff");
	private static final String CLIFF_REPLACEMENT = cleanWordsReplacementPlural("Clf");
	private static final Pattern SHORE = cleanWordsPlural("shore");
	private static final String SHORE_REPLACEMENT = cleanWordsReplacementPlural("Shr");
	private static final Pattern MOUNT = cleanWords("mount");
	private static final String MOUNT_REPLACEMENT = cleanWordsReplacement("Mt");
	private static final Pattern MOUNTAIN = cleanWords("mountain");
	private static final String MOUNTAIN_REPLACEMENT = cleanWordsReplacement("Mtn");
	private static final Pattern MARKET = cleanWords("market");
	private static final String MARKET_REPLACEMENT = cleanWordsReplacement("Mkt");
	private static final Pattern BUILDING = cleanWords("building");
	private static final String BUILDING_REPLACEMENT = cleanWordsReplacement("Bldg");
	private static final Pattern GREENS = cleanWords("greens");
	private static final String GREENS_REPLACEMENT = cleanWordsReplacement("Grns");
	private static final Pattern PARKWAY = cleanWords("parkway");
	private static final String PARKWAY_REPLACEMENT = cleanWordsReplacement("Pkwy");
	private static final Pattern ISLAND = cleanWordsPlural("island");
	private static final String ISLAND_REPLACEMENT = cleanWordsReplacementPlural("Isl");
	// https://public.oed.com/how-to-use-the-oed/abbreviations/
	private static final Pattern NEIGHBOURHOOD_ = cleanWordsPlural("neighbourhood", "neighbour");
	private static final String NEIGHBOURHOOD_REPLACEMENT = cleanWordsReplacementPlural("Neighb");
	// not official
	private static final Pattern APARTMENT_ = cleanWords("apartment");
	private static final String APARTMENT_REPLACEMENT = cleanWordsReplacement("Apt"); // not official
	private static final Pattern PARK = cleanWords("park");
	private static final String PARK_REPLACEMENT = cleanWordsReplacement("Pk"); // not official
	private static final Pattern EXCHANGE_ = cleanWords("exchange");
	private static final String EXCHANGE_REPLACEMENT = cleanWordsReplacement("Exch"); // not official
	private static final Pattern TERMINAL_ = cleanWords("terminal");
	private static final String TERMINAL_REPLACEMENT = cleanWordsReplacement("Term"); // not official
	private static final Pattern GATE = cleanWordsPlural("gate");
	private static final String GATE_REPLACEMENT = cleanWordsReplacementPlural("Gt"); // not official
	private static final Pattern PARKING = cleanWords("parking");
	private static final String PARKING_REPLACEMENT = cleanWordsReplacement("Pkng"); // not official
	private static final Pattern HOSPITAL = cleanWords("hospital");
	private static final String HOSPITAL_REPLACEMENT = cleanWordsReplacement("Hosp"); // not official
	private static final Pattern OPPOSITE_ = cleanWords("opposite");
	private static final String OPPOSITE_REPLACEMENT = cleanWordsReplacement("Opp"); // not official
	private static final Pattern INDUSTRIAL_ = cleanWords("industrial");
	private static final String INDUSTRIAL_REPLACEMENT = cleanWordsReplacement("Ind"); // not official
	private static final Pattern COUNTER_CLOCKWISE_ = Pattern.compile("((^|\\W)counter( - |-|\\s)?clockwise(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String COUNTER_CLOCKWISE_REPLACEMENT = cleanWordsReplacement("CCW"); // not official
	private static final Pattern CLOCKWISE_ = cleanWords("clockwise");
	private static final String CLOCKWISE_REPLACEMENT = cleanWordsReplacement("CW"); // not official
	//
	private static final Pattern PLATFORM_ = Pattern.compile("( (platform (\\w+)))", Pattern.CASE_INSENSITIVE);
	private static final String PLATFORM_REPLACEMENT = " P:$3";

	@NotNull
	public static String cleanStreetTypes(@NotNull String string) {
		string = LANE.matcher(string).replaceAll(LANE_REPLACEMENT);
		string = PLACE.matcher(string).replaceAll(PLACE_REPLACEMENT);
		string = PLAZA.matcher(string).replaceAll(PLAZA_REPLACEMENT);
		string = DRIVE.matcher(string).replaceAll(DRIVE_REPLACEMENT);
		string = BOULEVARD.matcher(string).replaceAll(BOULEVARD_REPLACEMENT);
		string = HIGHWAY.matcher(string).replaceAll(HIGHWAY_REPLACEMENT);
		string = STREET.matcher(string).replaceAll(STREET_REPLACEMENT);
		string = AVENUE.matcher(string).replaceAll(AVENUE_REPLACEMENT);
		string = ROAD.matcher(string).replaceAll(ROAD_REPLACEMENT);
		string = CRESCENT.matcher(string).replaceAll(CRESCENT_REPLACEMENT);
		string = HEIGHTS.matcher(string).replaceAll(HEIGHTS_REPLACEMENT);
		string = GROVE.matcher(string).replaceAll(GROVE_REPLACEMENT);
		string = POINT.matcher(string).replaceAll(POINT_REPLACEMENT);
		string = POINTE.matcher(string).replaceAll(POINTE_REPLACEMENT);
		string = TERRACE.matcher(string).replaceAll(TERRACE_REPLACEMENT);
		string = MANOR.matcher(string).replaceAll(MANOR_REPLACEMENT);
		string = GREEN.matcher(string).replaceAll(GREEN_REPLACEMENT);
		string = VALLEY.matcher(string).replaceAll(VALLEY_REPLACEMENT);
		string = LAKE.matcher(string).replaceAll(LAKE_REPLACEMENT);
		string = MEADOW.matcher(string).replaceAll(MEADOW_REPLACEMENT);
		string = CIRCLE.matcher(string).replaceAll(CIRCLE_REPLACEMENT);
		string = GLEN.matcher(string).replaceAll(GLEN_REPLACEMENT);
		string = RIDGE.matcher(string).replaceAll(RIDGE_REPLACEMENT);
		string = GARDEN.matcher(string).replaceAll(GARDEN_REPLACEMENT);
		string = CENTER.matcher(string).replaceAll(CENTER_REPLACEMENT);
		string = HILL.matcher(string).replaceAll(HILL_REPLACEMENT);
		string = ESTATE.matcher(string).replaceAll(ESTATE_REPLACEMENT);
		string = LANDING.matcher(string).replaceAll(LANDING_REPLACEMENT);
		string = TRAIL.matcher(string).replaceAll(TRAIL_REPLACEMENT);
		string = SPRING.matcher(string).replaceAll(SPRING_REPLACEMENT);
		string = VIEW.matcher(string).replaceAll(VIEW_REPLACEMENT);
		string = VILLAGE.matcher(string).replaceAll(VILLAGE_REPLACEMENT);
		string = STATION.matcher(string).replaceAll(STATION_REPLACEMENT);
		string = RANCH.matcher(string).replaceAll(RANCH_REPLACEMENT);
		string = COVE.matcher(string).replaceAll(COVE_REPLACEMENT);
		string = SQUARE.matcher(string).replaceAll(SQUARE_REPLACEMENT);
		string = BROOK.matcher(string).replaceAll(BROOK_REPLACEMENT);
		string = CREEK.matcher(string).replaceAll(CREEK_REPLACEMENT);
		string = CROSSING.matcher(string).replaceAll(CROSSING_REPLACEMENT);
		string = CLIFF.matcher(string).replaceAll(CLIFF_REPLACEMENT);
		string = SHORE.matcher(string).replaceAll(SHORE_REPLACEMENT);
		string = PARKING.matcher(string).replaceAll(PARKING_REPLACEMENT);
		string = MOUNT.matcher(string).replaceAll(MOUNT_REPLACEMENT);
		string = MOUNTAIN.matcher(string).replaceAll(MOUNTAIN_REPLACEMENT);
		string = APARTMENT_.matcher(string).replaceAll(APARTMENT_REPLACEMENT);
		string = PARK.matcher(string).replaceAll(PARK_REPLACEMENT);
		string = NEIGHBOURHOOD_.matcher(string).replaceAll(NEIGHBOURHOOD_REPLACEMENT);
		string = EXCHANGE_.matcher(string).replaceAll(EXCHANGE_REPLACEMENT);
		string = TERMINAL_.matcher(string).replaceAll(TERMINAL_REPLACEMENT);
		string = GATE.matcher(string).replaceAll(GATE_REPLACEMENT);
		string = HOSPITAL.matcher(string).replaceAll(HOSPITAL_REPLACEMENT);
		string = MARKET.matcher(string).replaceAll(MARKET_REPLACEMENT);
		string = BUILDING.matcher(string).replaceAll(BUILDING_REPLACEMENT);
		string = GREENS.matcher(string).replaceAll(GREENS_REPLACEMENT);
		string = PARKWAY.matcher(string).replaceAll(PARKWAY_REPLACEMENT);
		string = ISLAND.matcher(string).replaceAll(ISLAND_REPLACEMENT);
		string = OPPOSITE_.matcher(string).replaceAll(OPPOSITE_REPLACEMENT);
		string = INDUSTRIAL_.matcher(string).replaceAll(INDUSTRIAL_REPLACEMENT);
		string = COUNTER_CLOCKWISE_.matcher(string).replaceAll(COUNTER_CLOCKWISE_REPLACEMENT); // before clockwise
		string = CLOCKWISE_.matcher(string).replaceAll(CLOCKWISE_REPLACEMENT);
		//
		string = PLATFORM_.matcher(string).replaceAll(PLACE_REPLACEMENT);
		return string;
	}

	// FR-CA : http://www.toponymie.gouv.qc.ca/ct/normes-procedures/terminologie-geographique/liste-termes-geographiques.html
	private static final Pattern FR_CA_AVENUE = cleanWords("avenue");
	private static final String FR_CA_AVENUE_REPLACEMENT = cleanWordsReplacement("Av");
	private static final Pattern FR_CA_AUTOROUTE = cleanWords("autoroute");
	private static final String FR_CA_AUTOROUTE_REPLACEMENT = cleanWordsReplacement("Aut");
	private static final Pattern FR_CA_BOULEVARD = cleanWords("boulevard");
	private static final String FR_CA_BOULEVARD_REPLACEMENT = cleanWordsReplacement("Boul");
	private static final Pattern FR_CA_CARREFOUR = cleanWords("carrefour");
	private static final String FR_CA_CARREFOUR_REPLACEMENT = cleanWordsReplacement("Carref");
	private static final Pattern FR_CA_MONTAGNE = cleanWords("montagne");
	private static final String FR_CA_MONTAGNE_REPLACEMENT = cleanWordsReplacement("Mgne");
	private static final Pattern FR_CA_MONTEE = cleanWords("mont[é|e]e");
	private static final String FR_CA_MONTEE_REPLACEMENT = cleanWordsReplacement("Mtée");
	private static final Pattern FR_CA_PARC_INDUSTRIEL = cleanWords("parc industriel");
	private static final String FR_CA_PARC_INDUSTRIEL_REPLACEMENT = cleanWordsReplacement("Parc Ind");
	private static final Pattern FR_CA_RIVIERE = cleanWords("rivi[e|è]re");
	private static final String FR_CA_RIVIERE_REPLACEMENT = cleanWordsReplacement("Riv");
	private static final Pattern FR_CA_SECTEUR = cleanWords("secteur");
	private static final String FR_CA_SECTEUR_REPLACEMENT = cleanWordsReplacement("Sect");
	private static final Pattern FR_CA_STATION_DE_METRO = cleanWords("Station de m[é|e]tro");
	private static final String FR_CA_STATION_DE_METRO_REPLACEMENT = cleanWordsReplacement("Ston mét");
	private static final Pattern FR_CA_STATION = cleanWords("station");
	private static final String FR_CA_STATION_REPLACEMENT = cleanWordsReplacement("Ston");
	private static final Pattern FR_CA_STATIONNEMENT = cleanWords("stationnement");
	private static final String FR_CA_STATIONNEMENT_REPLACEMENT = cleanWordsReplacement("Stat");
	private static final Pattern FR_CA_TERRASSE = cleanWords("terrasse");
	private static final String FR_CA_TERRASSE_REPLACEMENT = cleanWordsReplacement("Tsse");
	private static final Pattern FR_CA_TERRASSES = cleanWords("terrasses");
	private static final String FR_CA_TERRASSES_REPLACEMENT = cleanWordsReplacement("Tsses");
	// not official
	private static final Pattern FR_CA_TERMINUS = cleanWords("terminus");
	private static final String FR_CA_TERMINUS_REPLACEMENT = cleanWordsReplacement("Term");
	private static final Pattern FR_CA_TEMPORAIRE = cleanWords("temporaire");
	private static final String FR_CA_TEMPORAIRE_REPLACEMENT = cleanWordsReplacement("Temp");
	//
	private static final Pattern FR_CA_QUAI_ = Pattern.compile("( (quai (\\w+)))", Pattern.CASE_INSENSITIVE);
	private static final String FR_CA_QUAI_REPLACEMENT = " Q:$3";
	private static final Pattern FR_CA_PORTE_ = Pattern.compile("( (porte (\\w+)))", Pattern.CASE_INSENSITIVE);
	private static final String FR_CA_PORTE_REPLACEMENT = " P:$3";

	@NotNull
	public static String cleanStreetTypesFRCA(@NotNull String string) {
		string = FR_CA_AVENUE.matcher(string).replaceAll(FR_CA_AVENUE_REPLACEMENT);
		string = FR_CA_AUTOROUTE.matcher(string).replaceAll(FR_CA_AUTOROUTE_REPLACEMENT);
		string = FR_CA_BOULEVARD.matcher(string).replaceAll(FR_CA_BOULEVARD_REPLACEMENT);
		string = FR_CA_CARREFOUR.matcher(string).replaceAll(FR_CA_CARREFOUR_REPLACEMENT);
		string = FR_CA_MONTAGNE.matcher(string).replaceAll(FR_CA_MONTAGNE_REPLACEMENT);
		string = FR_CA_MONTEE.matcher(string).replaceAll(FR_CA_MONTEE_REPLACEMENT);
		string = FR_CA_PARC_INDUSTRIEL.matcher(string).replaceAll(FR_CA_PARC_INDUSTRIEL_REPLACEMENT);
		string = FR_CA_RIVIERE.matcher(string).replaceAll(FR_CA_RIVIERE_REPLACEMENT);
		string = FR_CA_SECTEUR.matcher(string).replaceAll(FR_CA_SECTEUR_REPLACEMENT);
		string = FR_CA_STATION_DE_METRO.matcher(string).replaceAll(FR_CA_STATION_DE_METRO_REPLACEMENT);
		string = FR_CA_STATION.matcher(string).replaceAll(FR_CA_STATION_REPLACEMENT);
		string = FR_CA_STATIONNEMENT.matcher(string).replaceAll(FR_CA_STATIONNEMENT_REPLACEMENT);
		string = FR_CA_TERRASSE.matcher(string).replaceAll(FR_CA_TERRASSE_REPLACEMENT);
		string = FR_CA_TERRASSES.matcher(string).replaceAll(FR_CA_TERRASSES_REPLACEMENT);
		// not official
		string = FR_CA_TERMINUS.matcher(string).replaceAll(FR_CA_TERMINUS_REPLACEMENT);
		string = FR_CA_TEMPORAIRE.matcher(string).replaceAll(FR_CA_TEMPORAIRE_REPLACEMENT);
		//
		string = FR_CA_QUAI_.matcher(string).replaceAll(FR_CA_QUAI_REPLACEMENT);
		string = FR_CA_PORTE_.matcher(string).replaceAll(FR_CA_PORTE_REPLACEMENT);
		return string;
	}
}
