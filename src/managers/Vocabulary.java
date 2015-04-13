package managers;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import model.Application;
import model.Word;

public class Vocabulary implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 8816913577751358916L;
	private Map<Integer, Word> corpusVoc = new HashMap<>();
	private Set<Word> corpusVocSet = new HashSet<>();
	private Map<String, Map<Integer, Word>> appVoc = new HashMap<>();
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
			for (Entry<String, Map<Integer, Word>> app : appVoc.entrySet()) {
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

	public Word getWord(int keywordid, Application app) throws SQLException {
		Word w;
		if (app != null) {
			w = appVoc.get(app.getAppID()).get(keywordid);
			if (w == null) {
				w = ReviewDB.getInstance().querySingleWord(keywordid, app);
				if (w != null)
					appVoc.get(app.getAppID()).put(keywordid, w);
			}
		} else {
			w = corpusVoc.get(keywordid);
		}
		return w;
	}

	// implement later
	public void loadCorpusVoc() {

	}
}
