package com.crawler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;
import org.jsoup.Connection.Response;

import com.google.gson.Gson;

import action.Action;
import util.DB;
import bean.Link;
import bean.Node;
import bean.Relation;
import bean.Unfinish;
import bean.User;
import bean.Weibo;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

/**
 * ʵ����Action��������΢����ȡ����ʵ����action��ҵ���߼����е���ȡģ�����Ҫ��������ģ����ϵͳ��ӵ�һ������
 * 
 * @author ��������ҵ��ѧ-12�����ѧԺ-������
 * @Time 2016-06-14 15:02:29
 *
 */
public class MainCrawler extends Action {
	/**
	 * ��save�����в�����������properties�ļ��Ĳ������������Ϊ��cc.propertiesΪ��������properties�ļ�
	 */
	private static volatile int cc = 0;
	/**
	 * ����ȡ���У�����Ԫ�ص����ݽṹ�����˴���ȡ�������ǳƺ���ҳurl,��ϸ�μ�Unfinish.java�ļ�
	 */
	private final ConcurrentLinkedQueue<Unfinish> queue = new ConcurrentLinkedQueue<Unfinish>();
	/**
	 * ������ȡ��queue���еģ��̻߳�δ�Ը�Ԫ�ؽ�����ȡ΢���Ĳ�����Ԫ�أ���Ԫ�ؿ�ʼ������ȡ΢��ʱ����Ԫ���Ƴ��ö��С�
	 * ������ض����Ե�Ԫ����Ϊ����Ͻ�����δ�����UnfinishԪ�ض��ݴ�����ݿ��еĲ�����
	 */
	private final ConcurrentLinkedQueue<Unfinish> queue_temp = new ConcurrentLinkedQueue<Unfinish>();
	/**
	 * ��Ͻ�����δ�����UnfinishԪ�ض��ݴ�����ݿ��еĲ��������������ݽṹ��
	 */
	private ConcurrentHashMap<String, Integer> rightNow = new ConcurrentHashMap<String, Integer>();
	/**
	 * ��Ͻ�����δ�����UnfinishԪ�ض��ݴ�����ݿ��еĲ��������������ݽṹ��
	 */
	private ConcurrentLinkedQueue<ParsingPage> pList = new ConcurrentLinkedQueue<ParsingPage>();
	/**
	 * ��ȡ����΢�����У�ÿ����һ��΢�������ö���
	 */
	private ConcurrentLinkedQueue<Weibo> weiboQueue = new ConcurrentLinkedQueue<Weibo>();
	/**
	 * ��ȡ����������ϸ��Ϣ����
	 */
	private ConcurrentLinkedQueue<User> userQueue = new ConcurrentLinkedQueue<User>();
	/**
	 * �̳߳����������������ȡ����΢���ľ���2-4����ȡ�߳����㹻��ȡ����΢�����߳��ٶ�����΢���ͻ��ip��ֹ����
	 */
	private static final Integer THREAD_NUM = 2;
	/**
	 * �����µ������߳̾��ύ�����̳߳���ȴ�ִ��
	 */
	private final static ExecutorService POOL = Executors.newFixedThreadPool(THREAD_NUM);
	/**
	 * ��������ص��̳߳�
	 */
	private final static ExecutorService POOL_PAGE = Executors.newFixedThreadPool(THREAD_NUM);
	/**
	 * ��־��
	 */
	public final static Logger logger = Logger.getLogger(MainCrawler.class);
	/**
	 * ��ȡ����΢����עҳ���������ʽ
	 */
	private static final Pattern PAT_FOLLOW = Pattern.compile(
			"<div\\sclass=\"info_name\\sW_fb\\sW_f14\"><a class=\"S_txt1\" target=\"_blank\"(.+?)href=\"(.+?)\"(.*?)>(.+?)</a>");
	/**
	 * ����Ƿ��v��������ʽ
	 */
	private static final Pattern PAT_V = Pattern.compile(
			"<div class=\"PCD_header\">(.+?)<a(.+?)href=\"http://verified.weibo.com/verify\"(.+?)>(.+?)(>\u5173\u6ce8<)(.+?)(>\u79c1\u4fe1<)");
	/**
	 * ����΢����ͨ��ǰ׺
	 */
	private static final String URL_PART1 = "http://weibo.com/p/";
	/**
	 * ��עҳ���ͨ�ø�ʽ
	 */
	private static final String URL_PART2 = "/follow?from=";
	/**
	 * ��עҳ����ͨ�ø�ʽ
	 */
	private static final String URL_PART3 = "/follow?page=";
	private static final String PAGE_ID = "page_id";
	private static final String PID = "pid";
	private static final String PAT_COUNT1 = "<a page-limited=\"true\" class=\"page S_txt1\" href=\"(.+?)\">(.+?)</a>";
	private static final String PAT_COUNT2 = "<a action-type=\"page\" class=\"page S_txt1\" href=\"(.+?)\">(.+?)</a>";
	/**
	 * Ϊ������������ʱ���ڴ��ռ�����������մ������漰����ҳ����ȡ��������ֻʹ�ø�ParsingPage����
	 */
	private ParsingPage pp = new ParsingPage("", "");
	/**
	 * ������������json��ʽ����
	 */
	private static final String optionS = "{\"tooltip\":{},\"toolbox\":{\"show\":true,\"feature\":{\"dataView\":{\"show\":true,\"readOnly\":false},\"restore\":{\"show\":true},\"saveAsImage\":{\"show\":true}}},\"animationDuration\":1500,\"animationEasingUpdate\":\"quinticInOut\"}";
	/**
	 * MainCrawler�����ʱ����������־������
	 */
	static {
		PropertyConfigurator.configure("D:/WeiBoProject/log4j.properties");
		// try {
		// Class.forName("com.crawler.Crawler", true,
		// MainCrawler.class.getClassLoader());
		// } catch (ClassNotFoundException e) {
		// // TODO Auto-generated catch block
		// e.printStackTrace();
		// }
	}

