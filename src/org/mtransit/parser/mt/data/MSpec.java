package org.mtransit.parser.mt.data;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.apache.commons.lang3.text.WordUtils;
import org.apache.commons.lang3.text.translate.CharSequenceTranslator;
import org.apache.commons.lang3.text.translate.LookupTranslator;
import org.mtransit.parser.Constants;
import org.mtransit.parser.Utils;

public class MSpec {

	public List<MAgency> agencies;
	public List<MStop> stops;
	public List<MRoute> routes;
	public List<MTrip> trips;
	public List<MTripStop> tripStops;
	public List<MServiceDate> serviceDates;
	public Map<Integer, List<MSchedule>> stopSchedules;
	public Map<Long, List<MFrequency>> routeFrequencies;

	public MSpec(List<MAgency> agencies, List<MStop> stops, List<MRoute> routes, List<MTrip> trips, List<MTripStop> tripStops, List<MServiceDate> serviceDates,
			Map<Integer, List<MSchedule>> routeSchedules, Map<Integer, List<MSchedule>> stopSchedules, Map<Long, List<MFrequency>> routeFrequencies) {
		this.stops = stops;
		this.routes = routes;
		this.trips = trips;
		this.tripStops = tripStops;
		this.serviceDates = serviceDates;
		this.stopSchedules = stopSchedules;
		this.routeFrequencies = routeFrequencies;
	}

	public static final Pattern CLEAN_SLASHES = Pattern.compile("(\\w)[\\s]*[/][\\s]*(\\w)");
	public static final String CLEAN_SLASHES_REPLACEMENT = "$1 / $2";
	public static final Pattern CLEAN_EN_DASHES = Pattern.compile("(\\w)[\\s]*[–][\\s]*(\\w)");
	public static final String CLEAN_EN_DASHES_REPLACEMENT = "$1–$2";
	private static final String PARENTHESE1 = "\\(";
	private static final String PARENTHESE2 = "\\)";
	public static final Pattern CLEAN_PARENTHESE1 = Pattern.compile("[" + PARENTHESE1 + "][\\s]*(\\w)");
	public static final String CLEAN_PARENTHESE1_REPLACEMENT = PARENTHESE1 + "$1";
	public static final Pattern CLEAN_PARENTHESE2 = Pattern.compile("(\\w)[\\s]*[" + PARENTHESE2 + "]");
	public static final String CLEAN_PARENTHESE2_REPLACEMENT = "$1" + PARENTHESE2;

	private static final CharSequenceTranslator ESCAPE = new LookupTranslator(new String[][] { { "\'", "\'\'" }, { "_", Constants.EMPTY } });

	public static String escape(String string) {
		return ESCAPE.translate(string);
	}

