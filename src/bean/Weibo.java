package bean;

import java.sql.Timestamp;

public class Weibo {
	private String onick;
	private String page_id;
	private String url;
	private String weibo_id;
	private String weibo_url;
	private String weiboContent;
	private Timestamp date;
	private String topic = "-1";
	private Integer checked = 0;

	public Weibo() {
	}

	public String getOnick() {
		return onick;
	}

	public void setOnick(String onick) {
		this.onick = onick;
	}

	public String getPage_id() {
		return page_id;
	}

	public void setPage_id(String page_id) {
		this.page_id = page_id;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getWeiboContent() {
		return weiboContent;
	}

	public void setWeiboContent(String weiboContent) {
		this.weiboContent = weiboContent;
	}

	public Timestamp getDate() {
		return date;
	}

	public void setDate(Timestamp date) {
		this.date = date;
	}

	public Integer getChecked() {
		return checked;
	}

	public void setChecked(Integer checked) {
		this.checked = checked;
	}

	public String getTopic() {
		return topic;
	}

	public void setTopic(String topic) {
		this.topic = topic;
	}

	public String getWeibo_id() {
		return weibo_id;
	}

	public void setWeibo_id(String weibo_id) {
		this.weibo_id = weibo_id;
	}

	public String getWeibo_url() {
		return weibo_url;
	}

	public void setWeibo_url(String weibo_url) {
		this.weibo_url = weibo_url;
	}
}
