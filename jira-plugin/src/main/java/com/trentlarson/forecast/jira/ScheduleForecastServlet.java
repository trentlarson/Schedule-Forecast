package com.trentlarson.forecast.jira;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.trentlarson.forecast.core.actions.TimeScheduleAction;
import com.trentlarson.forecast.core.scheduling.IssueDigraph;
import com.trentlarson.forecast.core.scheduling.TimeScheduleCreatePreferences;
import com.trentlarson.forecast.core.scheduling.TimeScheduleDisplayPreferences;
import com.trentlarson.forecast.core.scheduling.TimeScheduleWriter;
import com.trentlarson.forecast.core.scheduling.TimeScheduleDisplayPreferences.NoSuchIssueException;


public class ScheduleForecastServlet extends HttpServlet {
	
	private static final long serialVersionUID = 9043778864764874640L;

	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, java.io.IOException {
		TimeScheduleCreatePreferences sPrefs = new TimeScheduleCreatePreferences(0, 1.0);
		IssueDigraph graph = TimeScheduleAction.regenerateGraph(sPrefs);
		try {
			TimeScheduleDisplayPreferences dPrefs = 
				TimeScheduleDisplayPreferences.createForIssues
				(7, 0, true, false, false, new String[]{"FOURU-1002"}, false, graph);
			TimeScheduleWriter.writeIssueTable(graph, resp.getWriter(), sPrefs, dPrefs);
		} catch (NoSuchIssueException e) {
			resp.getWriter().write("No such issue: " + e.getMessage());
		}
	}

}