	public static String cleanLabel(String label) {
		label = label.replaceAll("\\s+", " ");
		label = WordUtils.capitalize(label, Constants.SPACE, '-', '–', '/', Constants.STRING_DELIMITER, '(', '.');
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

	private static final Pattern[] START_WITH_CHARS = new Pattern[] { //
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

	public static final Pattern[] SPACE_CHARS = new Pattern[] { //
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

	private static final Pattern[] SLASH_CHARS = new Pattern[] {//
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
	private static final String PLACE_CHAR_AV = "av. ";
	private static final String PLACE_CHAR_AVENUE = "avenue ";
	private static final String PLACE_CHAR_BOUL = "boul. ";
	private static final String PLACE_CHAR_BOULEVARD = "boulevard ";
	private static final String PLACE_CHAR_CH = "ch. ";
	private static final String PLACE_CHAR_CIVIQUE = "civique ";
	private static final String PLACE_CHAR_CROISS = "croiss. ";
	private static final String PLACE_CHAR_QUARTIER = "quartier ";
	private static final String PLACE_CHAR_RTE = "rte ";
	private static final String PLACE_CHAR_RUE = "rue ";
	private static final String PLACE_CHAR_TSSE = "tsse ";

	private static final Pattern[] START_WITH_ST = new Pattern[] { //
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

	public static final Pattern[] SPACE_ST = new Pattern[] { //
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

	private static final Pattern[] SLASH_ST = new Pattern[] { //
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

	public static final Pattern SAINT = Pattern.compile("(saint)", Pattern.CASE_INSENSITIVE);
	public static final String SAINT_REPLACEMENT = "St";

	public static final Pattern POINT = Pattern.compile("(point)", Pattern.CASE_INSENSITIVE);
	public static final String POINT_REPLACEMENT = "Pt";

	public static final Pattern ET = Pattern.compile("( et )", Pattern.CASE_INSENSITIVE);
	public static final String ET_REPLACEMENT = " & ";

	public static final Pattern CONVERT_ET_TO_SLASHES = Pattern.compile("(\\w)[\\s]+(et)[\\s]+(\\w)", Pattern.UNICODE_CHARACTER_CLASS
			| Pattern.CASE_INSENSITIVE);
	public static final String CONVERT_ET_TO_SLASHES_REPLACEMENT = "$1 / $3";

	public static final String SPACE = " ";
	public static final String SLASH_SPACE = "/ ";

	public static String cleanLabelFR(String label) {
		label = CLEAN_SLASHES.matcher(label).replaceAll(CLEAN_SLASHES_REPLACEMENT);
		label = CLEAN_PARENTHESE1.matcher(label).replaceAll(CLEAN_PARENTHESE1_REPLACEMENT);
		label = CLEAN_PARENTHESE2.matcher(label).replaceAll(CLEAN_PARENTHESE2_REPLACEMENT);
		label = SAINT.matcher(label).replaceAll(SAINT_REPLACEMENT);
		label = Utils.replaceAll(label.trim(), START_WITH_ST, SPACE); // StringUtils.EMPTY); // SPACE);
		label = Utils.replaceAll(label, SLASH_ST, SLASH_SPACE);
		label = Utils.replaceAll(label.trim(), START_WITH_CHARS, SPACE); // , StringUtils.EMPTY); //
		label = Utils.replaceAll(label, SLASH_CHARS, SLASH_SPACE);
		return cleanLabel(label);
	}

	private static final Pattern FIRST = Pattern.compile("(^|\\s){1}(first)($|\\s){1}", Pattern.CASE_INSENSITIVE);
	private static final String FIRST_REPLACEMENT = "$11st$3";
	private static final Pattern SECOND = Pattern.compile("(^|\\s){1}(second)($|\\s){1}", Pattern.CASE_INSENSITIVE);
	private static final String SECOND_REPLACEMENT = "$12nd$3";
	private static final Pattern THIRD = Pattern.compile("(^|\\s){1}(third)($|\\s){1}", Pattern.CASE_INSENSITIVE);
	private static final String THIRD_REPLACEMENT = "$13rd$3";
	private static final Pattern FOURTH = Pattern.compile("(^|\\s){1}(fourth)($|\\s){1}", Pattern.CASE_INSENSITIVE);
	private static final String FOURTH_REPLACEMENT = "$14th$3";
	private static final Pattern FIFTH = Pattern.compile("(^|\\s){1}(fifth)($|\\s){1}", Pattern.CASE_INSENSITIVE);
	private static final String FIFTH_REPLACEMENT = "$15th$3";
	private static final Pattern SIXTH = Pattern.compile("(^|\\s){1}(sixth)($|\\s){1}", Pattern.CASE_INSENSITIVE);
	private static final String SIXTH_REPLACEMENT = "$16th$3";
	private static final Pattern SEVENTH = Pattern.compile("(^|\\s){1}(seventh)($|\\s){1}", Pattern.CASE_INSENSITIVE);
	private static final String SEVENTH_REPLACEMENT = "$17th$3";
	private static final Pattern EIGHTH = Pattern.compile("(^|\\s){1}(eighth)($|\\s){1}", Pattern.CASE_INSENSITIVE);
	private static final String EIGHTH_REPLACEMENT = "$18th$3";
	private static final Pattern NINTH = Pattern.compile("(^|\\s){1}(ninth)($|\\s){1}", Pattern.CASE_INSENSITIVE);
	private static final String NINTH_REPLACEMENT = "$19th$3";

	public static String cleanNumbers(String string) {
		string = FIRST.matcher(string).replaceAll(FIRST_REPLACEMENT);
		string = SECOND.matcher(string).replaceAll(SECOND_REPLACEMENT);
		string = THIRD.matcher(string).replaceAll(THIRD_REPLACEMENT);
		string = FOURTH.matcher(string).replaceAll(FOURTH_REPLACEMENT);
		string = FIFTH.matcher(string).replaceAll(FIFTH_REPLACEMENT);
		string = SIXTH.matcher(string).replaceAll(SIXTH_REPLACEMENT);
		string = SEVENTH.matcher(string).replaceAll(SEVENTH_REPLACEMENT);
		string = EIGHTH.matcher(string).replaceAll(EIGHTH_REPLACEMENT);
		string = NINTH.matcher(string).replaceAll(NINTH_REPLACEMENT);
		return string;
	}
}
