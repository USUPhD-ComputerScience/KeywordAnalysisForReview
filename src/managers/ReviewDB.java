package managers;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import model.Application;
import model.ReviewForAnalysis;
import model.ReviewForCrawler;
import model.Word;
import util.PostgreSQLConnector;

public class ReviewDB {
	public static final String DBLOGIN = "postgres";
	public static final String DBPASSWORD = "phdcs2014";
	public static final String REVIEWDB = "reviewext";
	public static final String APPS_TABLE = "apps";/*
													// release_dates: l,l,l,l
													CREATE TABLE apps(
													appid	TEXT PRIMARY KEY  NOT NULL,
													name		TEXT    NOT NULL,
													count	BIGINT NOT NULL,
													release_dates TEXT,
													start_date BIGINT
													);*/
	public static final String REVIEWS_TABLE = "reviews"; /*
															// cleansed_text: i,i,i;i,i,i,i,i;i,i,i,i
															CREATE TABLE reviews(
															reviewid			TEXT PRIMARY KEY NOT NULL,
															appid			TEXT	references apps(appid),
															title			TEXT,
															raw_text			TEXT,
															cleansed_text	TEXT,
															document_version	TEXT,
															device			TEXT,
															rating			INT 	NOT NULL,
															creation_time	BIGINT 	NOT NULL,
															UNIQUE (appid, reviewid)
															);*/
	public static final String KEYWORDS_TABLE = "keywords"; /*
															// ratex_byday: i,i,i,i,i,i,i
															// POS: pos,i;pos,i;pos,i
															CREATE TABLE keywords(
															ID			INT PRIMARY KEY NOT NULL,
															appid		TEXT references apps(appid),
															keyword		TEXT,
															rate1_byday	TEXT,
															rate2_byday	TEXT,
															rate3_byday	TEXT,
															rate4_byday	TEXT,
															rate5_byday	TEXT,
															POS			TEXT,
															UNIQUE (appid, keyword)
															);*/
	private static final PostgreSQLConnector dbconnector = new PostgreSQLConnector(
			DBLOGIN, DBPASSWORD, REVIEWDB);
	private static ReviewDB instance = null;

	public static synchronized ReviewDB getInstance() {
		if (instance == null) {
			instance = new ReviewDB();
		}
		return instance;
	}

	private ReviewDB() {

	}

	public List<Application> queryMultipleAppsInfo(int minReviews)
			throws SQLException {
		List<Application> appList = new ArrayList<>();
		String fields[] = { "appid", "name", "count", "release_dates" };
		String condition = "count>=" + minReviews;

		ResultSet results;
		results = dbconnector.select(APPS_TABLE, fields, condition);

		while (results.next()) {
			String name = results.getString("name");
			String appid = results.getString("appid");
			int count = results.getInt("count");
			Array release_dates = results.getArray("release_dates");
			Long[] releaseDates = text2long1D(results
					.getString("release_dates"));

			if (appid != null) {
				appList.add(new Application(appid, name, count, releaseDates));
			}
		}
		return appList;
	}

	public Application querySingleAppInfo(String appid) throws SQLException {
		String fields[] = { "appid", "name", "count", "release_dates" };
		String condition = "appid='" + appid + "'";
		// condition = // "count>1000";

		ResultSet results;
		results = dbconnector.select(APPS_TABLE, fields, condition);

		while (results.next()) {
			String name = results.getString("name");
			int count = results.getInt("count");
			Array release_dates = results.getArray("release_dates");
			Long[] releaseDates = (Long[]) release_dates.getArray();

			if (appid != null) {
				return new Application(appid, name, count, releaseDates);
			}
		}
		return null;
	}

	public boolean isAppIDexist(String appid) throws SQLException {
		String fields[] = { "appid", "name", "count", "release_dates" };
		String condition = "appid='" + appid + "'";
		// condition = // "count>1000";

		ResultSet results;
		results = dbconnector.select(APPS_TABLE, fields, condition);

		while (results.next()) {
			return true;
		}
		return false;
	}

	public String getName(String appid) throws SQLException {
		String fields[] = { "name" };
		String condition = "appid='" + appid + "'";
		// condition = // "count>1000";

		ResultSet results;
		results = dbconnector.select(APPS_TABLE, fields, condition);

		while (results.next()) {
			return results.getString("name");
		}
		return null;
	}

	public void updateReviewNumberForApp(int revCount, String appid)
			throws SQLException {
		String fields[] = { "count" };
		String condition = "appid='" + appid + "'";
		// condition = // "count>1000";

		ResultSet results;
		results = dbconnector.select(APPS_TABLE, fields, condition);
		int returnCount = 0;
		while (results.next()) {
			returnCount = results.getInt("count");
		}
		returnCount += revCount;
		dbconnector.update(APPS_TABLE, "count=" + returnCount, condition);
	}

