package com.trentlarson.forecast.core.scheduling.external;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import com.icentris.sql.SimpleSQL;
import com.trentlarson.forecast.core.dao.TeamHours;
import com.trentlarson.forecast.core.scheduling.IssueDigraph;
import com.trentlarson.forecast.core.scheduling.IssueTree;
import com.trentlarson.forecast.core.scheduling.Teams;
import com.trentlarson.forecast.core.scheduling.Teams.UserTimeKey;
import com.trentlarson.forecast.core.scheduling.TimeSchedule;
import com.trentlarson.forecast.core.scheduling.TimeScheduleCreatePreferences;
import com.trentlarson.forecast.core.scheduling.TimeSchedule.HoursForTimeSpan;
import com.trentlarson.forecast.core.scheduling.TimeSchedule.WeeklyWorkHours;

/**
Assumptions:
- There is no assignee with a 3-letter name.  We make 3-letter assignee names out of project names.
- The subtask link ID is hard-coded.
 */
public class TimeScheduleLoader {

  public static final String CYCLE_MARKER = "cyclical reference";

  // If I use the class, then other loggers below don't show in the "Logging and Profiling" screen.  Why?
  //private static final Category log4jLog = Category.getInstance(TimeScheduleLoader.class);
  //private static final Category log4jLog = Category.getInstance("com.trentlarson.forecast.core.scheduling.TimeScheduleLoader");
  public static final Logger log4jLog = Logger.getLogger("com.trentlarson.forecast.core.scheduling.external.TimeScheduleLoader");







  /**
     This method reloads data from the DB.
     @return IssueTree for key, or null if no issue matches that key
  */
  public static IssueDigraph getGraph(String project, String[] issueKeys, String[] users, TimeScheduleCreatePreferences sPrefs, Connection conn) {
    try {
      return getGraphThrows(project, issueKeys, users, sPrefs, conn);
    } catch (SQLException e) {
      throw new java.lang.reflect.UndeclaredThrowableException(e);
    }
  }

  /**
     This method does not reload data from the DB.
  */
  public static IssueDigraph reschedule(IssueDigraph graph, TimeScheduleCreatePreferences sPrefs) {
    return IssueDigraph.schedulesForUserIssues
      (graph.getAssignedUserDetails(), graph.getUserWeeklyHoursAvailable(), sPrefs);
  }

  /**
     This method reloads data from the DB.
  */
  public static IssueDigraph getEntireGraph(TimeScheduleCreatePreferences sPrefs, Connection conn) {
    try {
      return getEntireGraphThrows(sPrefs, conn);
    } catch (SQLException e) {
      throw new java.lang.reflect.UndeclaredThrowableException(e);
    }
  }

  /**
     This method loads open issues for everyone from the DB.
  */
  public static IssueDigraph getEntireGraphThrows(TimeScheduleCreatePreferences sPrefs, Connection conn) throws SQLException {

    // load all users
    String[] userArray = null;
    ResultSet rset = null;
    try {
      String sql = "select username from userbase";
      rset = SimpleSQL.executeQuery(sql, new String[0], conn);
      ArrayList<String> userList = new ArrayList<String>();
      while (rset.next()) {
        userList.add(rset.getString(1));
      }
      rset.close();
      userArray = userList.toArray(new String[0]);
    } finally {
      try { rset.close(); } catch (Exception e) {}
    }

    // now load all issues
    return getGraphThrows(null, new String[0], userArray, sPrefs, conn);
  }

  private static long TIMER_START = 0L;

