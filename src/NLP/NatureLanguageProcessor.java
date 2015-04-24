package NLP;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Scanner;
import java.util.Set;
import au.com.bytecode.opencsv.CSVReader;
import edu.stanford.nlp.tagger.maxent.MaxentTagger;

public class NatureLanguageProcessor {
	public static final String[] POSLIST = { "''", "(", ")", ",", "--", ".",
			":", "UH", "VB", "VBD", "VBG", "VBN", "VBP", "VBZ", "WDT", "WP",
			"WP$", "WRB", "$", "``", "NNPS", "NNS", "PDT", "POS", "PRP",
			"PRP$", "RB", "RBR", "RBS", "RP", "SYM", "TO", "CC", "CD", "DT",
			"EX", "FW", "IN", "JJ", "JJR", "JJS", "LS", "MD", "NN", "NNP" };

	public static final Set<String> POSSET = new HashSet<>(
			Arrays.asList(POSLIST));
	private Set<String> stopWordSet;// less extensive

	public Set<String> getStopWordSet() {
		return stopWordSet;
	}

	private static NatureLanguageProcessor instance = null;
	MaxentTagger PoSTagger;
	private static final HashMap<String, Integer> realDictionary = new HashMap<>();
	private static final HashMap<String, String[]> correctionMap = new HashMap<>();

	public static synchronized NatureLanguageProcessor getInstance() {
		if (instance == null)
			instance = new NatureLanguageProcessor();
		return instance;
	}

	private static void loadCorrectionMap(File file)
			throws FileNotFoundException {
		// TODO Auto-generated method stub
		Scanner br = new Scanner(new FileReader(file));
		while (br.hasNextLine()) {
			String[] pair = br.nextLine().split(",");
			if (pair.length == 2)
				correctionMap.put(pair[0], pair[1].split(" "));

		}
		br.close();
	}

	private static void loadDictionary(File[] fileLists) throws Exception {
		for (File file : fileLists) {
			Scanner br = new Scanner(new FileReader(file));
			while (br.hasNext())
				realDictionary.put(br.next(), 0);
			br.close();
		}
	}

	private NatureLanguageProcessor() {
		readStopWordsFromFile();
		PoSTagger = new MaxentTagger(
				"lib/dictionary/POS/english-left3words-distsim.tagger");
		try {
			loadCorrectionMap(new File("lib/dictionary/Map/wordMapper.txt"));
			loadDictionary(new File("lib/dictionary/improvised").listFiles());
			Porter2StemmerInit();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private void readStopWordsFromFile() {
		stopWordSet = new HashSet<>();
		System.err
				.println(">>Read StopWords from file - englishImprovised.stop");
		CSVReader reader = null;
		try {
			reader = new CSVReader(new FileReader(
					"lib/dictionary/stop/englishImprovised.stop"));
			String[] row = null;
			while ((row = reader.readNext()) != null) {
				stopWordSet.add(row[0]);
			}

			PrintWriter stop = new PrintWriter(
					"lib/dictionary/stop/englishImprovised.stop");
			for (String w : stopWordSet)
				stop.println(w);
			stop.close();
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
	}

	/**
	 * Standardize the text and then split it into sentences, separated by DOT
	 * 
	 * @param text
	 *            - a text
	 * @return a String array of all the sentences.
	 */
	public String[] extractSentence(String text) {
		return text.split("\\.+");
	}

	/**
	 * Return the index of the corresponding PoS tag in the list provided by
	 * this Class. This provides a way to reduce the memory for String objects.
	 * Instead of storing the String of PoS tag, we can store its index.
	 * 
	 * @param PoS
	 *            - a PoS tag
	 * @return the index of that PoS tag or -1 if it is not in the list
	 */
	public boolean checkValidityOfPOS(String PoS) {
		return POSSET.contains(PoS);
	}

	public List<String> extractWordsFromText(String text) {
		text = text.toLowerCase();
		String[] words = text.split("[^a-z0-9']+");
		// SymSpell symspell = SymSpell.getInstance();
		ArrayList<String> wordList = new ArrayList<>();
		for (String word : words) {
			if (word.equals("null") || word.length() == 0 || word.equals("'")
					|| word.equals(""))
				continue;

			String[] wordarray = correctionMap.get(word);
			if (wordarray != null)
				wordList.addAll(Arrays.asList(wordarray));
			else
				wordList.add(word);
		}
		double totalScore = 0, bigramScore = 0, unigramScore = 0;
		boolean previousInDic = false;
		for (String word : wordList) {
			Integer wCount = realDictionary.get(word);
			double score = 1.0;
			if (wCount != null) {
				// score /= Math.log(wCount);
				unigramScore += score;
				if (previousInDic)
					bigramScore += score;
				previousInDic = true;
			} else
				previousInDic = false;

			totalScore += score;
		}
		double biproportion = bigramScore / totalScore;
		double uniproportion = unigramScore / totalScore;
		if (biproportion < 0.4 && uniproportion < 0.5)
			return null;

		return wordList;
	}

	/**
	 * This function will stem the words in the input List using Porter2/English
	 * stemmer and replace the String value of that word with the stemmed
	 * version.
	 * 
	 * @param wordList
	 *            - a List contains a String array of 2 elements: 0-word, 1-PoS
	 */
	public List<String[]> stem(List<String[]> wordList) {
		List<String[]> results = new ArrayList<>();
		CustomStemmer stemmer = CustomStemmer.getInstance();
		for (String[] pair : wordList) {
			if (pair.length < 2)
				continue;
			// System.out.print(count++ + ": " + pair[0]);
			if (!stopWordSet.contains(pair[0]))
				pair = stemmer.stem(pair);

			results.add(pair);
			// System.out.println("-" + pair[0] + "<->" + pair[1]);
		}
		return results;
	}

	private void Porter2StemmerInit() throws ClassNotFoundException,
			InstantiationException, IllegalAccessException {
		Class stemClass = Class
				.forName("org.tartarus.snowball.ext.englishStemmer");
	}

	public List<String[]> findPosTag(List<String> wordList) {
		if (wordList == null)
			return null;
		SymSpell spellCorrector = SymSpell.getInstance();
		StringBuilder textForTag = new StringBuilder();
		String prefix = "";
		for (String word : wordList) {
			textForTag.append(prefix + word);
			prefix = " ";
		}
		// The tagged string
		String tagged = PoSTagger.tagString(textForTag.toString());
		// Output the result
		// System.out.println(tagged);

		String[] words = tagged.split(" ");
		// System.out.println("length = " + words.length);

		List<String[]> results = new ArrayList<>();
		for (int i = 0; i < words.length; i++) {
			String[] w = words[i].split("_");
			// if (!stopWordSet.contains(w[0]))
			if (w.length == 2)
				results.add(w);
		}
		return results;
	}
}
