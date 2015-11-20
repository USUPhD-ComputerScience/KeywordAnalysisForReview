package model;

import java.io.Serializable;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import managers.ReviewDB;


public class Word {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1820812245518387992L;
	private int mdbID;
	private String mWord;
	private Map<String, Integer> mPOSSet;
	private Map<String, Integer> mPOSSetBuffer;// store POS for the last day
	private Application mApplication;
	private int hash;
	private String mPOS;
	private int mPOSMaxCount = 0;
	private int mCount = 0;
	private int[] mCountByRating;
	Map<Long, Integer> mCountByDay_R1 = new HashMap<>();
	Map<Long, Integer> mCountByDay_R2 = new HashMap<>();
	Map<Long, Integer> mCountByDay_R3 = new HashMap<>();
	Map<Long, Integer> mCountByDay_R4 = new HashMap<>();
	Map<Long, Integer> mCountByDay_R5 = new HashMap<>();

	public Map<Long, Integer> getCountByDay_R1() {
		return mCountByDay_R1;
	}

	public Map<Long, Integer> getCountByDay_R2() {
		return mCountByDay_R2;
	}

	public Map<Long, Integer> getCountByDay_R3() {
		return mCountByDay_R3;
	}

	public Map<Long, Integer> getCountByDay_R4() {
		return mCountByDay_R4;
	}

	public Map<Long, Integer> getCountByDay_R5() {
		return mCountByDay_R5;
	}

	// private int mDayIndex = 0;
	private boolean mGeneralUsage = false;
	private ReviewDB retriever = ReviewDB.getInstance();

	// public int getCurrentDay() {
	// return mDayIndex;
	// }

	// priority: 0-4
	// severity:
	// 0: minor_byday
	// 1: normal_byday
	// 2: blocker_byday
	// 3: trivial_byday
	// 4: enhancement_byday
	// 5: major_byday
	// 6: critical_byday
	public void increaseCount(int rating, long reviewDate)
			throws ParseException {
		long thisDate = util.Util.normalizeDate(reviewDate);
		// long thisDate = mProduct.getStartDate() + Product.DAYMILIS *
		// mDayIndex;
		Integer count = null;
		switch (rating) {
		case 0:
			count = mCountByDay_R1.get(thisDate);
			if (count == null)
				mCountByDay_R1.put(thisDate, 1);
			else
				mCountByDay_R1.put(thisDate, count + 1);
			break;
		case 1:
			count = mCountByDay_R2.get(thisDate);
			if (count == null)
				mCountByDay_R2.put(thisDate, 1);
			else
				mCountByDay_R2.put(thisDate, count + 1);

			break;

		case 2:
			count = mCountByDay_R3.get(thisDate);
			if (count == null)
				mCountByDay_R3.put(thisDate, 1);
			else
				mCountByDay_R3.put(thisDate, count + 1);

			break;

		case 3:
			count = mCountByDay_R4.get(thisDate);
			if (count == null)
				mCountByDay_R4.put(thisDate, 1);
			else
				mCountByDay_R4.put(thisDate, count + 1);

			break;

		case 4:
			count = mCountByDay_R5.get(thisDate);
			if (count == null)
				mCountByDay_R5.put(thisDate, 1);
			else
				mCountByDay_R5.put(thisDate, count + 1);
			break;
		}
		mCount += 1;
	}

	// public int[][] getTimeSeriesByPriority() {
	// return mTimeSeriesByPriority;
	// }
	//
	// public int[][] getTimeSeriesBySeverity() {
	// return mTimeSeriesBySeverity;
	// }

	public Map<String, Integer> getPOSSet() {
		return mPOSSet;
	}

	public void addPOS(String POS) {
		Integer PoScount = mPOSSetBuffer.get(POS);
		if (PoScount == null)
			mPOSSetBuffer.put(POS, 1);
		else
			mPOSSetBuffer.put(POS, PoScount + 1);
	}

	public int getWordID() {
		return mdbID;
	}

	public Word(String w) {
		mWord = w.intern();
		hash = (mWord).hashCode();
		mPOSSet = new HashMap<>();
		mGeneralUsage = true;
	}

