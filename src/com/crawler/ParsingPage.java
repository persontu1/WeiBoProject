package com.crawler;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.jsoup.Jsoup;
import org.jsoup.Connection.Method;
import org.jsoup.Connection.Response;

import util.DB;
import util.Native2AsciiUtils;
import bean.Relation;
import bean.Unfinish;
import bean.User;
import bean.Weibo;

/**
 * 实现了Runnable接口的爬取模块，所有的微博爬取工作（如页面处理、分析等）由其负责。
 * 
 * @author 哈尔滨工业大学-12级软件学院-杨埔生
 * @Time 2016-06-14 15:02:29
 *
 */
public class ParsingPage implements Runnable {
	/**
	 * Cookie文件路径，目的用于正确访问新浪微博。 这里一定要配置好，目前未实现运行时指定Cookie路径的方法，路径写死在程序中
	 */
	private static final String COOKIE_LOC = "D:\\eclipse\\新浪微博登录包.txt";
	/**
	 * 分析Cookie文件的正则表达式
	 */
	private static final String PAT_COOKIE_ENTRY = "(.+?)=(.+?);";
	/**
	 * 用于制定byte[]数组大小
	 */
	private static final Integer BYTE = 2 * 1024;
	/**
	 * 连接超时时间，这里设为5秒。
	 */
	private static final Integer SLEEP_TIME = 5 * 1000;
	/**
	 * 从博主主页中获取博主主要信息的正则表达式
	 */
	private static final String PAGE_ENTRY = "\\['(.+?)'\\]='(.+?)'";
	/**
	 * 参与生成url
	 */
	private static final String AND = "&";
	private static final String DOMAIN_EQUAL = "domain=";
	private static final String DOMAIN = "domain";
	private static final String PAGEBAR_EQUAL = "pagebar=";
	private static final String DOMAIN_OP_EQUAL = "domain_op=";
	private static final String PAGE_EQUAL = "page=";
	private static final String PRE_PAGE_EQUAL = "pre_page=";
	private static final String ID_EQUAL = "id=";
	private static final String PAGE_ID = "page_id";
	/**
	 * 分析博主微博的总页数的regex（正则表达式）
	 */
	private static final String PAT_COUNTPAGE_EQUAL = "countPage=(.+?)\"";
	/**
	 * 新浪微博页面返回进行提取有效内容之前需先将页面里含有的\n,\t,\r,\去除，这是去除regex
	 */
	private static final String TRIM = "\\\\n|\\\\t|\\\\r|\\\\";
	/**
	 * 提取微博内容的regex
	 */
	private static final String PAT_WEIBO_PART = "<div class=\"WB_detail\">(.+?)<div class=\"WB_from S_txt2\">(.+?)<a name=(.+?)\\s(.+?)href=\"(.+?)\" title=\"(.+?)\"(.+?)</div>(\\s+)<div class=\"WB_text W_f14\" node-type=\"feed_list_content\"(.+?)>(.+?)</div>";
	private static final String ONICK = "onick";
	private static final String CONNECTION_WARNING = "io错误\n";
	private static final String CONNECTION_WARN = "连接超时，重新获取\n";
	private static final String TIME_TAIL = ":00";
	/**
	 * 获取参与的话题的regex
	 */
	private static final String PAT_TOPIC = "#(.+)#";
	/**
	 * 参与生成url
	 */
	private static final String SEARCH_URL = "http://s.weibo.com/ajax/topsuggest.php?key=";
	private static final String BASE_WEIBO = "http://weibo.com/";
	private static final String BASE_WEIBO1 = "http://weibo.com";
	/**
	 * 设定最大线程池线程个数为2个，根据爬取新浪微博的经验10M带宽设置2-4个最大线程是最好的，不至于被封
	 */
	private static final Integer THREAD_NUM = 2;
	/**
	 * 线程池
	 */
	private static final ExecutorService POOL = Executors.newFixedThreadPool(THREAD_NUM);
	/**
	 * 提取微博内容的regex
	 * 
	 */
	private static final Pattern PAT_WEIBO = Pattern.compile(PAT_WEIBO_PART, Pattern.MULTILINE | Pattern.DOTALL);
	/**
	 * 提取微博内容的regex
	 */
	private static final Pattern TOPIC_PAT = Pattern.compile(PAT_TOPIC);
	/**
	 * 分析博主微博的总页数的regex（正则表达式）
	 * 
	 */
	private static final Pattern COUNT_EQUAL_PAT = Pattern.compile(PAT_COUNTPAGE_EQUAL);
	/**
	 * 从博主主页中获取博主主要信息的正则表达式
	 */
	private static final Pattern PAT_CONFIG = Pattern.compile(PAGE_ENTRY);
	/**
	 * 去除页面中多余信息的正则表达式
	 */
	private static final Pattern PAT_TRIM1 = Pattern.compile("\"html\":\"(.+?)\"}");
	/**
	 * 新浪微博页面返回进行提取有效内容之前需先将页面里含有的\n,\t,\r,\去除，这是去除regex
	 */
	private static final Pattern PAT_TRIM2 = Pattern.compile("\\\\n|\\\\t|\\\\r|\\\\");
	/**
	 * 新浪微博页面返回进行提取有效内容之前需先将页面里含有的\n,\t,\r,\去除，这是去除regex（好像和上面那个重复了）
	 */
	private static final Pattern PAT_TRIMULTI = Pattern.compile(TRIM);
	/**
	 * 分析Cookie文件的正则表达式
	 */
	private static final Pattern COOKIE_ENTRY_PAT = Pattern.compile(PAT_COOKIE_ENTRY);
	/**
	 * 分析博主是否是大V的regex
	 */
	private static final Pattern PAT_V = Pattern.compile(
			"http://verified.weibo.com/verify\\?from=profile|http://company.verified.weibo.com/verify/orgapply\\?from=profile");
	/**
	 * 存放从文件提取的cookie
	 */
	private static Map<String, String> cookies;
	/**
	 * 博主用户的基本信息
	 */
	private Map<String, String> pageInfo;
	private final static String urlAjax = "http://weibo.com/p/aj/v6/mblog/mbloglist?ajwvr=6&is_all=1&feed_type=0";
	private volatile int timeout_count = 0;
	/**
	 * 微博队列，每提取到一个微博便入队
	 */
	private ConcurrentLinkedQueue<Weibo> list = new ConcurrentLinkedQueue<Weibo>();
	/**
	 * 参与保存断点的操作
	 */
	private Entry<String, Integer> pageID_page = new AbstractMap.SimpleEntry<String, Integer>("", 0);
	public Weibo info;
	private String url;
	/**
	 * 用于外部控制爬取进程的暂停与恢复
	 */
	private String lock;
	/**
	 * 用于保存当前进程状态
	 */
	private ConcurrentLinkedQueue<ParsingPage> pList;

