package main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map.Entry;

import au.com.bytecode.opencsv.CSVReader;
import managers.ReviewDB;
import model.Application;
import model.ReviewForCrawler;
import util.PostgreSQLConnector;
import Crawler.GooglePlayCrawler;

public class MovingDBmain {
	public static final String LOGIN = "usualt1@gmail.com";
	public static final String PASSWORD = "phdcs2014";
	// private static final String LOGIN = "rio.app.test1@gmail.com";
	// private static final String PASSWORD = "abc13579";

	public static final String ANDROID = "3FA8A9EFF6CA06E0";
	// private static final String ANDROID = "32F52476388F20DE";
	// private static final String ANDROID = "dead000beef";
	public static final int GOOGLE_PLAY = 1;

	public static final String DBLOGIN = "postgres";
	public static final String DBPASSWORD = "phdcs2014";
	public static final String REVIEWDB = "reviewdb";
	public static final String APPID_TABLE = "appid"; // name, ID, gplay,
														// amarket
	public static final String REVIEWS_TABLE = "reviews"; // text, title, appid,
	private static GooglePlayCrawler GPcrawler = new GooglePlayCrawler(LOGIN,
			PASSWORD, ANDROID);

	public static void main(String[] args) throws Throwable {
		// movingDB();
		updateDBfromOldDB();
		// ChoosingAppsToCrawl();
	}

	private static void ChoosingAppsToCrawl() throws Throwable {
		List<Application> appList = ReviewDB.getInstance()
				.queryMultipleAppsInfo(0);
		List<String> specialList = readAppInforFromFile("AoI.txt");
		int fileCount = 0;
		int appCount = 0;
		PrintWriter pw = new PrintWriter(new File("AoI" + fileCount + ".txt"));
		for (Application app : appList) {
			String appID = app.getAppID();
			int numDL = GPcrawler.getNumberOfDownload(appID);
			if (numDL > 10000000) {
				if (appID.equals("com.pandora.android"))
					System.out.println();
				if (!specialList.contains(appID)) {
					appCount++;
					pw.print(appID + "\n");
					System.out
							.println(appID
									+ " has passed the test with a dl count of"
									+ numDL);
					if (appCount > 30) {
						appCount = 0;
						fileCount++;
						pw.close();
						pw = new PrintWriter(new File("AoI" + fileCount
								+ ".txt"));
					}
				}
			}
		}
		pw.close();
	}

	private static void updateDBfromOldDB() throws Throwable {
		System.out
				.println(">> Transfering reviews and apps data from old DB to new DB");
		PostgreSQLConnector db = new PostgreSQLConnector(DBLOGIN, DBPASSWORD,
				REVIEWDB);
		PostgreSQLConnector db1 = new PostgreSQLConnector(DBLOGIN, DBPASSWORD,
				REVIEWDB);
		SimpleDateFormat f = new SimpleDateFormat("MMM dd,yyyy");
		Date date;
		date = (Date) f.parse("Jan 01,2015");
		long startDate = date.getTime();

		String fields[] = { "name", "ID" };
		String condition = "count>500";
		ResultSet results;
		results = db.select(APPID_TABLE, fields, condition);
		int count = 0, revCount = 0;
		ReviewDB reviewDB = ReviewDB.getInstance();
		while (results.next()) {
			String appID = results.getString("name");
			int dbID = results.getInt("ID");
			try {
				count++;
				// add the reviews
				String revfields[] = { "title", "text", "rating",
						"creationtime", "documentversion", "reviewid", "device" };
				condition = "appid=" + dbID + " AND creationtime>=" + startDate;
				ResultSet revResults;
				revResults = db1.select(REVIEWS_TABLE, revfields, condition);
				revCount = 0;
				while (revResults.next()) {
					ReviewForCrawler.ReviewBuilder reviewBuilder = new ReviewForCrawler.ReviewBuilder();
					reviewBuilder.title(revResults.getString("title"));
					reviewBuilder.text(revResults.getString("text"));
					reviewBuilder.reviewId(revResults.getString("reviewid"));
					reviewBuilder.deviceName(revResults.getString("device"));
					reviewBuilder.documentVersion(revResults
							.getString("documentversion"));
					reviewBuilder.rating(revResults.getInt("rating"));
					reviewBuilder.creationTime(revResults
							.getLong("creationtime"));
					ReviewForCrawler rev = reviewBuilder.createReview();
					try {
						if (reviewDB.insertReview(rev, appID))
							revCount++;
					} catch (SQLException e) {
					}
				}
				revResults.close();
				reviewDB.updateReviewNumberForApp(revCount, appID);
				System.out.println("Imported " + appID + " (" + revCount
						+ " reviews)");
			} catch (SQLException e) {
				System.out.println(e.getMessage());
			}
		}
		System.out.println("Transfered " + count + " into <apps>");
	}

