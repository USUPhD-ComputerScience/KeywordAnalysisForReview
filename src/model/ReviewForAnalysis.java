package model;

import java.io.PrintWriter;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.Objects;

import managers.Vocabulary;

public class ReviewForAnalysis implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = -720406586403564687L;
	// private List<Sentence> sentenceList;
	// private List<Integer> wordIDList;
	private int[][] sentences;
	private String deviceName;
	private String documentVersion;
	private long creationTime;
	private String reviewId; // commentID and VersionID
	private Application application;
	private int rating;
	private String rawText;

	public String getRawText() {
		return rawText;
	}

	public int[][] getSentences() {
		return sentences;
	}

	public int getRating() {
		return rating;
	}

	public String getDeviceName() {
		return deviceName;
	}

	public String getDocumentVersion() {
		return documentVersion;
	}

	public long getCreationTime() {
		return creationTime;
	}

	public String getReviewId() {
		return reviewId;
	}

	public ReviewForAnalysis(String rawText, int[][] cleansedText,
			int nestedRating, String nestedDeviceName,
			String nestedDocumentVersion, long nestedCreationTime,
			String nestedReviewId, Application app) {
		// TODO Auto-generated constructor stub
		rating = nestedRating;
		deviceName = nestedDeviceName;
		documentVersion = nestedDocumentVersion;
		creationTime = nestedCreationTime;
		reviewId = nestedReviewId.intern();
		this.rawText = rawText.intern();
		application = app;
		sentences = cleansedText;
	}

	@Override
	public int hashCode() {
		return Objects.hash(reviewId, application);
	}

	@Override
	public boolean equals(Object arg0) {
		if (this == arg0)
			return true;
		if (!(arg0 instanceof ReviewForAnalysis))
			return false;
		ReviewForAnalysis obj = (ReviewForAnalysis) arg0;
		if (this.reviewId.equals(obj.reviewId)
				&& this.application.equals(obj.application))
			return true;
		return false;
	}

	/**
	 * Extract the sentences of this review and store them
	 * 
	 * @param sentences
	 *            - the String contains the sentences that have already been
	 *            standardized.
	 * 
	 */
	// private void extractSentence(String[] sentences) {
	// sentenceList = new ArrayList<>();
	// for (String fullSentence : sentences) {
	// Sentence s = new Sentence(fullSentence);
	// if (!s.getWordIDList().isEmpty())
	// sentenceList.add(s);
	// }
	// }

	/**
	 * 
	 * @return the list of Sentences
	 * 
	 */
	// public List<Sentence> getSentenceList() {
	// return sentenceList;
	// }

	public static class ReviewBuilder {
		private String nestedRawText;
		private int[][] nestedCleansedText;
		private int nestedRating;
		private String nestedDeviceName;
		private String nestedDocumentVersion;
		private long nestedCreationTime;
		private String nestedReviewId;
		private Application nestedApplication;

		public ReviewBuilder() {
			nestedRawText = null;
			nestedCleansedText = null;
			nestedRating = 0;
			nestedDeviceName = null;
			nestedDocumentVersion = null;
			nestedCreationTime = 0;
			nestedReviewId = null;
		}

		public ReviewBuilder application(Application app) {
			this.nestedApplication = app;
			return this;
		}

		public ReviewBuilder rawText(String text) {
			this.nestedRawText = text.intern();
			return this;
		}

		public ReviewBuilder cleansedText(int[][] SentenceArrays) {
			this.nestedCleansedText = SentenceArrays;
			return this;
		}

		public ReviewBuilder rating(int rating) {
			this.nestedRating = rating;
			return this;
		}

		public ReviewBuilder deviceName(String deviceName) {
			this.nestedDeviceName = deviceName;
			return this;
		}

		public ReviewBuilder documentVersion(String documentVersion) {
			this.nestedDocumentVersion = documentVersion;
			return this;
		}

		public ReviewBuilder creationTime(long creationTime) {
			this.nestedCreationTime = creationTime;
			return this;
		}

		public ReviewBuilder reviewId(String reviewID) {
			this.nestedReviewId = reviewID.intern();
			return this;
		}

		public ReviewForAnalysis createReview() {
			return new ReviewForAnalysis(nestedRawText, nestedCleansedText,
					nestedRating, nestedDeviceName, nestedDocumentVersion,
					nestedCreationTime, nestedReviewId, nestedApplication);
		}
	}

	public void writeSentenceToFile(PrintWriter fileWriter) {
		// TODO Auto-generated method stub
		fileWriter.println(toProperString());
	}

	/**
	 * @return the full review with each word separated by a space
	 */
	public String toString() {
		Vocabulary voc = Vocabulary.getInstance();
		StringBuilder strBld = new StringBuilder();
		String prefix = "";
		for (int[] sentence : sentences) {
			for (int wordID : sentence) {
				Word w;
				try {
					w = voc.getWord(wordID, application);
					if (w != null) {
						strBld.append(prefix);
						strBld.append(w.toString());
						prefix = " ";
					}
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return strBld.toString();
	}

	/**
	 * @return the full review with each word separated by a space and sentences
	 *         are separated by .
	 */
	public String toProperString() {
		Vocabulary voc = Vocabulary.getInstance();
		StringBuilder strBld = new StringBuilder();
		String prefix = "";
		for (int[] sentence : sentences) {
			for (int wordID : sentence) {
				Word w;
				try {
					w = voc.getWord(wordID, application);
					if (w != null) {
						strBld.append(prefix);
						strBld.append(w.toString());
						prefix = " ";
					}

				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			strBld.append(".");
		}
		return strBld.toString();
	}

}
