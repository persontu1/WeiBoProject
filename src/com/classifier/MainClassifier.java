package com.classifier;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;

import com.github.abel533.echarts.Option;
import com.github.abel533.echarts.code.Trigger;
import com.google.gson.JsonObject;
import com.segmenter.MainSegmenter;
import com.segmenter.Segmenter;

import java.util.Set;
import java.util.TreeSet;

import action.Action;
import action.ActionFactory;
import bean.Word;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import util.DB;
import bean.TrainedData;
import bean.Weibo;
import bean.WeiboCount;

/**
 * 实现了Action抽象基类的分类器，实现了action的业务逻辑并承担分类器模块的工作
 * 
 * @author 杨埔生
 * @Time 2016-06-14 15:02:29
 *
 */
public class MainClassifier extends Action {
	/**
	 * 训练文本的数据结构，String是文本的类型名字，List<Set<Word>>是该类型对应的文本列表
	 */
	private static Map<String, List<Set<Word>>> articles = null;
	/**
	 * 计算py概率存储的中间值
	 */
	private Map<String, Double> pY = null;
	/**
	 * 保存article的文章总数
	 */
	private Integer count;
	/**
	 * 线程的最大个数，设为10
	 */
	private static final Integer THREAD_NUM = 10;
	/**
	 * 线程池，所有新开线程均需在线程池里执行
	 */
	private final static ExecutorService POOL = Executors.newFixedThreadPool(THREAD_NUM);
	// private static final String optionS =
	// "{\"tooltip\":{},\"toolbox\":{\"show\":true,\"feature\":{\"dataView\":{\"show\":true,\"readOnly\":false},\"restore\":{\"show\":true},\"saveAsImage\":{\"show\":true}}},\"animationDuration\":1500,\"animationEasingUpdate\":\"quinticInOut\"}";
	/**
	 * 半径模式，供生成json数据使用
	 */
	private static final String series1S = "{name:'半径模式',type:'pie',radius : [20, 110],center : ['25%', 200],roseType : 'radius',label: {normal: {show: false},emphasis: {show: true}},lableLine: {normal: {show: false},emphasis: {show: true}}}";
	/**
	 * 面积模式，供生成json数据使用
	 */
	private static final String series2S = "{name:'面积模式',type:'pie',radius : [30, 110],center : ['75%', 200],roseType : 'area'}";
	/**
	 * 需要添加的额外数据，供生成json数据使用
	 */
	private static final String legendS = "{x : 'center',y : 'bottom'}";
	/**
	 * 需要添加的额外数据，供生成json数据使用
	 */
	private static final String titleS = "{text: '微博情感倾向图',subtext: '如图'}";
	/**
	 * 需要添加的额外数据，供生成json数据使用
	 */
	private static final String optionS = " {tooltip : {trigger: 'item',formatter: \"{a} <br/>{b} : {c} ({d}%)\"}, toolbox: {show : true,feature : {mark : {show: true},dataView : {show: true, readOnly: false},magicType : {show: true,type: ['pie', 'funnel']},restore : {show: true},saveAsImage : {show: true}}},calculable : true}";

	public MainClassifier(Map<String, List<Set<Word>>> articles) {
		this.articles = articles;
	}

	public MainClassifier() {

	}

