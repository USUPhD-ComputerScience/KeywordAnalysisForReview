package managers;

import java.io.PrintWriter;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import model.Application;
import model.ReviewForAnalysis;

public class ApplicationManager implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6981176569155372512L;
	private static ApplicationManager instance = null;
	private Map<String, Application> appMap;
	private int totalReviewCount;

	public static synchronized ApplicationManager getInstance() {
		if (instance == null) {
			instance = new ApplicationManager();
		}
		return instance;
	}

	private ApplicationManager() {
		appMap = new HashMap<>();
		totalReviewCount = 0;
	}

	public void addApp(String appid) throws SQLException {
		Application app = appMap.get(appid);
		if (app == null) {
			ReviewDB reviewDB = ReviewDB.getInstance();
			app = reviewDB.querySingleAppInfo(appid);
			if (app != null) {
				appMap.put(appid, app);
				for (ReviewForAnalysis rev : reviewDB.queryReviews(app)) {
					if (app.addReview(rev) != null)
						totalReviewCount++;
				}
			}
		}

	}

	public void writeSentenceToFile(PrintWriter fileWriter) {
		for (Entry<String, Application> app : appMap.entrySet()) {
			app.getValue().writeSentenceToFile(fileWriter);
		}
	}

	public void addWholeCorpus(int minReviews) throws SQLException {
		ReviewDB reviewDB = ReviewDB.getInstance();
		for (Application app : reviewDB.queryMultipleAppsInfo(minReviews)) {
			if (app != null && appMap.get(app.getAppID()) != null) {
				appMap.put(app.getAppID(), app);
				for (ReviewForAnalysis rev : reviewDB.queryReviews(app)) {
					if (app.addReview(rev) != null)
						totalReviewCount++;
				}
			}
		}
	}

	public int getTotalReviewCount() {
		return totalReviewCount;
	}

	public int applicationsNumber() {
		return appMap.size();
	}

}
