package main;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.concurrent.TimeUnit;

import Crawler.Crawler;
import au.com.bytecode.opencsv.CSVReader;

public class CrawlerMain {
	public static String login, password, androidid;

	public static void main(String[] args) throws Throwable {
		// TODO Auto-generated method stub
		System.out.println("====== Crawling reviews from Google Play ======");
		if (args.length != 2) {

			System.out
					.println("Need to provide both Apps List AND login credential files. Exitting..");
			return;
		}
		Scanner reader = new Scanner(new FileReader(args[1]));
		while (reader.hasNext()) {
			String[] values = reader.nextLine().split(",");
			if (values.length == 3) {
				login = values[0];
				password = values[1];
				androidid = values[2];
			} else {
				System.out.println("login file " + args[1]
						+ " has been corrupted");
				return;
			}
		}
		reader.close();
		crawl(args[0]);
	}

	private static void crawl(String sourceFile) throws SQLException {
		Crawler crawler = new Crawler(login, password, androidid);
		int iterationCount = 0;
		int failedAttempts = 0;
		while (true) {
			long start = System.currentTimeMillis();
			System.out.println(">> New iteration. *NEW*");
			List<String> AoI = readAppInforFromFile(sourceFile);
			System.out.println(">>>> Extracting reviews:");
			for (String appID : AoI) {
				if (!crawler.extractReviews(appID))
					failedAttempts++;
				if (failedAttempts == 5)
					break;
			}
			if (failedAttempts == 5) {
				System.out
						.println(">> There are problems with server, restarting in 15 mins.");
				try {
					TimeUnit.MINUTES.sleep(15);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
				failedAttempts = 0;
				iterationCount = 0;
				crawler = new Crawler(login, password, androidid);
				continue;
			}

			long stop = System.currentTimeMillis();

			System.out.println(">> This iteration took " + (stop - start)
					/ 1000 / 60 + " minutes. Wait for 30 mins!");
			System.out.println("============================");
			iterationCount++;
			try {
				TimeUnit.MINUTES.sleep(30);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			if (iterationCount == 12) {
				iterationCount = 0;
				failedAttempts = 0;
				crawler = new Crawler(login, password, androidid);
			}

		}
	}

	private static List<String> readAppInforFromFile(String fileName) {
		System.out.println(">>>> Reading appid from - " + fileName);
		CSVReader reader = null;
		List<String> appIDList = new ArrayList<>();
		try {
			reader = new CSVReader(new FileReader(fileName));
			String[] row = null;
			while ((row = reader.readNext()) != null) {
				if (row[0].length() > 5)
					appIDList.add(row[0]);
			}
			System.out.println("    List of apps: ");
			for (String appid : appIDList)
				System.out.println("     " + appid);
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
		return appIDList;
	}
}
