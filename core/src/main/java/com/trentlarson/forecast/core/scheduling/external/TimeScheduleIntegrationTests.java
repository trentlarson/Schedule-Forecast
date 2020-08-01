package com.trentlarson.forecast.core.scheduling.external;

import java.io.PrintWriter;
import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.trentlarson.forecast.core.dao.TeamHours;
import com.trentlarson.forecast.core.helper.ForecastUtil;
import com.trentlarson.forecast.core.scheduling.IssueDigraph;
import com.trentlarson.forecast.core.scheduling.IssueTree;
import com.trentlarson.forecast.core.scheduling.Teams;
import com.trentlarson.forecast.core.scheduling.TimeSchedule;
import com.trentlarson.forecast.core.scheduling.TimeScheduleLoader;
import com.trentlarson.forecast.core.scheduling.TimeScheduleWriter;
import com.trentlarson.forecast.core.scheduling.TimeScheduleDisplayPreferences;
import com.trentlarson.forecast.core.scheduling.TimeScheduleCreatePreferences;

public class TimeScheduleIntegrationTests {

  public static void main(String[] args) throws Exception {
    
    // This enables basic Log4J logging to standard out.
    // ... but note that you'll have to turn off a bunch of logs.
    org.apache.log4j.BasicConfigurator.configure();
    
    //IssueLoader.log4jLog.setLevel(org.apache.log4j.Level.DEBUG);
    TimeSchedule.log4jLog.setLevel(org.apache.log4j.Level.ERROR);
    TimeScheduleLoader.log4jLog.setLevel(org.apache.log4j.Level.ERROR);
    TimeScheduleDisplayPreferences.log4jLog.setLevel(org.apache.log4j.Level.ERROR);
    TimeScheduleWriter.log4jLog.setLevel(org.apache.log4j.Level.ERROR);
    //TimeSchedule.fnebLog.setLevel(org.apache.log4j.Level.DEBUG);
    //TimeSchedule.wsbLog.setLevel(org.apache.log4j.Level.ERROR);
    
    PrintWriter out = null;
    try {
      out = new PrintWriter(System.out);

      integrationMain(out);
      testIntegrationDynamicLoadAssigneeProblem(out);
    } finally {
      out.close();
    }
  }

  /**
   * This reads from the database set up by jira-test-db.sql
   * and creates the output found in gantt-test-db.html
   */
  public static void integrationMain(PrintWriter out) throws Exception {

    Connection conn = ForecastUtil.getConnection();

    TimeScheduleCreatePreferences sPrefs = new TimeScheduleCreatePreferences(0, new java.util.Date(), 1.0);
    String mainIssueKey = "FOURU-1002";
    IssueDigraph graph = TimeScheduleLoader.getGraph("", new String[]{ mainIssueKey }, new String[0], sPrefs, conn);

    // print out single-user time schedule
    {
      String user = "trent";
      out.println("Schedule for " + user + ".<br>");
      List<TimeSchedule.IssueSchedule<IssueTree>> schedule = new ArrayList<TimeSchedule.IssueSchedule<IssueTree>>();
      List<IssueTree> userIssueList = (List<IssueTree>) graph.getAssignedUserDetails().get(new Teams.AssigneeKey(null, user));
      for (int i = 0; i < userIssueList.size(); i++) {
        schedule.add
          (graph.getIssueSchedules().get
           (((TimeSchedule.IssueWorkDetail) userIssueList.get(i)).getKey()));
      }
      TimeSchedule.writeIssueSchedule
        (schedule, sPrefs.getTimeMultiplier(), true, out);
    }

    // print out that issue
    out.println("Gantt for " + mainIssueKey + ".<br>");
    TimeScheduleDisplayPreferences dPrefs =
      TimeScheduleDisplayPreferences.createForIssues
      (2, Calendar.MONTH, true, false, false, new String[]{ mainIssueKey }, false, graph);

    TimeScheduleWriter.writeIssueTable(graph, out, sPrefs, dPrefs);
    
    
    // now let's load and schedule everything
    
    graph = TimeScheduleLoader.getEntireGraph(sPrefs, conn);
    
    // Show that the first team has a lot of work while the second putters around.
    out.println("Gantt for team overloaded and underloaded teams.<br>");
    out.println("Team " + TimeScheduleTestSetup.team1Id + "<br>");
    dPrefs = TimeScheduleDisplayPreferences.createForTeam(2, 0, true, false, false, TimeScheduleTestSetup.team1Id, false, graph);
    TimeScheduleWriter.writeIssueTable(graph, out, sPrefs, dPrefs);

    out.println("Team " + TimeScheduleTestSetup.team2Id + "<br>");
    dPrefs = TimeScheduleDisplayPreferences.createForTeam(2, 0, true, false, false, TimeScheduleTestSetup.team2Id, false, graph);
    TimeScheduleWriter.writeIssueTable(graph, out, sPrefs, dPrefs);
    
    // Show that two teams get work done... in the same way, because we currently only allow one team to work on each project.
    out.println("Gantt for balanced teamwork -- BROKEN!<br>");
    out.println("Team " + TimeScheduleTestSetup.team3Id + "<br>");
    dPrefs = TimeScheduleDisplayPreferences.createForTeam(2, 0, true, false, false, TimeScheduleTestSetup.team3Id, false, graph);
    TimeScheduleWriter.writeIssueTable(graph, out, sPrefs, dPrefs);

    out.println("Team " + TimeScheduleTestSetup.team4Id + "<br>");
    dPrefs = TimeScheduleDisplayPreferences.createForTeam(2, 0, true, false, false, TimeScheduleTestSetup.team4Id, false, graph);
    TimeScheduleWriter.writeIssueTable(graph, out, sPrefs, dPrefs);
    
  }


  /**
   * See README.txt for usage.
   */
  public static void testIntegrationDynamicLoadAssigneeProblem(PrintWriter out) throws Exception {
    
    Connection conn = ForecastUtil.getConnection();
    
    TimeScheduleCreatePreferences sPrefs = new TimeScheduleCreatePreferences(0, new java.util.Date(), 1.0);
    String mainIssueKey = "MP-5";
    IssueDigraph graph = TimeScheduleLoader.getGraph(null, new String[]{ mainIssueKey }, new String[0], sPrefs, conn);

    // print out that issue
    /**
     * This will break when it tries to render time-assignment for MAR-10.  It has something to do with multiple objects being 
     * loaded with the same key, and while the assignee for one is fixed, the assignee for another is not, so the renderer attempts 
     * to find the time assignment for a non-existent entity, eg. "User john on Team 10" instead of "User john on any Team"
     */
    out.println("Gantt for " + mainIssueKey + ".  See code commentary for more info.<br>");
    TimeScheduleDisplayPreferences dPrefs =
      TimeScheduleDisplayPreferences.createForIssues
      (2, Calendar.MONTH, true, false, false, new String[]{ mainIssueKey }, false, graph);

    TimeScheduleWriter.writeIssueTable(graph, out, sPrefs, dPrefs);

  }


}