	public boolean accumulateInfo(Word oldWord) throws SQLException {
		if (!mGeneralUsage)
			return false;
		int[] ratingCounts = oldWord.getCountByRating();
		if (mCountByRating == null)
			mCountByRating = new int[5];
		for (int k = 0; k < 5; k++) {
			mCountByRating[k] += ratingCounts[k];
			mCount += ratingCounts[k];
		}
		Map<String, Integer> POSs = oldWord.getPOSSet();
		for (Entry<String, Integer> entry : POSs.entrySet()) {
			int newCount = entry.getValue();
			String pos = entry.getKey();
			Integer oldCount = mPOSSet.get(pos);
			if (oldCount == null)
				mPOSSet.put(pos, newCount);
			else
				mPOSSet.put(pos, newCount + oldCount);
		}
		for (Entry<String, Integer> pos : mPOSSet.entrySet()) {
			int POScount = pos.getValue();
			if (POScount > mPOSMaxCount) {
				mPOSMaxCount = POScount;
				mPOS = pos.getKey();
			}
		}
		//
		// int[][] timeSeries = oldWord.getTimeSeriesByPriority();
		// if (mDayIndex < oldWord.getCurrentDay()) {
		// mDayIndex = oldWord.getCurrentDay();
		// int[][] tempTS = new int[5][mDayIndex];
		// if (mTimeSeriesByPriority != null) {
		// for (int i = 0; i < mTimeSeriesByPriority[0].length; i++) {
		// tempTS[0][i] += mTimeSeriesByPriority[0][i];
		// tempTS[1][i] += mTimeSeriesByPriority[1][i];
		// tempTS[2][i] += mTimeSeriesByPriority[2][i];
		// tempTS[3][i] += mTimeSeriesByPriority[3][i];
		// tempTS[4][i] += mTimeSeriesByPriority[4][i];
		//
		// }
		// }
		// mTimeSeriesByPriority = tempTS;
		// }
		// for (int i = 0; i < oldWord.getCurrentDay(); i++) {
		// mTimeSeriesByPriority[0][i] += timeSeries[0][i];
		// mTimeSeriesByPriority[1][i] += timeSeries[1][i];
		// mTimeSeriesByPriority[2][i] += timeSeries[2][i];
		// mTimeSeriesByPriority[3][i] += timeSeries[3][i];
		// mTimeSeriesByPriority[4][i] += timeSeries[4][i];
		// }
		// timeSeries = oldWord.getTimeSeriesBySeverity();
		// if (mDayIndex < oldWord.getCurrentDay()) {
		// mDayIndex = oldWord.getCurrentDay();
		// int[][] tempTS = new int[7][mDayIndex];
		// if (mTimeSeriesBySeverity != null) {
		// for (int i = 0; i < mTimeSeriesBySeverity[0].length; i++) {
		// tempTS[0][i] += mTimeSeriesBySeverity[0][i];
		// tempTS[1][i] += mTimeSeriesBySeverity[1][i];
		// tempTS[2][i] += mTimeSeriesBySeverity[2][i];
		// tempTS[3][i] += mTimeSeriesBySeverity[3][i];
		// tempTS[4][i] += mTimeSeriesBySeverity[4][i];
		// tempTS[5][i] += mTimeSeriesBySeverity[5][i];
		// tempTS[6][i] += mTimeSeriesBySeverity[6][i];
		//
		// }
		// }
		// mTimeSeriesBySeverity = tempTS;
		// }
		// for (int i = 0; i < oldWord.getCurrentDay(); i++) {
		// mTimeSeriesBySeverity[0][i] += timeSeries[0][i];
		// mTimeSeriesBySeverity[1][i] += timeSeries[1][i];
		// mTimeSeriesBySeverity[2][i] += timeSeries[2][i];
		// mTimeSeriesBySeverity[3][i] += timeSeries[3][i];
		// mTimeSeriesBySeverity[4][i] += timeSeries[4][i];
		// mTimeSeriesBySeverity[5][i] += timeSeries[5][i];
		// mTimeSeriesBySeverity[6][i] += timeSeries[6][i];
		// }
		return true;
	}

	public Word(int id, String w, Map<String, Integer> POSs, Application app) {
		mdbID = id;
		mWord = w.intern();

		if (POSs == null)
			mPOSSet = new HashMap<>();
		else
			mPOSSet = POSs;
		mApplication = app;

		for (Entry<String, Integer> pos : mPOSSet.entrySet()) {
			int POScount = pos.getValue();
			if (POScount > mPOSMaxCount) {
				mPOSMaxCount = POScount;
				mPOS = pos.getKey();
			}
		}
		hash = (mWord).hashCode();
	}

	public String getPOS() {
		return mPOS;
	}

	@Override
	public int hashCode() {
		return hash;
	}

	@Override
	public boolean equals(Object arg0) {
		if (this == arg0)
			return true;
		if (!(arg0 instanceof Word))
			return false;
		Word obj = (Word) arg0;
		if (this.mWord.equals(obj.mWord))
			return true;
		return false;
	}

	public boolean isEqual(String w2) {
		if (mWord.equals(w2))
			return true;
		return false;
	}

	/**
	 * @return the string of this word
	 */
	public String toString() {
		return mWord;
	}

	public void doPOS() {

		// create new POS buffer
		if (mPOSSetBuffer != null) {
			for (Entry<String, Integer> entry : mPOSSetBuffer.entrySet()) {
				String key = entry.getKey();
				Integer count = mPOSSet.get(key);
				if (count != null)
					mPOSSet.put(key, count + entry.getValue());
			}
		}
		mPOSSetBuffer = new HashMap<>();
	}

	public int getCount() {
		// TODO Auto-generated method stub
		return mCount;
	}

	public int[] getCount_db() throws SQLException {
		// TODO Auto-generated method stub
		mCountByRating = retriever.getCountByRating(mdbID);
		return mCountByRating;
	}

	public int[] getCountByRating() throws SQLException {
		// TODO Auto-generated method stub
		return mCountByRating;
	}

	public String getVocID() {
		// TODO Auto-generated method stub
		return mWord;
	}
}
