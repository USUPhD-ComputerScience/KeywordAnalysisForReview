package managers;

import java.io.PrintWriter;
import java.io.Serializable;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import model.Application;
import model.ReviewForAnalysis;

public class ApplicationManager {

	private static ApplicationManager instance = null;
	private Map<String, Application> appMap;

	public Collection<Application> getAppSet() {
		return appMap.values();
	}

	public static synchronized ApplicationManager getInstance() {
		if (instance == null) {
			instance = new ApplicationManager();
		}
		return instance;
	}

	private ApplicationManager() {
		appMap = new HashMap<>();
	}

	public List<Application> getAppList() {
		List<Application> list = new ArrayList<Application>(appMap.values());
		return list;
	}

	public void addApp(String appid) throws SQLException {
		Application app = appMap.get(appid);
		if (app == null) {
			ReviewDB reviewDB = ReviewDB.getInstance();
			app = reviewDB.querySingleAppInfo(appid);
			if (app != null) {
				appMap.put(appid, app);
			}
		}

	}

	public void removeApp(String id) {
		appMap.remove(id);
	}

	public void addApp(Application app) {
		appMap.put(app.getAppID(), app);
	}

	public void writeTrainingDataToFile(PrintWriter fileWriter) {
		for (Entry<String, Application> app : appMap.entrySet()) {
			app.getValue().writeTrainingDataToFile(fileWriter);
		}
	}

	public void addWholeCorpus(int minReviews) throws SQLException {
		ReviewDB reviewDB = ReviewDB.getInstance();

		for (Application app : reviewDB.queryMultipleAppsInfo(minReviews)
				.getAppList()) {
			if (app != null && appMap.get(app.getAppID()) != null) {
				appMap.put(app.getAppID(), app);
			}
		}
	}

	public int applicationsNumber() {
		return appMap.size();
	}

	public Application getApp(String appid) {
		return appMap.get(appid);
	}

}
