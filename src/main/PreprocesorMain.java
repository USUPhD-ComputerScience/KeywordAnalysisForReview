package main;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.List;
import java.util.Set;

import managers.ApplicationManager;
import managers.ReviewDB;
import managers.Vocabulary;
import model.Application;
import model.ReviewForAnalysis;

public class PreprocesorMain {

	public static void main(String[] args) throws Throwable {
		// TODO Auto-generated method stub
		processData(1000);
	}

	// extract the keywords for each app
	// extract the sentences for each app
	private static void processData(int minReviews) throws Throwable {
		// read each review from the database: Table Reviews.
		System.out.println(">> Querying apps that has at least " + minReviews
				+ " reviews");
		Vocabulary voc = Vocabulary.getInstance();
		ReviewDB reviewDB = ReviewDB.getInstance();
		ApplicationManager appMan = reviewDB.queryMultipleAppsInfo(minReviews);
		List<Application> appList = appMan.getAppList();
		System.out.println("====> Queried " + appList.size() + " apps!");
		System.out.println(">> Processing reviews for each apps now:");
		PrintWriter pw = new PrintWriter(new FileWriter(
				"lib/dictionary/word2vecTrainingData/reviewDataSet.txt"), true);
		int nonenglish = 0, totalReview = 0;
		for (Application app : appList) {
			long start = System.currentTimeMillis();
			String appID = app.getAppID();
			System.out.println(">>" + appID + ":");
			System.out.print(" Querying raw reviews: ");
			int reviewsCount = reviewDB.queryReviewsforAProduct(app,true);
			Set<ReviewForAnalysis> reviewList = app.getReviewList();
			System.out.println("====> Queried " + reviewsCount + " reviews!");
			System.out.println(reviewList.size() + " reviews");
			int processedReview = 0, processedTokens = 0;
			for (ReviewForAnalysis rev : reviewList) {
				processedTokens += rev.extractSentences();
				processedReview++;
				if (processedReview % 1000 == 0)
					System.out.println("            ... processed "
							+ processedReview + " reviews so far (" + processedTokens
							+ " tokens)");
			}
			System.out.println("            ... processed "
					+ processedReview + " reviews (" + processedTokens
					+ " tokens)");
			// done processing, update keywords and day index and reviews to db
			// the day prior to current day
			// also write data down for word2vec training
			System.out.println(" Update preprocessed data to DB");
			reviewDB.updateIndexesForApp(appID,
					app.getDayIndex() - 1);
			//
			voc.updateKeywordDB(app);
			long lastDayStart = app.getStartDate()
					+ app.getDayIndex() * app.DAYMILIS;
			for (ReviewForAnalysis rev : reviewList) {
				if (rev.getCreationTime() >= lastDayStart)
					continue;
				reviewDB.updateCleansedText(rev);
				//retriever.insertBug(bug);
				// retriever.updateCleansedText(bug);
				rev.writeTrainingDataToFile(pw);
			}
			System.out.println(" This app spans in " + app.getDayIndex()
					+ " days");
			System.out.println(" Done! This App took "
					+ (double) (System.currentTimeMillis() - start) / 1000 / 60
					+ "minutes");
			appMan.removeApp(appID);
			voc.removeKeywordsOfAProduct(appID);

		}

		System.out.println("Done! Found " + nonenglish + "/" + totalReview
				+ " non-English reviews");
	}
}
