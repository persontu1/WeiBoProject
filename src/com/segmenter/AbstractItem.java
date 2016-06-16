package com.segmenter;

public abstract class AbstractItem {
	private String word_attr;
	private String word;
	private int count;
	private int total;

	public String getWord_attr() {
		return word_attr;
	}

	public void setWord_attr(String word_attr) {
		this.word_attr = word_attr;
	}

	public String getWord() {
		return word;
	}

	public void setWord(String word) {
		this.word = word;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public int getTotal() {
		return total;
	}

	public void setTotal(int total) {
		this.total = total;
	}

}
