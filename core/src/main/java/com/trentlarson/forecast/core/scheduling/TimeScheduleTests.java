package com.trentlarson.forecast.core.scheduling;

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

public class TimeScheduleTests {

  public static void main(String[] args) throws Exception {
    
    //IssueLoader.log4jLog.setLevel(org.apache.log4j.Level.DEBUG);
    //TimeScheduleLoader.log4jLog.setLevel(org.apache.log4j.Level.DEBUG);
    //TimeSchedule.log4jLog.setLevel(org.apache.log4j.Level.DEBUG);
    //TimeScheduleWriter.log4jLog.setLevel(org.apache.log4j.Level.DEBUG);
    
    PrintWriter out = null;
    try {
      out = new PrintWriter(System.out);

      unitMain(out);
      //integrationMain(out);
      //testIntegrationDynamicLoadAssigneeProblem(out);
    } finally {
      out.close();
    }
  }
  
  
  
  private static final SimpleDateFormat SLASH_DATE = new SimpleDateFormat("yyyy/MM/dd");
  private static final SimpleDateFormat SLASH_TIME = new SimpleDateFormat("yyyy/MM/dd HH:mm");

  /**
   * This generates HTML output that can be compared with gantt-test.html
   */
  public static void unitMain(PrintWriter out) throws Exception {

    out.println("<P>");
    out.println("<H1>Here are the scheduling tests for team tasks.</H2>");

    // another test: allow start/end times outside 0-8 AM range
    // another test: I believe the lighter colors don't work for teams

    outputDaylightSavingsTestResults(out);

    outputWithoutTeamHoursTestResults(out);

    outputVariableTimeTestResults(out);

    outputSplitTeamTestResults(out);

    outputStartTimeTestResults(out);

    outputBlockedSubtaskTestResults(out);

    outputManyBlockedTestResults(out);

    out.println("<P>");
    out.println("<H1>Here are the basic TimeSchedule tests.</H2>");
    TimeSchedule.outputTestResults(out);

  }

  /**
     A case with a very long task (which broke this).
   */
  public static void outputDaylightSavingsTestResults(PrintWriter out) throws Exception {
    
    out.println("<P>");
    out.println("<H2>... around a daylight-savings time switch.</H2>");

    String username = "matt";
    IssueTree[] manyIssues = {
      new IssueTree
      ("TEST-230", "~18-week issue", username, 1L,
       (int) (751.5 * 3600), 0, 0.0, null, null, 5, false)
    };
    
    Map<Teams.UserTimeKey,List<TeamHours>> userWeeklyHours =
      new TreeMap<Teams.UserTimeKey,List<TeamHours>>();
    List<TeamHours> hourList = new ArrayList();
    hourList.add(new TeamHours(1L, null, username, SLASH_TIME.parse("2006/12/03 00:00"), 40.0));
    hourList.add(new TeamHours(2L, null, username, SLASH_DATE.parse("2007/03/19"), 0.0));
    hourList.add(new TeamHours(3L, null, username, SLASH_TIME.parse("2007/03/19 04:00"), 40.0));
    userWeeklyHours.put(new Teams.UserTimeKey(1L, username), hourList);

    Map<Teams.AssigneeKey,List<IssueTree>> userDetails =
      createUserDetails(manyIssues, userWeeklyHours);
    TimeScheduleCreatePreferences sPrefs =
      new TimeScheduleCreatePreferences(0, SLASH_DATE.parse("2006/12/03"), 1);
    IssueDigraph graph =
      TimeScheduleLoader.schedulesForUserIssues3
      (userDetails, userWeeklyHours, sPrefs);

    Teams.AssigneeKey user = new Teams.AssigneeKey(1L, "matt");

    // print out team table schedule
    List<TimeSchedule.IssueSchedule<IssueTree>> schedule = new ArrayList();
    List<IssueTree> userIssueList = graph.getAssignedUserDetails().get(user);
    for (int i = 0; i < userIssueList.size(); i++) {
      schedule.add
        (graph.getIssueSchedules().get
         (((TimeSchedule.IssueWorkDetail) userIssueList.get(i)).getKey()));
    }
    TimeSchedule.writeIssueSchedule
      (schedule, sPrefs.getTimeMultiplier(), true, out);

  }




