package servlet;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Map;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;

import action.Action;
import action.ActionFactory;
import util.Loader;

/**
 * Servlet implementation class MainServlet
 */
@WebServlet("/MainServlet")
public class MainServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private static final String ONICK = "onick";
	private static final String WORK = "work";
	private static final String RELATION = "relation";
//	private static final Loader LOADER = Loader.NewInstance1("");

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public MainServlet() {
		super();
		// TODO Auto-generated constructor stub
	}

	static {
		
//		try {
//			Action mc = ActionFactory.newInstance("MainClassifier");
//			mc.putAction("init_ariticle", ActionFactory.initFileSeg1(null));
//			mc.action("init_ariticle");
//		} catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		Map<String, String[]> main = request.getParameterMap();
		System.out.println(main);
		String[] tt;
		request.setCharacterEncoding("UTF-8");
		System.out.println(System.getProperty("java.classpath"));
		if (main.get(WORK) != null) {
			if ((tt = main.get(ONICK)) != null && tt.length != 0) {
				String onick = tt[0];
				request.setAttribute(ONICK, onick);
				Action action = ActionFactory.newInstance("com.crawler.MainCrawler");
				action.putAction(ONICK, onick);
				Object o = action.action(ONICK);
				if (o != null) {
					action.putAction("get_data", o);
					action.putAction("get_link", o);
					request.setAttribute("data", action.action("get_data"));
					request.setAttribute("link", action.action("get_link"));
					request.setAttribute("onick", new Gson().toJson(onick + "的关注"));
					request.setAttribute("subText", new Gson().toJson("关系图"));
				} else {
					request.setAttribute("data", new Gson().toJson(new ArrayList<String>()));
					request.setAttribute("link", new Gson().toJson(new ArrayList<String>()));
					request.setAttribute("onick", new Gson().toJson("查无此人"));
					request.setAttribute("subText", new Gson().toJson("无"));
				}
				request.getRequestDispatcher("../NewFile.jsp").forward(request, response);
			} else if ((tt = main.get("ajax_onick")) != null && tt.length != 0) {
				System.out.println("success");
				String onick = tt[0];
				request.setAttribute(ONICK, onick);
				Action action = ActionFactory.newInstance("com.crawler.MainCrawler");
				action.putAction("get_weibo", onick);
				action.putAction("offset", main.get("offset")[0]);
				action.putAction("limit", main.get("limit")[0]);
				Object o = action.action("get_weibo");
				response.setContentType("application/json");
				response.setCharacterEncoding("gb2312");
				OutputStream writer = response.getOutputStream();
				writer.write(o.toString().getBytes());
			} else if ((tt = main.get("ajax_relation")) != null && tt.length != 0) {
				System.out.println("success");
				String onick = tt[0];
				request.setAttribute(ONICK, onick);
				Action action = ActionFactory.newInstance("com.crawler.MainCrawler");
				action.putAction(ONICK, onick);
				Object o = action.action(ONICK);
				action.putAction("ajax_relation", o);
				Object o1 = action.action("ajax_relation");
				response.setContentType("application/json");
				response.setCharacterEncoding("gb2312");
				OutputStream writer = response.getOutputStream();
				writer.write(o1.toString().getBytes());
			} else if ((tt = main.get("ajax_compute_emotion")) != null && tt.length != 0) {
				System.out.println("success");
				String weiboContent = main.get("weiboContent")[0];
				Action segmenter = ActionFactory.newInstance("com.segmenter.MainSegmenter");
				segmenter.putAction("segment", weiboContent);
				Object words = segmenter.action("segment");
				Action classifier = ActionFactory.newInstance("com.classifier.MainClassifier");
				classifier.putAction("classify", words);
				Object result = classifier.action("classify");
				response.setContentType("application/json");
				response.setCharacterEncoding("gb2312");
				OutputStream writer = response.getOutputStream();
				writer.write(result.toString().getBytes());
			} else if ((tt = main.get("ajax_compute_emotions")) != null && tt.length != 0) {
				System.out.println("success");
				String name = main.get("ajax_compute_emotions")[0];
				Action crawler = ActionFactory.newInstance("com.crawler.MainCrawler");
				crawler.putAction("all_weibo", name);
				Object weibos = crawler.action("all_weibo");
				
				Action segmenter = ActionFactory.newInstance("com.segmenter.MainSegmenter");
				segmenter.putAction("segments", weibos);
				Object words = segmenter.action("segments");
				
				Action classifier = ActionFactory.newInstance("com.classifier.MainClassifier");
				classifier.putAction("classifies", words);
				Object result = classifier.action("classifies");
				
				response.setContentType("application/json");
				response.setCharacterEncoding("gb2312");
				OutputStream writer = response.getOutputStream();
				writer.write(result.toString().getBytes());
			}
		} else if (main.get("reload") != null) {
			Action action = ActionFactory.newInstance("com.crawler.MainCrawler");
			action.putAction("reload", "");
			action.action("reload");
		} else if (main.get("main") != null) {
			request.getRequestDispatcher("../main.jsp").forward(request, response);
		} else {
			request.setAttribute(ONICK, "<br><br>haha");
		}

	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		doGet(request, response);
	}

}
