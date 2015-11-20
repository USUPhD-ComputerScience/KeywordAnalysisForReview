package managers;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import model.Application;
import model.ReviewForAnalysis;
import model.Word;

public class Vocabulary {
	/**
	 * 
	 */
	ReviewDB reviewDB = ReviewDB.getInstance();
	private Map<String, Word> corpusVoc = new HashMap<>();
	private Map<String, Map<Integer, Word>> appVocSearchForWord = new HashMap<>();
	private Map<String, Map<Word, Integer>> appVocSearchForID = new HashMap<>();
	private static Vocabulary instance = null;

	public static synchronized Vocabulary getInstance() {
		if (instance == null) {
			instance = new Vocabulary();
		}
		return instance;
	}

	// explicitly build the corpus voc from all app's voc
	// public void buildCorpusVoc() {
	// corpusVoc = new HashMap<>();
	// for (Entry<String, Map<Integer, Word>> appVoc : appVocSearchForWord
	// .entrySet()) {
	// for (Entry<Integer, Word> w : appVoc.getValue().entrySet()) {
	// Word newWord = w.getValue();
	// String wstr = newWord.toString();
	// Word oldWord = corpusVoc.get(wstr);
	// if (oldWord == null) {
	// corpusVoc.put(wstr, newWord);
	// } else {
	// Word word = new Word(oldWord.toString());
	// word.accumulateInfo(oldWord.getCountByRating(),
	// oldWord.getPOSSet());
	// word.accumulateInfo(newWord.getCountByRating(),
	// newWord.getPOSSet());
	// corpusVoc.put(wstr, word);
	// }
	// }
	// }
	// }
	public Map<String, Word> buildCustomVoc(List<String> IDList)
			throws SQLException {

		Map<String, Word> customVoc = new HashMap<>();
		System.out.println("start to build voc");
		for (String ID : IDList) {
			System.out.println("Product: " + ID);

			Map<Integer, Word> appVoc = appVocSearchForWord.get(ID);

			System.out.println("Has " + appVoc.size() + " words");
			for (Entry<Integer, Word> w : appVoc.entrySet()) {
				Word newWord = w.getValue();
				String wstrID = newWord.getVocID();
				Word oldWord = customVoc.get(wstrID);
				if (oldWord == null) {
					customVoc.put(wstrID, newWord);
				} else {
					// Word word = new Word(oldWord.toString(), true);
					// word.accumulateInfo(oldWord);
					oldWord.accumulateInfo(newWord);
					// customVoc.put(wstrID, word);
				}
			}
		}

		return customVoc;
	}

	public Map<String, Word> getCorpusVoc() {
		return corpusVoc;
	}

	public void writeWordsToFile(String fileName, boolean corpus)
			throws FileNotFoundException {
		System.out.print(">>Writing Words to file");

		PrintWriter pw = new PrintWriter(fileName);
		if (corpus) {
			for (Entry<String, Word> entry : corpusVoc.entrySet()) {
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
		List<Word> wordListFromDB = ReviewDB.getInstance()
				.queryWordsForAnApp(app);
		for (Word word : wordListFromDB) {
			// add to voc
			addNewWord(word, appid);
		}
		return wordListFromDB.size();
	}

	// rating: 0-4
	public int addWord(String w, String POS, Application app, int rating,
			ReviewForAnalysis rev) throws SQLException, ParseException {

		String appID = app.getAppID();
		Map<Word, Integer> vocOfThisApp = appVocSearchForID.get(appID);
		Integer wordID = vocOfThisApp.get(new Word(0, w, null, null));
		// not in voc, create a new entry for this word with the same dayLength
		// as other words.
		if (wordID == null) {
			// query from db
			// not in db, create new words
			wordID = reviewDB.addKeyWord(w, POS, appID);
			Map<String, Integer> POSs = new HashMap<>();
			POSs.put(POS, 1);
			Word word = new Word(wordID, w, POSs, app);

			word.doPOS();
			// add to voc
			addNewWord(word, appID);
		}
		// update PoSs and timeseries
		Word word = appVocSearchForWord.get(appID).get(wordID);
		word.increaseCount(rating, rev.getCreationTime());
		word.addPOS(POS);

		return wordID;
	}

	// must be called when a review passed a new day
	public void updateKeywordDB(Application app) throws SQLException {
		ReviewDB reviewdb = ReviewDB.getInstance();
		// for (Entry<Word, Integer> entry : appVocSearchForID.get(appid)
		// .entrySet()) {
		// reviewdb.updateKeyWord(entry.getKey(), appid);
		// }

		try {
			int count = 0;
			System.out.println(
					"Starting to upload keywords of " + app.getAppID());
			long start = System.currentTimeMillis();
			int insertedEntries = 0;
			for (Entry<Word, Integer> entry : appVocSearchForID
					.get(app.getAppID()).entrySet()) {
				insertedEntries += reviewdb.updateKeyWordByDays(entry.getKey());
				count++;
				if (count % 1000 == 0) {
					System.out
							.println("==> progress: " + count + " keywords for "
									+ (double) (System.currentTimeMillis()
											- start) / 1000 / 60
									+ " minutes (" + insertedEntries
									+ " entries inserted)");
				}
			}
			System.out
					.println(
							"==> progress: " + count + " keywords for "
									+ (double) (System.currentTimeMillis()
											- start) / 1000 / 60
									+ " minutes (" + insertedEntries
									+ " entries inserted)");
		} catch (Throwable e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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

	public Word getWord(int keywordid, Application app, boolean corpus)
			throws SQLException {
		Word w;
		w = appVocSearchForWord.get(app.getAppID()).get(keywordid);
		if (w == null) {
			w = ReviewDB.getInstance().querySingleWord(keywordid, app);
			if (w != null)
				appVocSearchForWord.get(app.getAppID()).put(keywordid, w);
		}
		if (corpus) {
			if (w == null)
				return null;
			w = corpusVoc.get(w.toString());
		}
		return w;
	}

	public Word getWord(String keyword, Application app) throws SQLException {
		Word w;
		Integer wid = appVocSearchForID.get(app.getAppID())
				.get(new Word(keyword));
		if (wid == null)
			return null;
		w = appVocSearchForWord.get(app.getAppID()).get(wid);
		return w;
	}

	public Word getWord(String word) {
		return corpusVoc.get(word);
	}

//	public void extendKeywordsTimeseries(int neededSlot, String appID) {
//		// TODO Auto-generated method stub
//
//		Map<Word, Integer> vocOfThisApp = appVocSearchForID.get(appID);
//		for (Entry<Word, Integer> entry : vocOfThisApp.entrySet()) {
//			entry.getKey().extendTimeseries(neededSlot);
//		}
//	}
	public void doKeywordsPOS(String product_id) {
		// TODO Auto-generated method stub

		Map<Word, Integer> vocOfThisApp = appVocSearchForID
				.get(product_id);
		for (Entry<Word, Integer> entry : vocOfThisApp.entrySet()) {
			entry.getKey().doPOS();
		}
	}

	public void removeKeywordsOfAProduct(String product_ID) {
		appVocSearchForWord.remove(product_ID);
		appVocSearchForID.remove(product_ID);
	}
}