	/**
	 * 外部控制从文件提取COOKIE操作
	 */
	public static void setCookie() {
		File file = new File(COOKIE_LOC);
		FileInputStream is;
		try {
			is = new FileInputStream(file);
			byte[] buf = new byte[BYTE];
			is.read(buf);
			String text = new String(buf);
			Matcher mat = COOKIE_ENTRY_PAT.matcher(text);
			cookies = new HashMap<String, String>();
			while (mat.find()) {
				cookies.put(mat.group(1), mat.group(2));
			}
			is.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void setLock(String lock) {
		this.lock = lock;
	}

	public String getLock() {
		return lock;
	}

	public ParsingPage(String url, String lock) {
		this.url = url;
		this.lock = lock;
	}

	/**
	 * 构造器
	 * 
	 * @param url
	 * @param lock
	 * @param pList
	 * @param list
	 */
	public ParsingPage(String url, String lock, ConcurrentLinkedQueue<ParsingPage> pList,
			ConcurrentLinkedQueue<Weibo> list) {
		this.url = url;
		this.lock = lock;
		this.pList = pList;
		this.list = list;
	}

	/**
	 * 
	 * @param un
	 * @param url
	 * @param lock
	 * @param pList
	 * @param list
	 * @param unfinishList
	 */
	public ParsingPage(Unfinish un, String url, String lock, ConcurrentLinkedQueue<ParsingPage> pList,
			ConcurrentLinkedQueue<Weibo> list, ConcurrentLinkedQueue<Unfinish> unfinishList) {
		this.url = url;
		this.lock = lock;
		this.pList = pList;
		this.list = list;
	}

	public void setQueue(ConcurrentLinkedQueue<Weibo> list) {
		this.list = list;
	}

	public void setPageID_page(Entry<String, Integer> pageID_page) {
		this.pageID_page = pageID_page;
	}

	public Entry<String, Integer> getPageID_page() {
		return pageID_page;
	}

	public Response getResponse() {
		return getResponse(url);
	}

	/**
	 * 根据url访问资源（html/json)
	 * 
	 * @param url
	 * @return
	 */
	public Response getResponse(String url) {// 登录并获取页面
		File file = new File(COOKIE_LOC);
		try {
			if (cookies == null) {
				FileInputStream is = new FileInputStream(file);
				byte[] buf = new byte[BYTE];
				is.read(buf);
				String text = new String(buf);
				Matcher mat = COOKIE_ENTRY_PAT.matcher(text);
				cookies = new HashMap<String, String>();
				while (mat.find()) {
					cookies.put(mat.group(1), mat.group(2));
				}
				is.close();
			}
			synchronized (lock) {
				Response res = Jsoup.connect(url).timeout(15000).ignoreContentType(true).cookies(cookies)
						.method(Method.GET).execute();
				return res;
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return getResponse(url);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			System.out.println(CONNECTION_WARNING + url);
			timeout_count++;
			if (e instanceof java.net.SocketTimeoutException && timeout_count >= 10) {
				try {
					System.out.println(CONNECTION_WARN);
					Thread.sleep(SLEEP_TIME);
					timeout_count = 0;
				} catch (InterruptedException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}
			}
			return getResponse(url);
		}
	}

	/**
	 * 根据返回的页面提取博主用户信息
	 * 
	 * @param res
	 *            返回头
	 * @return 用户信息表
	 */
	public Map<String, String> getPageInfo(Response res) {
		String body = res.body();
		Matcher mat_config = PAT_CONFIG.matcher(body);
		Map<String, String> map = new HashMap<String, String>();
		while (mat_config.find()) {
			// System.out.println(mat_config.group(1)+"="+mat_config.group(2));
			map.put(mat_config.group(1), mat_config.group(2));
		}
		this.pageInfo = map;
		return map;
	}

	public boolean contains(String body, String regex) {
		if (getURL(body, regex) != null)
			return true;
		return false;
	}

	public void getU(String body, String regex) {
		Pattern pat = Pattern.compile(regex);
		Matcher mat = pat.matcher(body);
		List<String> list = new ArrayList<String>();
		if (mat.find())
			list.add(mat.group(1));
		System.out.println(list.get(0));
	}

	/**
	 * 获取微博的操作流程，若要获取完整一页的微博需要访问新浪微博3次
	 * 
	 * @param body
	 *            第page页的内容
	 * @param onick
	 *            用户昵称
	 * @param page
	 *            第page页
	 */
	public void getWeibos(String body, String onick, int page) {
		getWeibo(body, onick);
		for (int i = 0; i < 2; i++) {// 后两次的url比较复杂
			String url = append(urlAjax, AND, DOMAIN_EQUAL + pageInfo.get(DOMAIN), PAGEBAR_EQUAL + i,
					DOMAIN_OP_EQUAL + pageInfo.get(DOMAIN), PAGE_EQUAL + page, PRE_PAGE_EQUAL + page,
					ID_EQUAL + pageInfo.get(PAGE_ID));
			Response res = getResponse(url);
			String bb = Native2AsciiUtils.ascii2Native(res.body());
			Matcher mat = PAT_TRIMULTI.matcher(bb);
			String b = mat.replaceAll("");
			getWeibo(b, onick);
		}
		pageID_page.setValue(pageID_page.getValue() > page ? page : pageID_page.getValue());
	}

	/**
	 * 按昵称搜索博主
	 * 
	 * @param name
	 *            用户昵称
	 * @return 查询到的博主完整用户信息，若查无此人则返回null
	 */
	public Object weiboSearch(String name) {
		String resUrl = null;
		try {
			resUrl = SEARCH_URL + URLEncoder.encode(name, "UTF-8");// 需处理成unicode数据
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Response res = getResponse(resUrl);
		String body = res.body();
		String b = trimUltimate(Native2AsciiUtils.ascii2Native(body), TRIM);
		Pattern pat = Pattern.compile("\"user\":\\[(.*?)\\]");
		Matcher mat = pat.matcher(b);
		if (mat.find()) {
			String temp = mat.group(1);
			pat = Pattern.compile("\"(.+?)\":(.+?),");
			mat = pat.matcher(temp);
			while (mat.find())
				if (mat.group(1).equals("u_id")) {
					String uid = mat.group(2);
					url = BASE_WEIBO + uid + "?is_all=1";
					res = getResponse(url);
					Map<String, String> map = getPageInfo(res);
					User user = new User();
					user.setOnick(map.get(ONICK));
					user.setPage_id(map.get(PAGE_ID));
					if (PAT_V.matcher(trim(res.body())).find())
						user.setV(1);
					else
						user.setV(0);
					// parsingPage();
					List<User> list = new ArrayList<User>();
					list.add(user);
					new Thread(new DB<User>(list, User.class)).start();
					return user;
				}
		}
		return null;
	}

	private static final String ONE = "1";

	public String lastPage() {
		String url = append(urlAjax, AND, DOMAIN_EQUAL + pageInfo.get(DOMAIN), PAGEBAR_EQUAL + 1,
				DOMAIN_OP_EQUAL + pageInfo.get(DOMAIN), PAGE_EQUAL + 1, PRE_PAGE_EQUAL + 1,
				ID_EQUAL + pageInfo.get(PAGE_ID));
		Response res = getResponse(url);
		return trimUltimate(Native2AsciiUtils.ascii2Native(res.body()), TRIM);
	}

	/**
	 * 获取微博的总页数
	 * 
	 * @return 总页数
	 */
	public int getCountPage() {
		String url = append(urlAjax, AND, DOMAIN_EQUAL + pageInfo.get(DOMAIN), PAGEBAR_EQUAL + ONE,
				DOMAIN_OP_EQUAL + pageInfo.get(DOMAIN), PAGE_EQUAL + ONE, PRE_PAGE_EQUAL + ONE,
				ID_EQUAL + pageInfo.get(PAGE_ID));
		Response res = getResponse(url);
		String bb = Native2AsciiUtils.ascii2Native(res.body());
		Matcher mat = PAT_TRIMULTI.matcher(bb);
		String b = mat.replaceAll("");
		return getCountPage(b);
	}

	/**
	 * 
	 * 获取微博的总页数
	 * 
	 * @return 总页数
	 */
	public int getCountPage(String body) {
		Matcher mat = COUNT_EQUAL_PAT.matcher(body);
		if (mat.find())
			return Integer.parseInt(mat.group(1));
		return -1;
	}

	/**
	 * 根据当前页面获取微博
	 * 
	 * @param body
	 *            页面内容
	 * @param onick
	 *            用户昵称
	 */
	public void getWeibo(String body, String onick) {
		Matcher mat = PAT_WEIBO.matcher(body);
		while (mat.find()) {
			try {
				Weibo weibo = new Weibo();
				weibo.setDate(new Timestamp(DB.ddd.get().parse(mat.group(6).trim() + TIME_TAIL).getTime()));
				weibo.setOnick(pageInfo.get(ONICK));
				weibo.setPage_id(pageInfo.get(PAGE_ID));
				weibo.setUrl(url);
				weibo.setWeibo_url(BASE_WEIBO1 + mat.group(5));
				weibo.setWeibo_id(mat.group(3));
				String t = properString(mat.group(10), "<.+?>", Pattern.DOTALL);
				String topic;
				if ((topic = getTopic(t)) != null)
					weibo.setTopic(topic);
				weibo.setWeiboContent(t);
				System.out.println(t + "    " + mat.group(3) + "    " + mat.group(5) + "    " + mat.group(6));
				list.add(weibo);
			} catch (ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
	}

	/**
	 * 从微博中获取微博话题
	 * 
	 * @param wb
	 *            某条微博
	 * @return 若有话题，则返回该话题，无则返回null
	 */
	private String getTopic(String wb) {
		Matcher mat = TOPIC_PAT.matcher(wb);
		if (mat.find())
			return mat.group(1);
		return null;
	}

	/**
	 * 
	 * @param body
	 * @param onick
	 * @param weibos
	 * @return
	 */
	@Deprecated
	public List<String> getWeibo(String body, String onick, List<String> weibos) {
		Pattern pat = Pattern.compile(
				"<div class=\"WB_text W_f14\" node-type=\"feed_list_content\" nick-name=\"" + onick + "\">(.+?)</div>",
				Pattern.MULTILINE | Pattern.DOTALL);
		Matcher mat = pat.matcher(body);
		while (mat.find()) {
			String t = properString(mat.group(1), "<.+?>", Pattern.DOTALL);
			System.out.println(t);
			weibos.add(t);
		}
		return weibos;
	}

	/**
	 * 生成正则表达式的连接操作
	 * 
	 * @param base
	 *            初始块
	 * @param op
	 *            连接符
	 * @param rests
	 *            剩余待连接块
	 * @return 链接结果
	 */
	public String append(String base, String op, String... rests) {
		String x = new String(base.toCharArray());
		for (String rest : rests)
			x += op + rest;
		return x;
	}

	/**
	 * 获取页面上单个url
	 * 
	 * @param body
	 *            页面内容
	 * @param regExp
	 *            正则表达式
	 * @return url
	 */
	public String getURL(String body, String regExp) {
		Pattern pat = Pattern.compile(regExp);
		Matcher mat = pat.matcher(body);
		if (mat.find())
			return mat.group(1);
		return null;
	}

	/**
	 * 获取页面上多个url，用list保存
	 * 
	 * @param body
	 *            页面内容
	 * @param regExp
	 *            正则表达式
	 * @return url list集合
	 */
	public List<String> getURLs(String body, String regExp) {
		Pattern pat = Pattern.compile(regExp);
		Matcher mat = pat.matcher(body);
		List<String> list = new ArrayList<String>();
		while (mat.find())
			list.add(mat.group(1));
		return list;
	}

	/**
	 * 去除冗余的页面信息
	 * 
	 * @param body
	 *            原始页面
	 * @return 处理过的页面
	 */
	public String trim(String body) {
		Matcher matcher = PAT_TRIM1.matcher(body);
		StringBuffer sb = new StringBuffer();
		while (matcher.find()) {
			Matcher matcher1 = PAT_TRIM2.matcher(matcher.group(1));
			String string = matcher1.replaceAll("");
			sb.append(string);
		}
		return sb.toString();
	}

	/**
	 * 处理ajax请求新浪微博微博的返回页面
	 * 
	 * @param body
	 *            返回的页面
	 * @return
	 */
	public String trim1(String body) {
		Pattern pat = Pattern.compile("\\\\n|\\\\t|\\\\r");
		Matcher matcher = pat.matcher(body);
		return matcher.replaceAll("");
	}

	public String trimUltimate(String body, String reg) {
		Pattern pat = Pattern.compile(reg);
		Matcher matcher = pat.matcher(body);
		return matcher.replaceAll("");
	}

	public String properString(String str, String regex, Integer... flags) {
		int i = 0;
		for (int f : flags)
			i |= f;
		Pattern pat = Pattern.compile(regex, i);
		Matcher matcher = pat.matcher(str);
		String string = matcher.replaceAll("");
		return string.trim();
	}

	public Object getLines(String str, String regex, int pos, int next, Object t, Integer... flags) {
		int i = 0;
		for (int f : flags)
			i |= f;
		Pattern pat;
		if (flags.length == 0) {
			pat = Pattern.compile(regex);
		} else {
			pat = Pattern.compile(regex, i);
		}
		Matcher matcher = pat.matcher(str);
		if (t instanceof List) {
			List<String> list = (List<String>) t;
			while (matcher.find())
				list.add(matcher.group(pos));
			return t;
		} else if (t instanceof String) {
			String temp = "0";
			if (next < 0) {
				while (matcher.find()) {
					temp = matcher.group(pos);
				}
			} else {
				for (int j = 0; j < next; j++)
					if (matcher.find())
						temp = matcher.group(pos);
			}
			return temp;
		}
		return "0";
	}

	/**
	 * 保存指定页面到指定位置中
	 * 
	 * @param body
	 *            页面内容
	 * @param location
	 *            路径
	 */
	public void outputPage(String body, String location) {
		try {
			File file = new File(location);
			if (!file.exists()) {
				file.createNewFile();
			} else {
				file.delete();
				file = new File(location);
			}
			FileOutputStream os = new FileOutputStream(file);
			os.write(body.getBytes());
			os.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public List<String> getFellows(String body) {

		return null;
	}

	public void save() {
		DB<Weibo> ddb = new DB<Weibo>(list, Weibo.class);
		new Thread(ddb).start();
	}

	/**
	 * 获取微博的程序入口点
	 */
	public void parsingPage() {
		Response res = getResponse(url);
		getPageInfo(res);
		int countPage = getCountPage();// 获取微博总页数
		System.out.println(countPage);
		List<FutureTask<Integer>> cs = new ArrayList<FutureTask<Integer>>();
		for (int i = pageID_page.getValue(); i <= countPage; i++) {// 每一页生成一个页面线程
			Crawler c;
			c = new Crawler(url, i, this, pageID_page);
			FutureTask<Integer> ft = new FutureTask<Integer>(c);
			cs.add(ft);
		}
		for (int i = 0; i <= countPage - 1; i++)// 提交线程池执行
			POOL.submit(cs.get(i));
		for (int i = 0; i <= countPage - 1; i++) {// 等待线程完成
			try { //
				cs.get(i).get();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		DB<Weibo> ddb = new DB<Weibo>(list, Weibo.class);
		new Thread(ddb).start();// 保存到数据库中
		System.out.println(list.size());
		// new Thread(db).start();
		pList.remove(this);
		pageID_page = null;
	}

	public ConcurrentLinkedQueue<Weibo> getQueue() {
		return list;
	}

	public static volatile int j = 0;
	public Object test = null;

	public static void main(String[] args) {
		ParsingPage pp = new ParsingPage("http://weibo.com/yaochen?is_all=1", "");
		pp.outputPage(pp.trim(pp.getResponse("http://weibo.com/yaochen?is_all=1").body()), "test.html");
		System.out.println(((User) pp.weiboSearch("长歌君")).getV());
		// Response res =
		// pp.getResponse("http://weibo.com/yaochen?is_all=1&page=19");
		// pp.getPageInfo(res);
		// String body = pp.trim(res.body());
		// pp.outputPage(body, "Test.html");
		// pp.getWeibos(body, "", 19);
		pp.getPageInfo(pp.getResponse());
		// for (int i = 1; i <= pp.getCountPage(); i++) {
		// Response res =
		// pp.getResponse("http://weibo.com/wuxymsn?is_all=1&page=" + i);
		// String r = pp.trim(res.body());
		// // pp.outputPage(r, "weibo0.html");
		// pp.getWeibos(r, pp.getPageInfo(res).get("onick"), i);
		// }
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		pList.add(this);
		parsingPage();
	}

	public Map<String, String> getPageInfo() {
		return pageInfo;
	}

	public void setPageInfo(Map<String, String> pageInfo) {
		this.pageInfo = pageInfo;
	}
}