  public static void outputWithoutTeamHoursTestResults(PrintWriter out) throws Exception {

    out.println("<P>");
    out.println("<H2>... without any teams defined.</H2>");

    IssueTree[] manyIssues = {
        new IssueTree
        ("TEST-231", "null team & assignee", null, null,
         40 * 3600, 0, 0.0, null, null, 5, false)
      };

    Map<Teams.UserTimeKey,List<TeamHours>> userWeeklyHours = new TreeMap<Teams.UserTimeKey,List<TeamHours>>();

    Map<Teams.AssigneeKey,List<IssueTree>> userDetails = createUserDetails(manyIssues, userWeeklyHours);
    TimeScheduleCreatePreferences sPrefs = new TimeScheduleCreatePreferences(0, SLASH_DATE.parse("2006/12/03"), 1);
    IssueDigraph graph = TimeScheduleLoader.schedulesForUserIssues3(userDetails, userWeeklyHours, sPrefs);

    out.println("<br><br>");
    out.println("Unassigned user gantt chart, even though there are no team hours defined.<br>");
    TimeScheduleWriter.writeIssueTable
    (graph, out, sPrefs,
     TimeScheduleDisplayPreferences
     .createForUser(1, 0, true, false, false, (String)null, false, graph));

    out.println("<br><br>");
    out.println("Schedule table, even though there are no team hours defined.<br>");
    List<TimeSchedule.IssueSchedule<IssueTree>> schedule = new ArrayList<TimeSchedule.IssueSchedule<IssueTree>>();
    Teams.AssigneeKey user = new Teams.AssigneeKey(null, null);
    List<IssueTree> userIssueList = graph.getAssignedUserDetails().get(user);
    for (int i = 0; i < userIssueList.size(); i++) {
      schedule.add
        (graph.getIssueSchedules().get
         (((TimeSchedule.IssueWorkDetail) userIssueList.get(i)).getKey()));
    }
    TimeSchedule.writeIssueSchedule
      (schedule, sPrefs.getTimeMultiplier(), true, out);
    
    
    //// Now try it with some team time defined.
    userWeeklyHours = new TreeMap<Teams.UserTimeKey,List<TeamHours>>();
    List<TeamHours> hourList = new ArrayList();
    hourList.add(new TeamHours(1L, null, null, SLASH_TIME.parse("2006/12/03 00:00"), 80.0));
    userWeeklyHours.put(new Teams.UserTimeKey(null, null), hourList);

    userDetails = createUserDetails(manyIssues, userWeeklyHours);
    sPrefs = new TimeScheduleCreatePreferences(0, SLASH_DATE.parse("2006/12/03"), 1);
    graph = TimeScheduleLoader.schedulesForUserIssues3(userDetails, userWeeklyHours, sPrefs);

    out.println("<br><br>");
    out.println("Unassigned user-team gantt chart, where 80 null-team hours are defined.<br>");
    TimeScheduleWriter.writeIssueTable
      (graph, out, sPrefs,
       TimeScheduleDisplayPreferences
       .createForUser(1, 0, true, false, false, user, false, graph));

  }


  /**
     Now we test where a team may have issues to schedule, and it may have more
     (or fewer) than the standard 40 hours per week.
   */
  public static void outputVariableTimeTestResults(PrintWriter out) throws Exception {

    out.println("<P>");
    out.println("<H2>... for teams with various time availability.</H2>");

    Date startDate = SLASH_DATE.parse("2005/04/05");
    int jira_day = 8 * 60 * 60;

    IssueTree[] manyIssues = {
      new IssueTree
      ("TEST-200", "5-day issue", null, 1L,
       5 * jira_day, 0 * jira_day, 0.0,
       null, null, 5, false)
      ,
      new IssueTree
      ("TEST-201", "3-day issue", null, 1L,
       3 * jira_day, 0 * jira_day, 0.0,
       null, null, 6, false)
      ,
      new IssueTree
      ("TEST-202", "3 days again", null, 1L,
       3 * jira_day, 0 * jira_day, 0.0,
       SLASH_DATE.parse("2005/04/11"), null, 6, false)
      ,
      new IssueTree
      ("TEST-203", "14-day issue", null, 1L,
       14 * jira_day, 0 * jira_day, 0.0,
       null, null, 3, false)
      ,
      new IssueTree
      ("TEST-204", "9-day issue", null, 1L,
       9 * jira_day, 0 * jira_day, 0.0,
       null, null, 6, false)
      ,
      new IssueTree
      ("TEST-204.1", "8-day issue", null, 1L,
       8 * jira_day, 0 * jira_day, 0.0,
       null, null, 7, false)
    };

    
    Map<Teams.UserTimeKey,List<TeamHours>> userWeeklyHours =
      new TreeMap<Teams.UserTimeKey,List<TeamHours>>();
    List<TeamHours> hourList = new ArrayList();
    hourList.add(new TeamHours(0L, 1L, null, SLASH_DATE.parse("2005/04/04"), 120.0));
    userWeeklyHours.put(new Teams.UserTimeKey(1L, null), hourList);

    Map<Teams.AssigneeKey,List<IssueTree>> userDetails =
      createUserDetails(manyIssues, userWeeklyHours);
    TimeScheduleCreatePreferences sPrefs =
      new TimeScheduleCreatePreferences(0, SLASH_DATE.parse("2005/04/05"), 1);
    IssueDigraph graph =
      TimeScheduleLoader.schedulesForUserIssues3
      (userDetails, userWeeklyHours, sPrefs);
      


    // print out team Gantt chart
    Teams.AssigneeKey user = new Teams.AssigneeKey(1L, null);
    out.println("<br><br>");
    out.println("Tree for " + user + ", which should not be longer than 3 weeks.<br>");
    TimeScheduleWriter.writeIssueTable
      (graph, out, sPrefs,
       TimeScheduleDisplayPreferences
       .createForUser(1, 0, true, false, false, user, false, graph));

    // print out team table schedule
    List<TimeSchedule.IssueSchedule<IssueTree>> schedule = new ArrayList();
    List<IssueTree> userIssueList = graph.getAssignedUserDetails().get(user);
    for (int i = 0; i < userIssueList.size(); i++) {
      schedule.add
        (graph.getIssueSchedules().get
         (((TimeSchedule.IssueWorkDetail) userIssueList.get(i)).getKey()));
    }
    TimeSchedule.writeIssueSchedule
      (schedule, sPrefs.getTimeMultiplier(), true, out);

    
    
    // Now show what happens when we assign an issue to someone.
    manyIssues[0] =
      new IssueTree
      ("TEST-200.1", "5-day issue", "trent", 1L,
       5 * jira_day, 0 * jira_day, 0.0, null, null, 5, false);

    userDetails =
      createUserDetails(manyIssues, userWeeklyHours);
    sPrefs =
      new TimeScheduleCreatePreferences(0, SLASH_DATE.parse("2005/04/05"), 1);
    graph =
      TimeScheduleLoader.schedulesForUserIssues3
      (userDetails, userWeeklyHours, sPrefs);
    
    user = new Teams.AssigneeKey(1L, null);
    out.println("<br><br>");
    out.println("Tree for " + user + ", which should now be shorter since 'trent' handles one task (TEST-200.1).<br>");
    TimeScheduleWriter.writeIssueTable
      (graph, out, sPrefs,
       TimeScheduleDisplayPreferences
       .createForUser(1, 0, true, false, false, user, false, graph));
  
  }

