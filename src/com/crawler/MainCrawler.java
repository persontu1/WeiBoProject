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
 * 实现了Action抽象基类的微博爬取器，实现了action的业务逻辑并承担爬取模块的主要工作。该模块是系统最复杂的一个部分
 * 
 * @author 哈尔滨工业大学-12级软件学院-杨埔生
 * @Time 2016-06-14 15:02:29
 *
 */
public class MainCrawler extends Action {
	/**
	 * 在save方法中参与序列生成properties文件的操作，具体操作为以cc.properties为名字生成properties文件
	 */
	private static volatile int cc = 0;
	/**
	 * 待爬取队列，队列元素的数据结构保存了待爬取博主的昵称和主页url,详细参见Unfinish.java文件
	 */
	private final ConcurrentLinkedQueue<Unfinish> queue = new ConcurrentLinkedQueue<Unfinish>();
	/**
	 * 保存已取出queue队列的，线程还未对该元素进行爬取微博的操作的元素，当元素开始进行爬取微博时将该元素移出该队列。
	 * 保存该特定属性的元素是为了配合将所有未处理的Unfinish元素都暂存进数据库中的操作。
	 */
	private final ConcurrentLinkedQueue<Unfinish> queue_temp = new ConcurrentLinkedQueue<Unfinish>();
	/**
	 * 配合将所有未处理的Unfinish元素都暂存进数据库中的操作而设立的数据结构。
	 */
	private ConcurrentHashMap<String, Integer> rightNow = new ConcurrentHashMap<String, Integer>();
	/**
	 * 配合将所有未处理的Unfinish元素都暂存进数据库中的操作而设立的数据结构。
	 */
	private ConcurrentLinkedQueue<ParsingPage> pList = new ConcurrentLinkedQueue<ParsingPage>();
	/**
	 * 爬取到的微博队列，每爬到一条微博便加入该队列
	 */
	private ConcurrentLinkedQueue<Weibo> weiboQueue = new ConcurrentLinkedQueue<Weibo>();
	/**
	 * 爬取到博主的详细信息队列
	 */
	private ConcurrentLinkedQueue<User> userQueue = new ConcurrentLinkedQueue<User>();
	/**
	 * 线程池最大数量，根据爬取新浪微博的经验2-4个爬取线程已足够爬取新浪微博，线程再多新浪微博就会封ip禁止访问
	 */
	private static final Integer THREAD_NUM = 2;
	/**
	 * 该类下的所有线程均提交到该线程池里等待执行
	 */
	private final static ExecutorService POOL = Executors.newFixedThreadPool(THREAD_NUM);
	/**
	 * 跟保存相关的线程池
	 */
	private final static ExecutorService POOL_PAGE = Executors.newFixedThreadPool(THREAD_NUM);
	/**
	 * 日志器
	 */
	public final static Logger logger = Logger.getLogger(MainCrawler.class);
	/**
	 * 爬取新浪微博关注页面的正则表达式
	 */
	private static final Pattern PAT_FOLLOW = Pattern.compile(
			"<div\\sclass=\"info_name\\sW_fb\\sW_f14\"><a class=\"S_txt1\" target=\"_blank\"(.+?)href=\"(.+?)\"(.*?)>(.+?)</a>");
	/**
	 * 检测是否大v的正则表达式
	 */
	private static final Pattern PAT_V = Pattern.compile(
			"<div class=\"PCD_header\">(.+?)<a(.+?)href=\"http://verified.weibo.com/verify\"(.+?)>(.+?)(>\u5173\u6ce8<)(.+?)(>\u79c1\u4fe1<)");
	/**
	 * 新浪微博的通用前缀
	 */
	private static final String URL_PART1 = "http://weibo.com/p/";
	/**
	 * 关注页面的通用格式
	 */
	private static final String URL_PART2 = "/follow?from=";
	/**
	 * 关注页数的通用格式
	 */
	private static final String URL_PART3 = "/follow?page=";
	private static final String PAGE_ID = "page_id";
	private static final String PID = "pid";
	private static final String PAT_COUNT1 = "<a page-limited=\"true\" class=\"page S_txt1\" href=\"(.+?)\">(.+?)</a>";
	private static final String PAT_COUNT2 = "<a action-type=\"page\" class=\"page S_txt1\" href=\"(.+?)\">(.+?)</a>";
	/**
	 * 为尽量减少运行时堆内存的占用与垃圾回收次数，涉及到的页面爬取操作尽量只使用该ParsingPage对象
	 */
	private ParsingPage pp = new ParsingPage("", "");
	/**
	 * 参与生成最后的json格式数据
	 */
	private static final String optionS = "{\"tooltip\":{},\"toolbox\":{\"show\":true,\"feature\":{\"dataView\":{\"show\":true,\"readOnly\":false},\"restore\":{\"show\":true},\"saveAsImage\":{\"show\":true}}},\"animationDuration\":1500,\"animationEasingUpdate\":\"quinticInOut\"}";
	/**
	 * MainCrawler类加载时进行配置日志器操作
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
	 * 默认无参构造器，用于反射实例化无参对象
	 */
	public MainCrawler() {

	}

