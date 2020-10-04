// from https://raw.githubusercontent.com/eugenp/tutorials/master/libraries-server/src/main/java/com/baeldung/jetty/BlockingServlet.java
// from https://www.baeldung.com/jetty-embedded

package com.trentlarson.forecast.core.helper;

import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.trentlarson.forecast.core.scheduling.IssueDigraph;
import com.trentlarson.forecast.core.scheduling.IssueTree;
import com.trentlarson.forecast.core.scheduling.TimeScheduleCreatePreferences;
import com.trentlarson.forecast.core.scheduling.TimeScheduleDisplayPreferences;
import com.trentlarson.forecast.core.scheduling.TimeScheduleWriter;

public class ScheduleAndDisplayServlet extends HttpServlet {

  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    response.setContentType("application/json");
    response.setStatus(HttpServletResponse.SC_OK);
    response.getWriter().println("{ \"status\": \"ok\"}");
  }
  
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    try {
      GsonBuilder builder = new GsonBuilder();
      Gson creator = builder.create();
      InputInterfaces.ScheduleAndDisplayInput input =
          creator.fromJson(request.getReader(), InputInterfaces.ScheduleAndDisplayInput.class);

      InputInterfaces.ScheduleAndDisplayAfterChecks checked = input.check();

      TimeScheduleWriter.writeIssueTable
          (checked.graph, response.getWriter(),
              checked.graph.getTimeScheduleCreatePreferences(),
              checked.displayPreferences);

      response.setContentType("text/html");
      response.setStatus(HttpServletResponse.SC_OK);

    } catch (JsonSyntaxException e) {
      String message = e.getMessage() + " ... due to: " + (e.getCause() == null ? "" : e.getCause().getMessage());
      System.out.println("Client JsonSyntaxException: " + message);
      response.setContentType("text/plain");
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      response.getWriter().println(message);
    }
  }

}
