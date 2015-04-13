package model;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map.Entry;

// This is the word model for the entire analysis
public class Word implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = -1820812245518387992L;
	private String word;
	private int count;
	private HashMap<String, Integer> POSSet;
	private Application application;
	private int hash;
	private String POS;
	private int POSMaxCount = 0;
	private int[] CountByRating;
	private int[][] TimeSeriesByRating;

	public int[] getCountByRating() {
		return CountByRating;
	}

	public int[][] getTimeSeriesByRating() {
		return TimeSeriesByRating;
	}

	public HashMap<String, Integer> getPOSSet() {
		return POSSet;
	}

	public Word(String w, HashMap<String, Integer> POSs, Application app,
			int[] count, int[][] timeSeries) {
		word = w.intern();
		CountByRating = count;
		TimeSeriesByRating = timeSeries;
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

	public void increaseCount() {
		count++;
	}

	public int getCount() {
		return count;
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

}