	/**
	 * 加载该类时初始化article
	 */
	static {
		try {
			articles = initFileSeg1("D:\\eclipse\\new");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * 从数据库中获取训练文本
	 */
	private void initArticleFromDB() {
		long s = System.currentTimeMillis();
		ConcurrentLinkedQueue<TrainedData> queue = new ConcurrentLinkedQueue<TrainedData>();
		DB<TrainedData> db = new DB<TrainedData>();
		db.retrieve(queue, TrainedData.class, "select * from trainedData");

		double d = (double) (System.currentTimeMillis() - s);
		System.out.println(d / 1000 + "秒");
	}

	/**
	 * 计算重复词的总数
	 * 
	 * @return 返回重复词总数
	 */
	private int totalWords() {
		int count = 0;
		for (Entry<String, List<Set<Word>>> type : articles.entrySet())
			for (Set<Word> set : type.getValue())
				for (Word word : set)
					count += word.getCount();
		return count;
	}

	/**
	 * 计算文本总数
	 * 
	 * @return 返回文本总数
	 */
	private int totalArticles() {
		if (count == null) {
			count = 0;
			for (Entry<String, List<Set<Word>>> type : articles.entrySet())
				count += type.getValue().size();
			return count;
		} else
			return count;
	}

	/**
	 * 计算不重复词总数
	 * 
	 * @return 返回不重复词总数
	 */
	private long unRepeat() {
		long count = 0;
		for (Entry<String, List<Set<Word>>> type : articles.entrySet()) {
			List<Set<Word>> arts = type.getValue();
			for (Set<Word> set : arts)
				for (Word word : set)
					count++;
		}
		return count;
	}

	/**
	 * 工具方法，返回封装好的键值对
	 * 
	 * @param t
	 *            键
	 * @param d
	 *            值
	 * @return 键值对
	 */
	private static <T, D> Entry<T, D> entry(T t, D d) {
		return new AbstractMap.SimpleEntry<T, D>(t, d);
	}

	/**
	 * 计算pY概率
	 * 
	 * @return pY概率
	 */
	private Map<String, Double> computePY() {
		pY = new LinkedHashMap<String, Double>();
		for (Entry<String, List<Set<Word>>> type : articles.entrySet())
			pY.put(type.getKey(), ((double) type.getValue().size() / totalArticles()));
		return pY;
	}

	/****
	 * 通过传入的文本words计算P(x|y)的值
	 * 
	 * @param words
	 *            已分好词的集合，即文本向量
	 * @return 返回计算结果集合，该集合是文本向量在各个类型下的条件概率降序集合
	 */
	private Map<String, Double> computeXInY(Set<Word> words) {
		Map<String, Double> map = new LinkedHashMap<String, Double>();
		for (Entry<String, List<Set<Word>>> type : articles.entrySet()) {
			double temp = 1;
			List<Set<Word>> arts = type.getValue();
			String ty = type.getKey();
			long c2 = 0;
			for (Set<Word> art : arts)/// 保存该类下文章的总词数
				for (Word w : art)
					c2 += w.getCount();
			// long c3 = unRepeat();
			for (Word word : words) {
				long c1 = 0;
				for (Set<Word> art : arts)/// 统计该词下在该类下的频数
					for (Word w : art)
						if (w.equals(word))
							c1 += w.getCount();
				temp *= ((double) (c1 + 1)) / (c2);
			}
			map.put(ty, temp);
		}
		return map;
	}

	/**
	 * 计算P(XY)值，返回计算结果集合，该集合是文本向量与各个类型下的条件概率降序集合
	 * 
	 * @param words
	 *            已分好词的集合，即文本向量，类型Word是抽象bean，由tomcat自带WebAppClassLoader加载，
	 *            有多种派生类，如WeiboCount类等
	 * @return 返回计算结果集合
	 */
	private Set<Entry<String, Double>> computXY(Set<Word> words) {
		long s = System.currentTimeMillis();
		Map<String, Double> xInY = computeXInY(words);// 计算条件概率P(X|Y)，返回的是条件概率集合，根据P(XY)=P(X|Y)*P(Y)
		Map<String, Double> Y = computePY();// 计算P(Y)
		Set<Entry<String, Double>> set = new TreeSet<Entry<String, Double>>(
				(o1, o2) -> (o1.getValue() - o2.getValue()) > 0 ? -1 : 1);// 降序集合set
		for (Entry<String, List<Set<Word>>> type : articles.entrySet()) {
			String ty = type.getKey();
			Double xIy = xInY.get(ty);
			Double y = Y.get(ty);
			Double xy = xIy * y;// 计算最终P(XY)
			set.add(entry(ty, xy));
		}
		double d = (double) (System.currentTimeMillis() - s);
		System.out.println("计算xy时间：" + d / 1000 + "秒");
		return set;
	}

	/**
	 * 测试用例
	 */
	private void showArticles() {
		for (Entry<String, List<Set<Word>>> en : this.articles.entrySet()) {
			for (Set<Word> set : en.getValue()) {
				for (Word word : set) {
					System.out.print(word + "  ");
				}
				System.out.println("");
				System.out.println("------------------");
			}
			System.out.println("-------类型分割线----------");
		}
	}

	/**
	 * 保存训练文本到数据库中
	 */
	private void saveArticles() {
		long s = System.currentTimeMillis();
		List<TrainedData> list = new ArrayList<TrainedData>();
		for (Entry<String, List<Set<Word>>> en : this.articles.entrySet()) {
			String typeName = en.getKey();
			for (Set<Word> set : en.getValue()) {
				for (Word word : set) {
					TrainedData td = new TrainedData();
					td.setType(typeName);
					td.setAttr(word.getAttr());
					td.setWord(word.getWord());
					td.setCount(word.getCount());
					list.add(td);
				}
			}
		}
		new DB<TrainedData>().insert(list, TrainedData.class);
		double d = (double) (System.currentTimeMillis() - s);
		System.out.println("保存时间：" + d / 1000 + "秒");
	}

	/**
	 * MainClassifier类的action实现，封装了业务逻辑
	 */
	@SuppressWarnings("unchecked")
	@Override
	public Object action(String act) {
		// TODO Auto-generated method stub
		Object value = getAction(act);
		if (value != null) {
			if (act.equals("init_ariticle")) {// 初始化训练文本
				articles = (Map<String, List<Set<Word>>>) value;
				System.out.println(articles.size());
				System.out.println(totalWords());
			} else if (act.equals("classify")) {// 分类操作
				if (value instanceof Set) {// 若传入的值为Set类型，表明该Set是一个文本向量，计算P(XY)值集合，并生成json数据返回上层servlet
					Set<Entry<String, Double>> result = computXY((Set<Word>) value);// 计算P(XY)值获得降序集合
					JSONObject option = JSONObject.fromObject(optionS);
					JSONObject title = JSONObject.fromObject(titleS);
					JSONObject legend = JSONObject.fromObject(legendS);
					JSONObject series1 = JSONObject.fromObject(series1S);
					JSONObject series2 = JSONObject.fromObject(series2S);
					JSONArray data = new JSONArray();
					JSONArray legenData = new JSONArray();
					double r = 0;
					for (Entry<String, Double> en : result)
						r = en.getValue();
					System.out.println(r);
					for (Entry<String, Double> en : result) {
						JSONObject d = new JSONObject();
						d.accumulate("value", (100 + (int) (en.getValue() / r)));
						d.accumulate("name", en.getKey());
						legenData.add(en.getKey());
						data.add(d);
					}
					legend.accumulate("data", legenData);
					option.accumulate("legend", legend);

					option.accumulate("title", title);

					series1.accumulate("data", data);
					series2.accumulate("data", data);
					JSONArray series = new JSONArray();
					series.add(series1);
					series.add(series2);
					option.accumulate("series", series);
					return option;
				}
			} else if (act.equals("save_article")) {// 保存训练文本操作
				saveArticles();
			} else if (act.equals("classifies")) {// 大规模分类，对文本向量队列进行并发分类，这里的并发模型采用了生产者-消费者模型，但没有生产者，共有10个消费者
				ConcurrentLinkedQueue<Set<Word>> input = (ConcurrentLinkedQueue<Set<Word>>) value;
				List<FutureTask<Integer>> list = new ArrayList<FutureTask<Integer>>();
				Map<String, Integer> result = new LinkedHashMap<String, Integer>();
				for (int i = 0; i < 10; i++)
					list.add(new FutureTask<Integer>(() -> {
						Set<Word> set;
						while ((set = input.poll()) != null) {
							Set<Entry<String, Double>> re = computXY(set);
							Entry<String, Double> f = new AbstractMap.SimpleEntry<String, Double>("", 0.0);
							for (Entry<String, Double> en : re) {
								f = en;
								break;
							}
							synchronized (result) {
								if (result.containsKey(f.getKey())) {
									result.put(f.getKey(), result.get(f.getKey()) + 1);
								} else {
									result.put(f.getKey(), 1);
								}
							}
						}
					}, 0));
				for (int i = 0; i < 10; i++)
					POOL.submit(list.get(i));
				for (int i = 0; i < 10; i++)
					try {
						list.get(i).get();
					} catch (InterruptedException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} catch (ExecutionException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				JSONObject option = JSONObject.fromObject(optionS);
				JSONObject title = JSONObject.fromObject(titleS);
				JSONObject legend = JSONObject.fromObject(legendS);
				JSONObject series1 = JSONObject.fromObject(series1S);
				JSONObject series2 = JSONObject.fromObject(series2S);
				JSONArray data = new JSONArray();
				JSONArray legenData = new JSONArray();

				for (Entry<String, Integer> en : result.entrySet()) {
					JSONObject d = new JSONObject();
					d.accumulate("value", en.getValue());
					d.accumulate("name", en.getKey());
					legenData.add(en.getKey());
					data.add(d);
				}
				legend.accumulate("data", legenData);
				option.accumulate("legend", legend);

				option.accumulate("title", title);

				series1.accumulate("data", data);
				series2.accumulate("data", data);
				JSONArray series = new JSONArray();
				series.add(series1);
				series.add(series2);
				option.accumulate("series", series);
				return option;
			}
		}
		return null;
	}

	public static void main(String[] args) {
		MainClassifier mc = new MainClassifier();
		File file = new File("test11.txt");
		try {
			Scanner sc = new Scanner(file);
			Map<String, Integer> count = new HashMap<String, Integer>();
			ExecutorService pool = Executors.newFixedThreadPool(20);
			ConcurrentLinkedQueue<String> queue = new ConcurrentLinkedQueue<String>();
			while (sc.hasNextLine())
				queue.add(sc.nextLine());
			for (String s : queue) {
				Set<Word> words = Segmenter.segment(s, new WeiboCount());
				Set<Entry<String, Double>> re = mc.computXY(words);
				// System.out.println(re);
				Entry<String, Double> max = null;
				for (Entry<String, Double> e : re) {
					max = e;
					break;
				}
				synchronized (count) {
					if (!count.containsKey(max.getKey()))
						count.put(max.getKey(), 1);
					else
						count.put(max.getKey(), count.get(max.getKey()) + 1);
				}
			}
			sc.close();
			System.out.println(count);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public static Number getRealVaule(double value, int resLen) {
		if (resLen == 0)
			// 原理:123.456*10=1234.56+5=1239.56/10=123
			// 原理:123.556*10=1235.56+5=1240.56/10=124
			return Math.round(value * 10 + 5) / 10;
		double db = Math.pow(10, resLen);
		return Math.round(value * db) / db;
	}

	/**
	 * 初始化训练文本操作1.0（处理已分好词的文本）
	 * 
	 * @param url
	 *            制定训练文本文件夹位置
	 * @return 返回训练文本集合
	 * @throws FileNotFoundException
	 */
	public static Map<String, List<Set<Word>>> initFileSeg1(String url) throws FileNotFoundException {
		long ss = System.currentTimeMillis();
		File file = null;
		if (url == null)
			file = new File("new");
		else
			file = new File(url);
		if (!file.isDirectory())
			return null;
		Map<String, List<Set<Word>>> articles = new LinkedHashMap<String, List<Set<Word>>>();
		for (File f : file.listFiles()) {
			String typeName = f.getName().substring(0, f.getName().length() - 4);
			Scanner sc = new Scanner(f);
			List<Set<Word>> list = new ArrayList<Set<Word>>();
			MainSegmenter<Weibo, WeiboCount> ms = new MainSegmenter<Weibo, WeiboCount>(Weibo.class, WeiboCount.class);
			while (sc.hasNextLine()) {
				String s = sc.nextLine();
				Weibo weibo = new Weibo();
				weibo.setWeiboContent(s);
				ms.put(weibo);
			}
			sc.close();
			try {
				ms.start(true, 1);
				list.add(ms.getCount11());
				articles.put(typeName, list);
			} catch (InterruptedException e) {
				System.out.println(ms.getCount1().size());
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (ExecutionException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}
		double d = (double) (System.currentTimeMillis() - ss);
		System.out.println("获取训练文本时间：" + d / 1000 + "秒");
		return articles;
	}

	/**
	 * 初始化训练文本操作2.0（处理未分好词的版本）
	 * 
	 * @param url
	 *            制定训练文本文件夹位置
	 * @return 返回训练文本集合
	 * @throws FileNotFoundException
	 */
	public static Map<String, List<Set<Word>>> initFileSeg(String url) throws FileNotFoundException {
		File file = null;
		if (url == null)
			file = new File("htl_del_4000");
		else
			file = new File(url);
		if (!file.isDirectory())
			return null;
		Map<String, List<Set<Word>>> articles = new LinkedHashMap<String, List<Set<Word>>>();
		for (File f : file.listFiles()) {
			if (f.isDirectory()) {
				String typeName = f.getName();
				List<Set<Word>> list = new ArrayList<Set<Word>>();
				MainSegmenter<Weibo, WeiboCount> ms = new MainSegmenter<Weibo, WeiboCount>(Weibo.class,
						WeiboCount.class);
				for (File f1 : f.listFiles()) {
					StringBuffer sb = new StringBuffer();
					Scanner sc = new Scanner(f1);
					while (sc.hasNextLine()) {
						String s = sc.nextLine();
						if (s.equals("content>"))
							continue;
						sb.append(s);
					}
					sc.close();
					Weibo weibo = new Weibo();
					weibo.setWeiboContent(sb.toString());
					ms.put(weibo);
				}
				try {
					ms.start(true, 0);
					list.add(ms.getCount11());
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ExecutionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
				articles.put(typeName, list);
			}
		}
		return articles;
	}
}
