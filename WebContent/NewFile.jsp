<%@ page language="java" contentType="text/html; charset=UTF-8"
	pageEncoding="UTF-8"%>
<%@ page isELIgnored="false"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>

<!DOCTYPE html PUBLIC "-//W3C//DTD HTML 4.01 Transitional//EN" "http://www.w3.org/TR/html4/loose.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>用户行为分析系统</title>
<script src="../echarts.min.js"></script>
<script src="../jquery-2.1.4/jquery.js"></script>
<script src="../jquery-2.1.4/jquery.min.js"></script>
<link rel="stylesheet" type="text/css" href="../dist/css/bootstrap.min.css">
</head>
<body>
	<nav class="navbar navbar-default" role="navigation">
   <div class="navbar-header">
      <a class="navbar-brand" href="#">W3Cschool</a>
   </div>
   <div>
      <form class="navbar-form navbar-left" role="search">
         <div class="form-group">
            <input type="text" class="form-control" placeholder="Search">
         </div>
         <button type="submit" class="btn btn-default">提交按钮</button>
      </form>    
      <button type="button" class="btn btn-default navbar-btn">
         导航栏按钮
      </button>
   </div>
</nav>
	<div id="main" style="position:absolute;left:0px;top:50px;width: 800px; height: 533px"></div>
    <div id="list" style="position:absolute;left:805px;top:50px;width: 315px; height: 533px">asdfasd</div>
	<script type="text/javascript">
		var myChart = echarts.init(document.getElementById('main'));
		var listChart=echarts.init(document.getElementById('list'));
		var o={backgroundColor: "#F2F2F2"};
		listChart.setOption(o);
		listChart.showLoading();
		var categories = [ {
			name : "0"
		}, {
			name : "1"
		} ];
		var d = ${requestScope.data};
		var l = ${requestScope.link};
		var te = ${requestScope.onick};
		var subte=${requestScope.subText};
		var option = {
		    backgroundColor: "#F2F2F2",
			title : {
				text : te,
				subtext : subte,
				top : 'top',
				left : 'left'
			},
			tooltip : {},
			toolbox: {
		        show: true,
		        feature: {
		            dataView: {show: true, readOnly: false},
		            restore: {show: true},
		            saveAsImage: {show: true}
		        }
		    },
			legend : [ {
				// selectedMode: 'single',
				data : categories.map(function(a) {
					return a.name;
				})
			} ],
			animationDuration : 1500,
			animationEasingUpdate : 'quinticInOut',
			series : [ {
				name : '关系',
				type : 'graph',
				layout : 'circular',
				data : d,
				links : l,
				categories : categories,
				label : {
					normal : {
						position : 'right'
					}
				},
				roam : true,
				lineStyle : {
					normal : {
						curveness : 0.3
					}
				}
			}]
		};

		myChart.setOption(option);
		myChart.on('click', function (params) {
			
			if(params.componentType==='series'&&params.seriesType === 'graph'&&params.dataType === 'node'){
				myChart.showLoading();
				$.getJSON('http://localhost:8080/WeiBoProject/servlet/main?work=&ajax='+params.name,function (data) {
					option.series[0].data.push({"category":"0","draggable":true,"id":""+option.series[0].data.length,"name":"姚晨","symbolSize":"25"});
					option.series.push({
						name : '关系',
						type : 'graph',
						layout : 'circular',
						data : d,
						links : l,
						categories : categories,
						label : {
							normal : {
								position : 'right'
							}
						},
						roam : true,
						lineStyle : {
							normal : {
								curveness : 0.3
							}
						}
					});
					console.log(option.series[0].data);
					listChart.setOption(option);
					myChart.setOption(option);
					myChart.hideLoading();
					listChart.hideLoading();
				});
			}
		});
	</script>
	杨埔生
	<br>${requestScope.relation}
	<c:forEach items="${requestScope.segment}" var="item">
	${item}
	</c:forEach>
</body>
</html>