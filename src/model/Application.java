package model;

import java.io.PrintWriter;
import java.io.Serializable;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import managers.Vocabulary;
import util.PostgreSQLConnector;

public class Application implements Serializable {
	/**
	 * 
	 */

	public static final long MONTHMILIS = 2592000000L;
	public static final long HOURMILIS = 3600000L;
	public static final long DAYMILIS = 86400000L;
	private static final long serialVersionUID = -2497544910820359076L;
	//private Map<String, ReviewForAnalysis> reviewMap;

	private Set<ReviewForAnalysis> mReviews = new HashSet<>();
	private Set<Long> releaseDates;
	private String appID;
	private String name;
	private int dbCount;
	private int WorkingCount;
	private long startDate;
	private int dayIndex = -1;
	public Set<ReviewForAnalysis> getReviews() {
		return mReviews;
	}
	public Set<ReviewForAnalysis> getReviewList() {
		return mReviews;
	}

	public long getPreprocessedDate() {
		return (dayIndex + 1) * DAYMILIS + startDate;
	}

	public long getStartDate() {
		return startDate;
	}

	public int getDayIndex() {
		return dayIndex;
	}

	public int syncDayIndex(long creationTime) {
		int daysSinceStart = (int) ((creationTime - startDate) / DAYMILIS);
		// Date d = new Date(creationTime);
		int neededSlot = daysSinceStart - dayIndex;
		if (neededSlot > 0) {
			// update timeseries for all words in this app (+ slot number of new
			// 0 to all timeseries)
			Vocabulary.getInstance().doKeywordsPOS(appID);
			// update dayIndex and dayIndex_time
			dayIndex = daysSinceStart;
		}
		return neededSlot;
	}
	
	public String getAppID() {
		return appID;
	}

	public String getName() {
		return name;
	}

	public int getDBCount() {
		return dbCount;
	}

	public int getWorkingCount() {
		return WorkingCount;
	}

	// @Override
	// public int hashCode() {
	// return appID.hashCode();
	// }
	//
	// @Override
	// public boolean equals(Object arg0) {
	// if (this == arg0)
	// return true;
	// if (!(arg0 instanceof Application))
	// return false;
	// Application obj = (Application) arg0;
	// if (this.appID.equals(obj.appID))
	// return true;
	// return false;
	// }
	//
	// public String getAppID() {
	// return appID;
	// }

	/**
	 * Add a review to this Application
	 * 
	 */
//	public ReviewForAnalysis addReview(ReviewForAnalysis rev) {
//		ReviewForAnalysis review = reviewMap.get(rev.getReviewId().intern());
//		if (review == null)
//			return reviewMap.put(rev.getReviewId().intern(), rev);
//		return null;
//	}

	public void addReview(ReviewForAnalysis rev, boolean data) {
		long ct = rev.getCreationTime();
		if (ct < 1420095600000l)
			return;
		if (ct < startDate)
			startDate = ct;
		mReviews.add(rev);
	}

	
	
	public void writeTrainingDataToFile(PrintWriter fileWriter) {

		for (ReviewForAnalysis review : mReviews) {
			if (review.getSentences() == null
					|| review.getSentences().length == 0)
				continue;
			try {
				review.writeTrainingDataToFile(fileWriter);
			} catch (Exception e) {
				System.err.println(e.getMessage());
			}

		}
	}

//	public boolean contains(String reviewID) {
//		return mReviews.containsKey(reviewID);
//	}

	public Application(String appID, String name, int count,
			Long[] releaseDates, long start_date, int dayID) {
		this.appID = appID;
		//reviewMap = new HashMap<>();
		if (releaseDates == null)
			this.releaseDates = new HashSet<>();
		else
			this.releaseDates = new HashSet<>(Arrays.asList(releaseDates));
		dbCount = count;
		startDate = start_date;
		dayIndex = dayID;
		//dayIndex_time = start_date + dayID * DAYMILIS;
		Vocabulary.getInstance().addNewApp(appID);
	}

	/**
	 * Add a new update date for this application
	 * 
	 * @param updateDate
	 *            - Date format of MMM dd,yyy
	 * @return the type Long version of the date, or throw ParseException in
	 *         case of wrong format.
	 */
	public long addAnUpdateDate(String updateDate) throws ParseException {
		SimpleDateFormat f = new SimpleDateFormat("MMM dd,yyyy");
		Date date;
		date = (Date) f.parse(updateDate);
		long update = date.getTime();
		releaseDates.add(update);
		return update;
	}

	public Set<Long> getUpdates() {
		return releaseDates;
	}

}