  public static void outputSplitTeamTestResults(PrintWriter out) throws Exception {
    
    out.println("<P>");
    out.println("<H2>... for people split across teams.</H2>");

    int jira_day = 8 * 60 * 60;


    IssueTree[] manyIssues = {
      new IssueTree
      ("TEST-205", "3-day issue", "trent", 1L,
       3 * jira_day, 0 * jira_day, 0.0,
       null, null, 5, false)
      ,
      new IssueTree
      ("TEST-206", "3-day issue", "trent", 1L,
       3 * jira_day, 0 * jira_day, 0.0,
       null, null, 6, false)
      ,
      new IssueTree
      ("TEST-207", "3-day issue", "trent", 2L,
       3 * jira_day, 0 * jira_day, 0.0,
       null, null, 4, false)
      ,
      new IssueTree
      ("TEST-208", "3-day issue", "trent", null,
       3 * jira_day, 0 * jira_day, 0.0,
       null, null, 5, false)
      ,
      new IssueTree
      ("TEST-209", "3-day issue", "trent", null,
       3 * jira_day, 0 * jira_day, 0.0,
       null, null, 6, false)
      ,
      new IssueTree
      ("TEST-210", "3-day issue", "trent", null,
       3 * jira_day, 0 * jira_day, 0.0,
       null, null, 7, false)
      ,
      new IssueTree
      ("TEST-211", "3-day issue", "trent", null,
       3 * jira_day, 0 * jira_day, 0.0,
       null, null, 8, false)
      ,
      new IssueTree
      ("TEST-212", "3-day issue", "trent", null,
       3 * jira_day, 0 * jira_day, 0.0,
       null, null, 9, false)
    };



    Map<Teams.UserTimeKey,List<TeamHours>> userWeeklyHours =
      new TreeMap<Teams.UserTimeKey,List<TeamHours>>();
    List<TeamHours> hourList = new ArrayList();

    hourList = new ArrayList();
    hourList.add(new TeamHours(0L, 1L, "trent", SLASH_DATE.parse("2005/04/04"), 8.0));
    hourList.add(new TeamHours(1L, 1L, "trent", SLASH_DATE.parse("2005/04/18"), 16.0));
    userWeeklyHours.put(new Teams.UserTimeKey(1L, "trent"), hourList);
    hourList = new ArrayList();
    hourList.add(new TeamHours(2L, null, "trent", SLASH_DATE.parse("2005/04/04"), 8.0));
    hourList.add(new TeamHours(3L, null, "trent", SLASH_DATE.parse("2005/04/18"), 30.0));
    userWeeklyHours.put(new Teams.UserTimeKey(null, "trent"), hourList);

    Map<Teams.AssigneeKey,List<IssueTree>> userDetails =
      createUserDetails(manyIssues, userWeeklyHours);

    TimeScheduleCreatePreferences sPrefs =
      new TimeScheduleCreatePreferences(0, SLASH_DATE.parse("2005/04/11"), 1);
    IssueDigraph graph =
      TimeScheduleLoader.schedulesForUserIssues3
      (userDetails, userWeeklyHours, sPrefs);

    out.println("<br><br>");
    out.println("Trees with hours on teams with hours: " + graph.getUserWeeklyHoursAvailable());

    out.println("<br><br>");
    out.println("Graph for trent");
    Teams.AssigneeKey userKey = new Teams.AssigneeKey(1L, "trent");
    TimeScheduleWriter.writeIssueTable
      (graph, out, sPrefs,
       TimeScheduleDisplayPreferences
       .createForUser(1, 0, true, false, true, userKey.getUsername(), false, graph));




    out.println("<br><br>");
    out.println("Schedule for trent on team 1");
    userKey = new Teams.AssigneeKey(1L, "trent");
    List<TimeSchedule.IssueSchedule<IssueTree>> schedule = new ArrayList();
    List<IssueTree> userIssueList = userDetails.get(userKey);
    for (int i = 0; i < userIssueList.size(); i++) {
      schedule.add
        (graph.getIssueSchedules().get
         (((TimeSchedule.IssueWorkDetail) userIssueList.get(i)).getKey()));
    }
    TimeSchedule.writeIssueSchedule
      (schedule, sPrefs.getTimeMultiplier(), true, out);


    out.println("<br><br>");
    out.println("Schedule for trent on team 2");
    userKey = new Teams.AssigneeKey(2L, "trent");
    schedule = new ArrayList();
    userIssueList = userDetails.get(userKey);
    for (int i = 0; i < userIssueList.size(); i++) {
      schedule.add
        (graph.getIssueSchedules().get
         (((TimeSchedule.IssueWorkDetail) userIssueList.get(i)).getKey()));
    }
    TimeSchedule.writeIssueSchedule
      (schedule, sPrefs.getTimeMultiplier(), true, out);

    out.println("<br><br>");
    out.println("Schedule for trent on no team");
    userKey = new Teams.AssigneeKey(null, "trent");
    schedule = new ArrayList();
    userIssueList = userDetails.get(userKey);
    for (int i = 0; i < userIssueList.size(); i++) {
      schedule.add
        (graph.getIssueSchedules().get
         (((TimeSchedule.IssueWorkDetail) userIssueList.get(i)).getKey()));
    }
    TimeSchedule.writeIssueSchedule
      (schedule, sPrefs.getTimeMultiplier(), true, out);
    
    /**
    out.println("<br><br>");
    out.println("Schedule for unassigned on team 1");
    userKey = new Teams.AssigneeKey(1L, null);
    schedule = new ArrayList();
    userIssueList = userDetails.get(userKey);
    for (int i = 0; i < userIssueList.size(); i++) {
      schedule.add
        (graph.getIssueSchedules().get
         (((TimeSchedule.IssueWorkDetail) userIssueList.get(i)).getKey()));
    }
    TimeSchedule.writeIssueSchedule
      (schedule, sPrefs.getTimeMultiplier(), true, out);
    **/
  }




