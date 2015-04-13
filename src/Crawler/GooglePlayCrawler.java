package Crawler;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import model.ReviewForCrawler;

import com.akdeniz.googleplaycrawler.GooglePlay.GetReviewsResponse;
import com.akdeniz.googleplaycrawler.GooglePlay.ReviewResponse;
import com.akdeniz.googleplaycrawler.GooglePlayAPI;
import com.akdeniz.googleplaycrawler.GooglePlayAPI.REVIEW_SORT;

public class GooglePlayCrawler {
	private GooglePlayAPI service;

	public GooglePlayCrawler(String login, String password, String androidID) {
		this.service = new GooglePlayAPI(login, password, androidID);
		try {
			service.login();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public String getVersion(String appid) {
		String ver = "unknown";
		try {
			ver = service.details(appid).getDocV2().getDetails()
					.getAppDetails().getVersionString();
			TimeUnit.SECONDS.sleep(1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return ver;
	}

	public int getNumberOfDownload(String appid) {
		String numd = "0";
		try {
			numd = service.details(appid).getDocV2().getDetails()
					.getAppDetails().getNumDownloads();
			numd = numd.replaceAll("[^0-9]", "");
			TimeUnit.SECONDS.sleep(1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return Integer.parseInt(numd);
	}

	public String getName(String appid) {
		String name = "unknown";
		try {
			TimeUnit.SECONDS.sleep(1);
			name = service.details(appid).getDocV2().getTitle();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			if (!e.getMessage().contains("Item not found"))
				e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return name;
	}

	public long getUploadDate(String appid) {
		String versionDate;
		long uploadDate = 0;
		try {
			versionDate = service.details(appid).getDocV2().getDetails()
					.getAppDetails().getUploadDate();
			SimpleDateFormat f = new SimpleDateFormat("MMM dd,yyyy");
			Date date = (Date) f.parse(versionDate);
			uploadDate = date.getTime();
			TimeUnit.SECONDS.sleep(1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return uploadDate;
	}

	public String getDescription(String appid) {
		StringBuilder desc = new StringBuilder();
		try {
			desc.append(service.details(appid).getDocV2().getDescriptionHtml()
					.replaceAll("&quot;", "").replaceAll("<br>", "\n"));
			desc.append("\n");
			desc.append(service.details(appid).getDocV2().getDetails()
					.getAppDetails().getRecentChangesHtml()
					.replaceAll("&quot;", "").replaceAll("<br>", "\n"));

			TimeUnit.SECONDS.sleep(1);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return desc.toString();
	}

	public boolean getReviewsForApp(String appid,
			List<ReviewForCrawler> reviewList, REVIEW_SORT sort) {

		int start = 0;
		boolean stop = false;
		int failAttempts = 0;
		while (!stop) {
			try {
				TimeUnit.SECONDS.sleep(1);
				ReviewResponse reviews;
				reviews = service.reviews(appid, sort, start, 20);
				String url = reviews.getNextPageUrl();
				if (url.length() == 0)
					stop = true;
				start = start + 20;
				GetReviewsResponse response = reviews.getGetResponse();
				for (com.akdeniz.googleplaycrawler.GooglePlay.Review review : response
						.getReviewList()) {
					ReviewForCrawler.ReviewBuilder reviewBuilder = new ReviewForCrawler.ReviewBuilder();
					reviewBuilder.title(review.getTitle());
					reviewBuilder.text(review.getComment());
					reviewBuilder.reviewId(review.getCommentId());
					reviewBuilder.deviceName(review.getDeviceName());
					reviewBuilder.documentVersion(review.getDocumentVersion());
					reviewBuilder.rating(review.getStarRating());
					reviewBuilder.creationTime(review.getTimestampMsec());
					reviewList.add(reviewBuilder.createReview());
				}
				failAttempts = 0;
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				failAttempts++;
				if (failAttempts == 10)
					return false;
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return true;
	}

}