	public boolean insertReview(ReviewForCrawler rev, String appid)
			throws SQLException {
		String values[] = new String[9];

		values[0] = rev.getReviewId();
		values[1] = appid; // appid
		values[2] = rev.getTitle();
		values[3] = rev.getText();
		values[4] = "null";
		values[5] = rev.getDocument_version() + "_";
		values[6] = rev.getDevice_name();
		values[7] = String.valueOf(rev.getRating());
		values[8] = String.valueOf(rev.getCreationTime());
		int arrays[] = new int[] { 0, 0, 0, 0, 0, 0, 0, 1, 2 };
		int id = 0;
		try {
			id = dbconnector.insert(REVIEWS_TABLE, values, arrays);
		} catch (SQLException e) {
		}
		if (id == 0)
			return false;
		return true;
	}

	public void addNewApp(String appid, String name) throws SQLException {

		String values[] = new String[5];
		values[0] = appid; // appid
		values[1] = name;
		values[2] = String.valueOf(0);
		values[3] = "null";
		values[4] = String.valueOf(System.currentTimeMillis());
		int arrays[] = new int[] { 0, 0, 2, 0, 2 };
		dbconnector.insert(APPS_TABLE, values, arrays);

	}

	public void close() {
		dbconnector.close();
	}

	public List<ReviewForAnalysis> queryReviews(Application app)
			throws SQLException {
		List<ReviewForAnalysis> reviews = new ArrayList<>();
		String fields[] = { "title", "raw_text", "cleansed_text",
				"document_version", "reviewid", "device", "rating",
				"creation_time" };
		String condition = "appid='" + app.getAppID() + "'";
		ResultSet results;
		results = dbconnector.select(REVIEWS_TABLE, fields, condition);
		while (results.next()) {
			String reviewID = results.getString("reviewid");
			long creationTime = results.getLong("creationtime");
			String raw_text = results.getString("raw_text");
			int[][] cleansed_text = (int[][]) results.getArray("cleansed_text")
					.getArray();
			if (raw_text.indexOf('\t') < 0) // Not from Android Market
				raw_text = results.getString("title") + "." + raw_text;

			ReviewForAnalysis.ReviewBuilder reviewBuilder = new ReviewForAnalysis.ReviewBuilder();
			reviewBuilder.rawText(raw_text);
			reviewBuilder.cleansedText(cleansed_text);
			reviewBuilder.reviewId(reviewID);
			reviewBuilder.deviceName(results.getString("device"));
			reviewBuilder.documentVersion(results.getString("documentversion"));
			reviewBuilder.rating(results.getInt("rating"));
			reviewBuilder.creationTime(creationTime);
			reviewBuilder.application(app);
			reviews.add(reviewBuilder.createReview());
		}
		return reviews;
	}

	private int[] text2Int1D(String text) {
		int[] argInt = null;
		if (!text.equals("null")) {
			String[] ints = text.split(",");
			argInt = new int[ints.length];
			for (int j = 0; j < argInt.length; j++)
				argInt[j] = Integer.parseInt(ints[j]);
		}
		return argInt;
	}

	private int[][] text2Int2D(String text) {
		int[][] argIntArray = null;
		if (!text.equals("null")) {
			// "1,2,3;1,2,3;1,2,3"
			String[] intArray = text.split(";");
			argIntArray = new int[intArray.length][];
			for (int j = 0; j < argIntArray.length; j++) {
				String[] arr = intArray[j].split(",");
				argIntArray[j] = new int[arr.length];
				for (int k = 0; k < arr.length; k++)
					argIntArray[j][k] = Integer.parseInt(arr[k]);
			}
		}
		return argIntArray;
	}

	private Map<String, Integer> text2Map(String text) {
		Map<String, Integer> daMap = null;
		if (!text.equals("null")) {
			// "pos,i;pos,i;pos,i"
			daMap = new HashMap<>();
			String[] entries = text.split(";");

			for (int j = 0; j < entries.length; j++) {
				String[] arr = entries[j].split(",");
				daMap.put(arr[0], Integer.parseInt(arr[1]));
			}
		}
		return daMap;
	}

	private Long[] text2long1D(String text) {
		Long[] argLong = null;
		if (!text.equals("null")) {
			String[] bigints = text.split(",");
			argLong = new Long[bigints.length];
			for (int j = 0; j < argLong.length; j++)
				argLong[j] = Long.parseLong(bigints[j]);
		}
		return argLong;
	}

	public Word querySingleWord(int DBID, Application app) throws SQLException {
		String fields[] = { "keyword", "appid", "rating_count", "time_series",
				"POS" };
		String condition = "ID=" + DBID;
		ResultSet results;
		results = dbconnector.select(REVIEWS_TABLE, fields, condition);
		while (results.next()) {
			String[] posDATA = (String[]) results.getArray("POS").getArray();
			HashMap<String, Integer> POSset = null;
			if (posDATA != null || posDATA.length > 0) {
				POSset = new HashMap<>();
			}
			int[] count = (int[]) results.getArray("rating_count").getArray();
			int[][] timeSeries = (int[][]) results.getArray("time_series")
					.getArray();
			return new Word(results.getString("keyword"), POSset, app, count,
					timeSeries);
		}
		return null;
	}
}
