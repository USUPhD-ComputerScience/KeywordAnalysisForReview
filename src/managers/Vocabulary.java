package managers;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import model.Application;
import model.Word;

public class Vocabulary {
	/**
	 * 
	 */
	ReviewDB reviewDB = ReviewDB.getInstance();
	private Map<Integer, Word> corpusVoc = new HashMap<>();
	private Set<Word> corpusVocSet = new HashSet<>();
	private Map<String, Map<Integer, Word>> appVocSearchForWord = new HashMap<>();
	private Map<String, Map<Word, Integer>> appVocSearchForID = new HashMap<>();
	private static Vocabulary instance = null;

	public static synchronized Vocabulary getInstance() {
		if (instance == null) {
			instance = new Vocabulary();
		}
		return instance;
	}

	public void writeWordsToFile(String fileName, boolean corpus)
			throws FileNotFoundException {
		System.out.print(">>Writing Words to file");

		PrintWriter pw = new PrintWriter(fileName);
		if (corpus) {
			for (Entry<Integer, Word> entry : corpusVoc.entrySet()) {
				Word word = entry.getValue();
				pw.println(word.toString() + "," + word.getCount() + ","
						+ word.getPOSSet().toString());
			}
		} else {
			for (Entry<String, Map<Integer, Word>> app : appVocSearchForWord
					.entrySet()) {
				for (Entry<Integer, Word> entry : app.getValue().entrySet()) {
					Word word = entry.getValue();
					pw.println(app.getKey() + "," + word.toString() + ","
							+ word.getCount() + ","
							+ word.getPOSSet().toString());
				}
			}
		}

		pw.close();

	}

	private Vocabulary() {
	}

	public int loadDBKeyword(Application app) throws SQLException {
		String appid = app.getAppID();
		List<Word> wordListFromDB = ReviewDB.getInstance().queryWordsForAnApp(
				app);
		for (Word word : wordListFromDB) {
			// add to voc
			addNewWord(word, appid);
		}
		return wordListFromDB.size();
	}

	// rating: 0-4
	public int addWord(String w, String POS, Application app, int rating)
			throws SQLException {
		String appID = app.getAppID();
		Map<Word, Integer> vocOfThisApp = appVocSearchForID.get(appID);
		Integer wordID = vocOfThisApp.get(new Word(0, w, null, null, null, 0));
		// not in voc, create a new entry for this word with the same dayLength
		// as other words.
		if (wordID == null) {
			// query from db
			// not in db, create new words
			wordID = reviewDB.addKeyWord(w, POS, appID);
			Map<String, Integer> POSs = new HashMap<>();
			POSs.put(POS, 1);
			Word word = new Word(wordID, w, POSs, app, null, app.getDayIndex());
			word.extendTimeseries(app.getDayIndex() + 1);
			// add to voc
			addNewWord(word, appID);
		}
		// update PoSs and timeseries
		Word word = appVocSearchForWord.get(appID).get(wordID);
		word.increaseCount(rating);
		Map<String, Integer> PoSs = word.getPOSSet();
		Integer PoScount = PoSs.get(POS);
		if (PoScount == null)
			PoSs.put(POS, 1);
		else
			PoSs.put(POS, PoScount + 1);

		return wordID;
	}

	// must be called when a review passed a new day
	public void updateKeywordDB(String appid) throws SQLException {
		ReviewDB reviewdb = ReviewDB.getInstance();
		for (Entry<Word, Integer> entry : appVocSearchForID.get(appid)
				.entrySet()) {
			reviewdb.updateKeyWord(entry.getKey(), appid);
		}
	}

	private void addNewWord(Word w, String appid) {
		Map<Integer, Word> vocSearchWord = appVocSearchForWord.get(appid);
		Map<Word, Integer> vocSearchID = appVocSearchForID.get(appid);
		int id = w.getWordID();
		vocSearchID.put(w, id);
		vocSearchWord.put(id, w);
	}

	public void addNewApp(String appid) {
		appVocSearchForID.put(appid, new HashMap<>());
		appVocSearchForWord.put(appid, new HashMap<>());
	}

	public Word getWord(int keywordid, Application app) throws SQLException {
		Word w;
		if (app != null) {
			w = appVocSearchForWord.get(app.getAppID()).get(keywordid);
			if (w == null) {
				w = ReviewDB.getInstance().querySingleWord(keywordid, app);
				if (w != null)
					appVocSearchForWord.get(app.getAppID()).put(keywordid, w);
			}
		} else {
			w = corpusVoc.get(keywordid);
		}
		return w;
	}

	// implement later
	public void loadCorpusVoc() {

	}

	public void extendKeywordsTimeseries(int neededSlot, String appID) {
		// TODO Auto-generated method stub

		Map<Word, Integer> vocOfThisApp = appVocSearchForID.get(appID);
		for (Entry<Word, Integer> entry : vocOfThisApp.entrySet()) {
			entry.getKey().extendTimeseries(neededSlot);
		}
	}
}