	/**
	 * 有参构造器
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
	 * 有参构造器
	 * 
	 * @param lock
	 *            断点设置
	 * @param pList
	 *            设置pList
	 * @param weiboQueue
	 *            设置微博队列
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
	 * 从数据库和properties文件中恢复上次运行的断点
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
	 * 保存当前运行断点
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
	 * 爬取队列操作，该模块爬取微博的入口点
	 * 
	 * @param count
	 *            设定的爬取次数，最多爬取count个博主
	 */
	public void start(int count) {
		Unfinish addr;
		Map<String, String> map;
		////// 问题已找到。。给导师演示时发现的bug，bug处在之前循环判定使用了queue.peak()方法而非queue.poll(),前者取队头元素但不删除队头，后者取队头元素并删除
		for (int j = 0; j < count && (addr = queue.poll()) != null; j++) {
			queue_temp.add(addr);
			Response res = pp.getResponse(addr.getUrl());// 获取主页信息
			map = pp.getPageInfo(res);
			String url1 = URL_PART1 + map.get(PAGE_ID) + URL_PART2 + map.get(PID);
			String b = pp.trim(pp.getResponse(url1).body());
			// pp.outputPage(b, "G:\\毕设\\wirshark抓包数据\\page7.html");
			User user = new User();
			user.setOnick(addr.getOnick());
			user.setPage_id(map.get(PAGE_ID));
			if (PAT_V.matcher(b).find())
				user.setV(1);
			else
				user.setV(0);
			userQueue.add(user);// 保存用户信息
			logger.debug("添加新user到userQueue队列中：" + user);
			String temp = (String) pp.getLines(b, PAT_COUNT1, 2, 2, new String());
			int pageCount = Integer.parseInt(temp);
			int tem = Integer.parseInt((String) pp.getLines(b, PAT_COUNT2, 2, -1, new String()));
			if (pageCount == 0) {
				pageCount = tem >= 5 ? 5 : tem;
				if (pageCount == 0)
					pageCount = 1;
			} else
				pageCount = pageCount >= 5 ? 5 : pageCount;
			logger.debug("该微博用户的关注页数" + pageCount);// 获取到关注页数（新浪微博限制，最多查看到5页关注）
			List<FutureTask<Integer>> li = new ArrayList<FutureTask<Integer>>();
			queue_temp.remove(addr);
			ConcurrentLinkedQueue<Relation> re = new ConcurrentLinkedQueue<Relation>();
			for (int i = 1; i <= pageCount; i++)
				li.add(new FutureTask<Integer>(
						getFollowCrawler(i, map.get(PAGE_ID), map.get("onick"), pageCount, pp.getLock(), re)));
			for (int i = 0; i < pageCount; i++)// 提交pageCount个FollowCrawler线程到线程池执行，每个线程爬取第i页
				POOL.submit(li.get(i));
			for (int i = 0; i < li.size(); i++)
				try {
					li.get(i).get();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
			// 执行网页分析线程
			ParsingPage pp1 = new ParsingPage(addr.getUrl(), pp.getLock(), pList, weiboQueue);
			Entry<String, Integer> en = new AbstractMap.SimpleEntry<String, Integer>(addr.getUrl(), 1);// 设置断点
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
	 * 返回内部类FollowCrawler的实例化对象
	 * 
	 * @param page
	 *            欲爬取的页
	 * @param url
	 *            博主主页地址
	 * @param onick
	 *            昵称
	 * @param pageCount
	 *            总页数
	 * @param lock
	 *            断点
	 * @param re
	 *            保存关系
	 * @return
	 */
	private FollowCrawler getFollowCrawler(int page, String url, String onick, int pageCount, String lock,
			ConcurrentLinkedQueue<Relation> re) {
		return new FollowCrawler(page, url, onick, pageCount, lock, re);
	}

	/**
	 * 访问新浪微博获取博主关注列表并返回关系队列
	 * 
	 * @param page_id
	 * @return 返回关系队列
	 */
	public Object getrelation(String page_id) {
		Response res = pp.getResponse(URL_PART1 + page_id + "/follow");// 获取关注页面
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
	 * 返回生成的json数据，为自定义的bean.Node数据,为参与生成关系图的节点数据
	 * 
	 * @param re
	 * @return
	 */
	private String genNode(Collection<Relation> re) {
		List<Node> list = new ArrayList<Node>();
		int i = 0;
		for (Relation r : re) {
			if (i == 0) {
				Node main = new Node().name(r.getOnick1()).id("0").category("本体").symbolSize("20");
				list.add(main);
			}
			i++;
			Node node = new Node().id("" + i).name(r.getOnick2()).category("关注").symbolSize("10");
			list.add(node);
		}
		String s = new Gson().toJson(list);
		System.out.println(s);
		return s;
	}

	/**
	 * 返回生成的json数据，为自定义的bean.Link数据，为参与生成关系图的关系数据
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
	 * 爬取关注页面线程实现。
	 * 
	 * @author 哈尔滨工业大学-12级软件学院-杨埔生
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
		 * 生成FollowCrawler对象
		 * 
		 * @param page
		 *            欲爬的页数
		 * @param page_id
		 *            博主的page_id
		 * @param onick
		 *            昵称
		 * @param pageCount
		 *            总页数
		 * @param lock1
		 *            锁
		 * @param re
		 *            关系队列，获取到的关系保存进该队列中
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
		// 挖坑不填是为有病。。
		// new
		// MainCrawler("http://weibo.com/anthree9?refer_flag=1005050006_&is_all=1").start();

	}

	/**
	 * action方法实现
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	@Override
	public Object action(String act) {
		Object re = getAction(act);
		if (re == null)
			return null;
		if (act.equals("onick")) {// 仅仅查询关系，先查数据库，有则返回，无则访问新浪微博
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
		} else if (act.equals("ajax_relation")) {// 生成echarts关系图的json数据
			JSONObject j = JSONObject.fromObject(optionS);
			JSONArray legend = JSONArray.fromObject("[]");
			JSONArray legend_data = JSONArray.fromObject("[]");
			legend_data.add("本体");
			legend_data.add("关注");
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
			series.accumulate("name", "关系");
			series.accumulate("type", "graph");
			series.accumulate("layout", "force");
			JSONArray cat = JSONArray.fromObject("[]");
			cat.add(JSONObject.fromObject("{}").accumulate("name", "本体"));
			cat.add(JSONObject.fromObject("{}").accumulate("name", "关注"));
			series.accumulate("categories", cat);
			series.accumulate("label", JSONObject.fromObject("{}").accumulate("normal",
					JSONObject.fromObject("{}").accumulate("position", "right")));
			series.accumulate("roam", true);
			series.accumulate("lineStyle", JSONObject.fromObject("{}").accumulate("normal",
					JSONObject.fromObject("{}").accumulate("curveness", "0.3")));
			set.iterator().hasNext();
			JSONObject title = JSONObject.fromObject("{}");
			title.accumulate("text", set.iterator().next().getOnick1() + "的关注");
			title.accumulate("subtext", "关系图");
			title.accumulate("top", "top");
			title.accumulate("left", "left");
			j.accumulate("series", series);
			j.accumulate("title", title);
			System.out.println(j);
			return j;
		} else if (act.equals("get_weibo")) {// 根据用户昵称获取微博，返回weibo队列
			DB<Weibo> db = new DB<Weibo>();
			ConcurrentLinkedQueue<Weibo> q = new ConcurrentLinkedQueue<Weibo>();
			if (getAction("offset") != null) {// 需要分页展示时
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
			} else {// 非分页展示
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