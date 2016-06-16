package com.crawler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;

public class TE {
	public static Response getResponse(String url) {
		File file = new File("F:\\毕设\\wirshark抓包数据\\新浪微博登录包.txt");
		try {
			FileInputStream is = new FileInputStream(file);
			byte[] buf = new byte[1024 * 2];
			is.read(buf);
			// System.out.println(new String(buf));
			String text = new String(buf);
			Pattern pat = Pattern.compile("(.+?)=(.+?);");
			Matcher mat = pat.matcher(text);
			Map<String, String> map = new HashMap<>();
			while (mat.find()) {
				System.out.println(mat.group(1).trim() + " = "
						+ mat.group(2).trim());
				map.put(mat.group(1).trim(), mat.group(2).trim());
			}
			Response res = Jsoup.connect(url).cookies(map).method(Method.GET)
					.execute();
			return res;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public static void main(String[] args) {
		try {
			String ns = new String("\u4e0b\u9875\u541b".getBytes("unicode"),
					"GBK");
			System.out.println(ns);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// // Response res = getResponse("http://weibo.com");
		// // System.out.println(res.body());
		//
		// Context cx = Context.enter();
		// Scriptable scope = cx.initStandardObjects();
		// String script = "";
		// File file = new File("F:\\毕设\\wirshark抓包数据\\js.js");
		// try {
		// BufferedReader br = new BufferedReader(new FileReader(file));
		// String s = "";
		// while ((s = br.readLine()) != null) {
		// script += s + "\n";
		// }
		// cx.evaluateString(scope, script, file.getName(), 1, null);
		// // System.out.println(script);
		// } catch (FileNotFoundException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// } catch (IOException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
	}
}
