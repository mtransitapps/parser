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
		this.agencies = agencies;
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

	private static final Pattern CLEAN_SPACES = Pattern.compile("\\s+");
	private static final Pattern CLEAN_P1 = Pattern.compile("\\([\\s]+");
	private static final String CLEAN_P1_REPLACEMENT = "(";
	private static final Pattern CLEAN_P2 = Pattern.compile("[\\s]+\\)");
	private static final String CLEAN_P2_REPLACEMENT = ")";

	public static String cleanLabel(String label) {
		label = CLEAN_SPACES.matcher(label).replaceAll(SPACE);
		label = CLEAN_P1.matcher(label).replaceAll(CLEAN_P1_REPLACEMENT);
		label = CLEAN_P2.matcher(label).replaceAll(CLEAN_P2_REPLACEMENT);
		label = WordUtils.capitalize(label, Constants.SPACE, '-', '–', '/', '(', '.');
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

	private static final String REGEX_START_END = "((^|\\W){1}(%s)(\\W|$){1})";
	private static final String REGEX_START_END_REPLACEMENT = "$2%s$4";

	private static final Pattern STREET = Pattern.compile(String.format(REGEX_START_END, "street"), Pattern.CASE_INSENSITIVE);
	private static final String STREET_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "St");
	private static final Pattern AVENUE = Pattern.compile(String.format(REGEX_START_END, "avenue"), Pattern.CASE_INSENSITIVE);
	private static final String AVENUE_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Ave");
	private static final Pattern ROAD = Pattern.compile(String.format(REGEX_START_END, "road"), Pattern.CASE_INSENSITIVE);
	private static final String ROAD_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Rd");
	private static final Pattern HIGHWAY = Pattern.compile(String.format(REGEX_START_END, "highway"), Pattern.CASE_INSENSITIVE);
	private static final String HIGHWAY_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Hwy");
	private static final Pattern BOULEVARD = Pattern.compile(String.format(REGEX_START_END, "boulevard"), Pattern.CASE_INSENSITIVE);
	private static final String BOULEVARD_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Blvd");
	private static final Pattern DRIVE = Pattern.compile(String.format(REGEX_START_END, "drive"), Pattern.CASE_INSENSITIVE);
	private static final String DRIVE_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Dr");
	private static final Pattern PLACE = Pattern.compile(String.format(REGEX_START_END, "place"), Pattern.CASE_INSENSITIVE);
	private static final String PLACE_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Pl");
	private static final Pattern PLAZA = Pattern.compile(String.format(REGEX_START_END, "plaza"), Pattern.CASE_INSENSITIVE);
	private static final String PLAZA_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Plz");
	private static final Pattern LANE = Pattern.compile(String.format(REGEX_START_END, "lane"), Pattern.CASE_INSENSITIVE);
	private static final String LANE_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Ln");
	private static final Pattern CRESCENT = Pattern.compile(String.format(REGEX_START_END, "crescent"), Pattern.CASE_INSENSITIVE);
	private static final String CRESCENT_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Cr");
	private static final Pattern HEIGHTS = Pattern.compile(String.format(REGEX_START_END, "heights"), Pattern.CASE_INSENSITIVE);
	private static final String HEIGHTS_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Hts");
	private static final Pattern GROVE = Pattern.compile(String.format(REGEX_START_END, "grove"), Pattern.CASE_INSENSITIVE);
	private static final String GROVE_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Grv");
	public static final Pattern POINT = Pattern.compile(String.format(REGEX_START_END, "point"), Pattern.CASE_INSENSITIVE);
	public static final String POINT_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Pt");
	private static final Pattern POINTE = Pattern.compile(String.format(REGEX_START_END, "pointe"), Pattern.CASE_INSENSITIVE);
	private static final String POINTE_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Pte");
	private static final Pattern TERRACE = Pattern.compile(String.format(REGEX_START_END, "terrace"), Pattern.CASE_INSENSITIVE);
	private static final String TERRACE_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Ter");
	private static final Pattern MANOR = Pattern.compile(String.format(REGEX_START_END, "manor"), Pattern.CASE_INSENSITIVE);
	private static final String MANOR_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Mnr");
	private static final Pattern GREEN = Pattern.compile(String.format(REGEX_START_END, "green"), Pattern.CASE_INSENSITIVE);
	private static final String GREEN_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Grn");
	private static final Pattern VALLEY = Pattern.compile(String.format(REGEX_START_END, "valley|vallley"), Pattern.CASE_INSENSITIVE);
	private static final String VALLEY_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Vly");
	private static final Pattern HILL = Pattern.compile(String.format(REGEX_START_END, "hill|h ill"), Pattern.CASE_INSENSITIVE);
	private static final String HILL_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Hl");
	private static final Pattern HILLS = Pattern.compile(String.format(REGEX_START_END, "hills"), Pattern.CASE_INSENSITIVE);
	private static final String HILLS_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Hls");
	private static final Pattern LAKE = Pattern.compile(String.format(REGEX_START_END, "lake"), Pattern.CASE_INSENSITIVE);
	private static final String LAKE_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Lk");
	private static final Pattern MEADOW = Pattern.compile(String.format(REGEX_START_END, "meadow"), Pattern.CASE_INSENSITIVE);
	private static final String MEADOW_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Mdw");
	private static final Pattern MEADOWS = Pattern.compile(String.format(REGEX_START_END, "meadows"), Pattern.CASE_INSENSITIVE);
	private static final String MEADOWS_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Mdws");
	private static final Pattern CIRCLE = Pattern.compile(String.format(REGEX_START_END, "circle"), Pattern.CASE_INSENSITIVE);
	private static final String CIRCLE_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Cir");
	private static final Pattern GLEN = Pattern.compile(String.format(REGEX_START_END, "glen"), Pattern.CASE_INSENSITIVE);
	private static final String GLEN_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Gln");
	private static final Pattern RIDGE = Pattern.compile(String.format(REGEX_START_END, "ridge"), Pattern.CASE_INSENSITIVE);
	private static final String RIDGE_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Rdg");
	private static final Pattern GARDEN = Pattern.compile(String.format(REGEX_START_END, "garden"), Pattern.CASE_INSENSITIVE);
	private static final String GARDEN_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Gdn");
	private static final Pattern GARDENS = Pattern.compile(String.format(REGEX_START_END, "gardens"), Pattern.CASE_INSENSITIVE);
	private static final String GARDENS_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Gdns");
	private static final Pattern CENTER = Pattern.compile(String.format(REGEX_START_END, "center|centre"), Pattern.CASE_INSENSITIVE);
	private static final String CENTER_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Ctr");
	private static final Pattern ESTATE = Pattern.compile(String.format(REGEX_START_END, "estate"), Pattern.CASE_INSENSITIVE);
	private static final String ESTATE_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Est");
	private static final Pattern ESTATES = Pattern.compile(String.format(REGEX_START_END, "estates"), Pattern.CASE_INSENSITIVE);
	private static final String ESTATES_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Ests");
	private static final Pattern LANDING = Pattern.compile(String.format(REGEX_START_END, "landing"), Pattern.CASE_INSENSITIVE);
	private static final String LANDING_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Lndg");
	private static final Pattern TRAIL = Pattern.compile(String.format(REGEX_START_END, "trail"), Pattern.CASE_INSENSITIVE);
	private static final String TRAIL_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Trl");
	private static final Pattern SPRING = Pattern.compile(String.format(REGEX_START_END, "spring"), Pattern.CASE_INSENSITIVE);
	private static final String SPRING_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Spg");
	private static final Pattern SPRINGS = Pattern.compile(String.format(REGEX_START_END, "springs"), Pattern.CASE_INSENSITIVE);
	private static final String SPRINGS_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Spgs");
	private static final Pattern VIEW = Pattern.compile(String.format(REGEX_START_END, "view"), Pattern.CASE_INSENSITIVE);
	private static final String VIEW_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Vw");
	private static final Pattern VILLAGE = Pattern.compile(String.format(REGEX_START_END, "village"), Pattern.CASE_INSENSITIVE);
	private static final String VILLAGE_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Vlg");
	private static final Pattern STATION = Pattern.compile(String.format(REGEX_START_END, "station"), Pattern.CASE_INSENSITIVE);
	private static final String STATION_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Sta");
	private static final Pattern RANCH = Pattern.compile(String.format(REGEX_START_END, "ranch"), Pattern.CASE_INSENSITIVE);
	private static final String RANCH_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Rnch");
	private static final Pattern COVE = Pattern.compile(String.format(REGEX_START_END, "cove"), Pattern.CASE_INSENSITIVE);
	private static final String COVE_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Cv");
	private static final Pattern SQUARE = Pattern.compile(String.format(REGEX_START_END, "square"), Pattern.CASE_INSENSITIVE);
	private static final String SQUARE_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Sq");
	private static final Pattern BROOK = Pattern.compile(String.format(REGEX_START_END, "brook"), Pattern.CASE_INSENSITIVE);
	private static final String BROOK_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Brk");
	private static final Pattern CREEK = Pattern.compile(String.format(REGEX_START_END, "creek"), Pattern.CASE_INSENSITIVE);
	private static final String CREEK_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Crk");
	private static final Pattern CROSSING = Pattern.compile(String.format(REGEX_START_END, "crossing"), Pattern.CASE_INSENSITIVE);
	private static final String CROSSING_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Xing");
	private static final Pattern CLIFF = Pattern.compile(String.format(REGEX_START_END, "cliff"), Pattern.CASE_INSENSITIVE);
	private static final String CLIFF_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Clf");
	private static final Pattern CLIFFS = Pattern.compile(String.format(REGEX_START_END, "cliffs"), Pattern.CASE_INSENSITIVE);
	private static final String CLIFFS_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Clfs");
	private static final Pattern SHORE = Pattern.compile(String.format(REGEX_START_END, "shore"), Pattern.CASE_INSENSITIVE);
	private static final String SHORE_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Shr");
	private static final Pattern SHORES = Pattern.compile(String.format(REGEX_START_END, "shores"), Pattern.CASE_INSENSITIVE);
	private static final String SHORES_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Shrs");
	private static final Pattern PARK = Pattern.compile(String.format(REGEX_START_END, "park"), Pattern.CASE_INSENSITIVE);
	private static final String PARK_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Pk"); // not official
	private static final Pattern GATE = Pattern.compile(String.format(REGEX_START_END, "gate"), Pattern.CASE_INSENSITIVE);
	private static final String GATE_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Gt"); // not official
	private static final Pattern PARKING = Pattern.compile(String.format(REGEX_START_END, "parking"), Pattern.CASE_INSENSITIVE);
	private static final String PARKING_REPLACEMENT = String.format(REGEX_START_END_REPLACEMENT, "Pkng"); // not official

	public static String cleanStreetTypes(String string) {
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
		string = HILLS.matcher(string).replaceAll(HILLS_REPLACEMENT);
		string = LAKE.matcher(string).replaceAll(LAKE_REPLACEMENT);
		string = MEADOW.matcher(string).replaceAll(MEADOW_REPLACEMENT);
		string = MEADOWS.matcher(string).replaceAll(MEADOWS_REPLACEMENT);
		string = CIRCLE.matcher(string).replaceAll(CIRCLE_REPLACEMENT);
		string = GLEN.matcher(string).replaceAll(GLEN_REPLACEMENT);
		string = RIDGE.matcher(string).replaceAll(RIDGE_REPLACEMENT);
		string = GARDEN.matcher(string).replaceAll(GARDEN_REPLACEMENT);
		string = GARDENS.matcher(string).replaceAll(GARDENS_REPLACEMENT);
		string = CENTER.matcher(string).replaceAll(CENTER_REPLACEMENT);
		string = HILL.matcher(string).replaceAll(HILL_REPLACEMENT);
		string = ESTATE.matcher(string).replaceAll(ESTATE_REPLACEMENT);
		string = ESTATES.matcher(string).replaceAll(ESTATES_REPLACEMENT);
		string = LANDING.matcher(string).replaceAll(LANDING_REPLACEMENT);
		string = TRAIL.matcher(string).replaceAll(TRAIL_REPLACEMENT);
		string = SPRING.matcher(string).replaceAll(SPRING_REPLACEMENT);
		string = SPRINGS.matcher(string).replaceAll(SPRINGS_REPLACEMENT);
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
		string = CLIFFS.matcher(string).replaceAll(CLIFFS_REPLACEMENT);
		string = SHORE.matcher(string).replaceAll(SHORE_REPLACEMENT);
		string = SHORES.matcher(string).replaceAll(SHORES_REPLACEMENT);
		string = PARKING.matcher(string).replaceAll(PARKING_REPLACEMENT);
		string = PARK.matcher(string).replaceAll(PARK_REPLACEMENT);
		string = GATE.matcher(string).replaceAll(GATE_REPLACEMENT);
		return string;
	}
}
