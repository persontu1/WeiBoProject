package test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.LinkedHashMap;

import util.Loader;

public class Test<T> extends LinkedHashMap<T, String> {
	private static final long serialVersionUID = -7685536507878571805L;
	protected String name;

	public void setName(String name) {
		this.name = name;
	}

	public static void main(String[] args) {
		Loader loader = Loader.NewInstance("/Work/WeiBoProject/WebContent/Unfinish.class");
		Class<?> cl1 = loader.getClass("bean.Unfinish");
		System.out.println(cl1.getClassLoader());
	}
}

class dde extends Test {
	public void test() {
		System.out.println(name);
	}
}