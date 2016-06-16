package bean;

public class Unfinish {
	private String url;
	private String onick;

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getOnick() {
		return onick;
	}

	public void setOnick(String onick) {
		this.onick = onick;
	}

	public void test() {
		System.out.println(this.getClass().getClassLoader());
	}
}
