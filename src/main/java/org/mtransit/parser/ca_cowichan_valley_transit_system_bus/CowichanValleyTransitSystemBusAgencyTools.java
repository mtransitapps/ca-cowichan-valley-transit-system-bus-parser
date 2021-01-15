package org.mtransit.parser.ca_cowichan_valley_transit_system_bus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.mtransit.parser.CleanUtils;
import org.mtransit.parser.DefaultAgencyTools;
import org.mtransit.parser.MTLog;
import org.mtransit.parser.Pair;
import org.mtransit.parser.SplitUtils;
import org.mtransit.parser.SplitUtils.RouteTripSpec;
import org.mtransit.parser.StringUtils;
import org.mtransit.parser.Utils;
import org.mtransit.parser.gtfs.data.GCalendar;
import org.mtransit.parser.gtfs.data.GCalendarDate;
import org.mtransit.parser.gtfs.data.GRoute;
import org.mtransit.parser.gtfs.data.GSpec;
import org.mtransit.parser.gtfs.data.GStop;
import org.mtransit.parser.gtfs.data.GTrip;
import org.mtransit.parser.gtfs.data.GTripStop;
import org.mtransit.parser.mt.data.MAgency;
import org.mtransit.parser.mt.data.MDirectionType;
import org.mtransit.parser.mt.data.MRoute;
import org.mtransit.parser.mt.data.MTrip;
import org.mtransit.parser.mt.data.MTripStop;

import static org.mtransit.parser.Constants.EMPTY;

// https://www.bctransit.com/open-data
// https://www.bctransit.com/data/gtfs/cowichan-valley.zip
public class CowichanValleyTransitSystemBusAgencyTools extends DefaultAgencyTools {

	public static void main(String[] args) {
		if (args == null || args.length == 0) {
			args = new String[3];
			args[0] = "input/gtfs.zip";
			args[1] = "../../mtransitapps/ca-cowichan-valley-transit-system-bus-android/res/raw/";
			if (true) {
				args[1] = "../../mtransitapps-dev/ca-cowichan-valley-transit-system-bus-android/res/raw/";
			}
			args[2] = ""; // files-prefix
		}
		new CowichanValleyTransitSystemBusAgencyTools().start(args);
	}

	@Nullable
	private HashSet<String> serviceIds;

	@Override
	public void start(String[] args) {
		MTLog.log("Generating Cowichan Valley Regional Transit System bus data...");
		long start = System.currentTimeMillis();
		this.serviceIds = extractUsefulServiceIds(args, this, true);
		super.start(args);
		MTLog.log("Generating Cowichan Valley Regional Transit System bus data... DONE in %s.",
				Utils.getPrettyDuration(System.currentTimeMillis() - start));
	}

	@Override
	public boolean excludingAll() {
		return this.serviceIds != null && this.serviceIds.isEmpty();
	}

	@Override
	public boolean excludeCalendar(GCalendar gCalendar) {
		if (this.serviceIds != null) {
			return excludeUselessCalendar(gCalendar, this.serviceIds);
		}
		return super.excludeCalendar(gCalendar);
	}

	@Override
	public boolean excludeCalendarDate(GCalendarDate gCalendarDates) {
		if (this.serviceIds != null) {
			return excludeUselessCalendarDate(gCalendarDates, this.serviceIds);
		}
		return super.excludeCalendarDate(gCalendarDates);
	}

	private static final String INCLUDE_AGENCY_ID = "10"; // Cowichan Valley Regional Transit System only

	@Override
	public boolean excludeRoute(GRoute gRoute) {
		if (!INCLUDE_AGENCY_ID.equals(gRoute.getAgencyId())) {
			return true;
		}
		return super.excludeRoute(gRoute);
	}

	@Override
	public boolean excludeTrip(GTrip gTrip) {
		if (this.serviceIds != null) {
			return excludeUselessTrip(gTrip, this.serviceIds);
		}
		return super.excludeTrip(gTrip);
	}

	@Override
	public Integer getAgencyRouteType() {
		return MAgency.ROUTE_TYPE_BUS;
	}

	@Override
	public long getRouteId(GRoute gRoute) {
		return Long.parseLong(gRoute.getRouteShortName()); // use route short name as route ID
	}

	private static final String AGENCY_COLOR_GREEN = "34B233";// GREEN (from PDF Corporate Graphic Standards)
	@SuppressWarnings("unused")
	private static final String AGENCY_COLOR_BLUE = "002C77"; // BLUE (from PDF Corporate Graphic Standards)

	private static final String AGENCY_COLOR = AGENCY_COLOR_GREEN;

	@Override
	public String getAgencyColor() {
		return AGENCY_COLOR;
	}

