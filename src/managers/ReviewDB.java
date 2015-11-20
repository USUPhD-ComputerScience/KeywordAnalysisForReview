package managers;

import java.io.IOException;
import java.io.PushbackReader;
import java.io.StringReader;
import java.sql.Array;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import org.postgresql.PGConnection;
import org.postgresql.copy.CopyManager;

import model.Application;
import model.ReviewForAnalysis;
import model.ReviewForCrawler;
import model.Word;
import util.PostgreSQLConnector;

public class ReviewDB {
	public static final String DBLOGIN = "useal";
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

	public static final String DAYS_TABLE = "days";
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

	public ApplicationManager queryMultipleAppsInfo(int minReviews)
			throws SQLException {
		ApplicationManager appMan = ApplicationManager.getInstance();
		String fields[] = { "appid", "name", "count", "release_dates",
				"start_date", "day_index" };
		String condition = "count>=" + minReviews;

		ResultSet results;
		results = dbconnector.select(APPS_TABLE, fields, condition);

		while (results.next()) {
			String name = results.getString("name");
			String appid = results.getString("appid");
			int count = results.getInt("count");
			Long[] releaseDates = text2long1D(
					results.getString("release_dates"));

			if (appid != null) {
				appMan.addApp(new Application(appid, name, count, releaseDates,
						results.getLong("start_date"),
						results.getInt("day_index")));
				
			}
		}
		return appMan;
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
			Long[] release_dates = text2long1D(
					results.getString("release_dates"));

			if (appid != null) {
				return new Application(appid, name, count, release_dates,
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
			id = dbconnector.insert(REVIEWS_TABLE, values, arrays, false,
					false);
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
		String values[] = new String[3];
		values[0] = appid; // appid
		values[1] = w;

		values[2] = POS + ",1";
		int arrays[] = new int[] { 0, 0, 0 };
		return dbconnector.insert(KEYWORDS_TABLE, values, arrays, true, true);
	}
	
	


	/*UPDATE weather SET temp_lo = temp_lo+1, temp_hi = temp_lo+15, prcp = DEFAULT
	  WHERE city = 'San Francisco' AND date = '2003-07-03'
	  RETURNING temp_lo, temp_hi, prcp;*/
	public int updateKeyWord(int wordid, String appid, int[] rate1, int[] rate2,
			int[] rate3, int[] rate4, int[] rate5, Map<String, Integer> POSs)
					throws SQLException {
		String rate1Update = "rate1_byday=" + int1D2Text(rate1);
		String rate2Update = "rate2_byday=" + int1D2Text(rate2);
		String rate3Update = "rate3_byday=" + int1D2Text(rate3);
		String rate4Update = "rate4_byday=" + int1D2Text(rate4);
		String rate5Update = "rate5_byday=" + int1D2Text(rate5);
		String POSUpdate = "POS=" + map2Text(POSs);
		String updateFields = rate1Update + ", " + rate2Update + ", "
				+ rate3Update + ", " + rate4Update + ", " + rate5Update + ", "
				+ POSUpdate;
		return dbconnector.update(KEYWORDS_TABLE, updateFields,
				"ID=" + wordid + " AND " + "appid='" + appid + "'");
	}

	// public int updateKeyWord(Word word, String appid) throws SQLException {
	// int[][] timeseries = word.getTimeSeriesByRating();
	// int[] tem = new int[timeseries[0].length - 1];
	// for (int k = 0; k < tem.length; k++)
	// tem[k] = timeseries[0][k];
	// String rate1Update = "rate1_byday='" + int1D2Text(tem) + "'";
	// for (int k = 0; k < tem.length; k++)
	// tem[k] = timeseries[1][k];
	// String rate2Update = "rate2_byday='" + int1D2Text(tem) + "'";
	// for (int k = 0; k < tem.length; k++)
	// tem[k] = timeseries[2][k];
	// String rate3Update = "rate3_byday='" + int1D2Text(tem) + "'";
	// for (int k = 0; k < tem.length; k++)
	// tem[k] = timeseries[3][k];
	// String rate4Update = "rate4_byday='" + int1D2Text(tem) + "'";
	// for (int k = 0; k < tem.length; k++)
	// tem[k] = timeseries[4][k];
	// String rate5Update = "rate5_byday='" + int1D2Text(tem) + "'";
	//
	// String POSUpdate = "POS='" + map2Text(word.getPOSSet()) + "'";
	// String updateFields = rate1Update + ", " + rate2Update + ", "
	// + rate3Update + ", " + rate4Update + ", " + rate5Update + ", "
	// + POSUpdate;
	//
	// return dbconnector.update(KEYWORDS_TABLE, updateFields,
	// "ID=" + word.getWordID() + " AND " + "appid='" + appid + "'");
	//
	// }

	public int queryReviewsforAProduct(Application app,
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
			int[][] cleansed_text = text2Int2D(
					results.getString("cleansed_text"));
			if (!preprocessing)
				if (cleansed_text == null)
					continue;
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
			ReviewForAnalysis rev = reviewBuilder.createReview();
			if (rev.getCreationTime() < 1420095600000l)
				continue;
			reviews.add(rev);
			app.addReview(rev, true);
		}
		return reviews.size();
	}

	public int addKeyWord(String w, String POS, String product_id,
			boolean identifier) throws SQLException {
		String values[] = new String[4];
		values[0] = product_id; // product_id
		values[1] = w;

		values[2] = POS + ",1";
		if (identifier)
			values[3] = "1";
		else
			values[3] = "0";
		int arrays[] = new int[] { 0, 0, 0, 1 };
		return dbconnector.insert(KEYWORDS_TABLE, values, arrays, true, true);
	}

	public int[] getCountByRating(int mdbID) throws SQLException {
		// TODO Auto-generated method stub
		String query = "SUM(count) as count,type from"
				+ " days WHERE keyword_id=" + mdbID
				+ " AND type < 5 group by type";
		ResultSet results = dbconnector.select(query);
		int count[] = new int[5];
		while (results.next()) {
			count[results.getInt("type")] = results.getInt("count");
		}
		return count;
	}

	// Only used in the final stage of keyword extraction, after everything has
	// been done
	public int updateKeyWordByDays(Word word) throws Throwable {
		// COPY Inserts
		// store data - begin
		Connection conn = dbconnector.getConnection();
		conn.setAutoCommit(false);
		CopyManager cpManager = ((PGConnection) conn).getCopyAPI();
		PushbackReader reader = new PushbackReader(new StringReader(""), 10000);
		int countInsert = 0;
		int keyword_id = word.getWordID();
		StringBuilder sb = new StringBuilder();

		countInsert = copyCountByDayToDB(cpManager, reader, countInsert,
				keyword_id, sb, 0, word.getCountByDay_R1());
		countInsert = copyCountByDayToDB(cpManager, reader, countInsert,
				keyword_id, sb, 1, word.getCountByDay_R2());
		countInsert = copyCountByDayToDB(cpManager, reader, countInsert,
				keyword_id, sb, 2, word.getCountByDay_R3());
		countInsert = copyCountByDayToDB(cpManager, reader, countInsert,
				keyword_id, sb, 3, word.getCountByDay_R4());
		countInsert = copyCountByDayToDB(cpManager, reader, countInsert,
				keyword_id, sb, 4, word.getCountByDay_R5());

		reader.unread(sb.toString().toCharArray());
		cpManager.copyIn(
				"COPY days(date,keyword_id,type,count) FROM STDIN WITH CSV",
				reader);
		conn.commit();
		conn.setAutoCommit(true);
		return countInsert;
	}

	private int copyCountByDayToDB(CopyManager cpManager, PushbackReader reader,
			int countInsert, int keyword_id, StringBuilder sb, int type,
			Map<Long, Integer> countByDay) throws IOException, SQLException {
		for (Entry<Long, Integer> dayN : countByDay.entrySet()) {
			long date = dayN.getKey();
			int count = dayN.getValue();
			sb.append(date).append(",").append(keyword_id).append(",")
					.append(type).append(",").append(count).append("\n");
			countInsert++;
			if (countInsert % 200 == 0) {
				reader.unread(sb.toString().toCharArray());
				cpManager.copyIn(
						"COPY days(date,keyword_id,type,count) FROM STDIN WITH CSV",
						reader);
				sb.delete(0, sb.length());
			}
		}
		return countInsert;
	}

	// public Word querySingleWord(int DBID, Application app) throws
	// SQLException {
	// String fields[] = { "ID", "appid", "keyword", "rate1_byday",
	// "rate2_byday", "rate3_byday", "rate4_byday", "rate5_byday",
	// "POS" };
	// String condition = "ID=" + DBID + " AND appid='" + app.getAppID() + "'";
	// ResultSet results;
	// results = dbconnector.select(KEYWORDS_TABLE, fields, condition);
	// Word word = null;
	// while (results.next()) {
	// int ID = results.getInt("ID");
	// int[][] ratesByDays = new int[5][];
	// ratesByDays[0] = text2Int1D(results.getString("rate1_byday"));
	// ratesByDays[1] = text2Int1D(results.getString("rate2_byday"));
	// ratesByDays[2] = text2Int1D(results.getString("rate3_byday"));
	// ratesByDays[3] = text2Int1D(results.getString("rate4_byday"));
	// ratesByDays[4] = text2Int1D(results.getString("rate5_byday"));
	// Map<String, Integer> POSs = text2Map(results.getString("POS"));
	// word = new Word(DBID, results.getString("keyword"), POSs, app,
	// ratesByDays, ratesByDays[0].length);
	// }
	// return word;
	// }
	public Word querySingleWord(int DBID, Application app) throws SQLException {
		String fields[] = { "ID", "appid", "keyword", "POS" };
		String condition = "ID=" + DBID + " AND appid='" + app.getAppID()
				+ "'";
		ResultSet results;
		results = dbconnector.select(KEYWORDS_TABLE, fields, condition);
		Word word = null;
		while (results.next()) {
			int ID = results.getInt("ID");
			Map<String, Integer> POSs = text2Map(results.getString("POS"));
			word = new Word(DBID, results.getString("keyword"), POSs, app);
		}
		return word;
	}

	public List<Word> queryWordsForAProduct(Application app)
			throws SQLException {
		List<Word> wordList = new ArrayList<>();
		String fields[] = { "ID", "appid", "keyword", "POS" };
		String condition = "appid='" + app.getAppID() + "'";
		ResultSet results;
		results = dbconnector.select(KEYWORDS_TABLE, fields, condition);
		Word word = null;
		while (results.next()) {
			int ID = results.getInt("ID");
			Map<String, Integer> POSs = text2Map(results.getString("POS"));
			word = new Word(ID, results.getString("keyword"), POSs, app);
			wordList.add(word);
		}
		return wordList;
	}

//	public Word queryWordByKey(String key, Application app)
//			throws SQLException {
//		String fields[] = { "ID", "appid", "keyword", "rate1_byday",
//				"rate2_byday", "rate3_byday", "rate4_byday", "rate5_byday",
//				"POS" };
//		String condition = "appid='" + app.getAppID() + "' AND keyword='" + key
//				+ "'";
//		ResultSet results;
//		results = dbconnector.select(KEYWORDS_TABLE, fields, condition);
//		Word word = null;
//		while (results.next()) {
//			int ID = results.getInt("ID");
//			int[][] ratesByDays = new int[5][];
//			ratesByDays[0] = text2Int1D(results.getString("rate1_byday"));
//			ratesByDays[1] = text2Int1D(results.getString("rate2_byday"));
//			ratesByDays[2] = text2Int1D(results.getString("rate3_byday"));
//			ratesByDays[3] = text2Int1D(results.getString("rate4_byday"));
//			ratesByDays[4] = text2Int1D(results.getString("rate5_byday"));
//			Map<String, Integer> POSs = text2Map(results.getString("POS"));
//			word = new Word(ID, key, POSs, app, ratesByDays,
//					ratesByDays[0].length);
//		}
//		return word;
//	}

	public List<Word> queryWordsForAnApp(Application app) throws SQLException {
//		List<Word> wordList = new ArrayList<>();
//		String fields[] = { "ID", "product_id","keyword",
//				"POS" };
//		String condition = "product_id='" + app.getAppID() + "'";
//		ResultSet results;
//		results = dbconnector.select(KEYWORDS_TABLE, fields, condition);
//		Word word = null;
//		while (results.next()) {
//			int ID = results.getInt("ID");
//			int[][] ratesByDays = new int[5][];
//			ratesByDays[0] = text2Int1D(results.getString("rate1_byday"));
//			ratesByDays[1] = text2Int1D(results.getString("rate2_byday"));
//			ratesByDays[2] = text2Int1D(results.getString("rate3_byday"));
//			ratesByDays[3] = text2Int1D(results.getString("rate4_byday"));
//			ratesByDays[4] = text2Int1D(results.getString("rate5_byday"));
//			Map<String, Integer> POSs = text2Map(results.getString("POS"));
//			word = new Word(ID, results.getString("keyword"), POSs, app,
//					ratesByDays, app.getDayIndex());
//			wordList.add(word);
//		}
//		return wordList;
//		
		
		List<Word> wordList = new ArrayList<>();
		String fields[] = { "ID", "product_id", "keyword", "POS",
				"identifier" };
		String condition = "product_id='" + app.getAppID() + "'";
		ResultSet results;
		results = dbconnector.select(KEYWORDS_TABLE, fields, condition);
		Word word = null;
		while (results.next()) {
			int ID = results.getInt("ID");
			Map<String, Integer> POSs = text2Map(results.getString("POS"));
			word = new Word(ID, results.getString("keyword"), POSs, app);
			wordList.add(word);
		}
		return wordList;
	}

	public void updateCleansedText(ReviewForAnalysis rev) throws SQLException {
		// TODO Auto-generated method stub
		String cleansedText = int2D2Text(rev.getSentences());
		String condition = "reviewid = '" + rev.getReviewId() + "'";
		dbconnector.update(REVIEWS_TABLE,
				"cleansed_text ='" + cleansedText + "'", condition);
	}

	public void updateStartTime(Application app) throws SQLException {
		// TODO Auto-generated method stub
		String condition = "appid = '" + app.getAppID() + "'";
		dbconnector.update(REVIEWS_TABLE,
				"start_date ='" + app.getStartDate() + "'", condition);
	}
	// return the total bug retrieved
	// public int queryMultipleProductInfo(int minBugs, boolean cleansed)
	// throws SQLException, ParseException {
	// // TODO Auto-generated method stub
	// ResultSet results;
	// StringBuilder idListstr = new StringBuilder();
	// if (!cleansed) {
	// ArrayList<Integer> idList = new ArrayList<>();
	// String query = "appid, count(*) as count from apps group by product_id";
	// results = dbconnectorBugEclipse.select(query);
	// while (results.next()) {
	// int product_id = results.getInt("product_id");
	// int bugcount = results.getInt("bugcount");
	// if (bugcount > minBugs)
	// idList.add(product_id);
	// }
	// for (int i = 0; i < idList.size(); i++) {
	// idListstr.append(idList.get(i));
	// if (i < (idList.size() - 1))
	// idListstr.append(",");
	// }
	// } else {
	// ArrayList<String> idList = new ArrayList<>();
	// String query = "product_id, bug_count from products";
	// results = dbconnectorBugExt.select(query);
	// while (results.next()) {
	// String product_id = results.getString("product_id");
	// int bugcount = results.getInt("bug_count");
	// if (bugcount > minBugs)
	// idList.add(product_id);
	// }
	// for (int i = 0; i < idList.size(); i++) {
	// idListstr.append("'" + idList.get(i) + "'");
	// if (i < (idList.size() - 1))
	// idListstr.append(",");
	// }
	// }
	// return getBugsByIDs(idListstr.toString(), cleansed);
	// }

//	// return the total bug retrieved
//	public void queryMultipleProductInfo(List<String> idList)
//			throws SQLException, ParseException {
//		// TODO Auto-generated method stub
//		StringBuilder idListstr = new StringBuilder();
//		int productCount = 0;
//		ProductManager productMan = ProductManager.getInstance();
//		for (int i = 0; i < idList.size(); i++) {
//			idListstr.append("'" + idList.get(i) + "'");
//			if (i < (idList.size() - 1))
//				idListstr.append(",");
//		}
//		ResultSet results;
//		String query = "product_id,name,version,description,start_date,"
//				+ "classification_id,day_index  from products"
//				+ " where product_id IN (" + idListstr.toString() + ")";
//		results = dbconnectorBugExt.select(query);
//		while (results.next()) {
//			String product_id = results.getString("product_id");
//			String product_name = results.getString("name");
//			String product_version = results.getString("version");
//			String product_description = results.getString("description");
//			int product_classification_id = results.getInt("classification_id");
//			int dayIndex = results.getInt("day_index");
//			long startDate = results.getLong("start_date");
//			Product thisProduct = productMan.getProductByID(product_id);
//
//			if (thisProduct == null) {
//				productCount++;
//				thisProduct = productMan.addProduct(product_id, product_version,
//						product_name, product_description,
//						product_classification_id, startDate, dayIndex);
//				System.out.println(
//						productCount + ". Adding new product: " + product_id);
//			}
//		}
//	}

//	public int getBugsByIDs(String idListstr, boolean cleansed)
//			throws SQLException, ParseException {
//		ProductManager productMan = ProductManager.getInstance();
//		int bugCount = 0;
//		int productCount = 0;
//		if (!cleansed) {
//			SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.S");
//			String query = "bugs.creation_ts," + "bugs.version,bugs.product_id,"
//					+ "products.name,products.description,products.classification_id"
//					+ " from bugs,products where "
//					+ "bugs.product_id = products.id "
//					+ "AND bugs.product_id IN (" + idListstr + ")";
//			// System.out.println(query);
//			ResultSet results = dbconnectorBugEclipse.select(query);
//			while (results.next()) {
//				long creation_ts = f
//						.parse(results.getString("bugs.creation_ts")).getTime();
//				String version = results.getString("bugs.version");
//				int product_id = results.getInt("bugs.product_id");
//				String product_name = results.getString("products.name");
//				String product_description = results
//						.getString("products.description");
//				int product_classification_id = results
//						.getInt("products.classification_id");
//
//				Product thisProduct = productMan.getProductByID(
//						ProductManager.generateID(product_name, version));
//
//				if (thisProduct == null) {
//					productCount++;
//					thisProduct = productMan.addProduct(version, product_name,
//							product_description, product_classification_id,
//							creation_ts, -1);
//					thisProduct.setDBID(product_id);
//					System.out.println(productCount + ". Adding new product: "
//							+ product_name + "-"
//							+ util.Util.extractVersion(version));
//				} else {
//					// this version is already added
//				}
//
//				// thisProduct.addBug(bug_id, bug_severity, priority,
//				// creation_ts,
//				// short_desc, fullText, null, null);
//				// bugCount++;
//			}
//		} else {
//			String query = "bugs.bug_id,bugs.severity,bugs.creation_time,"
//					+ "bugs.short_desc,bugs.mPriority,bugs.product_id,"
//					+ "products.name,products.description,products.classification_id,"
//					+ "bugs.comments,bugs.nonidentifier_words,bugs.identifier_words,"
//					+ "products.day_index from bugs,products where "
//					+ "bugs.product_id = products.product_id "
//					+ "AND bugs.product_id IN (" + idListstr + ")";
//
//			// System.out.println(query);
//			ResultSet results = dbconnectorBugExt.select(query);
//			while (results.next()) {
//				int bug_id = results.getInt("bug_id");
//				String bug_severity = results.getString("severity");
//				long creation_ts = results.getLong("creation_time");
//				String short_desc = results.getString("short_desc");
//				String priority = results.getString("mPriority");
//				String product_id = results.getString("product_id");
//				String product_name = results.getString("name");
//				String product_description = results.getString("description");
//				int product_classification_id = results
//						.getInt("classification_id");
//				String fullText = results.getString("comments");
//
//				List<Integer> nonidentifier_words = text2Int1DList(
//						results.getString("nonidentifier_words"));
//				List<Integer> identifier_words = text2Int1DList(
//						results.getString("identifier_words"));
//				int day_index = results.getInt("day_index");
//				Product thisProduct = productMan.getProductByID(product_id);
//
//				if (thisProduct == null) {
//					productCount++;
//					thisProduct = productMan.addProduct(product_id,
//							product_name, product_description,
//							product_classification_id, creation_ts, day_index);
//					System.out.println(productCount + ". Adding new product: "
//							+ product_id);
//				}
//
//				thisProduct.addBug(bug_id, bug_severity, priority, creation_ts,
//						short_desc, fullText, identifier_words,
//						nonidentifier_words);
//				bugCount++;
//			}
//		}
//		return bugCount;
//	}

//	public int queryReviewsforAProduct(Application app)
//			throws SQLException, ParseException {
//		// TODO Auto-generated method stub
//		int bugCount = 0;
//		SimpleDateFormat f = new SimpleDateFormat("yyyy-MM-dd hh:mm:ss.S");
//		String query = "reviews.reviewid,reviews.rating,reviews.creation_time,"
//				+ "reviews.title,reviews.appid,"
//				+ "bugs_fulltext.comments from bugs,bugs_fulltext where "
//				+ "bugs.bug_id = bugs_fulltext.bug_id AND bugs.product_id = "
//				+ product.getDBID();
//		// System.out.println(query);
//		ResultSet results = dbconnectorBugEclipse.select(query);
//		Set<String> severity_type = new HashSet<>();
//		Set<String> priority_type = new HashSet<>();
//		while (results.next()) {
//			int bug_id = results.getInt("bugs.bug_id");
//			String bug_severity = results.getString("bugs.bug_severity");
//			severity_type.add(bug_severity);
//			long creation_ts = f.parse(results.getString("bugs.creation_ts"))
//					.getTime();
//			String short_desc = results.getString("bugs.short_desc");
//			String priority = results.getString("bugs.priority");
//			priority_type.add(priority);
//			String fullText = results.getString("bugs_fulltext.comments");
//
//			String version = results.getString("bugs.version");
//
//			if (ProductManager.generateID(product.getName(), version)
//					.equals(product.getID())) {
//				product.addBug(bug_id, bug_severity, priority, creation_ts,
//						short_desc, fullText, null, null);
//				bugCount++;
//			}
//		}
//		System.out.println("Severity type:");
//		System.out.println(severity_type);
//		System.out.println("Priority type:");
//		System.out.println(priority_type);
//		return bugCount;
//	}

	// /////////////////////////////////////////

	private String int1D2Text(int[] int1D) {
		if (int1D == null || int1D.length == 0)
			return "null";
		StringBuilder strBuilder = new StringBuilder();
		String prefix = "";
		for (int i : int1D) {
			strBuilder.append(prefix + i);
			prefix = ",";
		}
		return strBuilder.toString();
	}

	private String int1D2Text(List<Integer> int1D) {
		if (int1D == null || int1D.size() == 0)
			return "null";
		StringBuilder strBuilder = new StringBuilder();
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
		if (text != null) {
			if (!text.equals("null")) {
				String[] ints = text.split(",");
				argInt = new int[ints.length];
				for (int j = 0; j < argInt.length; j++)
					argInt[j] = Integer.parseInt(ints[j]);
			}
		}
		return argInt;
	}

	private List<Integer> text2Int1DList(String text) {
		List<Integer> argInt = new ArrayList<>();
		if (text != null) {
			if (!text.equals("null")) {
				String[] ints = text.split(",");
				for (int j = 0; j < ints.length; j++)
					argInt.add(Integer.parseInt(ints[j]));
			}
		}
		return argInt;
	}

	private int[][] text2Int2D(String text) {
		int[][] argIntArray = null;

		if (text != null) {
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
		}
		return argIntArray;
	}

	private Map<String, Integer> text2Map(String text) {
		Map<String, Integer> daMap = null;
		if (text != null) {
			if (!text.equals("null")) {
				// "pos,i;pos,i;pos,i"
				daMap = new HashMap<>();
				String[] entries = text.split(";");

				for (int j = 0; j < entries.length; j++) {
					String[] arr = entries[j].split(",");
					daMap.put(arr[0], Integer.parseInt(arr[1]));
				}
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
		if (text != null) {
			if (!text.equals("null")) {
				String[] bigints = text.split(",");
				argLong = new Long[bigints.length];
				for (int j = 0; j < argLong.length; j++)
					argLong[j] = Long.parseLong(bigints[j]);
			}
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
