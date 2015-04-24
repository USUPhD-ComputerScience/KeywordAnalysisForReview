package model;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

// This is the word model for the entire analysis
public class Word implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1820812245518387992L;
	private int dbID;
	private String word;
	private Map<String, Integer> POSSet;
	private Application application;
	private int hash;
	private String POS;
	private int POSMaxCount = 0;
	private int count = 0;
	private int[] CountByRating;
	private int[][] TimeSeriesByRating;
	private int dayIndex = 0;

	public int getCurrentDay() {
		return dayIndex;
	}

	// rate: 0-4
	public void increaseCount(int rate) {
		TimeSeriesByRating[rate][dayIndex] += 1;
		CountByRating[rate] += 1;
		count += 1;
	}

	public int[] getCountByRating() {
		return CountByRating;
	}

	public int[][] getTimeSeriesByRating() {
		return TimeSeriesByRating;
	}

	public Map<String, Integer> getPOSSet() {
		return POSSet;
	}

	public int getWordID() {
		return dbID;
	}

	public Word(int id, String w, Map<String, Integer> POSs, Application app,
			int[][] timeSeries, int dayIndex) {
		dbID = id;
		word = w.intern();
		if (timeSeries == null)
			TimeSeriesByRating = new int[5][];
		else
			TimeSeriesByRating = timeSeries;
		CountByRating = new int[5];
		if (timeSeries != null) {
			for (int i = 0; i < 5; i++) {
				if (timeSeries[i] == null)
					break;
				for (int k = 0; k < timeSeries[i].length; k++) {
					CountByRating[i] += timeSeries[i][k];
					count += timeSeries[i][k];
				}
			}
		}
		this.dayIndex = dayIndex;

		if (POSs == null)
			POSSet = new HashMap<>();
		else
			POSSet = POSs;
		application = app;

		for (Entry<String, Integer> pos : POSSet.entrySet()) {
			int POScount = pos.getValue();
			if (POScount > POSMaxCount) {
				POSMaxCount = POScount;
				POS = pos.getKey();
			}
		}
		hash = word.hashCode();
	}

	public String getPOS() {
		return POS;
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
		if (this.word.equals(obj.word))
			return true;
		return false;
	}

	public boolean isEqual(String w2) {
		if (word.equals(w2))
			return true;
		return false;
	}

	/**
	 * @return the string of this word
	 */
	public String toString() {
		return word;
	}

	public void extendTimeseries(int neededSlot) {
		// TODO Auto-generated method stub
		int[][] replacementTS = new int[5][neededSlot];
		for (int i = 0; i < 5; i++) {
			// newly created word, OR the data in db is less than 2 day new
			if (TimeSeriesByRating[i] != null) {
				replacementTS[i] = new int[TimeSeriesByRating[i].length
						+ neededSlot];
				for (int k = 0; k < TimeSeriesByRating[i].length; k++) {
					replacementTS[i][k] = TimeSeriesByRating[i][k];
				}
			}
		}
		TimeSeriesByRating = replacementTS;
		dayIndex = TimeSeriesByRating[0].length - 1;
	}

	public int getCount() {
		// TODO Auto-generated method stub
		return count;
	}

}