	@Override
	public String getRouteColor(GRoute gRoute) {
		if (StringUtils.isEmpty(gRoute.getRouteColor())) {
			int rsn = Integer.parseInt(gRoute.getRouteShortName());
			switch (rsn) {
			// @formatter:off
			case 2: return "17468B";
			case 3: return "80CC28";
			case 4: return "F68712";
			case 5: return "C06EBE";
			case 6: return "ED0790";
			case 7: return "49690F";
			case 8: return "49176D";
			case 9: return "B2BB1E";
			case 20: return "0073AD";
			case 21: return "A54499";
			case 31: return "FBBD09";
			case 34: return "0B6FAE";
			case 36: return "8A0C34";
			case 44: return "00AA4F";
			case 66: return "8CC63F";
			case 99: return "114D8A";
			// @formatter:on
			}
			if ("7x".equalsIgnoreCase(gRoute.getRouteShortName())) {
				return "ACA86E";
			}
			throw new MTLog.Fatal("Unexpected route color for %s!", gRoute);
		}
		return super.getRouteColor(gRoute);
	}

	private static final String TRAIL = "Trail";

	private static final HashMap<Long, RouteTripSpec> ALL_ROUTE_TRIPS2;
	static {
		HashMap<Long, RouteTripSpec> map2 = new HashMap<>();
		map2.put(5L, new RouteTripSpec(5L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Duncan", //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Eagle Hts") //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList( //
						"106205", // Polkey at Mearns (EB) #EagleHts
								"106225", // ++
								"104033" // Central at Cowichan (NB) #Duncan
						)) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList( //
						"104033", // Central at Cowichan (NB) #Duncan
								"136113", // ++
								"106205" // Polkey at Mearns (EB) #EagleHts
						)) //
				.compileBothTripSort());
		map2.put(7L, new RouteTripSpec(7L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Duncan", //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Cowichan Lk") //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList( //
						"136191", // South Shore 10 block (WB) #CowichanLake
								"136457", // == Cowichan Lake at Greendale (EB)
								"136397", // != Cowichan Valley at Skutz Falls (EB)
								"136034", // != Canada at Station (SB)
								"136202", // != Cowichan Lake at Lake Park (EB)
								"136110", // != Government at Station (EB)
								"104033" // == Central at Cowichan (NB) #Duncan
						)) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList( //
						"104033", // Central at Cowichan (NB) #Duncan
								"106148", // == Canada at Station (NB)
								"106131", // != Beverly at Duncan St (EB)
								"106159", // != Somenos at Cowichan Valley Hwy (NB)
								"136002", // != Ingram at Jubilee (WB)
								"136188", // != Cowichan Lake at Lake Park (WB)
								"136189", // == Cowichan Lake at Greendale (WB)
								"136190", // != Cowichan Lake at Stanley (WB)
								"136191", // xx South Shore 10 block (WB) #CowichanLake
								"136195", // != South Shore at Stone (SB)
								"136427", // != Somenos at Cowichan (NB)
								"136191" // xx South Shore 10 block (WB) #CowichanLake
						)) //
				.compileBothTripSort());
		map2.put(8L, new RouteTripSpec(8L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Duncan", //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Mill Bay") //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList( //
						"108060", // == Huckleberry at Noowick (NB) #MillBay
								"104000", // == Lodgepole at Frayne (NB)
								"108052", // != Deloume at Lodge Pole Rd (EB)
								"108090", // == Deloume Rd at Barry (NB)
								"136295", // xx Mill Bay at Handy (SB)
								"108006", // != Shawnigan Lake-Mill Bay at Trans Canada (WB)
								"108162", // ==
								"108056", // !=
								"104006", // !=
								"136280", // ==
								"104033" // Central at Cowichan (NB) #Duncan
						)) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList( //
						"104033", // Central at Cowichan (NB) #Duncan
								"136373", // Cowichan Bay at Telegraph (SB)
								"108051", // ==
								"106201", // !=
								"106202", // !=
								"108048", // ==
								"108038", // != Kilmalu AT Church Way (WB)
								"136295", // xx Mill Bay at Handy (SB)
								"136296", // == Mill Bay at Bay (SB)
								"136300", // != Mill Bay at Ferry (NB)
								"108060" // == Huckleberry at Noowick (NB) #MillBay
						)) //
				.compileBothTripSort());
		map2.put(9L, new RouteTripSpec(9L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Duncan", //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Mill Bay") //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList( //
						"108060", // Huckleberry at Noowick (NB) #MillBay
								"108090", // !=
								"136295", // xx Mill Bay at Handy (SB)
								"136302", // !=
								"108019", // ==
								"106201", // !=
								"106202", // !=
								"108018", // ==
								"106237", // !=
								"108089", // <>
								"136234", // !=
								"104033" // Central at Cowichan (NB) #Duncan
						)) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList( //
						"104033", // Central at Cowichan (NB) #Duncan
								"104021", // == !=
								"104020", // !=
								"108089", // != <>
								"136248", // == !=
								"108059", // !=
								"136295", // xx Mill Bay at Handy (SB)
								"136296", // !=
								"108060" // Huckleberry at Noowick (NB) #MillBay
						)) //
				.compileBothTripSort());
		map2.put(20L, new RouteTripSpec(20L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Cowichan Lk", //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Youbou") //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList( //
						"108045", // Youbou Rd 10700 Block (NB)
								"136193", // ++
								"108088" // 20 Block South Shore Rd (WB) #CowichanLk
						)) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList( //
						"108088", // 20 Block South Shore Rd (WB) #CowichanLk
								"108172", // ++
								"108045" // Youbou Rd 10700 Block (NB)
						)) //
				.compileBothTripSort());
		map2.put(21L, new RouteTripSpec(21L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Cowichan Lk", //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Honeymoon Bay") //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList( //
						"108168", // South Shore AT Park Ave (WB) #HoneymoonBay
								"136200", // ++
								"108088" // 20 Block South Shore Rd (WB) #CowichanLk
						)) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList( //
						"108088", // 20 Block South Shore Rd (WB) #CowichanLk
								"136196", // ++
								"108168" // South Shore AT Park Ave (WB) #HoneymoonBay
						)) //
				.compileBothTripSort());
		map2.put(31L, new RouteTripSpec(31L, //
				MDirectionType.EAST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Ladysmith", //
				MDirectionType.WEST.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Alderwood") //
				.addTripSort(MDirectionType.EAST.intValue(), //
						Arrays.asList( //
						"106078", // Birchwood at Maplewood (EB) #Alderwood
								"106063", // ++
								"106050" // 1st Ave at Symonds St (SB) #Ladysmith
						)) //
				.addTripSort(MDirectionType.WEST.intValue(), //
						Arrays.asList( //
						"106050", // 1st Ave at Symonds St (SB) #Ladysmith
								"106062", // ++
								"106078" // Birchwood at Maplewood (EB) #Alderwood
						)) //
				.compileBothTripSort());
		map2.put(34L, new RouteTripSpec(34L, //
				MDirectionType.NORTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Ladysmith", //
				MDirectionType.SOUTH.intValue(), MTrip.HEADSIGN_TYPE_STRING, "Chemainus") //
				.addTripSort(MDirectionType.NORTH.intValue(), //
						Arrays.asList( //
						"136142", // Pine at Lang (WB) #Chemainus
								"106095", // ++
								"106050" // 1st Ave at Symonds St (SB) #Ladysmith
						)) //
				.addTripSort(MDirectionType.SOUTH.intValue(), //
						Arrays.asList( //
						"106050", // 1st Ave at Symonds St (SB) #Ladysmith
								"106098", // ++
								"136142" // Pine at Lang (WB) #Chemainus
						)) //
				.compileBothTripSort());
		ALL_ROUTE_TRIPS2 = map2;
	}

	@Override
	public int compareEarly(long routeId, List<MTripStop> list1, List<MTripStop> list2, MTripStop ts1, MTripStop ts2, GStop ts1GStop, GStop ts2GStop) {
		if (ALL_ROUTE_TRIPS2.containsKey(routeId)) {
			return ALL_ROUTE_TRIPS2.get(routeId).compare(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop, this);
		}
		return super.compareEarly(routeId, list1, list2, ts1, ts2, ts1GStop, ts2GStop);
	}

	@Override
	public ArrayList<MTrip> splitTrip(MRoute mRoute, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return ALL_ROUTE_TRIPS2.get(mRoute.getId()).getAllTrips();
		}
		return super.splitTrip(mRoute, gTrip, gtfs);
	}

	@Override
	public Pair<Long[], Integer[]> splitTripStop(MRoute mRoute, GTrip gTrip, GTripStop gTripStop, ArrayList<MTrip> splitTrips, GSpec routeGTFS) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return SplitUtils.splitTripStop(mRoute, gTrip, gTripStop, routeGTFS, ALL_ROUTE_TRIPS2.get(mRoute.getId()), this);
		}
		return super.splitTripStop(mRoute, gTrip, gTripStop, splitTrips, routeGTFS);
	}

	@Override
	public void setTripHeadsign(MRoute mRoute, MTrip mTrip, GTrip gTrip, GSpec gtfs) {
		if (ALL_ROUTE_TRIPS2.containsKey(mRoute.getId())) {
			return; // split
		}
		mTrip.setHeadsignString(
			cleanTripHeadsign(gTrip.getTripHeadsignOrDefault()),
			gTrip.getDirectionIdOrDefault()
		);
	}

	private static final String EXCH = "Exch";
	private static final Pattern EXCHANGE_ = Pattern.compile("((^|\\W)(exchange)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String EXCHANGE_REPLACEMENT = "$2" + EXCH + "$4";

	private static final Pattern EXPRESS_ = Pattern.compile("((^|\\W)(express)(\\W|$))", Pattern.CASE_INSENSITIVE);
	private static final String EXPRESS_REPLACEMENT = "$2$4";

	private static final Pattern STARTS_WITH_VIA_DASH = Pattern.compile("([ ]?(\\-)?[ ]?via .*$)", Pattern.CASE_INSENSITIVE);
	private static final Pattern STARTS_WITH_TO_DASH = Pattern.compile("(^.*[ ]?[\\-]?[ ]?(to ))", Pattern.CASE_INSENSITIVE);

	private static final Pattern KEEP_TRAIL = Pattern.compile(String.format("((^|\\W){1}(%s)(\\W|$){1})", "trl"), Pattern.CASE_INSENSITIVE);
	private static final String KEEP_TRAIL_REPLACEMENT = String.format("$2%s$4", TRAIL);

	@Override
	public String cleanTripHeadsign(String tripHeadsign) {
		tripHeadsign = STARTS_WITH_VIA_DASH.matcher(tripHeadsign).replaceAll(EMPTY);
		tripHeadsign = STARTS_WITH_TO_DASH.matcher(tripHeadsign).replaceAll(EMPTY);
		tripHeadsign = EXPRESS_.matcher(tripHeadsign).replaceAll(EXPRESS_REPLACEMENT);
		tripHeadsign = CleanUtils.CLEAN_AND.matcher(tripHeadsign).replaceAll(CleanUtils.CLEAN_AND_REPLACEMENT);
		tripHeadsign = CleanUtils.cleanStreetTypes(tripHeadsign); // 1st
		tripHeadsign = KEEP_TRAIL.matcher(tripHeadsign).replaceAll(KEEP_TRAIL_REPLACEMENT); // 2nd
		tripHeadsign = CleanUtils.cleanSlashes(tripHeadsign);
		tripHeadsign = CleanUtils.cleanNumbers(tripHeadsign);
		return CleanUtils.cleanLabel(tripHeadsign);
	}

	@Override
	public boolean mergeHeadsign(MTrip mTrip, MTrip mTripToMerge) {
		List<String> headsignsValues = Arrays.asList(mTrip.getHeadsignValue(), mTripToMerge.getHeadsignValue());
		if (mTrip.getRouteId() == 4L) {
			if (Arrays.asList( //
					"East", //
					"Maple Bay" //
			).containsAll(headsignsValues)) {
				mTrip.setHeadsignString("Maple Bay", mTrip.getHeadsignId());
				return true;
			}
		}
		throw new MTLog.Fatal("Unexpected trips to merge %s & %s!", mTrip, mTripToMerge);
	}

	@NotNull
	private String[] getIgnoredUpperCaseWords() {
		return new String[]{"BC"};
	}

	private static final Pattern STARTS_WITH_DCOM = Pattern.compile("(^(\\(-DCOM-\\)))", Pattern.CASE_INSENSITIVE);
	private static final Pattern STARTS_WITH_IMPL = Pattern.compile("(^(\\(-IMPL-\\)))", Pattern.CASE_INSENSITIVE);

	@Override
	public String cleanStopName(String gStopName) {
		gStopName = CleanUtils.toLowerCaseUpperCaseWords(Locale.ENGLISH, gStopName, getIgnoredUpperCaseWords());
		gStopName = STARTS_WITH_DCOM.matcher(gStopName).replaceAll(EMPTY);
		gStopName = STARTS_WITH_IMPL.matcher(gStopName).replaceAll(EMPTY);
		gStopName = CleanUtils.cleanBounds(gStopName);
		gStopName = CleanUtils.CLEAN_AT.matcher(gStopName).replaceAll(CleanUtils.CLEAN_AT_REPLACEMENT);
		gStopName = EXCHANGE_.matcher(gStopName).replaceAll(EXCHANGE_REPLACEMENT);
		gStopName = CleanUtils.cleanStreetTypes(gStopName); // 1st
		gStopName = KEEP_TRAIL.matcher(gStopName).replaceAll(KEEP_TRAIL_REPLACEMENT); // 2nd
		gStopName = CleanUtils.cleanNumbers(gStopName);
		return CleanUtils.cleanLabel(gStopName);
	}
}
