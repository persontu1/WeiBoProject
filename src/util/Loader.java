package util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class Loader extends ClassLoader {
	private String path = "";
	private static DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	private static DocumentBuilder dbb;
	private static final String cN = "className";
	private static final String cFL = "classFileLocation";
	private static final String is = "isSpecified";
	private static final String n = "name";
	private static Loader loader;
	{
		try {
			dbb = dbf.newDocumentBuilder();
		} catch (ParserConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private Loader() {

	}

	private Loader(ClassLoader l) {
		super(l);
	}

	public static Loader NewInstance() {
		if (loader == null)
			return loader = new Loader();
		return loader;
	}

	public static synchronized Loader NewInstance1(String path, ClassLoader l) {
		if (loader != null) {
			loader.setPath(path);
			return loader;
		} else {
			loader = new Loader(path, l);
			return loader;
		}
	}

	public static synchronized Loader NewInstance1(ClassLoader l) {
		return new Loader(l);
	}

	public static Loader NewInstance(String path) {
		Loader loader = new Loader(path);
		return loader;
	}

	private Loader(String path) {
		this.path = path;
	}

	private Loader(String path, ClassLoader l) {
		super(l);
		this.path = path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	protected Class<?> findClass(String name) throws ClassNotFoundException {
		try {
			if (!isIn(path))
				return null;
			byte[] bytes = loadBytes();
			Class<?> cl = defineClass(name, bytes, 0, bytes.length);
			if (cl == null)
				throw new ClassNotFoundException(name);
			return cl;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			throw new ClassNotFoundException(name);
		}
	}

	private boolean isIn(String loc) {
		File file = new File(loc);
		if (file.exists())
			return true;
		return false;
	}

	public void generateXml(Class<?> cl) {
		Field[] fields = cl.getDeclaredFields();
		Method[] methods = cl.getDeclaredMethods();
		String className = cl.getName();
		String[] sp = className.split("\\.");
		String base = "D:\\eclipse\\classes\\";
		File file = new File(base + className + ".xml");
		Document doc = dbb.newDocument();
		Element root = doc.createElement("Class");
		doc.appendChild(root);
		root.setAttribute(cN, className);
		root.setAttribute(cFL, base + sp[sp.length - 1] + ".class");
		root.setAttribute(is, "specified");
		Element fE = doc.createElement("Fields");
		root.appendChild(fE);
		for (Field f : fields) {
			Element fe = doc.createElement("field");
			fE.appendChild(fe);
			Element feName = doc.createElement("name");
			Element feType = doc.createElement("type");
			feName.setTextContent(f.getName());
			feType.setTextContent(f.getType().getName());
			fe.appendChild(feName);
			fe.appendChild(feType);
		}
		Element mE = doc.createElement("Methods");
		root.appendChild(mE);
		for (Method m : methods) {
			Element me = doc.createElement("method");
			mE.appendChild(me);
			Element meName = doc.createElement("name");
			meName.setTextContent(m.getName());
			me.appendChild(meName);
			Element params = doc.createElement("params");
			Class<?>[] cls = m.getParameterTypes();
			for (Class<?> c : cls) {
				Element param = doc.createElement("param");
				param.setTextContent(c.getName());
				params.appendChild(param);
			}
			me.appendChild(params);
			Element returnType = doc.createElement("returnType");
			returnType.setTextContent(m.getReturnType().getName());
			me.appendChild(returnType);
		}
		try {
			Transformer t = TransformerFactory.newInstance().newTransformer();
			t.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
			t.setOutputProperty(OutputKeys.INDENT, "yes");
			if (!file.exists())
				file.createNewFile();
			else {
				file.delete();
				file.createNewFile();
			}
			PrintWriter pw = new PrintWriter(new FileOutputStream(file));
			StreamResult sr = new StreamResult(pw);
			DOMSource ds = new DOMSource(doc);
			t.transform(ds, sr);
		} catch (TransformerConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerFactoryConfigurationError e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public Class<?> getClass(String name) {
		try {
			return findClass(name);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public Class<?> getClass1(String name) {
		try {
			return loadClass(name);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}

	public List<Object> getClassInfo(String src) {
		try {
			List<Object> list = new ArrayList<Object>();
			Document doc = dbb.parse(src);
			NodeList nl = doc.getElementsByTagName("Class");
			Element el = (Element) nl.item(0);
			String className = el.getAttribute(cN);
			String loc = el.getAttribute(cFL);
			path = loc;
			String ftc = el.getAttribute(is);
			NodeList nl1;
			Class<?> cl = findClass(className);
			System.out.println(cl.getClassLoader());
			list.add(cl);
			if (ftc != null && ftc.equals("specified") && (nl1 = el.getElementsByTagName(ftc)) != null) {
				for (int i = 0; i < nl1.getLength(); i++) {
					Element e = (Element) nl1.item(i);
					String nm1 = e.getAttribute(n);
					Element ee = (Element) (e.getElementsByTagName(nm1).item(0));
					String xx = ee.getFirstChild().getNodeValue();
					list.add(xx);
				}
			}
			return list;
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			System.out.println("文件：" + src + " 不存在");
		} catch (ClassNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		return null;
	}

	private byte[] loadBytes(String path) throws IOException {
		Path pat = Paths.get(path);
		return Files.readAllBytes(pat);
	}

	private byte[] loadBytes() throws IOException {
		Path pat = Paths.get(path);
		File file = new File(path);
		System.out.println(file.exists());
		return Files.readAllBytes(pat);
	}

	public static void main(String[] args) {
		Loader loader = new Loader();
		loader.getClassInfo("Test.xml");
		File classLocation = new File("D:\\WeiBoProject\\build\\classes\\com");
		try {
			for (File f1 : classLocation.listFiles()) {
				for (File file : f1.listFiles()) {
					String fileName = file.getAbsolutePath().replace("\\", ".");
					Pattern pat = Pattern.compile("D:\\.WeiBoProject\\.build\\.classes\\.(.+?)\\.class");
					Matcher mat = pat.matcher(fileName);
					if (mat.matches())
						loader.generateXml(Class.forName(mat.group(1)));
				}
			}

			// loader.generateXml(Class.forName("com.crawler.MainCrawler$FollowCrawler"));
			// loader.generateXml(Class.forName("com.crawler.MainCrawler"));
			// loader.generateXml(Class.forName("com.crawler.Crawler"));
			// loader.generateXml(Class.forName("com.crawler.Crawler$LessThanOneException"));
			// loader.generateXml(Class.forName("com.crawler.ParsingPage"));
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// File file=new
		// File("K:\\Work\\WeiBoProject\\build\\classes\\com\\crawlsdfsdfer\\Crawler.class");
		// String s = file.getAbsolutePath();
		// String s1=s.replace("\\", ".");
		// System.out.println(s1);
		// Pattern pat =
		// Pattern.compile("K:\\.Work\\.WeiBoProject\\.build\\.classes\\.(.+?)\\.class");
		// System.out
		// .println(pat.matcher(s1).matches());
		// loader.generateXml(null);
	}
}
