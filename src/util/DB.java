package util;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map.Entry;
import java.util.Scanner;
import java.util.Set;
import java.util.TreeSet;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.sql.Date;

import bean.Weibo;
import bean.WeiboCount;
import bean.Word;

import com.crawler.MainCrawler;
import com.mysql.jdbc.exceptions.jdbc4.MySQLIntegrityConstraintViolationException;
import com.segmenter.MainSegmenter;
import com.sun.corba.se.spi.orbutil.fsm.State;

public class DB<T> implements Runnable {
	public static final ThreadLocal<SimpleDateFormat> ddd = new ThreadLocal<SimpleDateFormat>() {
		protected SimpleDateFormat initialValue() {
			return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		}
	};
	private Connection conn = null;

	private Connection getConnection() {
		if (conn == null) {
			try {
				Class.forName("com.mysql.jdbc.Driver");
				conn = DriverManager.getConnection(
						"jdbc:mysql://localhost:3306/weibo?useUnicode=true&characterEncoding=utf8", "root", "");
				return conn;
			} catch (ClassNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return conn;
	}

	private String firstSet(String name) {
		char[] cs = name.toCharArray();
		if (cs[0] <= 'z' && cs[0] >= 'a') {
			cs[0] = (char) (cs[0] - 32);
			return new String(cs);
		} else if (cs[0] <= 'Z' && cs[0] >= 'A') {
			cs[0] = (char) (cs[0] + 32);
			return new String(cs);
		}
		return null;
	}

	private String getLast(String name) {
		String[] ss = name.split("\\.");
		return ss[ss.length - 1];
	}

	public void truncate(Class<?> cla) {
		String sql = "truncate table " + firstSet(getLast(cla.getName()));
		Connection conn = getConnection();
		try {
			Statement stmt = conn.createStatement();
			stmt.executeUpdate(sql);
			conn.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	public boolean isIn(Object o) {
		Class<?> cla = o.getClass();
		String sql = "select * from " + firstSet(getLast(cla.getName())) + " where ";
		Connection conn = getConnection();
		Field[] fields = cla.getDeclaredFields();
		try {
			for (int i = 0; i < fields.length; i++) {
				fields[i].setAccessible(true);
				if (i != fields.length - 1)
					sql += fields[i].getName() + "='" + fields[i].get(o) + "' and ";
				else
					sql += fields[i].getName() + "='" + fields[i].get(o) + "'";
			}
			Statement stmt = conn.createStatement();
			stmt.executeQuery("set names utf8mb4");
			boolean tttt = stmt.executeQuery(sql).next();
			conn.close();
			return tttt;

		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return false;
	}

	public void retrieve(Collection<T> values, Class<?> cla, String sql) {
		if (values == null || cla == null || sql == null)
			throw new NullPointerException();
		Field[] fields = cla.getDeclaredFields();
		if (cla.getSuperclass() != Object.class) {
			Field[] fs = cla.getSuperclass().getDeclaredFields();
			Field[] ffinal = new Field[fields.length + fs.length];
			for (int i = 0; i < fields.length; i++)
				ffinal[i] = fields[i];
			for (int i = fields.length; i < fields.length + fs.length; i++)
				ffinal[i] = fs[i - fields.length];
			fields = ffinal;
		}
		if (fields.length == 0)
			try {
				throw new ZeroFieldException();
			} catch (ZeroFieldException e1) {
				e1.printStackTrace();
			}
		Connection conn = getConnection();
		PreparedStatement stmt;
		try {
			stmt = conn.prepareStatement(sql);
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				Object o = cla.newInstance();
				for (int i = 0; i < fields.length; i++) {
					fields[i].setAccessible(true);
					char[] cs = fields[i].getName().toCharArray();
					cs[0] = (char) (cs[0] - 32);
					if (fields[i].getGenericType().getTypeName().equals("java.sql.Timestamp"))
						fields[i].set(o, rs.getTimestamp(fields[i].getName()));
					else if (fields[i].getGenericType().getTypeName().equals("java.lang.String"))
						fields[i].set(o, rs.getString(fields[i].getName()));
					else if (fields[i].getGenericType().getTypeName().equals("java.lang.Integer"))
						fields[i].set(o, rs.getInt(fields[i].getName()));
					else if (fields[i].getGenericType().getTypeName().equals("java.lang.Float"))
						fields[i].set(o, rs.getFloat(fields[i].getName()));
					else if (fields[i].getGenericType().getTypeName().equals("java.lang.Double"))
						fields[i].set(o, rs.getDouble(fields[i].getName()));
				}
				values.add((T) o);
			}
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}

	}

	public void retrieve(Collection<T> values, Class<?> cla, List<Entry<String, String>> restriction) {
		if (values == null || cla == null || restriction == null)
			throw new NullPointerException();
		Field[] fields = cla.getDeclaredFields();
		if (cla.getSuperclass() != Object.class) {
			Field[] fs = cla.getSuperclass().getDeclaredFields();
			Field[] ffinal = new Field[fields.length + fs.length];
			for (int i = 0; i < fields.length; i++)
				ffinal[i] = fields[i];
			for (int i = fields.length; i < fields.length + fs.length; i++)
				ffinal[i] = fs[i - fields.length];
			fields = ffinal;
		}
		if (fields.length == 0)
			try {
				throw new ZeroFieldException();
			} catch (ZeroFieldException e1) {
				e1.printStackTrace();
			}
		Connection conn = getConnection();
		String sql = "select * from " + firstSet(getLast(cla.getName())) + " where ";
		for (Entry<String, String> e : restriction)
			sql += e.getKey() + "=" + e.getValue() + " and ";
		sql = sql.substring(0, sql.length() - 6);

		try {
			PreparedStatement stmt = conn.prepareStatement(sql);
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				Object o = cla.newInstance();
				for (int i = 0; i < fields.length; i++) {
					fields[i].setAccessible(true);
					char[] cs = fields[i].getName().toCharArray();
					cs[0] = (char) (cs[0] - 32);
					if (fields[i].getGenericType().getTypeName().equals("java.sql.Timestamp"))
						fields[i].set(o, rs.getTimestamp(fields[i].getName()));
					else if (fields[i].getGenericType().getTypeName().equals("java.lang.String"))
						fields[i].set(o, rs.getString(fields[i].getName()));
					else if (fields[i].getGenericType().getTypeName().equals("java.lang.Integer"))
						fields[i].set(o, rs.getInt(fields[i].getName()));
					else if (fields[i].getGenericType().getTypeName().equals("java.lang.Float"))
						fields[i].set(o, rs.getFloat(fields[i].getName()));
					else if (fields[i].getGenericType().getTypeName().equals("java.lang.Double"))
						fields[i].set(o, rs.getDouble(fields[i].getName()));
				}
				values.add((T) o);
			}
			conn.close();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (SecurityException e1) {
			e1.printStackTrace();
		} catch (IllegalArgumentException e1) {
			e1.printStackTrace();
		}
	}

	public DB<T> retrieve(Collection<T> values, Class<?> cla) {
		if (values == null || cla == null)
			throw new NullPointerException();
		Field[] fields = cla.getDeclaredFields();
		if (cla.getSuperclass() != Object.class) {
			Field[] fs = cla.getSuperclass().getDeclaredFields();
			Field[] ffinal = new Field[fields.length + fs.length];
			for (int i = 0; i < fields.length; i++)
				ffinal[i] = fields[i];
			for (int i = fields.length; i < fields.length + fs.length; i++)
				ffinal[i] = fs[i - fields.length];
			fields = ffinal;
		}
		if (fields.length == 0)
			try {
				throw new ZeroFieldException();
			} catch (ZeroFieldException e1) {
				e1.printStackTrace();
			}
		Connection conn = getConnection();
		String sql = "select * from " + firstSet(getLast(cla.getName()));
		try {
			PreparedStatement stmt = conn.prepareStatement(sql);
			ResultSet rs = stmt.executeQuery(sql);
			while (rs.next()) {
				Object o = cla.newInstance();
				for (int i = 0; i < fields.length; i++) {
					fields[i].setAccessible(true);
					char[] cs = fields[i].getName().toCharArray();
					cs[0] = (char) (cs[0] - 32);
					if (fields[i].getGenericType().getTypeName().equals("java.sql.Timestamp"))
						fields[i].set(o, rs.getTimestamp(fields[i].getName()));
					else if (fields[i].getGenericType().getTypeName().equals("java.lang.String"))
						fields[i].set(o, rs.getString(fields[i].getName()));
					else if (fields[i].getGenericType().getTypeName().equals("java.lang.Integer"))
						fields[i].set(o, rs.getInt(fields[i].getName()));
					else if (fields[i].getGenericType().getTypeName().equals("java.lang.Float"))
						fields[i].set(o, rs.getFloat(fields[i].getName()));
					else if (fields[i].getGenericType().getTypeName().equals("java.lang.Double"))
						fields[i].set(o, rs.getDouble(fields[i].getName()));
				}
				values.add((T) o);
			}
			conn.close();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (SecurityException e1) {
			e1.printStackTrace();
		} catch (IllegalArgumentException e1) {
			e1.printStackTrace();
		}

		return this;
	}

	public DB<T> insert(Class<?> cla) {
		return insert(list, cla);
	}

	public DB<T> insert(Collection<T> values, Class<?> cla) {
		if (values == null || cla == null)
			throw new NullPointerException();
		try {
			Connection conn = getConnection();
			Field[] fields = cla.getDeclaredFields();
			if (cla.getSuperclass() != Object.class) {
				Field[] fs = cla.getSuperclass().getDeclaredFields();
				Field[] ffinal = new Field[fields.length + fs.length];
				for (int i = 0; i < fields.length; i++)
					ffinal[i] = fields[i];
				for (int i = fields.length; i < fields.length + fs.length; i++)
					ffinal[i] = fs[i - fields.length];
				fields = ffinal;
			}
			if (fields.length == 0)
				try {
					throw new ZeroFieldException();
				} catch (ZeroFieldException e1) {
					e1.printStackTrace();
				}
			String sql = "replace into " + firstSet(getLast(cla.getName())) + " values(";
			for (int i = 0; i < fields.length; i++) {
				if (i == 0)
					sql += "?";
				else
					sql += ",?";
			}
			sql += ")";
			conn.prepareStatement("set names utf8mb4").executeQuery();
			PreparedStatement stmt = conn.prepareStatement(sql);
			conn.setAutoCommit(false);
			int j = 0;
			for (T o : values) {
				j++;
				for (int i = 0; i < fields.length; i++) {
					char[] cs = fields[i].getName().toCharArray();
					cs[0] = (char) (cs[0] - 32);
					String temp = "get" + new String(cs);
					Method me = cla.getMethod(temp);
					Object result = me.invoke(o);
					try {
						if (result instanceof Timestamp)
							stmt.setTimestamp(i + 1, (Timestamp) result);
						else if (result instanceof String)
							stmt.setString(i + 1, (String) result);
						else if (result instanceof Integer)
							stmt.setInt(i + 1, (Integer) result);
						else if (result instanceof Float)
							stmt.setFloat(i + 1, (Float) result);
						else if (result instanceof Double)
							stmt.setDouble(i + 1, (Double) result);
					} catch (SQLException e) {
						if (!(e instanceof MySQLIntegrityConstraintViolationException))
							e.printStackTrace();
					}
				}
				stmt.execute();
			}
			conn.commit();
			MainCrawler.logger.debug("保存微博：" + j + "条");
			conn.close();
		} catch (SQLException e) {
			if (!(e instanceof MySQLIntegrityConstraintViolationException))
				e.printStackTrace();
		} catch (NoSuchMethodException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			e.printStackTrace();
		}
		return this;
	}

	public void update(Class<?> cla, String sql) {
		if (cla == null || sql == null)
			throw new NullPointerException();
		Field[] fields = cla.getDeclaredFields();
		if (cla.getSuperclass() != Object.class) {
			Field[] fs = cla.getSuperclass().getDeclaredFields();
			Field[] ffinal = new Field[fields.length + fs.length];
			for (int i = 0; i < fields.length; i++)
				ffinal[i] = fields[i];
			for (int i = fields.length; i < fields.length + fs.length; i++)
				ffinal[i] = fs[i - fields.length];
			fields = ffinal;
		}
		if (fields.length == 0)
			try {
				throw new ZeroFieldException();
			} catch (ZeroFieldException e1) {
				e1.printStackTrace();
			}
		Connection conn = getConnection();
		PreparedStatement stmt;
		try {
			stmt = conn.prepareStatement(sql);
			stmt.executeUpdate(sql);
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public void close() {
		try {
			conn.close();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	public int getCount(String sql) {
		Connection conn = getConnection();
		try {
			PreparedStatement stmt = conn.prepareStatement(sql);
			ResultSet rs = stmt.executeQuery(sql);
			if (rs.next())
				return rs.getInt(1);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return 0;
	}

	public static void main(String[] args) {
		DB<Weibo> db = new DB<Weibo>();
		ConcurrentLinkedQueue<Weibo> list = new ConcurrentLinkedQueue<Weibo>();
		db.retrieve(list, Weibo.class, "select * from weibo LIMIT 0,1000");
		System.out.println(list.size());
		MainSegmenter<Weibo, WeiboCount> ms = new MainSegmenter<Weibo, WeiboCount>(Weibo.class, WeiboCount.class, list,
				"", new Semaphore(0));
		try {
			ms.start(true, 0);
			Set<Word> count = ms.getCount1();
			for (Word en : count)
				System.out.println(en.getWord() + ":" + en.getAttr() + "   " + en.getCount());
			System.out.println("【".equals("【"));
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		}
	}

	public DB() {
		getConnection();
	}

	private Collection<T> list;
	private Class<?> cla;

	static class ZeroFieldException extends Exception {

		/**
		 * 自动生成的序列化id
		 */
		private static final long serialVersionUID = 5579786946292292848L;

	}

	public DB(Collection<T> list, Class<?> cla) {
		this.list = list;
		this.cla = cla;
		getConnection();
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub
		insert(cla);
	}
}

class DateLocal extends ThreadLocal<SimpleDateFormat> {
	protected SimpleDateFormat initialValue() {

		return new SimpleDateFormat();

	}
}