  public static void outputStartTimeTestResults(PrintWriter out) throws Exception {

    out.println("<P>");
    out.println("<H2>... for tasks with a starting time.</H2>");

    Date startDate = SLASH_DATE.parse("2005/04/05");
    int jira_day = 8 * 60 * 60;

    IssueTree[] manyIssues =
      {
        new IssueTree
        ("TEST-100", "one week", "trent", 1L,
         5 * jira_day, 0 * jira_day, 0.0,
         null, null, 3, false)
        ,
        // This one has lower priority, but it should be scheduled first because
        // it has a start time.
        new IssueTree
        ("TEST-101", "one day", "trent", 1L,
         1 * jira_day, 0 * jira_day, 0.0,
         null, SLASH_DATE.parse("2005/04/11"), 6, false)
      };


    Map<Teams.UserTimeKey,List<TeamHours>> userWeeklyHours =
      new TreeMap<Teams.UserTimeKey,List<TeamHours>>();
    Map<Teams.AssigneeKey,List<IssueTree>> userDetails =
        createUserDetails(manyIssues, userWeeklyHours);
    List<TeamHours> hourList = new ArrayList();
    TimeScheduleCreatePreferences sPrefs =
      new TimeScheduleCreatePreferences(0, startDate, 2);
    IssueDigraph graph =
      TimeScheduleLoader.schedulesForUserIssues3
        (userDetails, userWeeklyHours, sPrefs);



    {
      String userPerson = "trent";
      Long userTeam = 1L;
      Teams.AssigneeKey userKey = new Teams.AssigneeKey(userTeam, userPerson);

      // print out single-user table schedule
      List<TimeSchedule.IssueSchedule<IssueTree>> schedule = new ArrayList();
      List<IssueTree> userIssueList = userDetails.get(userKey);
      for (int i = 0; i < userIssueList.size(); i++) {
        schedule.add
          (graph.getIssueSchedules().get
           (((TimeSchedule.IssueWorkDetail) userIssueList.get(i)).getKey()));
      }
      TimeSchedule.writeIssueSchedule
        (schedule, sPrefs.getTimeMultiplier(), true, out);

      // print out single-user Gantt chart
      out.println("<br><br>");
      out.println("Tree for " + userKey + ".<br>");
      TimeScheduleWriter.writeIssueTable
        (graph, out, sPrefs,
         TimeScheduleDisplayPreferences
         .createForUser(1, 0, true, false, false, userKey, false, graph));
    }

  }







