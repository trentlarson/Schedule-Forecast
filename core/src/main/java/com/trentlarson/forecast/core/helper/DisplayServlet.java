// from https://raw.githubusercontent.com/eugenp/tutorials/master/libraries-server/src/main/java/com/baeldung/jetty/BlockingServlet.java
// from https://www.baeldung.com/jetty-embedded

package com.trentlarson.forecast.core.helper;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.MalformedJsonException;
import com.trentlarson.forecast.core.scheduling.IssueDigraph;
import com.trentlarson.forecast.core.scheduling.IssueTree;
import com.trentlarson.forecast.core.scheduling.TimeScheduleCreatePreferences;
import com.trentlarson.forecast.core.scheduling.TimeScheduleDisplayPreferences;
import com.trentlarson.forecast.core.scheduling.TimeScheduleTests;
import com.trentlarson.forecast.core.scheduling.TimeScheduleWriter;

public class DisplayServlet extends HttpServlet {

  protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    response.setContentType("application/json");
    response.setStatus(HttpServletResponse.SC_OK);
    response.getWriter().println("{ \"status\": \"ok\"}");
  }
  
  protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
    try {
      //String body = request.getReader().lines().collect(Collectors.joining(System.lineSeparator()));
      GsonBuilder builder = new GsonBuilder();
      Gson creator = builder.create();
      IssueTree[] issues = creator.fromJson(request.getReader(), IssueTree[].class);
      System.out.println("Deserialized issues: " + Arrays.asList(issues));
      IssueDigraph graph = IssueDigraph.schedulesForIssues(issues);

      String[] keyArray = new String[issues.length];
      String[] keys =
          Arrays.asList(issues).stream().map(i -> i.getKey())
          .collect(Collectors.toList()).toArray(keyArray);

      response.setContentType("text/html");
      response.setStatus(HttpServletResponse.SC_OK);

      TimeScheduleCreatePreferences sPrefs = new TimeScheduleCreatePreferences(0, 2);
      TimeScheduleWriter.writeIssueTable
          (graph, response.getWriter(), sPrefs,
              TimeScheduleDisplayPreferences.createForIssues
                  (1, 0, true, false, true,
                      keys, false, graph));

    } catch (JsonSyntaxException e) {
      response.setContentType("text/plain");
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      response.getWriter().println(e.getMessage());
    }
  }
}
