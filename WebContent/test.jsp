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
	<table id="table">

	</table>

	<script type="text/javascript">
		var $table = $('#table');
		$table.bootstrapTable({
			dataType : "json",
			pagination : true, //分页
			singleSelect : false,

			search : true, //显示搜索框
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
			} ],
			data:[{"weiboContent":"df"}],
			onClickRow : function(
					name,
					args) {
				console
						.log(name);
			}
		});
	</script>
</body>
</html>