	private static List<String> readAppInforFromFile(String fileName) {
		System.out.println(">>>> Reading appid from - " + fileName);
		CSVReader reader = null;
		List<String> appIDList = new ArrayList<>();
		try {
			reader = new CSVReader(new FileReader(fileName));
			String[] row = null;
			while ((row = reader.readNext()) != null) {
				if (row[0].length() > 5)
					appIDList.add(row[0]);
			}
			System.out.println("    List of apps: ");
			for (String appid : appIDList)
				System.out.println("     " + appid);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (reader != null)
				try {
					reader.close();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
		return appIDList;
	}

	private static void movingDB() throws ParseException, SQLException {
		System.out
				.println(">> Transfering reviews and apps data from old DB to new DB");
		PostgreSQLConnector db = new PostgreSQLConnector(DBLOGIN, DBPASSWORD,
				REVIEWDB);
		PostgreSQLConnector db1 = new PostgreSQLConnector(DBLOGIN, DBPASSWORD,
				REVIEWDB);
		SimpleDateFormat f = new SimpleDateFormat("MMM dd,yyyy");
		Date date;
		date = (Date) f.parse("Jan 01,2015");
		long startDate = date.getTime();

		String fields[] = { "name", "ID" };
		String condition = null;
		ResultSet results;
		results = db.select(APPID_TABLE, fields, condition);
		int count = 0, revCount = 0;
		ReviewDB reviewDB = ReviewDB.getInstance();
		while (results.next()) {
			String appID = results.getString("name");
			int dbID = results.getInt("ID");
			if (appID != null) {
				if (reviewDB.isAppIDexist(appID))
					continue;
				// add the appID
				String appName = GPcrawler.getName(appID);
				if (!appName.equals("unknown")) {
					try {
						reviewDB.addNewApp(appID, appName);
						count++;
						// add the reviews
						String revfields[] = { "title", "text", "rating",
								"creationtime", "documentversion", "reviewid",
								"device" };
						condition = "appid=" + dbID + " AND creationtime>="
								+ startDate;
						ResultSet revResults;
						revResults = db1.select(REVIEWS_TABLE, revfields,
								condition);
						revCount = 0;
						while (revResults.next()) {
							ReviewForCrawler.ReviewBuilder reviewBuilder = new ReviewForCrawler.ReviewBuilder();
							reviewBuilder.title(revResults.getString("title"));
							reviewBuilder.text(revResults.getString("text"));
							reviewBuilder.reviewId(revResults
									.getString("reviewid"));
							reviewBuilder.deviceName(revResults
									.getString("device"));
							reviewBuilder.documentVersion(revResults
									.getString("documentversion"));
							reviewBuilder.rating(revResults.getInt("rating"));
							reviewBuilder.creationTime(revResults
									.getLong("creationtime"));
							ReviewForCrawler rev = reviewBuilder.createReview();
							try {
								reviewDB.insertReview(rev, appID);
								revCount++;
							} catch (SQLException e) {
							}
						}
						revResults.close();
						reviewDB.updateReviewNumberForApp(revCount, appID);
						System.out.println("Imported " + appID + " ("
								+ revCount + " reviews)");
					} catch (SQLException e) {
						System.out.println(e.getMessage());
					}
				} else {
					System.out.println(appID + " is not on GooglePlay");
				}
			}
		}
		System.out.println("Transfered " + count + " into <apps>");
	}
}
