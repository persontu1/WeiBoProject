package bean;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

import util.DB;

public class WeiboCount extends Word {
	private String page_id;
	private String onick;

	public String getPage_id() {
		return page_id;
	}

	public void setPage_id(String page_id) {
		this.page_id = page_id;
	}

	public String getOnick() {
		return onick;
	}

	public void setOnick(String onick) {
		this.onick = onick;
	}

	public static void main(String[] args) {
		WeiboCount wc = new WeiboCount();
		for (Field f : WeiboCount.class.getSuperclass().getDeclaredFields()) {
			f.setAccessible(true);
			try {
				if (f.getGenericType().getTypeName().equals("java.lang.String"))
					f.set(wc, "¹þ¹þ");
				else if (f.getGenericType().getTypeName().equals("java.lang.Integer"))
					f.set(wc, 1);
				System.out.println(wc.getAttr() + "  " + wc.getWord() + "  " + wc.getCount());
			} catch (IllegalArgumentException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IllegalAccessException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println(f);
		}
		System.out.println(Unfinish.class.getSuperclass() == Object.class);
		wc.setWord("sdf");
		wc.setPage_id("fdsf");
		wc.setOnick("hgdf");
		List<WeiboCount> values = new ArrayList<WeiboCount>();
		values.add(wc);
		new DB<WeiboCount>().insert(values, WeiboCount.class);
		System.out.println(wc.getWord());
	}
}
