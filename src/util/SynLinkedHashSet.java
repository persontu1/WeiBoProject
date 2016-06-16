package util;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Spliterator;
import java.util.regex.Pattern;

import org.jsoup.Connection.Response;

import com.crawler.ParsingPage;

public class SynLinkedHashSet<E> extends java.util.LinkedHashSet<E> {
	private LinkedHashSet<String> d = new LinkedHashSet<String>();
	{

	}

	public synchronized Spliterator<E> spliterator() {
		return super.spliterator();
	}

	public synchronized boolean add(E e) {
		return super.add(e);
	}

	public SynLinkedHashSet() {
		super();
	}

	public SynLinkedHashSet(int initialCapacity, float loadFactor) {
		super(initialCapacity, loadFactor);
	}

	public SynLinkedHashSet(int initialCapacity) {
		super(initialCapacity);
	}

	public static void main(String[] args) {
		ParsingPage pp = new ParsingPage("", "");
		// Response res = pp
		// .getResponse("http://weibo.com/p/1005052477339061/follow?from=page_100505&wvr=6&mod=headfollow#place");
		// Map<String, String> map = pp.getPageInfo(res);
		// String body = pp.trim(res.body());
		// pp.outputPage(body, "F:\\毕设\\wirshark抓包数据\\page.html");
		try {
			BufferedReader bf = new BufferedReader(
					new InputStreamReader(new FileInputStream("F:\\毕设\\wirshark抓包数据\\xxx.txt")));
			StringBuffer sb = new StringBuffer();
			String temp;
			while ((temp = bf.readLine()) != null)
				sb.append(temp);
			String body = sb.toString();
			String re = (String) pp.getLines(body, "<div class=\"WB_from S_txt2\">(.+?)<a(.+?)>(.+?)</a>", 3, 1,
					new String());
			System.out.println(re.trim());
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	static class T implements Runnable {
		private Set<String> set;
		private int n;

		public T(Set<String> set, int n) {
			this.set = set;
			this.n = n;
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			if (n % 2 == 0) {
				set.add(n + "");
				System.out.println("add");
			} else {
				// set.remove(o)
				synchronized (set) {
					Iterator<String> it = set.iterator();
					if (it.hasNext())
						it.remove();
					System.out.println("remove");
				}

			}
		}

	}
}