	/**
	 * Ĭ���޲ι����������ڷ���ʵ�����޲ζ���
	 */
	public MainCrawler() {

	}

	/**
	 * �вι�����
	 */
	public MainCrawler(String seed, String lock, ConcurrentLinkedQueue<ParsingPage> pList,
			ConcurrentLinkedQueue<Weibo> weiboQueue, ConcurrentLinkedQueue<User> userQueue) {
		Unfinish un = new Unfinish();
		un.setUrl(seed);
		ParsingPage p = new ParsingPage("", "");
		un.setOnick(p.getPageInfo(p.getResponse(seed)).get("onick"));
		queue.add(un);
		pp.setLock(lock);
		this.pList = pList;
		this.weiboQueue = weiboQueue;
		this.userQueue = userQueue;
	}

	/**
	 * �вι�����
	 * 
	 * @param lock
	 *            �ϵ�����
	 * @param pList
	 *            ����pList
	 * @param weiboQueue
	 *            ����΢������
	 */
	public MainCrawler(String lock, ConcurrentLinkedQueue<ParsingPage> pList, ConcurrentLinkedQueue<Weibo> weiboQueue) {
		pp.setLock(lock);
		this.pList = pList;
		this.weiboQueue = weiboQueue;
	}

	@Deprecated
	public synchronized Unfinish getFirst() {
		Unfinish unfin;
		if ((unfin = queue.poll()) != null)
			return unfin;
		return null;
	}

