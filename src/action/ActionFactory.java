package action;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ConcurrentHashMap;

import util.Loader;

/**
 * Action的工厂，用来获取Action的实例
 * 
 * @author 杨埔生
 * @Time 2016-06-14 15:02:29
 */
public class ActionFactory {
	/**
	 * loader自定义类加载器，负责加载业务逻辑相关的类，如com.*下的所有类，bean.*下的所有类，目的是将业务逻辑与servlet分离，
	 * 保持独立性。当ActionFactory类被tomcat的WebAppClassLoader加载时本字段被初始化，
	 * 参数是加载了ActionFactory的类加载器即WebAppClassLoader,
	 * 这表明该loader自定义加载器是的父加载器是WebAppClassLoader。
	 */
	private static final Loader loader = Loader.NewInstance1(ActionFactory.class.getClassLoader());
	/**
	 * 保存由loader加载进来的类，类型为并发安全的哈希表
	 */
	private static final ConcurrentHashMap<String, Class<?>> CLAMAP = new ConcurrentHashMap<>();
	/**
	 * 当tomcat的WebAppClassLoader加载该工厂类时，
	 * 由loader从class文件保存在ecilpse安装目录下的classes文件夹中加载业务相关类，所有业务相关类的class文件均保存在这里
	 * 
	 */
	static {
		File fileXMLLocation = new File("classes");
		for (File file : fileXMLLocation.listFiles())
			if (file.getName().endsWith(".xml")) {
				Class<?> cla = (Class<?>) loader.getClassInfo(file.getAbsolutePath()).get(0);
				CLAMAP.put(cla.getName(), cla);
			}

		System.out.println(CLAMAP.size());
	}

	/**
	 * 返回该自定义类加载器
	 * 
	 * @return
	 */
	public static Loader getLoader() {
		return loader;
	}

	/**
	 * 测试用例
	 */
	public static void test() {
		System.out.println("sdfs");
	}

	/**
	 * 通过制定类的全限定名与类路径使用自定义类加载器加载指定的实现了Action接口的类，并返回由该类无参构造方法实例化的对象,该方法是线程安全
	 * 
	 * @param name
	 *            类的全限定名，如com.crawler.MainCrawler。
	 * @param path
	 *            该类的.class文件路径名（相对绝对路径都行）
	 * @return action 若加载该类成功，返回该类实例化的Action对象，不成功则返回null
	 */
	public static synchronized Action newInstance1(String name, String path) {
		loader.setPath(path);
		Class<?> cl = loader.getClass(name);
		try {
			Action action = (Action) cl.newInstance();// 无参实例化
			return action;
		} catch (InstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	/**
	 * 通过制定类的全限定名获取CLAMAP里与该限定名成对的类，并用该类实例化该类的对象
	 * 
	 * @param name
	 *            接受类的全限定名，如com.crawler.MainCrawler等
	 * @return 如CLAMAP获取类成功，则返回该类实例化的对象，不成功则返回null；
	 */
	public static Action newInstance(String name) {
		if (CLAMAP.containsKey(name)) {
			if (name.equals("com.segmenter.MainSegmenter")) {
				try {
					return (Action) CLAMAP.get(name).getDeclaredConstructor(Class.class, Class.class)
							.newInstance(CLAMAP.get("bean.Weibo"), CLAMAP.get("bean.WeiboCount"));
				} catch (IllegalArgumentException | InvocationTargetException | NoSuchMethodException
						| SecurityException | InstantiationException | IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			} else {
				try {
					return (Action) CLAMAP.get(name).newInstance();
				} catch (InstantiationException | IllegalAccessException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		// if (name.equals(MainCrawler.class.getSimpleName()))
		// return new MainCrawler();
		// else if (name.equals(MainSegmenter.class.getSimpleName()))
		// return new MainSegmenter<Weibo, WeiboCount>(Weibo.class,
		// WeiboCount.class);
		// else if (name.equals(MainClassifier.class.getSimpleName()))
		// return new MainClassifier();
		// else if (name.equals(MainExtractor.class.getSimpleName()))
		// return new MainExtractor();
		return null;
	}

}
