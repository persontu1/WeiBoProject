package com.crawler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Cookie {

	public static void main(String[] args) {
		
	}
	
	public static Map<String,String> get(){
		File file = new File("G:\\毕设\\wirshark抓包数据\\新浪微博登录包.txt");
		FileInputStream is;
		try {
			is = new FileInputStream(file);
			byte[] buf = new byte[1024 * 2];
			is.read(buf);
			System.out.println(new String(buf));
			String text = new String(buf);
			Pattern pat = Pattern.compile("(.+?)=(.+?);");
			Matcher mat = pat.matcher(text);
			Map<String, String> map = new HashMap<>();
			while (mat.find()) {
				map.put(mat.group(1), mat.group(2));
			}
			is.close();
			return map;
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return new HashMap<String,String>();
	}
}