	/**
	 * �����ݿ��properties�ļ��лָ��ϴ����еĶϵ�
	 * 
	 * @param flag
	 */
	public void start(boolean flag) {
		new DB<Unfinish>().retrieve(queue, Unfinish.class);
		try {
			File file = new File("save1");
			File[] files = file.listFiles();
			for (File f : files) {
				FileInputStream fis = new FileInputStream(f);
				Properties p = new Properties();
				p.load(fis);
				String page_id = p.getProperty("page_id");
				String onick = p.getProperty("onick");
				int page1 = Integer.parseInt(p.getProperty("page"));
				int max = Integer.parseInt(p.getProperty("max"));
				fis.close();
				ConcurrentLinkedQueue<Relation> re = new ConcurrentLinkedQueue<Relation>();
				for (int i = page1; i <= max; i++)
					POOL.submit(getFollowCrawler(i, page_id, onick, max, pp.getLock(), re));
				f.delete();
			}
			file = new File("save2");
			files = file.listFiles();
			for (File f : files) {
				FileInputStream fis = new FileInputStream(f);
				Properties p = new Properties();
				p.load(fis);
				String page_id = p.getProperty("url");
				int page1 = Integer.parseInt(p.getProperty("page"));
				Entry<String, Integer> en = new AbstractMap.SimpleEntry<String, Integer>(page_id, page1);
				ParsingPage pp1 = new ParsingPage(page_id, pp.getLock(), pList, weiboQueue);
				pp1.setPageID_page(en);
				pList.add(pp1);
				POOL_PAGE.submit(pp1);
				f.delete();
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		start();
	}

	/**
	 * ���浱ǰ���жϵ�
	 */
	public void save() {
		File file = new File("save1");
		File[] files = file.listFiles();
		for (File f : files)
			f.delete();
		for (Entry<String, Integer> en : rightNow.entrySet()) {
			String temp = en.getKey();
			Pattern pat = Pattern.compile("(.+?):(.+?):(.+?)");
			Matcher mat = pat.matcher(temp);
			mat.find();
			String page_id = mat.group(1);
			int max = Integer.parseInt(mat.group(2));
			String onick = mat.group(3);
			int page = en.getValue();
			File f = new File("save1/" + page_id + ".properties");
			try {
				f.createNewFile();
				FileOutputStream fis = new FileOutputStream(f);
				Properties p = new Properties();
				p.setProperty("page_id", page_id);
				p.setProperty("page", page + "");
				p.setProperty("max", max + "");
				p.setProperty("onick", onick);
				p.store(fis, "");
				fis.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		file = new File("save2");
		files = file.listFiles();
		for (File f : files)
			f.delete();
		for (ParsingPage p : pList) {
			Entry<String, Integer> pageID_page = p.getPageID_page();
			String url = pageID_page.getKey();
			int page = pageID_page.getValue();
			File f = new File("save2/" + ++cc + ".properties");
			try {
				f.createNewFile();
				FileOutputStream fis = new FileOutputStream(f);
				Properties pro = new Properties();
				pro.setProperty("url", url);
				pro.setProperty("page", page + "");
				pro.store(fis, "");
				fis.close();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (queue_temp.size() != 0)
			for (Unfinish un : queue_temp)
				queue.add(un);
		new DB<Unfinish>().truncate(Unfinish.class);
		new DB<Unfinish>().insert(queue, Unfinish.class);
		new DB<Weibo>().insert(weiboQueue, Weibo.class);
		new DB<User>().insert(userQueue, User.class);
		System.out.println(weiboQueue.size());
		weiboQueue.clear();
	}

	@Deprecated
	private void reverse() {
		Unfinish temp = queue.poll();
		int c = 0;
		if (temp != null) {
			for (Unfinish un : queue) {
				if (c == 0)
					queue.add(queue.poll());
				else if (queue.poll().equals(temp))
					break;
				else
					queue.add(queue.poll());
			}
		}

	}

	public void start() {
		start(10000);
	}

	/**
	 * ��ȡ���в�������ģ����ȡ΢������ڵ�
	 * 
	 * @param count
	 *            �趨����ȡ�����������ȡcount������
	 */
	public void start(int count) {
		Unfinish addr;
		Map<String, String> map;
		////// �������ҵ���������ʦ��ʾʱ���ֵ�bug��bug����֮ǰѭ���ж�ʹ����queue.peak()��������queue.poll(),ǰ��ȡ��ͷԪ�ص���ɾ����ͷ������ȡ��ͷԪ�ز�ɾ��
		for (int j = 0; j < count && (addr = queue.poll()) != null; j++) {
			queue_temp.add(addr);
			Response res = pp.getResponse(addr.getUrl());// ��ȡ��ҳ��Ϣ
			map = pp.getPageInfo(res);
			String url1 = URL_PART1 + map.get(PAGE_ID) + URL_PART2 + map.get(PID);
			String b = pp.trim(pp.getResponse(url1).body());
			// pp.outputPage(b, "G:\\����\\wirsharkץ������\\page7.html");
			User user = new User();
			user.setOnick(addr.getOnick());
			user.setPage_id(map.get(PAGE_ID));
			if (PAT_V.matcher(b).find())
				user.setV(1);
			else
				user.setV(0);
			userQueue.add(user);// �����û���Ϣ
			logger.debug("�����user��userQueue�����У�" + user);
			String temp = (String) pp.getLines(b, PAT_COUNT1, 2, 2, new String());
			int pageCount = Integer.parseInt(temp);
			int tem = Integer.parseInt((String) pp.getLines(b, PAT_COUNT2, 2, -1, new String()));
			if (pageCount == 0) {
				pageCount = tem >= 5 ? 5 : tem;
				if (pageCount == 0)
					pageCount = 1;
			} else
				pageCount = pageCount >= 5 ? 5 : pageCount;
			logger.debug("��΢���û��Ĺ�עҳ��" + pageCount);// ��ȡ����עҳ��������΢�����ƣ����鿴��5ҳ��ע��
			List<FutureTask<Integer>> li = new ArrayList<FutureTask<Integer>>();
			queue_temp.remove(addr);
			ConcurrentLinkedQueue<Relation> re = new ConcurrentLinkedQueue<Relation>();
			for (int i = 1; i <= pageCount; i++)
				li.add(new FutureTask<Integer>(
						getFollowCrawler(i, map.get(PAGE_ID), map.get("onick"), pageCount, pp.getLock(), re)));
			for (int i = 0; i < pageCount; i++)// �ύpageCount��FollowCrawler�̵߳��̳߳�ִ�У�ÿ���߳���ȡ��iҳ
				POOL.submit(li.get(i));
			for (int i = 0; i < li.size(); i++)
				try {
					li.get(i).get();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
			// ִ����ҳ�����߳�
			ParsingPage pp1 = new ParsingPage(addr.getUrl(), pp.getLock(), pList, weiboQueue);
			Entry<String, Integer> en = new AbstractMap.SimpleEntry<String, Integer>(addr.getUrl(), 1);// ���öϵ�
			pp1.setPageID_page(en);
			FutureTask<Integer> ff = new FutureTask<Integer>(pp1, 0);
			POOL_PAGE.submit(ff);
			try {
				ff.get();
			} catch (InterruptedException | ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println(queue.size());
		}
	}

	/**
	 * �����ڲ���FollowCrawler��ʵ��������
	 * 
	 * @param page
	 *            ����ȡ��ҳ
	 * @param url
	 *            ������ҳ��ַ
	 * @param onick
	 *            �ǳ�
	 * @param pageCount
	 *            ��ҳ��
	 * @param lock
	 *            �ϵ�
	 * @param re
	 *            �����ϵ
	 * @return
	 */
	private FollowCrawler getFollowCrawler(int page, String url, String onick, int pageCount, String lock,
			ConcurrentLinkedQueue<Relation> re) {
		return new FollowCrawler(page, url, onick, pageCount, lock, re);
	}

	/**
	 * ��������΢����ȡ������ע�б����ع�ϵ����
	 * 
	 * @param page_id
	 * @return ���ع�ϵ����
	 */
	public Object getrelation(String page_id) {
		Response res = pp.getResponse(URL_PART1 + page_id + "/follow");// ��ȡ��עҳ��
		Map<String, String> map = pp.getPageInfo(res);
		String b = pp.trim(res.body());
		String temp = (String) pp.getLines(b, PAT_COUNT1, 2, 2, new String());
		int pageCount = Integer.parseInt(temp);
		int tem = Integer.parseInt((String) pp.getLines(b, PAT_COUNT2, 2, -1, new String()));
		if (pageCount == 0) {
			pageCount = tem >= 5 ? 5 : tem;
			if (pageCount == 0)
				pageCount = 1;
		} else
			pageCount = pageCount >= 5 ? 5 : pageCount;
		List<FutureTask<Integer>> li = new ArrayList<FutureTask<Integer>>();
		ConcurrentLinkedQueue<Relation> re = new ConcurrentLinkedQueue<Relation>();
		ExecutorService POOL = Executors.newCachedThreadPool();
		for (int i = 1; i <= pageCount; i++)
			li.add(new FutureTask<Integer>(
					getFollowCrawler(i, map.get(PAGE_ID), map.get("onick"), pageCount, pp.getLock(), re)));
		for (int i = 0; i < pageCount; i++)
			POOL.submit(li.get(i));
		for (int i = 0; i < li.size(); i++)
			try {
				li.get(i).get();
			} catch (InterruptedException e) {
				e.printStackTrace();
			} catch (ExecutionException e) {
				e.printStackTrace();
			}
		POOL.shutdown();
		return re;
	}

	/**
	 * �������ɵ�json���ݣ�Ϊ�Զ����bean.Node����,Ϊ�������ɹ�ϵͼ�Ľڵ�����
	 * 
	 * @param re
	 * @return
	 */
	private String genNode(Collection<Relation> re) {
		List<Node> list = new ArrayList<Node>();
		int i = 0;
		for (Relation r : re) {
			if (i == 0) {
				Node main = new Node().name(r.getOnick1()).id("0").category("����").symbolSize("20");
				list.add(main);
			}
			i++;
			Node node = new Node().id("" + i).name(r.getOnick2()).category("��ע").symbolSize("10");
			list.add(node);
		}
		String s = new Gson().toJson(list);
		System.out.println(s);
		return s;
	}

	/**
	 * �������ɵ�json���ݣ�Ϊ�Զ����bean.Link���ݣ�Ϊ�������ɹ�ϵͼ�Ĺ�ϵ����
	 * 
	 * @param re
	 * @return
	 */
	private String genLink(Collection<Relation> re) {
		List<Link> list = new ArrayList<Link>();
		int i = 0;
		for (Relation r : re) {
			Link link = new Link().id("" + i).name("" + i).source("0").target("" + (i + 1));
			list.add(link);
			i++;
		}
		String s = new Gson().toJson(list);
		System.out.println(s + "  " + list.size());
		return s;
	}

	/**
	 * 
	 * @param re
	 * @param onick
	 * @return
	 */
	@Deprecated
	private String generateRelationHTML(Collection<Relation> re, String onick) {

		StringBuffer sb = new StringBuffer();
		sb.append("<div class=\"canvas\" id=\"mainCanvas\"style=\""
				+ "width: 2500px; height: 2500px; border: 1px solid black;\">");
		sb.append(
				"<span class=\"block\" id=\"h1_block\"" + "style=\"left: 1250px; top: 1250px;\">" + onick + "</span>");
		int i = 0;
		int length = 700;
		int size = re.size();
		for (Relation r : re) {
			double radius = Math.PI * 2 * i / size;
			double cos = Math.cos(radius);
			double sin = Math.sin(radius);
			int leftPlus = 1250 + (int) (length * sin);
			int topMinus = 1250 - (int) (length * cos);
			sb.append("<span class=\"block draggable\" id=\"h" + (i + 2) + "_block\"" + "style=\"left: " + leftPlus
					+ "px; top: " + topMinus + "px;\">" + r.getOnick2() + "</span>");
			i++;
		}
		i = 0;
		for (i = 0; i < size; i++)
			sb.append("<div class=\"connector h1_block h" + (i + 2) + "_block\"></div>");
		sb.append("</div>");
		return sb.toString();
	}

	/**
	 * ��ȡ��עҳ���߳�ʵ�֡�
	 * 
	 * @author ��������ҵ��ѧ-12�����ѧԺ-������
	 * @Time 2016-06-14 15:02:29
	 *
	 */
	class FollowCrawler implements Callable<Integer> {
		private int page;
		private String url;
		private String lock1;
		private String page_id;
		private int pageCount;
		private String onick;
		private ConcurrentLinkedQueue<Relation> re;

		/**
		 * ����FollowCrawler����
		 * 
		 * @param page
		 *            ������ҳ��
		 * @param page_id
		 *            ������page_id
		 * @param onick
		 *            �ǳ�
		 * @param pageCount
		 *            ��ҳ��
		 * @param lock1
		 *            ��
		 * @param re
		 *            ��ϵ���У���ȡ���Ĺ�ϵ������ö�����
		 */
		public FollowCrawler(int page, String page_id, String onick, int pageCount, String lock1,
				ConcurrentLinkedQueue<Relation> re) {
			this.page = page;
			this.page_id = page_id;
			this.lock1 = lock1;
			this.pageCount = pageCount;
			url = URL_PART1 + page_id + URL_PART3 + page;
			this.re = re;
			this.onick = onick;
		}

		@Override
		public Integer call() throws Exception {
			rightNow.put(page_id + ":" + pageCount + ":" + onick, page);
			ParsingPage pp = new ParsingPage(url, lock1);
			Response res = pp.getResponse(url);
			pp.getPageInfo(res);
			String b = pp.trim(res.body());
			Matcher mat = PAT_FOLLOW.matcher(b);
			while (mat.find()) {
				Unfinish unfin = new Unfinish();
				unfin.setUrl("http://weibo.com" + mat.group(2) + "&is_all=1");
				unfin.setOnick(mat.group(4));
				Relation r = new Relation();
				r.setOnick1(onick);
				r.setOnick2(mat.group(4));
				re.add(r);
				System.out.println("http://weibo.com" + mat.group(2) + "&is_all=1" + "onick:" + mat.group(4));
				queue.add(unfin);
			}
			rightNow.remove(page_id + ":" + pageCount);
			new DB<Relation>().insert(re, Relation.class);
			return null;
		}

	}

	public static Callable<Integer> get() {
		// return new Callable<Integer>() {
		//
		// @Override
		// public Integer call() {
		// System.out.println(123);
		// return 0;
		// }
		// };
		return null;
	}

	public static void start(List<FutureTask<Integer>> list) {
		for (FutureTask<Integer> task : list)
			POOL.submit(task);
	}

	public static void wait(List<FutureTask<Integer>> list) {
		for (FutureTask<Integer> task : list)
			try {
				task.get();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}

	private static FutureTask<Integer> retrieve(List<FutureTask<Integer>> list) {
		int count = 0;
		while ((++count) <= 10) {
			for (FutureTask<Integer> task : list)
				if (task.isDone())
					return task;
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return null;
	}

	public static void main(String[] args) {
		// �ڿӲ�����Ϊ�в�����
		// new
		// MainCrawler("http://weibo.com/anthree9?refer_flag=1005050006_&is_all=1").start();

	}

	/**
	 * action����ʵ��
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Object action(String act) {
		Object re = getAction(act);
		if (re == null)
			return null;
		if (act.equals("onick")) {// ������ѯ��ϵ���Ȳ����ݿ⣬���򷵻أ������������΢��
			Lock1 lock = new Lock1();
			DB<Relation> db = new DB<Relation>();
			ConcurrentLinkedQueue<Relation> q = new ConcurrentLinkedQueue<Relation>();
			new Thread(() -> {
				db.retrieve(q, Relation.class, "select * from relation where onick1='" + re + "'");
				synchronized (lock) {
					if (q.size() > 0)
						lock.setLock("1");
				}
			}).start();
			Object o = pp.weiboSearch((String) re);
			if (lock.getLock().equals("1")) {
				new Thread(() -> {
					// ParsingPage.setCookie();
					// User user;
					// if (o != null) {
					// user = (User) o;
					// getrelation(user.getPage_id());
					// }
				}).start();
				return q;
			}
			ParsingPage.setCookie();
			User user;
			if (o != null) {
				user = (User) o;
				return getrelation(user.getPage_id());
			}
		} else if (act.equals("get_data")) {
			Collection<Relation> set = (Collection<Relation>) re;
			return genNode(set);
		} else if (act.equals("get_link")) {
			Collection set = (Collection) re;
			return genLink(set);
		} else if (act.equals("reload")) {
			ParsingPage.setCookie();
			return null;
		} else if (act.equals("ajax_relation")) {// ����echarts��ϵͼ��json����
			JSONObject j = JSONObject.fromObject(optionS);
			JSONArray legend = JSONArray.fromObject("[]");
			JSONArray legend_data = JSONArray.fromObject("[]");
			legend_data.add("����");
			legend_data.add("��ע");
			JSONObject bridge = JSONObject.fromObject("{}");
			bridge.accumulate("data", legend_data);
			legend.add(bridge);
			j.accumulate("legend", legend);
			Collection<Relation> set = (Collection<Relation>) re;
			JSONArray jad = JSONArray.fromObject(genNode(set));
			JSONArray jal = JSONArray.fromObject(genLink(set));
			JSONObject series = JSONObject.fromObject("{}");
			series.accumulate("data", jad);
			series.accumulate("links", jal);
			series.accumulate("name", "��ϵ");
			series.accumulate("type", "graph");
			series.accumulate("layout", "force");
			JSONArray cat = JSONArray.fromObject("[]");
			cat.add(JSONObject.fromObject("{}").accumulate("name", "����"));
			cat.add(JSONObject.fromObject("{}").accumulate("name", "��ע"));
			series.accumulate("categories", cat);
			series.accumulate("label", JSONObject.fromObject("{}").accumulate("normal",
					JSONObject.fromObject("{}").accumulate("position", "right")));
			series.accumulate("roam", true);
			series.accumulate("lineStyle", JSONObject.fromObject("{}").accumulate("normal",
					JSONObject.fromObject("{}").accumulate("curveness", "0.3")));
			set.iterator().hasNext();
			JSONObject title = JSONObject.fromObject("{}");
			title.accumulate("text", set.iterator().next().getOnick1() + "�Ĺ�ע");
			title.accumulate("subtext", "��ϵͼ");
			title.accumulate("top", "top");
			title.accumulate("left", "left");
			j.accumulate("series", series);
			j.accumulate("title", title);
			System.out.println(j);
			return j;
		} else if (act.equals("get_weibo")) {// �����û��ǳƻ�ȡ΢��������weibo����
			DB<Weibo> db = new DB<Weibo>();
			ConcurrentLinkedQueue<Weibo> q = new ConcurrentLinkedQueue<Weibo>();
			if (getAction("offset") != null) {// ��Ҫ��ҳչʾʱ
				int offset = Integer.parseInt((String) getAction("offset"));
				int limit = Integer.parseInt((String) getAction("limit"));
				db.retrieve(q, Weibo.class,
						"select * from weibo where onick='" + re + "' limit " + offset + "," + (limit + offset));
				if (q.size() == 0) {
					User user = (User) pp.weiboSearch((String) re);
					ParsingPage p1 = new ParsingPage(URL_PART1 + user.getPage_id() + "&is_all=1", "", null, q);
					p1.parsingPage();
				}
				JSONObject jo = new JSONObject();
				jo.accumulate("total", new DB<>().getCount("select count(*) from weibo where onick='" + re + "'"));
				JSONArray data = new JSONArray();
				int i = 0;
				for (Weibo weibo : q) {
					if (i == limit)
						break;
					JSONObject d = new JSONObject();
					d.accumulate("weiboContent", weibo.getWeiboContent());
					d.accumulate("date", weibo.getDate().toString());
					d.accumulate("topic", weibo.getTopic());
					d.accumulate("weibo_url", weibo.getWeibo_url());
					data.add(d);
					i++;
				}
				jo.accumulate("rows", data);
				return jo;
			} else {// �Ƿ�ҳչʾ
				db.retrieve(q, Weibo.class, "select * from weibo where onick='" + re + "'");
				if (q.size() == 0) {
					User user = (User) pp.weiboSearch((String) re);
					ParsingPage p1 = new ParsingPage(URL_PART1 + user.getPage_id() + "&is_all=1", "", null, q);
					p1.parsingPage();
				}
				// int count = q.size();
				return q;
			}
		} else if (act.equals("all_weibo")) {
			DB<Weibo> db = new DB<Weibo>();
			ConcurrentLinkedQueue<Weibo> q = new ConcurrentLinkedQueue<Weibo>();
			db.retrieve(q, Weibo.class, "select * from weibo where onick='" + re + "'");
			if (q.size() == 0) {
				User user = (User) pp.weiboSearch((String) re);
				ParsingPage p1 = new ParsingPage(URL_PART1 + user.getPage_id() + "&is_all=1", "", null, q);
				p1.parsingPage();
			}
			return q;
		} else if (act.equals("test_crawler")) {

			System.out.println(this.getClass());
			return new Crawler(null, 0, null, null);
		}
		return null;
	}
}

class Lock1 {
	private String lock = "-1";

	public String getLock() {
		return lock;
	}

	public void setLock(String lock) {
		this.lock = lock;
	}
}