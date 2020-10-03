// from https://raw.githubusercontent.com/eugenp/tutorials/master/libraries-server/src/main/java/com/baeldung/jetty/BlockingServlet.java
// from https://www.baeldung.com/jetty-embedded

package com.trentlarson.forecast.core.helper;

import java.io.IOException;
import java.util.Arrays;
import java.util.stream.Collectors;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.trentlarson.forecast.core.scheduling.IssueDigraph;
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
      ForecastInterfaces.ScheduleAndDisplayInput input =
          creator.fromJson(request.getReader(), ForecastInterfaces.ScheduleAndDisplayInput.class);

      // set the hourly schedule based on input hours
      IssueDigraph graph = IssueDigraph.schedulesForIssues(input.issues, input.createPreferences.getTimeScheduleCreatePreferences());

      response.setContentType("text/html");
      response.setStatus(HttpServletResponse.SC_OK);

      TimeScheduleDisplayPreferences dPrefs = input.displayPreferences;
      if (dPrefs == null
          || (dPrefs.showIssues.size() == 0 && dPrefs.showUsersInOneRow.size() == 0)) {
        // nothing is requested to be shown, so let's show all the keys
        String[] keyArray = new String[input.issues.length];
        String[] keys =
            Arrays.asList(input.issues).stream().map(i -> i.getKey())
                .collect(Collectors.toList()).toArray(keyArray);
        if (dPrefs == null) {
          // we'll guess at reasonable defaults
          dPrefs = TimeScheduleDisplayPreferences.createForIssues(
              1, 0, true, false,
              true, keys, false, false, graph
          );
        } else {
          dPrefs = TimeScheduleDisplayPreferences.createForIssues(
              dPrefs.timeGranularity, dPrefs.timeMarker, dPrefs.showBlocked, dPrefs.hideDetails,
              dPrefs.showResolved, keys, dPrefs.showChangeTools, dPrefs.embedJiraLinks, graph
          );
        }
      }

      TimeScheduleWriter.writeIssueTable
          (graph, response.getWriter(), graph.getTimeScheduleCreatePreferences(), dPrefs);

    } catch (JsonSyntaxException e) {
      String message = e.getMessage() + " ... due to: " + (e.getCause() == null ? "" : e.getCause().getMessage());
      System.out.println("Client JsonSyntaxException: " + message);
      response.setContentType("text/plain");
      response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
      response.getWriter().println(message);
    }
  }
}
