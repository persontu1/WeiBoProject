package com.crawler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Connection.Response;

import util.Native2AsciiUtils;

public class Test<T> extends java.util.LinkedList<T> {

	public Test() {
		super();
	}

	public synchronized T get(int i) {
		return super.get(i);
	}

	public synchronized T getFirst() {
		return super.getFirst();
	}

	public synchronized T getLast() {
		return super.getLast();
	}

	public synchronized boolean add(T t) {
		return super.add(t);
	}

	public synchronized T remove() {
		return super.remove();
	}

	private static ConcurrentLinkedQueue<String> test = new ConcurrentLinkedQueue<String>();

	public static void main(String[] args) {
		// try {
		// Scanner scan = new Scanner(new FileInputStream("tt.html"));
		// StringBuffer sb = new StringBuffer();
		// while (scan.hasNext())
		// sb.append(scan.nextLine());
		// ParsingPage pp = new ParsingPage("", "");
		// Response res =
		// pp.getResponse("http://weibo.com/3591355593/CrF4s7ecG");
		// pp.getPageInfo(res);
		// try {
		// FileInputStream fr = new FileInputStream("Test.html");
		// StringBuffer sb = new StringBuffer();
		// byte[] b = new byte[1024];
		// while (fr.read(b) != -1)
		// sb.append(new String(b));
		// pp.getWeibo(sb.toString(), "");
		// } catch (FileNotFoundException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// } catch (IOException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		Pattern pat = Pattern.compile("(.+?):(.+?)");
		Matcher mat = pat.matcher("123123:34");
		if (mat.find())
			System.out.println(mat.group(1));
		File f = new File("save2/dg.properties");
		try {
			f.createNewFile();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// pp.getWeibo(sb.toString(), "");
		// } catch (FileNotFoundException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// Pattern pat = Pattern.compile("(.+?)c(\\s*|\t|\r|\n)f(.+?)d");
		// Matcher mat = pat.matcher("rc \nfsf三观d");
		// while (mat.find())
		// System.out.println(mat.group(1));
		// for (int i = 0; i < 2; i++)
		// new Thread(new Runnable() {
		//
		// @Override
		// public void run() {
		// // TODO Auto-generated method stub
		// while (true) {
		// // synchronized (test) {
		// test.add("1");
		// // }
		// System.out.println("线程 " + test);
		// try {
		// Thread.sleep(1000);
		// } catch (InterruptedException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
		// }
		// }
		// }).start();
		// Scanner scan = new Scanner(System.in);
		// synchronized (test) {
		// scan.hasNext();
		// }
		// scan.close();

	}
}

class s implements Runnable {

	private int i;
	private Test<String> t;

	public s(int i, Test<String> t) {
		this.i = i;
		this.t = t;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		if (i % 2 == 0) {
			t.add(i + "");
			System.out.println("success");
		} else {
			t.remove();
			System.out.println("fail");
		}
	}
}
