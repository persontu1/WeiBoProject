package action;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.util.concurrent.ConcurrentHashMap;

import util.Loader;

/**
 * Action�Ĺ�����������ȡAction��ʵ��
 * 
 * @author ������
 * @Time 2016-06-14 15:02:29
 */
public class ActionFactory {
	/**
	 * loader�Զ�������������������ҵ���߼���ص��࣬��com.*�µ������࣬bean.*�µ������࣬Ŀ���ǽ�ҵ���߼���servlet���룬
	 * ���ֶ����ԡ���ActionFactory�౻tomcat��WebAppClassLoader����ʱ���ֶα���ʼ����
	 * �����Ǽ�����ActionFactory�����������WebAppClassLoader,
	 * �������loader�Զ���������ǵĸ���������WebAppClassLoader��
	 */
	private static final Loader loader = Loader.NewInstance1(ActionFactory.class.getClassLoader());
	/**
	 * ������loader���ؽ������࣬����Ϊ������ȫ�Ĺ�ϣ��
	 */
	private static final ConcurrentHashMap<String, Class<?>> CLAMAP = new ConcurrentHashMap<>();
	/**
	 * ��tomcat��WebAppClassLoader���ظù�����ʱ��
	 * ��loader��class�ļ�������ecilpse��װĿ¼�µ�classes�ļ����м���ҵ������࣬����ҵ��������class�ļ�������������
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
	 * ���ظ��Զ����������
	 * 
	 * @return
	 */
	public static Loader getLoader() {
		return loader;
	}

	/**
	 * ��������
	 */
	public static void test() {
		System.out.println("sdfs");
	}

	/**
	 * ͨ���ƶ����ȫ�޶�������·��ʹ���Զ��������������ָ����ʵ����Action�ӿڵ��࣬�������ɸ����޲ι��췽��ʵ�����Ķ���,�÷������̰߳�ȫ
	 * 
	 * @param name
	 *            ���ȫ�޶�������com.crawler.MainCrawler��
	 * @param path
	 *            �����.class�ļ�·��������Ծ���·�����У�
	 * @return action �����ظ���ɹ������ظ���ʵ������Action���󣬲��ɹ��򷵻�null
	 */
	public static synchronized Action newInstance1(String name, String path) {
		loader.setPath(path);
		Class<?> cl = loader.getClass(name);
		try {
			Action action = (Action) cl.newInstance();// �޲�ʵ����
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
	 * ͨ���ƶ����ȫ�޶�����ȡCLAMAP������޶����ɶԵ��࣬���ø���ʵ��������Ķ���
	 * 
	 * @param name
	 *            �������ȫ�޶�������com.crawler.MainCrawler��
	 * @return ��CLAMAP��ȡ��ɹ����򷵻ظ���ʵ�����Ķ��󣬲��ɹ��򷵻�null��
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
