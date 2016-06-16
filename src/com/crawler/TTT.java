package com.crawler;

import java.awt.BorderLayout;
import java.awt.EventQueue;

import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;

import bean.User;
import bean.Weibo;
import util.DB;

import javax.swing.JButton;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.awt.event.ActionEvent;
import javax.swing.JTextField;
import javax.swing.JSplitPane;
import javax.swing.JTextPane;

public class TTT extends JFrame {

	private static final long serialVersionUID = 1L;

	private static final Pattern pat = Pattern
			.compile("([a-zA-Z].+)://([a-zA-Z0-9].+)\\.([a-z].+)((/([0-9a-zA-Z].+))*)\\?((.+)=(.*))((&((.+)=(.+)))*)");

	private JPanel contentPane;
	private JTextField nameField;
	private JButton suspend;
	private ConcurrentLinkedQueue<Weibo> list = new ConcurrentLinkedQueue<Weibo>();
	private JButton conti;
	private Semaphore sema = new Semaphore(0);
	private JButton savePoint;
	private Entry<String, Integer> pageID_page = new AbstractMap.SimpleEntry<String, Integer>("", 0);
	private ParsingPage pp;
	private MainCrawler mc;
	private JTextField textField;
	private String lock = "";
	private JButton start;
	private ConcurrentLinkedQueue<ParsingPage> pList = new ConcurrentLinkedQueue<ParsingPage>();
	private boolean flag = false;
	private ConcurrentLinkedQueue<User> userQueue = new ConcurrentLinkedQueue<User>();

	/**
	 * Launch the application.
	 */
	public static void main(String[] args) {
		EventQueue.invokeLater(new Runnable() {
			public void run() {
				try {
					TTT frame = new TTT();
					frame.setVisible(true);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		});
	}

	/**
	 * Create the frame.
	 */
	public TTT() {
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setBounds(100, 100, 775, 478);
		contentPane = new JPanel();
		contentPane.setBorder(new EmptyBorder(5, 5, 5, 5));
		setContentPane(contentPane);
		contentPane.setLayout(null);

		JButton singleStart = new JButton("单步开始");
		singleStart.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String name = nameField.getText();
				Matcher mat = pat.matcher(name);
				if (mat.find()) {
					new Thread(new Runnable() {

						@Override
						public void run() {
							singleStart.setEnabled(false);
							pp = new ParsingPage(name, lock);
							pp.setQueue(list);
							pp.getPageInfo(pp.getResponse(name));
							Map<String, String> map = pp.getPageInfo(pp.getResponse(name));
							pageID_page = new AbstractMap.SimpleEntry<String, Integer>(map.get("page_id"), 0);
							pp.setPageID_page(pageID_page);
							suspend.setEnabled(true);
							pp.parsingPage();
							deletePoint();
						}
					}).start();
				}
			}
		});
		singleStart.setBounds(65, 37, 81, 23);
		contentPane.add(singleStart);

		nameField = new JTextField();
		nameField.setBounds(330, 38, 298, 21);
		contentPane.add(nameField);
		nameField.setColumns(10);

		suspend = new JButton("暂停");
		suspend.setEnabled(false);
		suspend.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new Thread(new Runnable() {
					@Override
					public void run() {
						synchronized (lock) {
							suspend.setEnabled(false);
							conti.setEnabled(true);
							savePoint.setEnabled(true);
							try {
								sema.acquire();
							} catch (InterruptedException e1) {
								e1.printStackTrace();
							}
						}
					}
				}).start();
			}
		});
		suspend.setBounds(156, 69, 66, 23);
		contentPane.add(suspend);

		conti = new JButton("继续");
		conti.setEnabled(false);
		conti.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				sema.release();
				suspend.setEnabled(true);
				conti.setEnabled(false);
				savePoint.setEnabled(false);
			}
		});
		conti.setBounds(156, 102, 66, 23);
		contentPane.add(conti);

		savePoint = new JButton("保存状态");
		savePoint.setEnabled(false);
		savePoint.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (!flag) {
					savePoint();
					pp.save();
				} else {
					mc.save();
				}
			}
		});
		savePoint.setBounds(232, 69, 81, 23);
		contentPane.add(savePoint);

		JPanel panel = new JPanel();
		panel.setBounds(65, 155, 374, 156);
		contentPane.add(panel);
		panel.setLayout(null);

		JButton search = new JButton("搜索");
		search.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
			}
		});
		search.setBounds(10, 10, 93, 23);
		panel.add(search);

		textField = new JTextField();
		textField.setBounds(113, 11, 198, 21);
		panel.add(textField);
		textField.setColumns(10);

		start = new JButton("开始");
		start.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				String name = nameField.getText();
				Matcher mat = pat.matcher(name);
				if (mat.find()) {
					flag = true;
					mc = new MainCrawler(name, lock, pList, list, userQueue);
					new Thread(new Runnable() {

						@Override
						public void run() {
							// TODO Auto-generated method stub
							start.setEnabled(false);
							singleStart.setEnabled(false);
							suspend.setEnabled(true);
							mc.start();
						}
					}).start();
					new Thread(new Runnable() {

						@Override
						public void run() {
							// TODO Auto-generated method stub
							while (true) {
								try {
									Thread.sleep(1000 * 60 * 5);
									new DB<Weibo>().insert(list, Weibo.class);
									list.clear();
								} catch (InterruptedException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}
							}
						}
					}).start();
				}
			}
		});
		start.setBounds(156, 37, 66, 23);
		contentPane.add(start);

		JButton btnNewButton = new JButton("恢复");
		btnNewButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				flag = true;
				mc = new MainCrawler(lock, pList, list);
				new Thread(new Runnable() {

					@Override
					public void run() {
						// TODO Auto-generated method stub
						start.setEnabled(false);
						singleStart.setEnabled(false);
						suspend.setEnabled(true);
						mc.start(true);
					}
				}).start();
				new Thread(new Runnable() {

					@Override
					public void run() {
						// TODO Auto-generated method stub
						while (true) {
							try {
								Thread.sleep(1000 * 60 * 5);
								new DB<Weibo>().insert(list, Weibo.class);
								list.clear();
							} catch (InterruptedException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}
					}
				}).start();

			}
		});
		btnNewButton.setBounds(232, 37, 81, 23);
		contentPane.add(btnNewButton);

		JButton btncookie = new JButton("\u8BBE\u7F6Ecookie");
		btncookie.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				ParsingPage.setCookie();
			}
		});
		btncookie.setBounds(330, 69, 93, 23);
		contentPane.add(btncookie);
	}

	private void savePoint() {
		try {
			File file = new File("save/" + pageID_page.getKey());
			if (!file.exists())
				file.createNewFile();
			FileWriter fw = new FileWriter(file);
			fw.write(pageID_page.getKey() + ":" + pageID_page.getValue());
			fw.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	private void deletePoint() {
		File file = new File("save/" + pageID_page.getKey());
		file.delete();
	}
}
