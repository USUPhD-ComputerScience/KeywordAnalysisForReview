package model;

public class ReviewForCrawler {

	/**
		 * 
		 */
	private String text;
	private int rating;
	private String title;
	private String deviceName;
	private String documentVersion;
	private long creationTime;
	private String reviewId; // commentID and VersionID

	private boolean duplicate; // only for crawler

	public boolean isDuplicate() {
		return duplicate;
	}

	public void setDuplicate(boolean duplicate) {
		this.duplicate = duplicate;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public int getRating() {
		return rating;
	}

	public void setRating(int rating) {
		this.rating = rating;
	}

	public String getTitle() {
		return title;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	public String getDevice_name() {
		return deviceName;
	}

	public void setDevice_name(String device_name) {
		this.deviceName = device_name;
	}

	public String getDocument_version() {
		return documentVersion;
	}

	public void setDocument_version(String document_version) {
		this.documentVersion = document_version;
	}

	public long getCreationTime() {
		return creationTime;
	}

	public String getReviewId() {
		return reviewId;
	}

	public void setReviewId(String reviewId) {
		this.reviewId = reviewId;
	}

	public ReviewForCrawler(String text, int rating, String title,
			String deviceName, String documentVersion, long creationTime,
			String reviewId, boolean afterUpdate) {
		setText(text);
		setRating(rating);
		setTitle(title);
		setDevice_name(deviceName);
		setDocument_version(documentVersion);
		setCreationTime(creationTime);
		setReviewId(reviewId);
		setDuplicate(false);
	}

	public void setCreationTime(long creationTime) {
		this.creationTime = creationTime;
	}

	public static class ReviewBuilder {
		private String nestedText;
		private int nestedRating;
		private String nestedTitle;
		private String nestedDeviceName;
		private String nestedDocumentVersion;
		private long nestedCreationTime;
		private String nestedReviewId;
		private boolean nestedIsAfterUpdate;

		public ReviewBuilder() {
			nestedText = null;
			nestedRating = 0;
			nestedTitle = null;
			nestedDeviceName = null;
			nestedDocumentVersion = null;
			nestedCreationTime = 0;
			nestedReviewId = null;
		}

		public ReviewBuilder text(String text) {
			this.nestedText = text;
			return this;
		}

		public ReviewBuilder rating(int rating) {
			this.nestedRating = rating;
			return this;
		}

		public ReviewBuilder title(String title) {
			this.nestedTitle = title;
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
			this.nestedReviewId = reviewID;
			return this;
		}

		public ReviewBuilder isAfterUpdate(boolean isAfterUpdate) {
			this.nestedIsAfterUpdate = isAfterUpdate;
			return this;
		}

		public ReviewForCrawler createReview() {
			return new ReviewForCrawler(nestedText, nestedRating, nestedTitle,
					nestedDeviceName, nestedDocumentVersion,
					nestedCreationTime, nestedReviewId, nestedIsAfterUpdate);
		}

	}

}
