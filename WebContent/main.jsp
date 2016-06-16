<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ page isELIgnored="false"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<meta name="viewport"
	content="width=device-width, initial-scale=1.0, maximum-scale=1.0, user-scalable=no">
<title>用户行为分析系统</title>
<script src="../jquery-2.1.4/jquery.min.js"></script>
<script src="../echarts.min.js"></script>
<script src="../dist/js/bootstrap.min.js"></script>


<script src="../bootstrap-table.min.js"></script>
<link rel="stylesheet" type="text/css"
	href="../dist/css/bootstrap.min.css">
<link rel="stylesheet" type="text/css" href="../dist/css/main.css">
<link rel="stylesheet" type="text/css" href="../bootstrap-table.min.css">
</head>
<body>
	<nav class="navbar navbar-default navbar-fixed-top">

	<div class="container-fluid">
		<div class="navbar-header">
			<button type="button" data-toggle="collapse"
				data-target="#navbar-collapsed" aria-expanded="false"
				class="navbar-toggle collapsed">
				<span class="sr-only">toggle</span><span class="icon-bar"></span><span
					class="icon-bar"></span><span class="icon-bar"></span>
			</button>
			<a class="navbar-brand">用户行为分析系统</a>
		</div>
		<div id="navbar-collapsed" class="collapse navbar-collapse">
			<ul class="nav navbar-nav navbar-left">
				<form class="navbar-form navbar-left" role="search">
					<div class="form-group">
						<input id="input1" type="text" class="form-control"
							placeholder="输入微博昵称">
					</div>
					<button id="check1" type="button" class="btn btn-info">查询</button>
					<button id="check2" type="button" class="btn btn-info">返回头部</button>
				</form>
			</ul>
			<ul class="nav navbar-nav navbar-right">
				<li class="highlight"><a href="http://echarts.baidu.com">ECharts</a></li>
			</ul>
		</div>
	</div>
	</nav>
</body>
<div id="top" class="panel panel-success"
	style="position: absolute; left: 0px; top: 55px; width: 50%; height: 533px">
	<div class="panel-heading">
		<h3 class="panel-title">关系图</h3>
	</div>
	<div class="panel-body">
		<div id="mains"
			style="position: absolute; left: 0px; top: 40px; width: 100%; height: 495px"></div>
	</div>
</div>
<div class="panel panel-success"
	style="overflow: scroll; position: absolute; left: 50%; top: 55px; width: 50%; height: 533px">
	<div class="panel-heading">
		<h3 class="panel-title">所有微博</h3>
	</div>
	<div class="panel-body">
		<button id="stat_weibos" type="button" class="btn btn-info">情感倾向统计</button>
		<table id="table">
			<thead>
				<tr>
					<th data-field="test">test</th>
				</tr>
			</thead>
		</table>
	</div>
	<ul class="list-group"></ul>
</div>
<div id="weibo" class="panel panel-success"
	style="overflow: scroll; position: absolute; left: 0px; top: 605px; width: 100%; height: 300px">
	<div class="panel-heading">
		<h3 class="panel-title">指定微博</h3>
	</div>
	<div class="panel-body">
		<table id="table1">
			<thead>
				<tr>
					<th data-field="test">test</th>
				</tr>
			</thead>
		</table>
		<br>
		<button id="get_emotion" type="button" class="btn btn-info">获取情感倾向</button>
	</div>
</div>
<div id="weibo_result" class="panel panel-success"
	style="position: absolute; left: 0px; top: 910px; width: 100%; height: 433px">
	<div class="panel-heading">
		<h3 class="panel-title">单条分析结果</h3>
	</div>
	<div class="panel-body">
		<div id="emotion_result"
			style="position: absolute; left: 0px; top: 50px; width: 100%; height: 495px"></div>
	</div>
</div>
<div id="weibos_result" class="panel panel-success"
	style="position: absolute; left: 0px; top: 1350px; width: 100%; height: 433px">
	<div class="panel-heading">
		<h3 class="panel-title">整体分析结果</h3>
	</div>
	<div class="panel-body">
		<div id="emotions_result"
			style="position: absolute; left: 0px; top: 50px; width: 100%; height: 495px"></div>
	</div>
</div>


