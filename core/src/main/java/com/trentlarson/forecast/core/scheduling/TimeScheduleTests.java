package com.trentlarson.forecast.core.scheduling;

import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.trentlarson.forecast.core.dao.TeamHours;

public class TimeScheduleTests {

  public static void main(String[] args) throws Exception {
    
    // This enables basic Log4J logging to standard out.
    // ... but note that you'll have to turn off a bunch of logs.
    org.apache.log4j.BasicConfigurator.configure();

    IssueDigraph.log4jLog.setLevel(org.apache.log4j.Level.ERROR);
    //IssueLoader.log4jLog.setLevel(org.apache.log4j.Level.DEBUG);
    TimeSchedule.log4jLog.setLevel(org.apache.log4j.Level.ERROR);
    TimeScheduleDisplayPreferences.log4jLog.setLevel(org.apache.log4j.Level.ERROR);
    TimeScheduleWriter.log4jLog.setLevel(org.apache.log4j.Level.ERROR);
    //TimeSchedule.fnebLog.setLevel(org.apache.log4j.Level.DEBUG);
    //TimeSchedule.wsbLog.setLevel(org.apache.log4j.Level.ERROR);
    
    PrintWriter out = null;
    try {
      out = new PrintWriter(System.out);
      unitMain(out);
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

    outputTimePerWeekTestResults(out);

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

    TimeScheduleCreatePreferences sPrefs =
      new TimeScheduleCreatePreferences(0, SLASH_DATE.parse("2006/12/03"), 1);
    IssueDigraph graph = IssueDigraph.schedulesForIssues(manyIssues, userWeeklyHours, sPrefs);

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

    TimeScheduleCreatePreferences sPrefs = new TimeScheduleCreatePreferences(0, SLASH_DATE.parse("2006/12/03"), 1);
    IssueDigraph graph = IssueDigraph.schedulesForIssues(manyIssues, userWeeklyHours, sPrefs);

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

    sPrefs = new TimeScheduleCreatePreferences(0, SLASH_DATE.parse("2006/12/03"), 1);
    graph = IssueDigraph.schedulesForIssues(manyIssues, userWeeklyHours, sPrefs);

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

    TimeScheduleCreatePreferences sPrefs =
      new TimeScheduleCreatePreferences(0, SLASH_DATE.parse("2005/04/05"), 1);
    IssueDigraph graph = IssueDigraph.schedulesForIssues(manyIssues, userWeeklyHours, sPrefs);



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

    sPrefs =
      new TimeScheduleCreatePreferences(0, SLASH_DATE.parse("2005/04/05"), 1);
    graph = IssueDigraph.schedulesForIssues(manyIssues, userWeeklyHours, sPrefs);

    user = new Teams.AssigneeKey(1L, null);
    out.println("<br><br>");
    out.println("Tree for " + user + ", which should now be shorter since 'trent' handles one task (TEST-200.1).<br>");
    TimeScheduleWriter.writeIssueTable
      (graph, out, sPrefs,
       TimeScheduleDisplayPreferences
       .createForUser(1, 0, true, false, false, user, false, graph));
  
  }

  public static void outputTimePerWeekTestResults(PrintWriter out) throws Exception {
    
    out.println("<P>");
    out.println("<H2>... for issues with hours-per-week settings.</H2>");

    int jira_day = 8 * 60 * 60;

    IssueTree[] manyIssues = {
        // let's make this one a few hours per week
      new IssueTree
      ("TEST-220-split", "3-day issue, 4 hr/wk", null, 1L,
       3 * jira_day, 0 * jira_day, 4.0,
       SLASH_DATE.parse("2005/05/20"), null, 4, false)
      ,
      new IssueTree
      ("TEST-221", "4.5-day issue", null, 1L,
       (int) (4.5 * jira_day), 0 * jira_day, 0.0,
       SLASH_DATE.parse("2005/04/15"), null, 5, false)
      ,
      new IssueTree
      ("TEST-222-split", "3 days, faster", null, 1L,
       3 * jira_day, 0 * jira_day, 8.0,
       SLASH_DATE.parse("2005/05/06"), null, 6, false)
      ,
      new IssueTree
      ("TEST-223", "4-day issue", null, 1L,
       4 * jira_day, 0 * jira_day, 0.0,
       null, null, 3, false)
      ,
      new IssueTree
      ("TEST-224-split", "9-day issue", null, 1L,
       9 * jira_day, 0 * jira_day, 32.0,
       null, null, 6, false)
      ,
      new IssueTree
      ("TEST-224.1", "8-day issue", null, 1L,
       8 * jira_day, 0 * jira_day, 0.0,
       null, null, 7, false)
    };
    
    TimeScheduleCreatePreferences sPrefs =
      new TimeScheduleCreatePreferences(0, SLASH_DATE.parse("2005/04/05"), 1);
    IssueDigraph graph = IssueDigraph.schedulesForIssues(manyIssues, sPrefs);


    // print out team Gantt chart
    Teams.AssigneeKey user = new Teams.AssigneeKey(1L, null);
    out.println("<br><br>");
    out.println("Tree for " + user + ".<br>");
    TimeScheduleWriter.writeIssueTable
      (graph, out, sPrefs,
       TimeScheduleDisplayPreferences
       .createForUser(1, 0, true, false, false, user, false, graph));
    
    
    // print out team table schedule
    List<TimeSchedule.IssueSchedule<IssueTree>> schedule = new ArrayList<TimeSchedule.IssueSchedule<IssueTree>>();
    List<IssueTree> userIssueList = graph.getAssignedUserDetails().get(user);
    for (int i = 0; i < userIssueList.size(); i++) {
      schedule.add
        (graph.getIssueSchedules().get
         (userIssueList.get(i).getKey()));
    }
    TimeSchedule.writeIssueSchedule
      (schedule, sPrefs.getTimeMultiplier(), true, out);

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

    TimeScheduleCreatePreferences sPrefs =
      new TimeScheduleCreatePreferences(0, SLASH_DATE.parse("2005/04/11"), 1);
    IssueDigraph graph = IssueDigraph.schedulesForIssues(manyIssues, userWeeklyHours, sPrefs);

    out.println("<br><br>");
    out.println("Trees with hours on teams with hours: " + graph.getUserWeeklyHoursAvailable());

    out.println("<br><br>");
    out.println("Graph for trent");
    Teams.AssigneeKey userKey = new Teams.AssigneeKey(1L, "trent");
    TimeScheduleWriter.writeIssueTable
      (graph, out, sPrefs,
       TimeScheduleDisplayPreferences
       .createForUser(1, 0, true, false, true, userKey.getUsername(), false, graph));




    Map<Teams.AssigneeKey,List<IssueTree>> userDetails = IssueDigraph.createUserDetails(manyIssues);

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


    List<TeamHours> hourList = new ArrayList();
    TimeScheduleCreatePreferences sPrefs =
      new TimeScheduleCreatePreferences(0, startDate, 2);
    IssueDigraph graph = IssueDigraph.schedulesForIssues(manyIssues, sPrefs);



    {
      String userPerson = "trent";
      Long userTeam = 1L;
      Teams.AssigneeKey userKey = new Teams.AssigneeKey(userTeam, userPerson);

      Map<Teams.AssigneeKey,List<IssueTree>> userDetails = IssueDigraph.createUserDetails(manyIssues);

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

    TimeScheduleCreatePreferences sPrefs =
      new TimeScheduleCreatePreferences(0, startDate, 2);
    IssueDigraph graph = IssueDigraph.schedulesForIssues(manyIssues, sPrefs);

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

    TestIssues testIssues = createTestIssues();

    Map<Teams.UserTimeKey,List<TeamHours>> userWeeklyHours = new TreeMap<Teams.UserTimeKey,List<TeamHours>>();

    {
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
    }

    Map<Teams.AssigneeKey,List<IssueTree>> userDetails = IssueDigraph.createUserDetails(testIssues.manyIssues());
    Date startDate = SLASH_DATE.parse("2005/04/05");
    TimeScheduleCreatePreferences sPrefs = new TimeScheduleCreatePreferences(0, startDate, 2);
    IssueDigraph graph = IssueDigraph.schedulesForIssues(testIssues.manyIssues(), userWeeklyHours, sPrefs);

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
      Collections.sort(schedule);
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
        (graph, out,sPrefs,
         TimeScheduleDisplayPreferences.createForUser(1, 0, true, false, false, userKey, true, graph));

      out.println("<br><br>");
      out.println("Tree for TEST_12 allowing modifications.<br>");
      TimeScheduleWriter.writeIssueTable
        (graph, out, sPrefs,
         TimeScheduleDisplayPreferences.createForIssues(1, 0, false, false, false, new String[]{"TEST_12"}, true, graph));

    }





    out.println("<p>Adding dependency relationships...<p>");

    testIssues.issueTest_0.addDependent(testIssues.issueTest_2);
    testIssues.issueTest_1.addDependent(testIssues.issueTest_3);
    testIssues.issueTest_2.addDependent(testIssues.issueTest_3);
    testIssues.issueTest_2.addDependent(testIssues.issueTest_5);
    testIssues.issueTest_3.addDependent(testIssues.issueTest_5);
    testIssues.issueTest_4.addDependent(testIssues.issueTest_5);
    testIssues.issueTest_5.addDependent(testIssues.issueTest_6);
    testIssues.issueTest_6.addDependent(testIssues.issueTest_7);
    testIssues.issueTest_6.addDependent(testIssues.issueTest_12);
    testIssues.issueTest_6.addDependent(testIssues.issueTest_15);
    testIssues.issueTest_7.addSubtask(testIssues.issueTest_11);
    testIssues.issueTest_7.addSubtask(testIssues.issueTest_12);
    testIssues.issueTest_7.addDependent(testIssues.issueTest_10);
    testIssues.issueTest_7.addDependent(testIssues.issueTest_13);
    testIssues.issueTest_8.addDependent(testIssues.issueTest_13);
    testIssues.issueTest_8.addSubtask(testIssues.issueTest_12);
    testIssues.issueTest_8.addSubtask(testIssues.issueTest_15);
    testIssues.issueTest_8.addSubtask(testIssues.issueTest_16);
    testIssues.issueTest_13.addSubtask(testIssues.issueTest_14);


    /**
       GRAPH OF SUBTASKS (+) AND BLOCKED TASKS (|)

. _0
.     _1
.  |----- _2
.      |----- _3
.          |- _3
.                 _4
.          |--------- _5
.              |----- _5
.                  |- _5
.                      |- _6
.                          |- _7
.                                 _8
.                              |--------- _10
.                              +---------------------------- _11
.                              +-------------------------------- _12
.                          |------------------------------------ _12
.                                  +---------------------------- _12
.                              |------------------------------------ _13
.                                  |-------------------------------- _13
.                                                                     +- _14
.                          |-------------------------------------------- _15
.                                  +------------------------------------ _15
.                                  +---------------------------------------- _16
. _17
    **/

    // now must recalculate
    graph = IssueDigraph.schedulesForUserIssues3(userDetails, userWeeklyHours, sPrefs);

    List branches1 = TimeScheduleSearch.findPredecessorBranches(testIssues.issueTest_7);
    out.println("<br><br>");
    out.println("All branches of TEST_7: ");
    out.println(branches1);

    out.println("<br><br>");
    out.println("Tree for issues 1 and 2.<br>");
    TimeScheduleWriter.writeIssueTable
      (graph, out, sPrefs,
       TimeScheduleDisplayPreferences.createForIssues
       (1, 0, true, false, true,
        new String[]{ testIssues.issueTest_7.getKey(), testIssues.issueTest_8.getKey() },
        false, graph));

    out.println("<p>");
    out.println("Squished schedule for issues 1 and 2.<br>");
    TimeScheduleWriter.writeIssueTable
      (graph, out, sPrefs,
       TimeScheduleDisplayPreferences.createForIssues
       (4, 0, true, false, true,
        new String[]{ testIssues.issueTest_7.getKey(), testIssues.issueTest_8.getKey() },
        false, graph));

    out.println("<p>");
    out.println("Tree for issue 1, w/o resolved.<br>");
    TimeScheduleWriter.writeIssueTable
      (graph, out, sPrefs,
       TimeScheduleDisplayPreferences.createForIssues
       (1, 0, true, false, false,
        new String[]{ testIssues.issueTest_7.getKey() },
        false, graph));

    out.println("<p>");
    out.println("Schedule for trent.<br>");
    TimeScheduleWriter.writeIssueTable
    (graph, out, sPrefs,
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
        (graph, out, sPrefs,
         TimeScheduleDisplayPreferences.createForUsers
         (1, 0, true, false, true, users, graph));

      out.println("<p>");
      out.println("Schedule for ken.<br>");
      Teams.AssigneeKey user = new Teams.AssigneeKey(1L, "ken");
      TimeScheduleWriter.writeIssueTable
        (graph, out, sPrefs,
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
      out.println("Critical Path for " + Arrays.asList(testIssues.issueTest_14.getKey()) + ".<br>");
      TimeScheduleWriter.writeIssueTable
      (graph, out, sPrefs,
       TimeScheduleDisplayPreferences.createForCriticalPaths(1, 0, false, false, new String[]{testIssues.issueTest_14.getKey()}, graph));
    }


    out.println("<xmp>");

    int expected;

    expected = 2 * 3600;
    out.println
      ((testIssues.issueTest_7.totalTimeSpent() == expected ? "pass" : "fail")
       + " (totalColumns: expected " + expected + "; got " + testIssues.issueTest_7.totalTimeSpent() + ")");
    expected = 10 * 3600;
    out.println
      ((testIssues.issueTest_7.totalEstimate() == expected ? "pass" : "fail")
       + " (totalColumns: expected " + expected + "; got " + testIssues.issueTest_7.totalEstimate() + ")");

    expected = 2 * 3600;
    out.println
      ((testIssues.issueTest_7.totalTimeSpent() == expected ? "pass" : "fail")
       + " (totalColumns: expected " + expected + "; got " + testIssues.issueTest_7.totalTimeSpent() + ")");
    expected = 10 * 3600;
    out.println
      ((testIssues.issueTest_7.totalEstimate() == expected ? "pass" : "fail")
       + " (totalColumns: expected " + expected + "; got " + testIssues.issueTest_7.totalEstimate() + ")");


    TimeSchedule.IssueSchedule sched13 =
        graph.getIssueSchedules().get(testIssues.issueTest_13.getKey());
    Calendar acal = Calendar.getInstance();
    acal = sched13.getAdjustedBeginCal();
    expected = 28;
    out.println
      ((acal.get(Calendar.DATE) == expected ? "pass" : "fail")
       + " (" + testIssues.issueTest_13.getKey() + " should start on Apr " + expected + "; got "
       + acal.get(Calendar.DATE) + ")");
    expected = 8;
    out.println
      ((acal.get(Calendar.HOUR_OF_DAY) == expected ? "pass" : "fail")
       + " (" + testIssues.issueTest_13.getKey() + " should start at " + expected + "; got " +
       + acal.get(Calendar.HOUR_OF_DAY) + ")");

    TimeSchedule.IssueSchedule sched4 =
        graph.getIssueSchedules().get(testIssues.issueTest_4.getKey());
    boolean notBefore = !sched13.getBeginDate().before(sched4.getEndDate());
    out.println((notBefore ? "pass" : "fail")
                + " (" + testIssues.issueTest_13.getKey() + " should start after " + testIssues.issueTest_4.getKey() + " ends)");

    TimeSchedule.IssueSchedule sched14 =
        graph.getIssueSchedules().get(testIssues.issueTest_15.getKey());
    acal = sched14.getAdjustedBeginCal();
    expected = 12;
    out.println
      ((acal.get(Calendar.DATE) == expected ? "pass" : "fail")
       + " (" + testIssues.issueTest_15.getKey() + " should start on day " + expected + "; got " +
       + acal.get(Calendar.DATE) + ")");

    TimeSchedule.IssueSchedule sched10 =
        graph.getIssueSchedules().get(testIssues.issueTest_10.getKey());
    acal = sched10.getAdjustedBeginCal();
    expected = 15;
    out.println
      ((acal.get(Calendar.DATE) == expected ? "pass" : "fail")
       + " (" + testIssues.issueTest_10.getKey() + " should start on day " + expected + "; got " +
       + acal.get(Calendar.DATE) + ")");

    List branches0 = TimeScheduleSearch.findPredecessorBranches(testIssues.issueTest_6);
    out.println
      ((branches0.size() == 6 ? "pass" : "fail")
       + " (should be 6 branches preceding 0; got " + branches0.size() + ")");

    out.println("</xmp>");

    out.println();
    out.println("All branches of TEST_6: ");
    out.println(branches0);

  }

  public static TestIssues createTestIssues() throws Exception {
    TestIssues result = new TestIssues();

    result.issueTest_0 =
        new IssueTree
            ("TEST_0", "Ancestor test issue", "trent", 1L,
                0 * 3600, 0 * 3600, 0.0, null, null, 1, false);
    result.issueTest_1 =
        new IssueTree
            ("TEST_1", "Ancestor test issue", "trent", 1L,
                4 * 3600, 0 * 3600, 0.0, null, null, 1, false);
    result.issueTest_2 =
        new IssueTree
            ("TEST_2", "Ancestor test issue", "ken", 1L,
                4 * 3600, 0 * 3600, 0.0, null, null, 1, false);
    result.issueTest_3 =
        new IssueTree
            ("TEST_3", "Ancestor test issue", "brent", 1L,
                4 * 3600, 0 * 3600, 0.0, null, null, 1, false);
    result.issueTest_4 =
        new IssueTree
            ("TEST_4", "Ancestor test issue", "trent", 1L,
                4 * 3600, 0 * 3600, 0.0, null, null, 1, false);
    result.issueTest_5 =
        new IssueTree
            ("TEST_5", "Ancestor test issue", "ken", 1L,
                4 * 3600, 0 * 3600, 0.0, null, null, 1, false);
    result.issueTest_6 =
        new IssueTree
            ("TEST_6", "Grandparent test issue", "fred", 1L,
                8 * 3600, 0 * 3600, 0.0,
                SLASH_DATE.parse("2005/01/01"), null, 1, false);
    result.issueTest_7 =
        new IssueTree
            ("TEST_7", "Parent test issue", "brent", 1L,
                10 * 3600, 0 * 3600, 0.0,
                SLASH_DATE.parse("2005/04/01"), null, 1, false);
    result.issueTest_8 =
        new IssueTree
            ("TEST_8", "issue", "ken", 1L, 24 * 3600, 1 * 3600, 0.0,
                SLASH_DATE.parse("2005/04/16"), null, 8, false);
    result.issueTest_10 =
        new IssueTree
            ("TEST_10", "top", "trent", 1L, 8 * 3600, 1 * 3600, 0.0,
                SLASH_DATE.parse("2005/04/13"), null, 4, false);
    result.issueTest_9 =
        new IssueTree
            ("TEST_9", "issue", "ken", 1L, 20 * 3600, 1 * 3600, 0.0,
                SLASH_DATE.parse("2005/04/05"), null, 3, false);
    result.issueTest_11 =
        new IssueTree
            ("TEST_11", "sub issue", "trent", 1L, 4 * 3600, 1 * 3600, 0.0,
                SLASH_DATE.parse("2005/04/15"), null, 3, true);
    result.issueTest_12 =
        new IssueTree
            ("TEST_12", "sub issue", "ken", 1L, 3 * 3600, 1 * 3600, 0.0,
                SLASH_DATE.parse("2005/06/01"), null, 2, false);
    result.issueTest_13 =
        new IssueTree
            ("TEST_13", "after TEST_7,2", "trent", 1L, 10 * 3600, 1 * 3600, 0.0,
                null, null, 2, true);
    result.issueTest_14 =
        new IssueTree
            ("TEST_14", "sub of TEST_13", "trent", 1L, 10 * 3600, 1 * 3600, 0.0,
                null, null, 9, false);
    result.issueTest_15 =
        new IssueTree
            ("TEST_15", "dependant issue", "trent", 1L, 16 * 3600, 1 * 3600, 0.0,
                SLASH_DATE.parse("2005/06/01"), null, 4, false);
    result.issueTest_16 =
        new IssueTree
            ("TEST_16", "dependant issue", "trent", 1L, 4 * 3600, 1 * 3600, 0.0,
                SLASH_DATE.parse("2005/04/07"), null, 5, false);
    result.issueTest_17 =
        new IssueTree
            ("TEST_17", "some issue", "trent", 1L, 12 * 3600, 1 * 3600, 0.0,
                SLASH_DATE.parse("2005/04/15"), null, 6, false);

    return result;
  }

  public static class TestIssues {
    IssueTree issueTest_0, issueTest_1, issueTest_2, issueTest_3, issueTest_4, issueTest_5,
      issueTest_6, issueTest_7, issueTest_8, issueTest_9, issueTest_10, issueTest_11, issueTest_12,
      issueTest_13, issueTest_14, issueTest_15, issueTest_16, issueTest_17;
    private IssueTree[] manyIssues() {
      return new IssueTree[]{
          issueTest_0, issueTest_1, issueTest_2, issueTest_3, issueTest_4, issueTest_5, issueTest_6, issueTest_7,
          issueTest_8, issueTest_9, issueTest_10, issueTest_11, issueTest_12, issueTest_13, issueTest_14,
          issueTest_15, issueTest_16, issueTest_17
      };
    }
  }

}
