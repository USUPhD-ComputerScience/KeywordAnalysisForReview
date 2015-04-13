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

import util.PostgreSQLConnector;

public class Application implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -2497544910820359076L;
	private Map<String, ReviewForAnalysis> reviewMap;
	private Set<Long> releaseDates;
	private String appID;
	private String name;
	private int dbCount;
	private int WorkingCount;

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
	public ReviewForAnalysis addReview(ReviewForAnalysis rev) {
		ReviewForAnalysis review = reviewMap.get(rev.getReviewId().intern());
		if (review == null)
			return reviewMap.put(rev.getReviewId().intern(), rev);
		return null;
	}

	public void writeSentenceToFile(PrintWriter fileWriter) {

		for (ReviewForAnalysis review : reviewMap.values()) {
			if (review.getSentences() == null
					|| review.getSentences().length == 0)
				continue;
			try {
				review.writeSentenceToFile(fileWriter);
			} catch (Exception e) {
				System.err.println(e.getMessage());
			}

		}
	}

	public boolean contains(String reviewID) {
		return reviewMap.containsKey(reviewID);
	}

	public Application(String appID, String name, int count, Long[] releaseDates) {
		this.appID = appID;
		reviewMap = new HashMap<>();
		if (releaseDates == null)
			this.releaseDates = new HashSet<>();
		else
			this.releaseDates = new HashSet<>(Arrays.asList(releaseDates));
		dbCount = count;
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

	public List<ReviewForAnalysis> getReviews() {
		return new ArrayList<ReviewForAnalysis>(reviewMap.values());
	}
}
