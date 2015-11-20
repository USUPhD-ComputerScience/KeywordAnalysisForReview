package Crawler;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import managers.ReviewDB;
import model.ReviewForCrawler;

import com.akdeniz.googleplaycrawler.GooglePlayAPI.REVIEW_SORT;

public class Crawler {
	private GooglePlayCrawler GPcrawler = null;
	public static final String LOGIN = "usualt1@gmail.com";
	public static final String PASSWORD = "phdcs2014";
	// private static final String LOGIN = "rio.app.test1@gmail.com";
	// private static final String PASSWORD = "abc13579";

	public static final String ANDROID = "3FA8A9EFF6CA06E0";
	// private static final String ANDROID = "32F52476388F20DE";
	// private static final String ANDROID = "dead000beef";
	public static final int GOOGLE_PLAY = 1;
	public static final int DELAYSECONDS = 3;

	public Crawler() {
		// run Google Play Crawler
		GPcrawler = new GooglePlayCrawler(LOGIN, PASSWORD, ANDROID);
	}

	public Crawler(String login, String password, String androidid) {
		// run Google Play Crawler
		GPcrawler = new GooglePlayCrawler(login, password, androidid);
	}

	public boolean extractReviews(String AppID) throws SQLException {
		List<ReviewForCrawler> reviewList = new ArrayList<>();
		System.out.print("     Start extracting reviews for " + AppID);
		ReviewDB reviewDB = ReviewDB.getInstance();
		if (!reviewDB.isAppIDexist(AppID)) {
			// add the appID
			String appName = GPcrawler.getName(AppID);
			if (appName.equals("unknown")) {
				System.out.println(" ---> This app is not on GooglePlay");
				return false;
			}
			reviewDB.addNewApp(AppID, appName);
		}
		REVIEW_SORT sort = REVIEW_SORT.HELPFUL;
		sort = REVIEW_SORT.NEWEST;
		// GPcrawler = new GooglePlayCrawler(LOGIN, PASSWORD, ANDROID);
		boolean success = GPcrawler.getReviewsForApp(AppID, reviewList, sort);
		int[] count = writeReviewsIntoDB(reviewList, AppID, GOOGLE_PLAY);
		System.out.println(" ---> extracted " + count[0]
				+ " new reviews (failed " + count[1] + " reviews)");
		return success;
	}

	private int[] writeReviewsIntoDB(List<ReviewForCrawler> reviewList,
			String appid, int option) throws SQLException {
		ReviewDB reviewDB = ReviewDB.getInstance();
		int count = 0, failedCount = 0;
		for (ReviewForCrawler rev : reviewList) {
			if (reviewDB.insertReview(rev, appid))
				count++;
			else
				failedCount++;
		}
		reviewDB.updateReviewNumberForApp(count, appid);
		return new int[] { count, failedCount };

	}
}
