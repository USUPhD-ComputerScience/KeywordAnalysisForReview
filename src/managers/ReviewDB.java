package managers;

import java.sql.Array;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
		String fields[] = { "appid", "name", "count", "release_dates",
				"start_date", "day_index" };
		String condition = "count>=" + minReviews;

		ResultSet results;
		results = dbconnector.select(APPS_TABLE, fields, condition);

		while (results.next()) {
			String name = results.getString("name");
			String appid = results.getString("appid");
			int count = results.getInt("count");
			Long[] releaseDates = text2long1D(results
					.getString("release_dates"));

			if (appid != null) {
				appList.add(new Application(appid, name, count, releaseDates,
						results.getLong("start_date"), results
								.getInt("day_index")));
			}
		}
		return appList;
	}

	public Application querySingleAppInfo(String appid) throws SQLException {
		String fields[] = { "appid", "name", "count", "release_dates",
				"start_date", "day_index" };
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
				return new Application(appid, name, count, releaseDates,
						results.getLong("start_date"),
						results.getInt("day_index"));
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

	public void updateIndexesForApp(String appid, int dayIndex)
			throws SQLException {
		String condition = "appid='" + appid + "'";
		dbconnector.update(APPS_TABLE, "day_index =" + dayIndex, condition);
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
			id = dbconnector
					.insert(REVIEWS_TABLE, values, arrays, false, false);
		} catch (SQLException e) {
		}
		if (id == 0)
			return false;
		return true;
	}

	public void addNewApp(String appid, String name) throws SQLException {

		String values[] = new String[6];
		values[0] = appid; // appid
		values[1] = name;
		values[2] = String.valueOf(0);
		values[3] = "null";
		values[4] = String.valueOf(System.currentTimeMillis());
		values[5] = String.valueOf(System.currentTimeMillis());
		int arrays[] = new int[] { 0, 0, 2, 0, 2, 2 };
		dbconnector.insert(APPS_TABLE, values, arrays, false, false);

	}

	public void close() throws SQLException {
		dbconnector.close();
	}

	public int addKeyWord(String w, String POS, String appid)
			throws SQLException {
		String values[] = new String[8];
		values[0] = appid; // appid
		values[1] = w;
		values[2] = "null";
		values[3] = "null";
		values[4] = "null";
		values[5] = "null";
		values[6] = "null";
		values[7] = POS + ",1";
		int arrays[] = new int[] { 0, 0, 0, 0, 0, 0, 0, 0 };
		return dbconnector.insert(KEYWORDS_TABLE, values, arrays, true, true);
	}

	/*UPDATE weather SET temp_lo = temp_lo+1, temp_hi = temp_lo+15, prcp = DEFAULT
	  WHERE city = 'San Francisco' AND date = '2003-07-03'
	  RETURNING temp_lo, temp_hi, prcp;*/
	public int updateKeyWord(int wordid, String appid, int[] rate1,
			int[] rate2, int[] rate3, int[] rate4, int[] rate5,
			Map<String, Integer> POSs) throws SQLException {
		String rate1Update = "rate1_byday=" + int1D2Text(rate1);
		String rate2Update = "rate2_byday=" + int1D2Text(rate2);
		String rate3Update = "rate3_byday=" + int1D2Text(rate3);
		String rate4Update = "rate4_byday=" + int1D2Text(rate4);
		String rate5Update = "rate5_byday=" + int1D2Text(rate5);
		String POSUpdate = "POS=" + map2Text(POSs);
		String updateFields = rate1Update + ", " + rate2Update + ", "
				+ rate3Update + ", " + rate4Update + ", " + rate5Update + ", "
				+ POSUpdate;
		return dbconnector.update(KEYWORDS_TABLE, updateFields, "ID=" + wordid
				+ " AND " + "appid='" + appid + "'");
	}

	public int updateKeyWord(Word word, String appid) throws SQLException {
		int[][] timeseries = word.getTimeSeriesByRating();
		int[] tem = new int[timeseries[0].length - 1];
		for (int k = 0; k < tem.length; k++)
			tem[k] = timeseries[0][k];
		String rate1Update = "rate1_byday='" + int1D2Text(tem) + "'";
		for (int k = 0; k < tem.length; k++)
			tem[k] = timeseries[1][k];
		String rate2Update = "rate2_byday='" + int1D2Text(tem) + "'";
		for (int k = 0; k < tem.length; k++)
			tem[k] = timeseries[2][k];
		String rate3Update = "rate3_byday='" + int1D2Text(tem) + "'";
		for (int k = 0; k < tem.length; k++)
			tem[k] = timeseries[3][k];
		String rate4Update = "rate4_byday='" + int1D2Text(tem) + "'";
		for (int k = 0; k < tem.length; k++)
			tem[k] = timeseries[4][k];
		String rate5Update = "rate5_byday='" + int1D2Text(tem) + "'";

		String POSUpdate = "POS='" + map2Text(word.getPOSSet()) + "'";
		String updateFields = rate1Update + ", " + rate2Update + ", "
				+ rate3Update + ", " + rate4Update + ", " + rate5Update + ", "
				+ POSUpdate;
		return dbconnector.update(KEYWORDS_TABLE, updateFields,
				"ID=" + word.getWordID() + " AND " + "appid='" + appid + "'");
	}

	public List<ReviewForAnalysis> queryReviews(Application app,
			boolean preprocessing) throws SQLException {
		List<ReviewForAnalysis> reviews = new ArrayList<>();
		String fields[] = { "title", "raw_text", "cleansed_text",
				"document_version", "reviewid", "device", "rating",
				"creation_time" };

		String condition = "appid='" + app.getAppID() + "'";
		if (preprocessing)
			condition += " AND creation_time >= " + app.getPreprocessedDate();
		condition += "ORDER BY creation_time ASC";

		ResultSet results;
		results = dbconnector.select(REVIEWS_TABLE, fields, condition);
		while (results.next()) {
			int rating = results.getInt("rating");
			if (rating == 0)
				continue;
			String reviewID = results.getString("reviewid");
			long creationTime = results.getLong("creation_time");
			String raw_text = results.getString("raw_text");
			int[][] cleansed_text = text2Int2D(results
					.getString("cleansed_text"));
			if (raw_text.indexOf('\t') < 0) // Not from Android Market
				raw_text = results.getString("title") + ". " + raw_text;

			ReviewForAnalysis.ReviewBuilder reviewBuilder = new ReviewForAnalysis.ReviewBuilder();
			reviewBuilder.rawText(raw_text);
			reviewBuilder.cleansedText(cleansed_text);
			reviewBuilder.reviewId(reviewID);
			reviewBuilder.deviceName(results.getString("device"));
			reviewBuilder
					.documentVersion(results.getString("document_version"));
			reviewBuilder.rating(rating);
			reviewBuilder.creationTime(creationTime);
			reviewBuilder.application(app);
			reviews.add(reviewBuilder.createReview());
		}
		return reviews;
	}

	public Word querySingleWord(int DBID, Application app) throws SQLException {
		String fields[] = { "ID", "appid", "keyword", "rate1_byday",
				"rate2_byday", "rate3_byday", "rate4_byday", "rate5_byday",
				"POS" };
		String condition = "ID=" + DBID + " AND appid='" + app.getAppID() + "'";
		ResultSet results;
		results = dbconnector.select(KEYWORDS_TABLE, fields, condition);
		Word word = null;
		while (results.next()) {
			int ID = results.getInt("ID");
			int[][] ratesByDays = new int[5][];
			ratesByDays[0] = text2Int1D(results.getString("rate1_byday"));
			ratesByDays[1] = text2Int1D(results.getString("rate2_byday"));
			ratesByDays[2] = text2Int1D(results.getString("rate3_byday"));
			ratesByDays[3] = text2Int1D(results.getString("rate4_byday"));
			ratesByDays[4] = text2Int1D(results.getString("rate5_byday"));
			Map<String, Integer> POSs = text2Map(results.getString("POS"));
			word = new Word(DBID, results.getString("keyword"), POSs, app,
					ratesByDays, ratesByDays[0].length);
		}
		return word;
	}

	public Word queryWordByKey(String key, Application app) throws SQLException {
		String fields[] = { "ID", "appid", "keyword", "rate1_byday",
				"rate2_byday", "rate3_byday", "rate4_byday", "rate5_byday",
				"POS" };
		String condition = "appid='" + app.getAppID() + "' AND keyword='" + key
				+ "'";
		ResultSet results;
		results = dbconnector.select(KEYWORDS_TABLE, fields, condition);
		Word word = null;
		while (results.next()) {
			int ID = results.getInt("ID");
			int[][] ratesByDays = new int[5][];
			ratesByDays[0] = text2Int1D(results.getString("rate1_byday"));
			ratesByDays[1] = text2Int1D(results.getString("rate2_byday"));
			ratesByDays[2] = text2Int1D(results.getString("rate3_byday"));
			ratesByDays[3] = text2Int1D(results.getString("rate4_byday"));
			ratesByDays[4] = text2Int1D(results.getString("rate5_byday"));
			Map<String, Integer> POSs = text2Map(results.getString("POS"));
			word = new Word(ID, key, POSs, app, ratesByDays,
					ratesByDays[0].length);
		}
		return word;
	}

	public List<Word> queryWordsForAnApp(Application app) throws SQLException {
		List<Word> wordList = new ArrayList<>();
		String fields[] = { "ID", "appid", "keyword", "rate1_byday",
				"rate2_byday", "rate3_byday", "rate4_byday", "rate5_byday",
				"POS" };
		String condition = "appid='" + app.getAppID() + "'";
		ResultSet results;
		results = dbconnector.select(KEYWORDS_TABLE, fields, condition);
		Word word = null;
		while (results.next()) {
			int ID = results.getInt("ID");
			int[][] ratesByDays = new int[5][];
			ratesByDays[0] = text2Int1D(results.getString("rate1_byday"));
			ratesByDays[1] = text2Int1D(results.getString("rate2_byday"));
			ratesByDays[2] = text2Int1D(results.getString("rate3_byday"));
			ratesByDays[3] = text2Int1D(results.getString("rate4_byday"));
			ratesByDays[4] = text2Int1D(results.getString("rate5_byday"));
			Map<String, Integer> POSs = text2Map(results.getString("POS"));
			word = new Word(ID, results.getString("keyword"), POSs, app,
					ratesByDays, app.getDayIndex());
			wordList.add(word);
		}
		return wordList;
	}

	public void updateCleansedText(ReviewForAnalysis rev) throws SQLException {
		// TODO Auto-generated method stub
		String cleansedText = int2D2Text(rev.getSentences());
		String condition = "reviewid = '" + rev.getReviewId() + "'";
		dbconnector.update(REVIEWS_TABLE, "cleansed_text ='" + cleansedText
				+ "'", condition);
	}

	// /////////////////////////////////////////

	private String int1D2Text(int[] int1D) {
		if (int1D == null || int1D.length == 0)
			return "null";
		StringBuilder strBuilder = new StringBuilder();
		strBuilder.append("{");
		String prefix = "";
		for (int i : int1D) {
			strBuilder.append(prefix + i);
			prefix = ",";
		}
		return strBuilder.toString();
	}

	private String int2D2Text(int[][] int2D) {
		if (int2D == null || int2D.length == 0)
			return "null";
		StringBuilder strBuilder = new StringBuilder();
		String prefix = "";
		for (int[] i : int2D) {
			strBuilder.append(prefix);
			prefix = "";
			for (int j : i) {
				strBuilder.append(prefix + j);
				prefix = ",";
			}
			prefix = ";";
		}
		return strBuilder.toString();
	}

	private int[] text2Int1D(String text) {
		int[] argInt = null;
		if (!text.equals("null")) {
			String[] ints = text.substring(1, text.length()).split(",");
			argInt = new int[ints.length];
			for (int j = 0; j < argInt.length; j++)
				argInt[j] = Integer.parseInt(ints[j]);
		}
		return argInt;
	}

	private int[][] text2Int2D(String text) {
		int[][] argIntArray = null;
		if (!text.equals("null")) {
			// "1,2,3;1,2,3;;1,2,3"
			String[] intArray = text.split(";");
			argIntArray = new int[intArray.length][];
			for (int j = 0; j < argIntArray.length; j++) {
				if (intArray[j].equals("") || intArray[j].length() == 0) {
					argIntArray[j] = new int[0];
					continue;
				}
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

	private String map2Text(Map<String, Integer> daMap) {
		// "pos,i;pos,i;pos,i"
		if (daMap == null || daMap.isEmpty())
			return "null";

		StringBuilder strBuilder = new StringBuilder();
		String prefix = "";
		for (Entry<String, Integer> entry : daMap.entrySet()) {
			strBuilder.append(prefix + entry.getKey() + "," + entry.getValue());
			prefix = ";";
		}

		return strBuilder.toString();
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

	private String long1D2Text(int[] long1D) {
		if (long1D == null || long1D.length == 0)
			return "null";
		StringBuilder strBuilder = new StringBuilder();
		String prefix = "";
		for (long i : long1D) {
			strBuilder.append(prefix + i);
			prefix = ",";
		}
		return strBuilder.toString();
	}
}