  public static void outputBlockedSubtaskTestResults(PrintWriter out) throws Exception {

    out.println("<P>");
    out.println("<H2>... for tasks blocked by a previous issue with a late subtask.</H2>");

    out.println("<br><br>");
    out.println("(TEST-22 should be blocked until the 21st.)");

    Date startDate = SLASH_DATE.parse("2005/04/05");

    IssueTree issue20 =
      new IssueTree
      ("TEST-20", "sub issue", "trent", 1L, 16 * 3600, 0, 0.0,
       null, null, 3, false);
    IssueTree issue21 =
      new IssueTree
      ("TEST-21", "some issue", "brent", 1L, 32 * 3600, 0, 0.0,
       null, null, 2, false);
    IssueTree issue22 =
      new IssueTree
      ("TEST-22", "some issue", "ken", 1L, 1 * 3600, 0, 0.0,
       null, null, 2, false);

    issue20.addSubtask(issue21);
    issue20.addDependent(issue22);

    IssueTree[] manyIssues = { issue20, issue21, issue22 };

    Map<Teams.AssigneeKey,List<IssueTree>> userDetails =
        createUserDetails(manyIssues, new HashMap());

    TimeScheduleCreatePreferences sPrefs =
      new TimeScheduleCreatePreferences(0, startDate, 2);
    IssueDigraph graph =
      TimeScheduleLoader.schedulesForUserIssues3
      (userDetails, new HashMap(), sPrefs);

    TimeScheduleWriter.writeIssueTable
      (graph, out, sPrefs,
       TimeScheduleDisplayPreferences
       .createForIssues(1, 0, true, false, false,
                        new String[]{"TEST-22"},
                        false, graph));

    TimeScheduleWriter.writeIssueTable
      (graph, out, sPrefs, TimeScheduleDisplayPreferences.createForCriticalPaths(1, 0, false, false, new String[]{"TEST-22"}, graph));

    out.println(TimeScheduleSearch.criticalPathFor(issue22, graph).toString());
    
  }