  private static IssueDigraph getGraphThrows
    (String project, String[] issueKeys, String[] users,
     TimeScheduleCreatePreferences sPrefs, Connection conn) throws SQLException {

    // load time data
    Map<Teams.UserTimeKey,SortedSet<TimeSchedule.HoursForTimeSpan>> userWeeklyHours =
      loadUserWeeklyHours(sPrefs.getStartTime(), conn);

    TIMER_START = System.currentTimeMillis();

    // load user data
    Map<Teams.AssigneeKey,List<IssueTree>> allUserDetails =
      //IssueLoader.loadDetails(project, issueKeys, users, conn); // Problem: load a DB with jiradb_412.dynamic-load-assignee-problem.sql and run TimeScheduleTests.testIntegrationDynamicLoadAssigneeProblem
      IssueLoader.loadAllDetails(conn); // looks like it doesn't find all dependent tasks(?)

    log4jLog.info("Loaded issues in " + ((System.currentTimeMillis() - TIMER_START) / 1000.0) + " seconds.");

    TIMER_START = System.currentTimeMillis();

    // now schedule
    IssueDigraph schedules =
      IssueDigraph.schedulesForUserIssues2(allUserDetails, userWeeklyHours, sPrefs);

    log4jLog.info("Scheduled " + schedules.getIssueSchedules().size()  + " issues in " + ((System.currentTimeMillis() - TIMER_START) / 1000.0) + " seconds.");

    return schedules;
  }


  /**
     Load from the TEAM_HOURS table (for all users with records) the
     time spans which affect this scheduling, meaning all the ones
     that are after startDate as well as the one equal to or
     immediately preceding it.

     Note that some functionality (ie. team-hours.jsp display, and I
     think the scheduling as well) implicitly depends on there being a
     record on or before startDate.
   */
  public static SortedMap<Teams.UserTimeKey,SortedSet<TimeSchedule.HoursForTimeSpan>> loadUserWeeklyHours
  (Date startDate, Connection conn) throws SQLException {
    
    SortedMap<Teams.UserTimeKey,SortedSet<TimeSchedule.HoursForTimeSpan>> userWeeklyHours =
      new TreeMap<UserTimeKey, SortedSet<HoursForTimeSpan>>();
    ResultSet rset = null;
    try {
      String timeSql = 
        "select team_id, username, start_of_week, hours_available"
        + " from team_hours"
        + " order by team_id, username, start_of_week desc";
      rset = SimpleSQL.executeQuery(timeSql, new Object[0], conn);

      Teams.UserTimeKey currentAssignee = null;
      // set when we don't need to go back any further in time for this assignee
      boolean doneWithCurrent = false;
      SortedSet<TimeSchedule.HoursForTimeSpan> weeklyHours = new TreeSet<HoursForTimeSpan>();
      while (rset.next()) {
        Long teamId = rset.getLong("team_id");
        if (rset.wasNull()) {
          teamId = null;
        }
        Teams.UserTimeKey nextAssignee =
          new Teams.UserTimeKey(teamId, rset.getString("username"));
        if (currentAssignee == null
            || currentAssignee.compareTo(nextAssignee) != 0) {
          currentAssignee = nextAssignee;
          doneWithCurrent = false;
          weeklyHours = new TreeSet<HoursForTimeSpan>();
          userWeeklyHours.put(currentAssignee, weeklyHours);
        }

        if (nextAssignee.compareTo(currentAssignee) == 0) {
          if (!doneWithCurrent) {
            // if the first has the same number of hours, replace it
            if (weeklyHours.size() > 0
                && (weeklyHours.first().getHoursAvailable()
                    == rset.getDouble("hours_available"))) {
              weeklyHours.remove(weeklyHours.first());
            }
            // add this time slice
            weeklyHours
              .add(new TimeSchedule.HoursForTimeSpanOriginal
                   (rset.getDate("start_of_week"),
                    rset.getDouble("hours_available")));
            // if this was before or equal to scheduleStartDate, we're finished
            if (!rset.getDate("start_of_week").after(startDate)) {
              doneWithCurrent = true;
            }
          }
        }
      }

    } finally {
      try { rset.close(); } catch (Exception e2) {}
    }

    log4jLog.debug("Result of user hours SQL: \n " + userWeeklyHours);

    return userWeeklyHours;
  }











}
