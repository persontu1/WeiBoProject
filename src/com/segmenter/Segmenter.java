package com.segmenter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.AbstractMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.sun.jna.Native;

import bean.WeiboCount;
import bean.Word;
import util.Loader;

public class Segmenter<T> implements Callable<Integer> {
	private static final String argu = "";
	private static final String system_charset = "UTF-8";
	private static final int charset_type = 1;
	private static final HashSet<String> STOP_WORDS = initSTOP_WORDS();
	private static final Pattern PAT_RESULT = Pattern.compile("(.+?)/(.+?) ");
	private final SegLibrary instance = (SegLibrary) Native.loadLibrary("NLPIR",
			SegLibrary.class);
	private static final SegLibrary sInstance = (SegLibrary) Native.loadLibrary("NLPIR",
			SegLibrary.class);
	public final int init_flag = init();
	public static final int init_flag1 = init1();
	private Semaphore sema;
	private ConcurrentLinkedQueue<T> queue;
	private LinkedHashMap<String, Entry<String, Integer>> count;
	private Class<?> cl;
	private String attrToC;
	private String lock;
	private int flag;
	private static final Loader loader = Loader.NewInstance();

	private static SegLibrary initLibrary() {
		return (SegLibrary) Native.loadLibrary("NLPIR", SegLibrary.class);
	}

	private static HashSet<String> initSTOP_WORDS() {
		HashSet<String> stops = new HashSet<String>();
		File file = new File("中文停用词表.txt");
		FileInputStream fis;
		try {
			fis = new FileInputStream(file);
			Scanner scan = new Scanner(fis);
			while (scan.hasNextLine())
				stops.add(scan.nextLine().trim());
			fis.close();
			scan.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return stops;
	}

	public Segmenter(Class<?> cl, ConcurrentLinkedQueue<T> queue, LinkedHashMap<String, Entry<String, Integer>> count,
			String lock, Semaphore sema, int flag) {
		this.sema = sema;
		this.cl = cl;
		attrToC = "weiboContent";
		this.queue = queue;
		this.count = count;
		this.lock = lock;
		this.flag = flag;
	}

	public Segmenter(String xmlSrc, ConcurrentLinkedQueue<T> queue, LinkedHashMap<String, Entry<String, Integer>> count,
			Semaphore sema) {
		this.sema = sema;
		List<Object> list = loader.getClassInfo(xmlSrc);
		cl = (Class<?>) list.get(0);
		attrToC = (String) list.get(1);
		this.queue = queue;
		this.count = count;
	}

	public Segmenter(String xmlSrc, ConcurrentLinkedQueue<T> queue, LinkedHashMap<String, Entry<String, Integer>> count,
			String lock, Semaphore sema) {
		List<Object> list = loader.getClassInfo(xmlSrc);
		cl = (Class<?>) list.get(0);
		attrToC = (String) list.get(1);
		this.queue = queue;
		this.count = count;
		this.lock = lock;
		this.sema = sema;
	}

	public Segmenter(Class<?> cl, ConcurrentLinkedQueue<T> queue, LinkedHashMap<String, Entry<String, Integer>> count) {
		this.cl = cl;
		attrToC = "weiboContent";
		this.queue = queue;
		this.count = count;
	}

	public Segmenter(String xmlSrc) {
		List<Object> list = loader.getClassInfo(xmlSrc);
		cl = (Class<?>) list.get(0);
		attrToC = (String) list.get(1);
		System.out.println(attrToC);
	}

	public Segmenter() {
		// TODO Auto-generated constructor stub
	}

	private String getWord(Object obj) {
		try {
			char[] cs = attrToC.toCharArray();
			cs[0] = (char) (cs[0] - 32);
			String temp = "get" + new String(cs);
			Method method = cl.getMethod(temp);
			String word = (String) method.invoke(obj);
			return word;
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		return null;
	}

	private void segment() {
		T ter;
		while ((ter = queue.poll()) != null) {
			String word = getWord(ter);
			count(word);
		}
		if (sema != null)
			sema.release();
	}

	public static <W extends Word> Set<W> segment(String text, W w) {
		Map<String, Entry<String, Integer>> count = new LinkedHashMap<String, Entry<String, Integer>>();
		try {
			Class.forName("com.segmenter.SegLibrary");
		} catch (ClassNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		 String result = sInstance.NLPIR_ParagraphProcess(text, 3);//test一发
//		String result = text;
		Matcher mat = PAT_RESULT.matcher(result);
		while (mat.find()) {
			String word = mat.group(1).trim();
			if (STOP_WORDS.contains(word))
				continue;
			String attr = mat.group(2).trim();
			if (!count.containsKey(word))
				count.put(word, new AbstractMap.SimpleEntry<String, Integer>(attr, 1));
			else
				count.put(word, new AbstractMap.SimpleEntry<String, Integer>(attr, count.get(word).getValue() + 1));

		}
		@SuppressWarnings("unchecked")
		Class<W> cl = (Class<W>) w.getClass();
		TreeSet<W> ts = new TreeSet<W>((o1, o2) -> o1.equals(o2) ? 0 : o2.getCount() - o1.getCount() > 0 ? 1 : -1);
		for (Entry<String, Entry<String, Integer>> en : count.entrySet()) {
			try {
				W word = cl.newInstance();
				word.setAttr(en.getValue().getKey());
				word.setCount(en.getValue().getValue());
				word.setWord(en.getKey());
				ts.add(word);
			} catch (InstantiationException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		System.out.println(ts.size());
		return ts;
	}

	private void count(String text) {
		String result;
		if (flag == 0)
			result = instance.NLPIR_ParagraphProcess(text, 3);
		else
			result = text;
		Matcher mat = PAT_RESULT.matcher(result);
		while (mat.find()) {
			String word = mat.group(1).trim();
			if (STOP_WORDS.contains(word))
				continue;
			String attr = mat.group(2).trim();
			synchronized (count) {
				if (!count.containsKey(word))
					count.put(word, new AbstractMap.SimpleEntry<String, Integer>(attr, 1));
				else
					count.put(word, new AbstractMap.SimpleEntry<String, Integer>(attr, count.get(word).getValue() + 1));
			}
			synchronized (lock) {
			}
		}
	}

	public void count() {

	}

	private int init() {
		try {
			return instance.NLPIR_Init(argu.getBytes(system_charset), charset_type, "0".getBytes(system_charset));
		} catch (UnsupportedEncodingException e) {
			return 0;
		}
	}

	private static int init1() {
		try {
			int i = sInstance.NLPIR_Init(argu.getBytes(system_charset), charset_type, "0".getBytes(system_charset));
			if (i == 0) {
				String s = sInstance.NLPIR_GetLastErrorMsg();
				System.out.println(s);
			}
			return i;
		} catch (UnsupportedEncodingException e) {
			return 0;
		}
	}

	public static void main(String[] args) {
		Set<Word> set = segment("知无不言，言无不尽", new WeiboCount());
		for (Word word : set)
			System.out.println(word.getWord());
		// new Segmenter("Test.xml");
		// Segmenter seg = new Segmenter("Test.xml");
		// Class<?> cl = seg.getClass();
		// try {
		// Method f = cl.getDeclaredMethod("count");
		// f.setAccessible(true);
		// f.invoke(seg);
		// } catch (SecurityException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// } catch (IllegalArgumentException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// } catch (NoSuchMethodException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// } catch (IllegalAccessException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// } catch (InvocationTargetException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// System.out.println(STOP_WORDS.contains("集思广益"));

	}

	@Override
	public Integer call() throws Exception {
		// TODO Auto-generated method stub
		segment();
		return null;
	}
}
