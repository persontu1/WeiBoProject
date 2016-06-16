package servlet;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;

import action.Action;
import action.ActionFactory;
import bean.Node;

/**
 * Servlet implementation class TestServlet
 */
@WebServlet("/TestServlet")
public class TestServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public TestServlet() {
		super();
		// TODO Auto-generated constructor stub
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
//	static {
//		try {
//			Action mc = ActionFactory.newInstance("MainClassifier");
//			mc.putAction("init_ariticle", ActionFactory.initFileSeg1(null));
//			mc.action("init_ariticle");
//		} catch (FileNotFoundException e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//	}

	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		// TODO Auto-generated method stub
		Node node = new Node().category("1").draggable(true).id("1").name("asdfasd").symbolSize("10");
		List<Node> list = new ArrayList<Node>();
		list.add(node);
		// SeriesData ssd=new SeriesData("");
		// graph.nodes(node.draggable(true).label("sdf").name("hahha").symbolSize(5).category(0).value(2));
		// graph.data(node.draggable(true).label("sdf").name("hahha").symbolSize(5).category(0).value(2));
		Gson gson = new Gson();
		System.out.println(gson.toJson(list));
		request.setAttribute("data", gson.toJson(list));
		// Action action = ActionFactory.newInstance("MainSegmenter");
		// action.putAction("segment",
		// "相信世间的真善美，能打败人生路上的“小怪兽”。这不仅是知识教育能够满足的，更需要素养教育的培养，教会宝宝学会思考、应对挫折。现在起，为更多的孩子筑梦，让他成为自己人生路上的英雄，即使平凡，也可以很伟大。");
		// Object set = action.action("segment");
		//
		// Action mc = ActionFactory.newInstance("MainClassifier");
		//
		// mc.putAction("classify", set);
		// request.setAttribute("segment", mc.action("classify"));

		// for (File f : file.listFiles())
		// System.out.println(f.getName());

		// request.setAttribute("segment", "<div class=\"canvas\"
		// id=\"mainCanvas\""
		// + "style=\"width: 2500px; height: 2500px; border: 1px solid
		// black;\">"
		// + "<h1 class=\"block draggable\" id=\"h1_block\" "
		// + "style=\"left: 10px; top: 10px;\">h1sdfasd block</h1> "
		// + "<h2 class=\"block draggable\" id=\"h2_block\" " + "style=\"left:
		// 200px; top: 100px;\">h2 block</h2>"
		// + "<h3 class=\"block draggable\" id=\"h3_block\" " + "style=\"left:
		// 500px; top: 500px;\">h2 block</h3>"
		// + "<div class=\"connector h1_block h2_block\">" + "</div>"
		// + "<div class=\"connector h1_block h3_block\"></div> " + "</div>");
		request.getRequestDispatcher("../NewFile.jsp").forward(request, response);
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
