// from https://raw.githubusercontent.com/eugenp/tutorials/master/libraries-server/src/main/java/com/baeldung/jetty/BlockingServlet.java
// from https://www.baeldung.com/jetty-embedded

package com.trentlarson.forecast.core.helper;

import java.io.IOException;
import java.util.ArrayList;
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
      ForecastInterfaces.ScheduleAndDisplayInput input =
          creator.fromJson(request.getReader(), ForecastInterfaces.ScheduleAndDisplayInput.class);

      // walk the input and link any dependents / subtasks at the top level of the list for scheduling
      IssueTree[] inputIssues = linkAllAtTopOfList(Arrays.asList(input.issues)).toArray(new IssueTree[]{});

      TimeScheduleCreatePreferences cPrefs = input.createPreferences != null
          ? input.createPreferences.getTimeScheduleCreatePreferences()
          : new TimeScheduleCreatePreferences();
      // set the hourly schedule based on input hours
      IssueDigraph graph = IssueDigraph.schedulesForIssues(inputIssues, input.createPreferences.getTimeScheduleCreatePreferences());

      response.setContentType("text/html");
      response.setStatus(HttpServletResponse.SC_OK);

      TimeScheduleDisplayPreferences dPrefs = input.displayPreferences;
      if (dPrefs == null
          || ((dPrefs.showIssues == null
               || dPrefs.showIssues.size() == 0)
              &&
              (dPrefs.showUsersInOneRow == null
               || dPrefs.showUsersInOneRow.size() == 0))) {
        // nothing is requested to be shown, so let's be nice and show all the keys
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
          int timeGranularity = dPrefs.timeGranularity;
          if (timeGranularity == 0) {
            timeGranularity = 1;
          }
          dPrefs = TimeScheduleDisplayPreferences.createForIssues(
              timeGranularity, dPrefs.timeMarker, dPrefs.showBlocked, dPrefs.hideDetails,
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

  /**
   *
   * @param issues a list of issue, with dependents & subtasks embedded
   * @return a set where all issues in the tree are listed
   */
  private Set<IssueTree> linkAllAtTopOfList(List<IssueTree> issues) {
    SortedSet<IssueTree> masterSet = new TreeSet<IssueTree>();
    Set<IssueTree> moreToCheck = new TreeSet<IssueTree>();
    moreToCheck.addAll(issues);
    linkAllAtTopOfList(masterSet, moreToCheck);
    return masterSet;
  }
  private void linkAllAtTopOfList(SortedSet<IssueTree> masterSet, Set<IssueTree> moreToCheck) {
    for (Iterator<IssueTree> issueIter =  moreToCheck.iterator(); issueIter.hasNext();) {
      IssueTree issue = issueIter.next();
      if (!masterSet.contains(issue)) {
        masterSet.add(issue);
        linkAllAtTopOfList(masterSet, issue.getDependents());
        linkAllAtTopOfList(masterSet, issue.getPrecursors());
        linkAllAtTopOfList(masterSet, issue.getSubtasks());
      }
    }
  }
}
