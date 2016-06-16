package com.crawler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

public class HttpC {

	public static void main(String[] args) {
		try {
			Response res = TE.getResponse("http://weibo.com/wuxymsn?is_hot=1");
			File file = new File("test1.html");
			if (!file.exists()) {
				file.createNewFile();
			} else {
				file.delete();
				file = new File("test1.html");
			}

			File file1 = new File("test1_result.html");
			if (!file1.exists()) {
				file1.createNewFile();
			} else {
				file1.delete();
				file1 = new File("test1_result.html");
			}
			FileOutputStream os = new FileOutputStream(file);
			os.write(res.body().getBytes());
			os.close();
			// System.out.println(Native2AsciiUtils.ascii2Native(res.body()));
			Pattern pat = Pattern.compile("\"html\":\"(.+?)\"}");
			// Pattern pat = Pattern.compile("<script>FM.view(.+?)</script>");
			String s = res.body();
			Matcher matcher = pat.matcher(s);
			int flag = 0;
			os = new FileOutputStream(file1);
			while (matcher.find()) {
				// if (flag == 1) {
				Pattern pat2 = Pattern.compile("\\\\n|\\\\t|\\\\r|\\\\");
				Matcher matcher1 = pat2.matcher(matcher.group(1));
				String string = matcher1.replaceAll("");
				System.out.println(string);
				os.write(string.getBytes());

				// break;
				// }
				// flag++;
			}
//			ParsingPage.getWeibos(res);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
