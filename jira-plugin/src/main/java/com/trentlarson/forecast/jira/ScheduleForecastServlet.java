package com.trentlarson.forecast.jira;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.codehaus.jackson.map.ObjectMapper;

import com.trentlarson.forecast.core.actions.TimeScheduleAction;
import com.trentlarson.forecast.core.scheduling.IssueDigraph;
import com.trentlarson.forecast.core.scheduling.TimeScheduleCreatePreferences;
import com.trentlarson.forecast.core.scheduling.TimeScheduleDisplayPreferences;
import com.trentlarson.forecast.core.scheduling.TimeScheduleWriter;


public class ScheduleForecastServlet extends HttpServlet {
	
	private static final long serialVersionUID = 9043778864764874640L;

	protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, java.io.IOException {
		
		TimeScheduleCreatePreferences sPrefs;
		ObjectMapper mapper = new ObjectMapper();
		if (req.getParameterMap().containsKey("cPrefs")) {
			sPrefs = mapper.readValue(req.getParameter("cPrefs"), TimeScheduleCreatePreferences.Pojo.class).getPrefs();
		} else {
			sPrefs = new TimeScheduleCreatePreferences(0, 1.0);
		}
		IssueDigraph graph = TimeScheduleAction.regenerateGraph(sPrefs);
		TimeScheduleDisplayPreferences dPrefs;
		if (req.getParameterMap().containsKey("dPrefs")) {
			dPrefs = mapper.readValue(req.getParameter("dPrefs"), TimeScheduleDisplayPreferences.Pojo.class).getPrefs();
		} else {
			dPrefs = TimeScheduleDisplayPreferences.createForIssues(7, 0, true, false, false, new String[]{"FOURU-1002"}, false, graph);
		}
		
		/**
		// This approach paints a canvas, laid under the HTML which is made slightly transparent. (duplicate)
		resp.getWriter().println("<html><head>	<script src='http://ajax.googleapis.com/ajax/libs/jquery/1.3.2/jquery.min.js' type='text/javascript'></script> 	<style type='text/css'> 	canvas { 		position:absolute; 		width: 800px; 		height: 600px; 	} 	.blk 	{ 		opacity: 0.8; 		border:1px solid #000; 		background:#fff; 		position:absolute; 		padding: 5px; 		z-index: 10 	} 	 	</style> 	<script type='text/javascript'> 	 	 	function updateCanvas(canvasJq, blkEls) 	{ 		var canvasEl = canvasJq[0]; 		canvasEl.width=canvasJq.width(); 		canvasEl.height=canvasJq.height(); 		var cOffset = canvasJq.offset(); 		var ctx = canvasEl.getContext('2d'); 		ctx.clearRect(0, 0, canvasEl.width, canvasEl.height); 		ctx.beginPath(); 		$(blkEls).each(function(){ 			$('span', this).each(function(){ 				var elem=$(this); 				if(elem.attr('rel')) 				{ 					var srcOffset=elem.offset(); 					var srcMidHeight=elem.height()/2; 					var targetElem=$('#'+elem.attr('rel')); 					if(targetElem.length) 					{ 						var trgOffset=targetElem.offset(); 						var trgMidHeight=elem.height()/2; 						ctx.moveTo(srcOffset.left - cOffset.left, srcOffset.top - cOffset.top + srcMidHeight); 						ctx.lineTo(trgOffset.left - cOffset.left, trgOffset.top - cOffset.top + trgMidHeight); 					} 				} 			}); 		}); 		ctx.stroke(); 		ctx.closePath(); 	} 	 	$(document).ready(function(){updateCanvas($('#canvas'), $('#source'));}); 	 	</script> </head><body>");
		resp.getWriter().println("<canvas id='canvas'></canvas>");
		resp.getWriter().println("<div id='source' class='blk'>");
		**/
		// This approach uses an external library to paint right on the HTML.
		resp.getWriter().println("<html><head>");
		resp.getWriter().println("	<script src='http://ajax.googleapis.com/ajax/libs/jquery/1.3.2/jquery.min.js' type='text/javascript'></script>");
		resp.getWriter().println("	<script src='http://eucaly61-java.googlecode.com/files/wz_jsgraphics.js' type='text/javascript'></script>");
		resp.getWriter().println("<script>");
		resp.getWriter().println("function drawAllLinks(elemName) {");
		resp.getWriter().println("  jg = new jsGraphics(elemName);");
		resp.getWriter().println("  outerElems = $('#' + elemName);");
		resp.getWriter().println("  outerElems.each(function() {");
		resp.getWriter().println("    $('span', this).each(function(){");
		resp.getWriter().println("      var sourceElem=$(this);");
		resp.getWriter().println("      if(sourceElem.attr('rel')) {");
		resp.getWriter().println("        var destElem=$('#'+sourceElem.attr('rel'));");
		resp.getWriter().println("        if(destElem.length) {");
		resp.getWriter().println("          jg.drawLine");
		resp.getWriter().println("          (sourceElem.offset().left + sourceElem.width() / 2,");
		resp.getWriter().println("           sourceElem.offset().top + sourceElem.height() / 2,");
		resp.getWriter().println("           destElem.offset().left + destElem.width() / 2,");
		resp.getWriter().println("           destElem.offset().top + destElem.height() / 2);");
		resp.getWriter().println("        }");
		resp.getWriter().println("      }");
		resp.getWriter().println("    })");
		resp.getWriter().println("  })");
		resp.getWriter().println("  jg.paint();");
		resp.getWriter().println("}");
		resp.getWriter().println("$(document).ready(function(){drawAllLinks('source')});");
		resp.getWriter().println("</script>");
		resp.getWriter().println("</head>");
		resp.getWriter().println("<body>");
		resp.getWriter().println("<div id='source'>");
		
		TimeScheduleWriter.writeIssueTable(graph, resp.getWriter(), sPrefs, dPrefs);
		
		resp.getWriter().println("</div>");
		resp.getWriter().println("</body></html>");
		
	}

}
