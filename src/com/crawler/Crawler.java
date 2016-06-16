package com.crawler;

import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.locks.Lock;

import org.jsoup.Connection.Response;

/**
 * ҳ����ȡ�߳�ʵ�֡�
 * 
 * @author ��������ҵ��ѧ-12�����ѧԺ-������
 * @Time 2016-06-14 15:02:29
 *
 */
public class Crawler implements Callable<Integer> {
	/**
	 * ��ʼurl
	 */
	private String baseUrl;
	private int page;
	private String url;
	private ParsingPage pp;
	/**
	 * �����ⲿ������ȡ���̵���ͣ��ָ�
	 */
	private Lock lock;
	private static final String ONICK = "onick";
	private static final String PAGE_EQUEAL = "&page=";
	private Entry<String, Integer> pageID_page;

	public Crawler(String baseUrl, int page, ParsingPage pp, Entry<String, Integer> pageID_page) {
		this.page = page;
		this.baseUrl = baseUrl;
		url = baseUrl + PAGE_EQUEAL + page;
		this.pp = pp;
		this.pageID_page = pageID_page;
	}

	public void retrieve() {
		pageID_page.setValue(page);
		Response res = pp.getResponse(url);
		Map<String, String> map = pp.getPageInfo(res);
		String body = pp.trim(res.body());
		pp.getWeibos(body, map.get(ONICK), page);
	}

	@Override
	public Integer call() throws Exception {
		retrieve();
		return 0;
	}

	static class LessThanOneException extends Exception {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

	}

	public static int doing(int i) throws LessThanOneException {
		if (i < 1)
			throw new LessThanOneException();
		return i;
	}

	public static int doOther(int i) {
		try {
			return doing(i);
		} catch (LessThanOneException e) {
			// TODO Auto-generated catch block
			System.out.println(i);
			return doOther(i + 1);
		}
	}

	public static void main(String[] args) {
		System.out.println(doOther(-4));
	}
}
