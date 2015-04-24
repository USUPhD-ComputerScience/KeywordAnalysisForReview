package main;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.List;

import managers.ApplicationManager;
import managers.ReviewDB;
import managers.Vocabulary;
import model.Application;
import model.ReviewForAnalysis;

public class PreprocesorMain {

	public static void main(String[] args) throws Throwable {
		// TODO Auto-generated method stub
		processData(10000);
	}

	// extract the keywords for each app
	// extract the sentences for each app
	private static void processData(int minReviews) throws Throwable {
		// read each review from the database: Table Reviews.
		System.out.println(">> Querying apps that has at least " + minReviews
				+ " reviews");
		Vocabulary voc = Vocabulary.getInstance();
		ReviewDB reviewDB = ReviewDB.getInstance();
		List<Application> appList = reviewDB.queryMultipleAppsInfo(minReviews);
		System.out.println("====> Queried " + appList.size() + " apps!");
		System.out.println(">> Processing reviews for each apps now:");
		PrintWriter pw = new PrintWriter(new FileWriter(
				"lib/dictionary/word2vecTrainingData/reviewDataSet.txt"));
		int nonenglish = 0, totalReview = 0;
		for (Application app : appList) {
			long start = System.currentTimeMillis();
			String appid = app.getAppID();
			System.out.println("      " + appid + ":");
			System.out.println("        Query Keywords: "
					+ voc.loadDBKeyword(app) + " keywords!");
			System.out.print("        Querying raw reviews: ");
			List<ReviewForAnalysis> reviewList = reviewDB.queryReviews(app,
					true);
			System.out.println(reviewList.size() + " reviews");
			for (ReviewForAnalysis rev : reviewList) {
				if (!rev.extractSentences())
					nonenglish++;
				totalReview++;

			}
			// done processing, update keywords and day index and reviews to db
			// the day prior to current day
			// also write data down for word2vec training
			System.out.println("        Update preprocessed data to DB");
			reviewDB.updateIndexesForApp(appid, app.getDayIndex() - 1);
			voc.updateKeywordDB(appid);
			long lastDayStart = app.getStartDate() + app.getDayIndex()
					* Application.DAYMILIS;
			for (ReviewForAnalysis rev : reviewList) {
				if (rev.getCreationTime() >= lastDayStart)
					continue;
				reviewDB.updateCleansedText(rev);
				rev.writeSentenceToFile(pw);
			}

			System.out.println("        Done! This app took "
					+ (double) (System.currentTimeMillis() - start) / 1000 / 60
					+ "minutes");
		}

		System.out.println("Done! Found " + nonenglish + "/" + totalReview
				+ " non-English reviews");
	}
}
