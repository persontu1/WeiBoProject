package com.segmenter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.Semaphore;

import action.Action;
import action.ActionFactory;

import bean.WeiboCount;
import bean.Word;
import bean.Weibo;

public class MainSegmenter<T, W extends Word> extends Action {
	private final static int THREAD_NUM = 10;
	private final static ExecutorService POOL = Executors.newFixedThreadPool(THREAD_NUM);
	private Semaphore sema;
	private ConcurrentLinkedQueue<T> queue = new ConcurrentLinkedQueue<T>();
	private LinkedHashMap<String, Entry<String, Integer>> count = new LinkedHashMap<String, Entry<String, Integer>>();
	private Set<Word> count1 = new TreeSet<Word>(
			(o1, o2) -> o1.equals(o2) ? 0 : o2.getCount() - o1.getCount() > 0 ? 1 : -1);
	private String lock;

	private Class<T> cl1;
	private Class<W> cl2;

	public void put(T t) {
		queue.add(t);
	}

	public Set<Word> getCount11() {
		return count1;
	}

	public MainSegmenter(Class<T> cl1, Class<W> cl2, ConcurrentLinkedQueue<T> queue, String lock, Semaphore sema) {
		this.cl1 = cl1;
		this.queue = queue;
		this.lock = lock;
		this.sema = sema;
		this.cl2 = cl2;
	}

	public MainSegmenter(Class<T> cl1, Class<W> cl2) {
		this.cl1 = cl1;
		this.cl2 = cl2;
		lock = "";
	}

	public MainSegmenter() {
		// TODO Auto-generated constructor stub
		lock = "";
	}

	public LinkedHashMap<String, Entry<String, Integer>> getCount() {
		return count;
	}

	public Set<Word> getCount1() {
		return count1;
	}

	private class Wait implements Runnable {
		private List<FutureTask<Integer>> list;

		public Wait(List<FutureTask<Integer>> list) {
			this.list = list;
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			for (FutureTask<Integer> task : list) {
				try {
					task.get();
				} catch (InterruptedException e) {
					e.printStackTrace();
				} catch (ExecutionException e) {
					e.printStackTrace();
				}
			}
			for (Entry<String, Entry<String, Integer>> en : count.entrySet()) {
				try {
					W w = cl2.newInstance();
					w.setAttr(en.getValue().getKey());
					w.setCount(en.getValue().getValue());
					w.setWord(en.getKey());
					count1.add(w);
				} catch (InstantiationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

	}

	public void start(boolean isBlock, int flag) throws InterruptedException, ExecutionException {
		List<FutureTask<Integer>> list = new ArrayList<FutureTask<Integer>>();
		for (int i = 0; i < 10; i++)
			list.add(new FutureTask<Integer>(new Segmenter<T>(cl1, queue, count, lock, sema, flag)));
		for (FutureTask<Integer> task : list)
			POOL.submit(task);
		if (isBlock) {/// ÊÇ·ñµÈ´ý
			for (FutureTask<Integer> task : list)
				task.get();
			for (Entry<String, Entry<String, Integer>> en : count.entrySet()) {
				try {
					W w = cl2.newInstance();
					w.setAttr(en.getValue().getKey());
					w.setCount(en.getValue().getValue());
					w.setWord(en.getKey());
					count1.add(w);
				} catch (InstantiationException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		} else
			new Thread(new Wait(list)).start();
	}

	public static void main(String[] args) {
		Semaphore sema = new Semaphore(0);
		try {
			sema.acquire();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(-THREAD_NUM + 1);
	}

	@SuppressWarnings("unchecked")
	@Override
	public Object action(String act) {
		// TODO Auto-generated method stub
		Object value = getAction(act);
		if (act.equals("segment")) {
			if (value != null) {
				Set<Word> set = Segmenter.segment((String) value, new WeiboCount());
				return set;
			}
		} else if (act.equals("ack_classifier")) {
			if (value instanceof String) {
				String s = (String) value;
				if (value.equals("init_file_seg")) {

					Action action = ActionFactory.newInstance("MainClassifier");
					action.putAction(act, value);
					action.action(act);
				}
			}
		} else if (act.equals("segments")) {
			if (value != null) {
				ConcurrentLinkedQueue<Weibo> queue = (ConcurrentLinkedQueue<Weibo>) value;
				ConcurrentLinkedQueue<Set<Word>> result = new ConcurrentLinkedQueue<Set<Word>>();
				List<FutureTask<Integer>> list = new ArrayList<FutureTask<Integer>>();
				for (int i = 0; i < 10; i++)
					list.add(new FutureTask<Integer>(() -> {
						Weibo word;
						while ((word = queue.poll()) != null)
							result.add(Segmenter.segment(word.getWeiboContent(), new WeiboCount()));
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
				return result;

			}

		}
		return null;
	}

}