<script type="text/javascript">
	document.getElementById("stat_weibos").disabled = true;
	var emChart = echarts.init(document.getElementById('emotion_result'));
	var emsChart = echarts.init(document.getElementById('emotions_result'));
	var $table2 = $('#table1');
	$table2.bootstrapTable({
		dataType : "json",
		singleSelect : false,
		formatNoMatches : function() { //没有匹配的结果
			return '无符合条件的记录';
		},
		columns : [ {
			title : '微博内容',
			field : 'weiboContent',
			align : 'center',
			valign : 'middle'
		}, {
			title : '发布时间',
			field : 'date',
			align : 'center',
			valign : 'middle'
		}, {
			title : '所属话题',
			field : 'topic',
			align : 'center',
			valign : 'middle'
		}, {
			title : '微博地址',
			field : 'weibo_url',
			align : 'center',
			valign : 'middle'
		} ]
	});
	var temp;
	var $table = $('#table');
	$table.bootstrapTable({
		onClickRow : function(name, args) {
			console.log(name);
			temp = name;
			var option = {
				dataType : "json",
				singleSelect : false,
				formatNoMatches : function() { //没有匹配的结果
					return '无符合条件的记录';
				},
				columns : [ {
					title : '微博内容',
					field : 'weiboContent',
					align : 'center',
					valign : 'middle'
				}, {
					title : '发布时间',
					field : 'date',
					align : 'center',
					valign : 'middle'
				}, {
					title : '所属话题',
					field : 'topic',
					align : 'center',
					valign : 'middle'
				}, {
					title : '微博地址',
					field : 'weibo_url',
					align : 'center',
					valign : 'middle'
				} ],
				data : [ name ]
			};
			console.log(option.data);
			$table2.bootstrapTable('destroy').bootstrapTable(option);
			var scroll_offset = $("#weibo").offset(); //得到pos这个div层的offset，包含两个值，top和left
			$("body,html").animate({
				scrollTop : scroll_offset.top
			//让body的scrollTop等于pos的top，就实现了滚动
			}, 500);
		},
		dataType : "json",
		pagination : true, //分页
		singleSelect : false,
		sidePagination : "server", //服务端处理分页
		formatLoadingMessage : function() {
			return "请稍等，正在加载中...";
		},
		formatNoMatches : function() { //没有匹配的结果
			return '无符合条件的记录';
		},
		columns : [ {
			title : '微博内容',
			field : 'weiboContent',
			align : 'center',
			valign : 'middle'
		}, {
			title : '发布时间',
			field : 'date',
			align : 'center',
			valign : 'middle'
		}, {
			title : '所属话题',
			field : 'topic',
			align : 'center',
			valign : 'middle'
		}, {
			title : '微博地址',
			field : 'weibo_url',
			align : 'center',
			valign : 'middle'
		} ]
	});
	var myChart = echarts.init(document.getElementById('mains'));
</script>
<script type="text/javascript">
	$("#check2").click(function() {
		var scroll_offset = $("#top").offset(); //得到pos这个div层的offset，包含两个值，top和left
		$("body,html").animate({
			scrollTop : 0
		//让body的scrollTop等于pos的top，就实现了滚动
		}, 500);
	});
	var name_i;
	$(function() {
		$("#check1")
				.click(
						function() {
							$("body,html").animate({
								scrollTop : 0
							}, 500);
							console.log($("#input1").val());
							var name = $("#input1").val();
							name_i = $("#input1").val();
							var table = $('#table');
							myChart.showLoading();
							$
									.getJSON(
											'http://localhost:8080/WeiBoProject/servlet/main?work=&ajax_relation='
													+ name,
											function(data) {
												myChart.setOption(data);
												myChart.hideLoading();
												table
														.bootstrapTable(
																'refresh',
																{
																	onClickRow : function(
																			name,
																			args) {
																		console
																				.log(name);
																	},
																	url : "http://localhost:8080/WeiBoProject/servlet/main?work=&ajax_onick="
																			+ name,

																	dataType : "json",
																	pagination : true, //分页
																	singleSelect : false,
																	sidePagination : "server", //服务端处理分页
																	formatLoadingMessage : function() {
																		return "请稍等，正在加载中...";
																	},
																	formatNoMatches : function() { //没有匹配的结果
																		return '无符合条件的记录';
																	},
																	columns : [ {
																		title : '活动名称',
																		field : 'test'
																	} ]
																});
											});
							document.getElementById("stat_weibos").disabled = false;
						});
	})
</script>
<script type="text/javascript">
	$("#get_emotion").click(
			function() {
				if (temp != null) {
					console.log(temp);
					emChart.showLoading();
					$
							.post(
									'http://localhost:8080/WeiBoProject/servlet/main?work=&ajax_compute_emotion='
											+ name, {
										"weiboContent" : temp.weiboContent
									}, function(data) {
										console.log(data);
										emChart.setOption(data);
										emChart.hideLoading();
										var scroll_offset = $("#weibo_result")
												.offset();
										$("body,html").animate({
											scrollTop : scroll_offset.top
										}, 500);
									}, "json");
				}
			});
	$("#stat_weibos").click(
			function() {
				emsChart.showLoading();
				$.post(
						'http://localhost:8080/WeiBoProject/servlet/main?work=&ajax_compute_emotions='
								+ name_i, function(data) {
							console.log(data);
							emsChart.setOption(data);
							emsChart.hideLoading();
							var scroll_offset = $("#weibos_result").offset();
							$("body,html").animate({
								scrollTop : scroll_offset.top
							}, 500);
						}, "json");
			});
</script>
</html>