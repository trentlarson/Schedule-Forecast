package com.trentlarson.forecast.jira;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.ServletException;

public class ScheduleForecastServlet extends HttpServlet {
	
  protected void doGet(HttpServletRequest req, HttpServletResponse resp)
    throws ServletException, java.io.IOException {
    resp.getWriter().print("That's the trick, Mr. Gizzard!");
  }

}