  public static void outputManyBlockedTestResults(PrintWriter out) throws Exception {

    out.println("<P>");
    out.println("<H2>... for tasks with complicated dependencies.</H2>");

    out.println("<p>Creating issues without dependencies...<p>");

    Date startDate = SLASH_DATE.parse("2005/04/05");

    IssueTree issue_6 =
      new IssueTree
      ("TEST--6", "Ancestor test issue", "trent", 1L,
       0 * 3600, 0 * 3600, 0.0, null, null, 1, false);
    IssueTree issue_5 =
      new IssueTree
      ("TEST--5", "Ancestor test issue", "trent", 1L,
       4 * 3600, 0 * 3600, 0.0, null, null, 1, false);
    IssueTree issue_4 =
      new IssueTree
      ("TEST--4", "Ancestor test issue", "ken", 1L,
       4 * 3600, 0 * 3600, 0.0, null, null, 1, false);
    IssueTree issue_3 =
      new IssueTree
      ("TEST--3", "Ancestor test issue", "brent", 1L,
       4 * 3600, 0 * 3600, 0.0, null, null, 1, false);
    IssueTree issue_2 =
      new IssueTree
      ("TEST--2", "Ancestor test issue", "trent", 1L,
       4 * 3600, 0 * 3600, 0.0, null, null, 1, false);
    IssueTree issue_1 =
      new IssueTree
      ("TEST--1", "Ancestor test issue", "ken", 1L,
       4 * 3600, 0 * 3600, 0.0, null, null, 1, false);
    IssueTree issue0 =
      new IssueTree
      ("TEST-0", "Grandparent test issue", "fred", 1L,
       8 * 3600, 0 * 3600, 0.0,
       SLASH_DATE.parse("2005/01/01"), null, 1, false);
    IssueTree issue1 =
      new IssueTree
      ("TEST-1", "Parent test issue", "brent", 1L,
       10 * 3600, 0 * 3600, 0.0,
       SLASH_DATE.parse("2005/04/01"), null, 1, false);
    IssueTree issue2 =
      new IssueTree
      ("TEST-2", "issue", "ken", 1L, 24 * 3600, 1 * 3600, 0.0,
       SLASH_DATE.parse("2005/04/16"), null, 8, false);
    IssueTree issue4 =
      new IssueTree
      ("TEST-4", "top", "trent", 1L, 8 * 3600, 1 * 3600, 0.0,
       SLASH_DATE.parse("2005/04/13"), null, 4, false);
    IssueTree issue9 =
      new IssueTree
      ("TEST-9", "issue", "ken", 1L, 20 * 3600, 1 * 3600, 0.0,
       SLASH_DATE.parse("2005/04/05"), null, 3, false);
    IssueTree issue11 =
      new IssueTree
      ("TEST-11", "sub issue", "trent", 1L, 4 * 3600, 1 * 3600, 0.0,
       SLASH_DATE.parse("2005/04/15"), null, 3, true);
    IssueTree issue12 =
      new IssueTree
      ("TEST-12", "sub issue", "ken", 1L, 3 * 3600, 1 * 3600, 0.0,
       SLASH_DATE.parse("2005/06/01"), null, 2, false);
    IssueTree issue13 =
      new IssueTree
      ("TEST-13", "after TEST-1,2", "trent", 1L, 10 * 3600, 1 * 3600, 0.0,
       null, null, 2, true);
    IssueTree issue13_1 =
      new IssueTree
      ("TEST-13-1", "sub of TEST-13", "trent", 1L, 10 * 3600, 1 * 3600, 0.0,
       null, null, 9, false);
    IssueTree issue14 =
      new IssueTree
      ("TEST-14", "dependant issue", "trent", 1L, 16 * 3600, 1 * 3600, 0.0,
       SLASH_DATE.parse("2005/06/01"), null, 4, false);
    IssueTree issue15 =
      new IssueTree
      ("TEST-15", "dependant issue", "trent", 1L, 4 * 3600, 1 * 3600, 0.0,
       SLASH_DATE.parse("2005/04/07"), null, 5, false);
    IssueTree issue16 =
      new IssueTree
      ("TEST-16", "some issue", "trent", 1L, 12 * 3600, 1 * 3600, 0.0,
       SLASH_DATE.parse("2005/04/15"), null, 6, false);

    IssueTree[] manyIssues = {
      issue_6, issue_5, issue_4, issue_3, issue_2, issue_1, issue0, issue1,
      issue2, issue4, issue9, issue11, issue12, issue13, issue13_1,
      issue14, issue15, issue16
    };

    Map<Teams.UserTimeKey,List<TeamHours>> userWeeklyHours =
      new TreeMap<Teams.UserTimeKey,List<TeamHours>>();
    List<TeamHours> hourList = new ArrayList();
    hourList.add(new TeamHours(0L, 1L, "trent", SLASH_DATE.parse("2005/01/01"), 40.0));
    userWeeklyHours.put(new Teams.UserTimeKey(1L, "trent"), hourList);
           
    hourList = new ArrayList();
    hourList.add(new TeamHours(1L, 1L, "ken", SLASH_DATE.parse("2005/01/01"), 40.0));
    userWeeklyHours.put(new Teams.UserTimeKey(1L, "ken"), hourList);

    hourList = new ArrayList();
    hourList.add(new TeamHours(2L, 1L, "brent", SLASH_DATE.parse("2005/01/01"), 40.0));
    userWeeklyHours.put(new Teams.UserTimeKey(1L, "brent"), hourList);

    hourList = new ArrayList();
    hourList.add(new TeamHours(3L, 1L, "fred", SLASH_DATE.parse("2005/01/01"), 40.0));
    userWeeklyHours.put(new Teams.UserTimeKey(1L, "fred"), hourList);

    Map<Teams.AssigneeKey,List<IssueTree>> userDetails =
        createUserDetails(manyIssues, userWeeklyHours);

    TimeScheduleCreatePreferences sPrefs =
      new TimeScheduleCreatePreferences(0, startDate, 2);
    IssueDigraph graph =
      TimeScheduleLoader.schedulesForUserIssues3
      (userDetails, userWeeklyHours, sPrefs);


    // print out single-user table schedule & Gantt chart
    {
      String userPerson = "ken";
      Long userTeam = 1L;
      Teams.AssigneeKey userKey = new Teams.AssigneeKey(userTeam, userPerson);

      List<TimeSchedule.IssueSchedule<IssueTree>> schedule = new ArrayList();
      List<IssueTree> userIssueList = userDetails.get(userKey);
      for (int i = 0; i < userIssueList.size(); i++) {
        schedule.add
          (graph.getIssueSchedules().get
           (((TimeSchedule.IssueWorkDetail) userIssueList.get(i)).getKey()));
      }
      TimeSchedule.writeIssueSchedule
        (schedule, sPrefs.getTimeMultiplier(), true, out);

      out.println("<br><br>");
      out.println("Tree for " + userKey + ".<br>");
      TimeScheduleWriter.writeIssueTable
        (graph, out, sPrefs,
         TimeScheduleDisplayPreferences.createForUser(1, 0, true, false, false, userKey, false, graph));


      out.println("<br><br>");
      out.println("Tree for " + userKey + " allowing modifications.<br>");
      TimeScheduleWriter.writeIssueTable
        (graph, out, sPrefs,
         TimeScheduleDisplayPreferences.createForUser(1, 0, true, false, false, userKey, true, graph));

    }





    out.println("<p>Adding dependency relationships...<p>");

    issue_6.addDependent(issue_4);
    issue_5.addDependent(issue_3);
    issue_4.addDependent(issue_3);
    issue_4.addDependent(issue_1);
    issue_3.addDependent(issue_1);
    issue_2.addDependent(issue_1);
    issue_1.addDependent(issue0);
    issue0.addDependent(issue1);
    issue0.addDependent(issue12);
    issue0.addDependent(issue14);
    issue1.addSubtask(issue11);
    issue1.addSubtask(issue12);
    issue1.addDependent(issue4);
    issue1.addDependent(issue13);
    issue2.addDependent(issue13);
    issue2.addSubtask(issue12);
    issue2.addSubtask(issue14);
    issue2.addSubtask(issue15);
    issue13.addSubtask(issue13_1);


    /**
       GRAPH OF SUBTASKS (+) AND BLOCKED TASKS (|)

. _6
.     _5
.  |----- _4
.      |----- _3
.          |- _3
.          |--------- _1
.              |----- _1
.                 _2
.                  |- _1
.                      |-  0
.                          |-  1
.                              +---------------------------- 11
.                              +-------------------------------- 12
.                          |------------------------------------ 12
.                          |-------------------------------------------- 14
.                                  2
.                                  +---------------------------- 12
.                                  +------------------------------------ 14
.                                  +---------------------------------------- 15
.                              |---------  4
.                              |------------------------------------ 13
.                                  |-------------------------------- 13
.                                                                     +_ 13_1
    **/

    graph = 
      TimeScheduleLoader.schedulesForUserIssues3
      (userDetails, userWeeklyHours, sPrefs);


    List branches1 = TimeScheduleSearch.findPredecessorBranches(issue1);
    out.println("<br><br>");
    out.println("All branches of TEST-1: ");
    out.println(branches1);

    out.println("<br><br>");
    out.println("Tree for issues 1 and 2.<br>");
    TimeScheduleWriter.writeIssueTable
      (graph, out,
       sPrefs,
       TimeScheduleDisplayPreferences.createForIssues
       (1, 0, true, false, true,
        new String[]{ issue1.getKey(), issue2.getKey() },
        false, graph));

    out.println("<p>");
    out.println("Squished schedule for issues 1 and 2.<br>");
    TimeScheduleWriter.writeIssueTable
      (graph, out,
       sPrefs,
       TimeScheduleDisplayPreferences.createForIssues
       (4, 0, true, false, true,
        new String[]{ issue1.getKey(), issue2.getKey() },
        false, graph));

    out.println("<p>");
    out.println("Tree for issue 1, w/o resolved.<br>");
    TimeScheduleWriter.writeIssueTable
      (graph, out,
       sPrefs,
       TimeScheduleDisplayPreferences.createForIssues
       (1, 0, true, false, false,
        new String[]{ issue1.getKey() },
        false, graph));

    out.println("<p>");
    out.println("Schedule for trent.<br>");
    TimeScheduleWriter.writeIssueTable
      (graph, out,
       sPrefs,
       TimeScheduleDisplayPreferences.createForUser
       (1, 0, true, false, true, new Teams.AssigneeKey(1L, "trent"),
        false, graph));

    {
      Teams.AssigneeKey user = new Teams.AssigneeKey(1L, "fred");
      out.println("<br><br>");
      out.println("Tree for " + user + ".<br>");
      TimeScheduleWriter.writeIssueTable
        (graph, out, sPrefs,
         TimeScheduleDisplayPreferences.createForUser(1, 0, true, false, false, user, false, graph));

    }

    {
      Teams.AssigneeKey[] users =
        { new Teams.AssigneeKey(1L, "fred"),
          new Teams.AssigneeKey(1L, "brent"),
          new Teams.AssigneeKey(1L, "ken"),
          new Teams.AssigneeKey(1L, "trent") };
      out.println("<p>");
      out.println("One-row schedule for all.<br>");
      TimeScheduleWriter.writeIssueTable
        (graph, out,
         sPrefs,
         TimeScheduleDisplayPreferences.createForUsers
         (1, 0, true, false, true, users, graph));

      out.println("<p>");
      out.println("Schedule for ken.<br>");
      Teams.AssigneeKey user = new Teams.AssigneeKey(1L, "ken");
      TimeScheduleWriter.writeIssueTable
        (graph, out,
         sPrefs,
         TimeScheduleDisplayPreferences.createForUser
         (1, 0, true, false, true, user, false, graph));
    }

    // print out single-user table schedule
    {
      Teams.AssigneeKey[] users = { new Teams.AssigneeKey(1L, "ken") };
      List<TimeSchedule.IssueSchedule<IssueTree>> schedule = new ArrayList();
      List<IssueTree> userIssueList = userDetails.get(users[0]);
      for (int i = 0; i < userIssueList.size(); i++) {
        schedule.add
          (graph.getIssueSchedules().get
           (((TimeSchedule.IssueWorkDetail) userIssueList.get(i)).getKey()));
      }
      out.println("Schedule for " + Arrays.asList(users) + ".<br>");
      TimeSchedule.writeIssueSchedule
        (schedule, sPrefs.getTimeMultiplier(), true, out);
    }

    {
      out.println("<p>");
      out.println("Critical Path for " + Arrays.asList(issue13_1.getKey()) + ".<br>");
      TimeScheduleWriter.writeIssueTable
      (graph, out, sPrefs,
       TimeScheduleDisplayPreferences.createForCriticalPaths(1, 0, false, false, new String[]{issue13_1.getKey()}, graph));
    }


    out.println("<xmp>");

    out.println
      ((issue1.totalTimeSpent() == 2 * 3600 ? "pass" : "fail")
       + " (totalColumns: expected " + 2 * 3600 + "; got " + issue1.totalTimeSpent() + ")");
    out.println
      ((issue1.totalEstimate() == 10 * 3600 ? "pass" : "fail")
       + " (totalColumns: expected " + 10 * 3600 + "; got " + issue1.totalEstimate() + ")");

    out.println
      ((issue1.totalTimeSpent() == 2 * 3600 ? "pass" : "fail")
       + " (totalColumns: expected " + 2 * 3600 + "; got " + issue1.totalTimeSpent() + ")");
    out.println
      ((issue1.totalEstimate() == 10 * 3600 ? "pass" : "fail")
       + " (totalColumns: expected " + 10 * 3600 + "; got " + issue1.totalEstimate() + ")");


    TimeSchedule.IssueSchedule sched13 =
      graph.getIssueSchedules().get(issue13.getKey());
    Calendar acal = Calendar.getInstance();
    acal = sched13.getAdjustedBeginCal();
    out.println
      ((acal.get(Calendar.DATE) == 28 ? "pass" : "fail")
       + " (" + issue13.getKey() + " should start on Apr 22; got "
       + acal.get(Calendar.DATE) + ")");
    out.println
      ((acal.get(Calendar.HOUR_OF_DAY) == 8 ? "pass" : "fail")
       + " (" + issue13.getKey() + " should start at 14; got " +
       + acal.get(Calendar.HOUR_OF_DAY) + ")");

    TimeSchedule.IssueSchedule sched2 =
      graph.getIssueSchedules().get(issue2.getKey());
    boolean notBefore = !sched13.getBeginDate().before(sched2.getEndDate());
    out.println((notBefore ? "pass" : "fail")
                + " (" + issue13.getKey() + " starts after " + issue2.getKey() + " ends)");

    TimeSchedule.IssueSchedule sched14 =
      graph.getIssueSchedules().get(issue14.getKey());
    acal = sched14.getAdjustedBeginCal();
    out.println
      ((acal.get(Calendar.DATE) == 12 ? "pass" : "fail")
       + " (" + issue14.getKey() + " should start on the 12th; got " +
       + acal.get(Calendar.DATE) + ")");

    TimeSchedule.IssueSchedule sched4 =
      graph.getIssueSchedules().get(issue4.getKey());
    acal = sched4.getAdjustedBeginCal();
    out.println
      ((acal.get(Calendar.DATE) == 15 ? "pass" : "fail")
       + " (" + issue4.getKey() + " should start on the 14th; got " +
       + acal.get(Calendar.DATE) + ")");

    List branches0 = TimeScheduleSearch.findPredecessorBranches(issue0);
    out.println
      ((branches0.size() == 6 ? "pass" : "fail")
       + " (should be 6 branches preceding 0; got " + branches0.size() + ")");

    out.println("</xmp>");

    out.println();
    out.println("All branches of TEST-0: ");
    out.println(branches0);

  }


  private static Map<Teams.AssigneeKey,List<IssueTree>> createUserDetails
  (IssueTree[] issues, Map<Teams.UserTimeKey,List<TeamHours>> userWeeklyHours) {

    Map<Teams.AssigneeKey,List<IssueTree>> userDetails = new HashMap();
    for (int i = 0; i < issues.length; i++) {
      addIssue(issues[i], userDetails, userWeeklyHours);
    }
    return userDetails;
  }
  /**
     Add issue to userDetails.
   */
  private static void addIssue
  (IssueTree issue,
   Map<Teams.AssigneeKey,List<IssueTree>> userDetails,
   Map<Teams.UserTimeKey,List<TeamHours>> userWeeklyHours) {

    Teams.AssigneeKey assignee = issue.getRawAssigneeKey();
    List<IssueTree> userIssues = userDetails.get(assignee);
    if (userIssues == null) {
      userIssues = new ArrayList<IssueTree>();
      userDetails.put(assignee, userIssues);
    }
    userIssues.add(issue);
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
