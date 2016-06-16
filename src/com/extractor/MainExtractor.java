package com.extractor;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.TreeSet;

import com.segmenter.Segmenter;

import action.Action;

import java.util.Set;
import bean.WeiboCount;
import bean.Word;

public class MainExtractor extends Action {
	private static Map<String, List<Set<Word>>> articles = initArticle();
	// private static Map<String, List<Map<Entry<String, String>, Integer>>> chi
	// = initChI();
	private static final String articlesLocation = "articles";
	private static final int c = 3;
	private static Comparator<Entry<Double, Entry<Entry<String, String>, Integer>>> com = new Comparator<Entry<Double, Entry<Entry<String, String>, Integer>>>() {
		@Override
		public int compare(Entry<Double, Entry<Entry<String, String>, Integer>> o1,
				Entry<Double, Entry<Entry<String, String>, Integer>> o2) {
			return (o1.getKey() - o2.getKey()) > 0 ? -1 : ((o1.getKey() - o2.getKey()) == 0 ? 0 : 1);
		}
	};
	// private static final DB db = new DB();

	private static Map<String, List<Set<Word>>> initArticle() {
		Map<String, List<Set<Word>>> article = new LinkedHashMap<String, List<Set<Word>>>();
		File file = new File(articlesLocation);
		for (File f : file.listFiles()) {
			List<Set<Word>> list = new ArrayList<Set<Word>>();
			for (File f1 : f.listFiles()) {
				try {
					Scanner scan = new Scanner(f1);
					StringBuffer sb = new StringBuffer();
					while (scan.hasNextLine())
						sb.append(scan.nextLine());
					scan.close();
					Set<Word> map = Segmenter.segment(sb.toString(), new WeiboCount());
					list.add(map);
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
			article.put(f.getName(), list);
		}
		return article;
	}

	// private static Map<String, List<Map<Entry<String, String>, Integer>>>
	// initChI() {
	// Map<String, List<Map<Entry<String, String>, Integer>>> CHI = new
	// LinkedHashMap<String, List<Map<Entry<String, String>, Integer>>>();
	// for (Entry<String, List<Map<Entry<String, String>, Integer>>> type :
	// articles.entrySet()) {
	// List<Map<Entry<String, String>, Integer>> list = new
	// ArrayList<Map<Entry<String, String>, Integer>>();
	// for (Map<Entry<String, String>, Integer> art : type.getValue()) {
	// TreeSet<Entry<Double, Entry<Entry<String, String>, Integer>>> sorted =
	// new TreeSet<Entry<Double, Entry<Entry<String, String>, Integer>>>(
	// com);
	// for (Entry<Entry<String, String>, Integer> word : art.entrySet()) {
	// int a, b, c, d;
	// a = aNum(type, word);
	// b = bNum(type, word);
	// c = cNum(type, word);
	// d = dNum(type, word);
	// double chi = ((double) (a * d - b * c) * (a * d - b * c)) / ((a + c) * (b
	// + d) * (a + b) * (c + d));
	// sorted.add(entry(chi, word));
	// }
	// int count = 0;
	// Map<Entry<String, String>, Integer> art1 = new
	// LinkedHashMap<Entry<String, String>, Integer>();
	// for (Entry<Double, Entry<Entry<String, String>, Integer>> sort : sorted)
	// {
	// if (!((count++) == 3)) {
	// Entry<Entry<String, String>, Integer> word = sort.getValue();
	// art1.put(word.getKey(), word.getValue());
	// }
	// }
	// list.add(art1);
	// }
	// }
	// return CHI;
	// }

	private static <T, D> Entry<T, D> entry(T t, D d) {
		return new AbstractMap.SimpleEntry<T, D>(t, d);
	}

	public Set<Entry<Double, Word>> calculateCHI(Set<Word> words) {
		Set<Entry<Double, Word>> art1 = new TreeSet<Entry<Double, Word>>(
				(o1, o2) -> (o2.getKey() - o2.getKey() > 0 ? 1 : -1));
		for (Entry<String, List<Set<Word>>> type : articles.entrySet()) {
			TreeSet<Entry<Double, Word>> sorted = new TreeSet<Entry<Double, Word>>(
					(o1, o2) -> (o2.getKey() - o2.getKey() > 0 ? 1 : -1));
			for (Word word : words) {
				int a = 0, b = 0, c = 0, d = 0;
				a = aNum(type, word);
				b = bNum(type, word);
				c = cNum(type, word);
				d = dNum(type, word);
				double chi = ((double) (a * d - b * c) * (a * d - b * c)) / ((a + c) * (b + d) * (a + b) * (c + d));
				sorted.add(entry(chi, word));
			}
			int count = 0;
			for (Entry<Double, Word> sort : sorted)
				if (!((count++) == c))
					art1.add(sort);
		}
		return art1;
	}

	private static int aNum(Entry<String, List<Set<Word>>> type, Word word) {
		int a = 0;
		for (Set<Word> article : type.getValue())
			if (article.contains(word))
				a++;
		return a;
	}

	private static int bNum(Entry<String, List<Set<Word>>> type, Word word) {
		int b = 0;
		for (Entry<String, List<Set<Word>>> type1 : articles.entrySet())
			if (!type.getKey().equals(type.getKey()))
				for (Set<Word> article : type1.getValue())
					if (article.contains(word))
						b++;
		return b;
	}

	private static int cNum(Entry<String, List<Set<Word>>> type, Word word) {
		int c = 0;
		for (Set<Word> article : type.getValue())
			if (!article.contains(word))
				c++;
		return c;
	}

	private static int dNum(Entry<String, List<Set<Word>>> type, Word word) {
		int d = 0;
		for (Entry<String, List<Set<Word>>> type1 : articles.entrySet())
			if (!type.getKey().equals(type.getKey()))
				for (Set<Word> article : type1.getValue())
					if (!article.contains(word))
						d++;
		return d;
	}

	public static void main(String[] args) {
		// System.out.println(articles.size());
		// System.out.println();
		Set<Word> map = Segmenter.segment("由此你可以算出银河系的总质量。", new WeiboCount());
		System.out.println(map.size());
		for (Word d : map) {
			System.out.println(d.getWord() + ":" + d.getAttr() + "   " + d.getCount());
		}
		TreeSet<Word> t = new TreeSet<Word>((o1, o2) -> o1.equals(o2) ? 0 : o2.getCount() - o1.getCount() > 0 ? 1 : -1);
		Word d = new WeiboCount();
		d.setWord("fs");
		d.setCount(1);
		Word d1 = new WeiboCount();
		d1.setWord("fs");
		d1.setCount(1);
		System.out.println(t.add(d));
		System.out.println(t.add(d1));
		System.out.println(t.size());
		Map<Entry<String, String>, Integer> map1 = new LinkedHashMap<Entry<String, String>, Integer>();
		Entry<String, String> en1 = new AbstractMap.SimpleEntry<String, String>("1", "1");
		Entry<String, String> en2 = new AbstractMap.SimpleEntry<String, String>("1", "1");
		map1.put(en2, 1);
		map1.put(en2, 2);
		// System.out.println(map1.size());
	}

	@Override
	public Object action(String act) {
		// TODO Auto-generated method stub
		return null;
	}
}
