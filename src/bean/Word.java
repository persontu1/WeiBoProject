package bean;

public abstract class Word {
	private String word;
	private String attr;
	private Integer count;

	public String getWord() {
		return word;
	}

	public void setWord(String word) {
		this.word = word;
	}

	public String getAttr() {
		return attr;
	}

	public void setAttr(String attr) {
		this.attr = attr;
	}

	public Integer getCount() {
		return count;
	}

	public void setCount(Integer count) {
		this.count = count;
	}

	public int hashCode() {
		return word.hashCode();
	}

	public boolean equals(Object wc) {
		if (wc instanceof Word)
			return word.equals(((Word) wc).getWord());
		return false;
	}

	public String toString() {
		return word + " " + attr + " " + count;
	}
}
