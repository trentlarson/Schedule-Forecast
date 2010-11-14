package com.trentlarson.forecast.core.scheduling;

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
import com.trentlarson.forecast.core.scheduling.Teams.AssigneeKey;
import com.trentlarson.forecast.core.scheduling.Teams.UserTimeKey;
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
  private static final Logger log4jLog = Logger.getLogger("com.trentlarson.forecast.core.scheduling.TimeScheduleLoader");







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
    return schedulesForUserIssues
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
      IssueLoader.loadDetails(project, issueKeys, users, conn);
      //IssueLoader.loadAllDetails(conn); // looks like it doesn't find all dependent tasks

    log4jLog.info("Loaded issues in " + ((System.currentTimeMillis() - TIMER_START) / 1000.0) + " seconds.");

    TIMER_START = System.currentTimeMillis();

    // now schedule
    IssueDigraph schedules =
      schedulesForUserIssues2(allUserDetails, userWeeklyHours, sPrefs);

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










  public static IssueDigraph schedulesForUserIssues3
  (Map<Teams.AssigneeKey,List<IssueTree>> userDetails,
   Map<Teams.UserTimeKey,List<TeamHours>> userWeeklyHours,
   TimeScheduleCreatePreferences sPrefs) {

    Map<Teams.UserTimeKey,SortedSet<TimeSchedule.HoursForTimeSpan>> userSpanHours =
      teamToUserHours(userWeeklyHours);
    return schedulesForUserIssues2(userDetails, userSpanHours, sPrefs);
  }

  public static IssueDigraph schedulesForUserIssues2
  (Map<Teams.AssigneeKey,List<IssueTree>> userDetails,
   Map<Teams.UserTimeKey,SortedSet<TimeSchedule.HoursForTimeSpan>> userSpanHourSet,
   TimeScheduleCreatePreferences sPrefs) {

    Map<Teams.UserTimeKey,TimeSchedule.WeeklyWorkHours> weeklyHoursFromKey =
      weeklyHoursToRange(userSpanHourSet);
    return schedulesForUserIssues(userDetails, weeklyHoursFromKey, sPrefs);

  }

  public static IssueDigraph schedulesForUserIssues
  (Map<Teams.AssigneeKey,List<IssueTree>> userDetails,
   Map<Teams.UserTimeKey,TimeSchedule.WeeklyWorkHours> weeklyHoursFromKey,
   TimeScheduleCreatePreferences sPrefs) {

    // (REFACTOR with IssueDigraph.reschedule (lots of overlap))

    UserDetailsAndHours newTimeDetails =
      adjustDetailsForAssignedHours(userDetails, weeklyHoursFromKey,
                                    sPrefs.getStartTime());

    Map<String,List<IssueTree>> assigneeStringToIssues =
      createMapFromAssigneeKeyStringToUserIssues(newTimeDetails.timeDetails);

    // clone the hourly data (since it's modified later)
    Map<Teams.UserTimeKey,TimeSchedule.WeeklyWorkHours> weeklyHoursFromKey2 =
      new HashMap<UserTimeKey, WeeklyWorkHours>();
    for (Teams.UserTimeKey user : newTimeDetails.hours.keySet()) {
      weeklyHoursFromKey2
        .put(user,
             (TimeSchedule.WeeklyWorkHours) newTimeDetails.hours.get(user).clone());
    }

    Map<String,TimeSchedule.WeeklyWorkHours> weeklyHoursFromString =
      createMapFromUserTimeKeyStringToWeeklyHours(weeklyHoursFromKey2);

    Map<String,TimeSchedule.IssueSchedule> schedules =
      TimeSchedule.schedulesForUserIssues
      (assigneeStringToIssues, weeklyHoursFromString,
       sPrefs.getStartTime(), sPrefs.getTimeMultiplier());

    // for displaying things in that priority order
    TimeSchedule.setInitialOrdering(userDetails);
    TimeSchedule.setInitialOrdering(newTimeDetails.timeDetails);

    return new IssueDigraph(schedules,
                            userDetails, newTimeDetails.timeDetails,
                            newTimeDetails.assigneeToAllocatedUsers,
                            newTimeDetails.hours, weeklyHoursFromKey2, sPrefs);
  }

  protected static class UserDetailsAndHours {
    public final Map<Teams.UserTimeKey,List<IssueTree>> timeDetails;
    public final Map<Teams.AssigneeKey,Teams.UserTimeKey> assigneeToAllocatedUsers;
    public final Map<Teams.UserTimeKey,TimeSchedule.WeeklyWorkHours> hours;
    public UserDetailsAndHours(Map<Teams.AssigneeKey,Teams.UserTimeKey> assigneeToAllocatedUsers_,
                               Map<Teams.UserTimeKey,List<IssueTree>> timeDetails_,
                               Map<Teams.UserTimeKey,TimeSchedule.WeeklyWorkHours> hours_) {
      this.assigneeToAllocatedUsers = assigneeToAllocatedUsers_;
      this.timeDetails = timeDetails_;
      this.hours = hours_;
    }
  }

  /**
     Translate details info to input good for scheduling.

     @param hoursRange is used to decide exactly where to assign an
     issue.  If the hours don't exist for the user, then we assume
     that there is the default amount of work time available and we
     insert that record; the one exception is: if both the team and
     the user are set but no UserTimeKey exists, then a UserTimeKey is
     created for the user on any team (ie. where team is null).

   */
  public static UserDetailsAndHours adjustDetailsForAssignedHours
     (Map<Teams.AssigneeKey,List<IssueTree>> detailsFromAssignee,
      Map<Teams.UserTimeKey,TimeSchedule.WeeklyWorkHours> hoursRange,
      Date startDate) {

    Map<Teams.AssigneeKey,Teams.UserTimeKey> assigneeToAllocatedUsers = new HashMap<Teams.AssigneeKey, UserTimeKey>();
    Map<Teams.UserTimeKey,List<IssueTree>> newDetailsByTime = new HashMap<Teams.UserTimeKey,List<IssueTree>>();
    Map<Teams.UserTimeKey,TimeSchedule.WeeklyWorkHours> newHoursRanges = new HashMap<Teams.UserTimeKey, WeeklyWorkHours>();

    newHoursRanges.putAll(hoursRange);

    for (Teams.AssigneeKey assigneeKey : detailsFromAssignee.keySet()) {
      for (IssueTree detail : detailsFromAssignee.get(assigneeKey)) {

        // create appropriate time-allocation user key for this time assignee
        Teams.UserTimeKey userKeyForTime = detail.getTimeAssigneeKey();
        if (!newHoursRanges.containsKey(userKeyForTime)) {
          userKeyForTime = new Teams.UserTimeKey(null, userKeyForTime.getUsername());
          if (log4jLog.isDebugEnabled()) {
            log4jLog.debug("Changing time assignee of issue " + detail.getKey()
                + " to " + userKeyForTime
                + " (from " + detail.getTimeAssigneeKey() + ").");
          }
          detail.setTimeAssigneeKey(userKeyForTime);
        }

        updateDetailAndLists(userKeyForTime, detail, newDetailsByTime, assigneeToAllocatedUsers, newHoursRanges, startDate);
      }
    }
    return new UserDetailsAndHours(assigneeToAllocatedUsers, newDetailsByTime, newHoursRanges);
  }

/** remove
  protected static <T extends Object> Teams.AssigneeKey userKeyIfNotAlreadyExisting
  (Teams.AssigneeKey userKey,
   Map<Teams.UserTimeKey,T> userWeeklyHours) {
    
    Teams.UserTimeKey timeKey = 
      new Teams.UserTimeKey(userKey.getTeamId(), userKey.getUsername());

    if (userKey.getUsername() != null
        && !userWeeklyHours.containsKey(timeKey)) {
      log4jLog.debug("Hours not allocated for " + userKey.getUsername() + ", so we're creating an assignee key for them.");
      return new Teams.AssigneeKey(null, userKey.getUsername());
    } else {
      return userKey;
    }
  }
**/

  /**
     Modify the 'detail' and the maps so that detail has time assignee 'assignee'.
   */
  private static void updateDetailAndLists
    (Teams.UserTimeKey assignee,
     IssueTree detail,
     Map<Teams.UserTimeKey,List<IssueTree>> detailMap,
     Map<Teams.AssigneeKey,Teams.UserTimeKey> assigneeToAllocatedUsers,
     Map<Teams.UserTimeKey,TimeSchedule.WeeklyWorkHours> hoursRanges,
     Date startDate) {

    // add key if it doesn't already exist
    if (!hoursRanges.containsKey(assignee)) {
      log4jLog.debug("Adding default hours for " + assignee);
      TimeSchedule.WeeklyWorkHours defaultHours = new TimeSchedule.WeeklyWorkHours();
      defaultHours.inject(startDate, TimeSchedule.TYPICAL_INDIVIDUAL_WORKHOURS_PER_WORKWEEK);
      hoursRanges.put(assignee, defaultHours);
    }
    
    List<IssueTree> detailMapEntry;
    if (detailMap.containsKey(assignee)) {
      detailMapEntry = detailMap.get(assignee);
    } else {
      detailMapEntry = new ArrayList<IssueTree>();
      detailMap.put(assignee, detailMapEntry);
    }
    detailMapEntry.add(detail);

    if (assigneeToAllocatedUsers.containsKey(detail.getRawAssigneeKey())) {
      if (!assigneeToAllocatedUsers
          .get(detail.getRawAssigneeKey())
          .equals(assignee)) {
        throw new IllegalStateException
          ("Contact a Jira developer to fix this issue."
           + "  Each user & team combo should only have one bucket for hours, but "
           + detail.getKey()
           + " with user " + detail.getRawAssignedPerson()
           + " and team " + detail.getRawAssignedTeamId()
           + " results in 2: "
           + assigneeToAllocatedUsers.get(detail.getRawAssigneeKey())
           + " and " + assignee);
      }
    } else {
      assigneeToAllocatedUsers.put(detail.getRawAssigneeKey(), assignee);
    }
    
  }

  /**
     Translate team info to input good for scheduling.
     Won't insert for same hours in an immediately succeeding time slice.
   */
  public static Map<Teams.UserTimeKey,SortedSet<TimeSchedule.HoursForTimeSpan>> teamToUserHours
     (Map<Teams.UserTimeKey,List<TeamHours>> allUserTeamHours) {

    Map<Teams.UserTimeKey,SortedSet<TimeSchedule.HoursForTimeSpan>> allUserSpanHours =
      new HashMap<UserTimeKey, SortedSet<HoursForTimeSpan>>();
    for (Iterator<Teams.UserTimeKey> users = allUserTeamHours.keySet().iterator();
         users.hasNext(); ) {
      Teams.UserTimeKey teamUser = users.next();

      SortedSet<TimeSchedule.HoursForTimeSpan> sortedHours =
        new TreeSet<TimeSchedule.HoursForTimeSpan>();
      List<TeamHours> teamHours = allUserTeamHours.get(teamUser);
      int prevHours = -1;
      for (int i = 0; i < teamHours.size(); i++) {
        TeamHours hours = teamHours.get(i);
        // only add this if it's a different weekly-hour rate
        if (hours.getHoursAvailable() != prevHours) {
          sortedHours
            .add
            (new TimeSchedule.HoursForTimeSpanOriginal
             (hours.getStartOfWeek(),
              hours.getHoursAvailable()));
        }
        prevHours = hours.getHoursAvailable().intValue();
      }
      allUserSpanHours.put(teamUser, sortedHours);
    }
    return allUserSpanHours;
  }

  /**
     Translate team info to working data for scheduling.
   */
  public static Map<Teams.UserTimeKey,TimeSchedule.WeeklyWorkHours> weeklyHoursToRange
     (Map<Teams.UserTimeKey,SortedSet<TimeSchedule.HoursForTimeSpan>> usersWeeklyHours) {

    Map<Teams.UserTimeKey,TimeSchedule.WeeklyWorkHours> rangesForUsers =
      new TreeMap<Teams.UserTimeKey,TimeSchedule.WeeklyWorkHours>();

    for (Iterator<Teams.UserTimeKey> users = usersWeeklyHours.keySet().iterator();
         users.hasNext(); ) {
      Teams.UserTimeKey user = users.next();
      TimeSchedule.WeeklyWorkHours range = new TimeSchedule.WeeklyWorkHours();
      rangesForUsers.put(user, range);
      for (Iterator<TimeSchedule.HoursForTimeSpan> hoursIter =
             usersWeeklyHours.get(user).iterator();
           hoursIter.hasNext(); ) {
        TimeSchedule.HoursForTimeSpan hours = hoursIter.next();
        if (range.isEmpty()
            || range.endingHours() != hours.getHoursAvailable()) {
          range.inject(hours.getStartOfTimeSpan(),
                       new Double(hours.getHoursAvailable()));
        }
      }
    }
    return rangesForUsers;
    
  }

  protected static Map<String,TimeSchedule.WeeklyWorkHours> createMapFromUserTimeKeyStringToWeeklyHours
    (Map<Teams.UserTimeKey,TimeSchedule.WeeklyWorkHours> origHours) {

    Map<String,TimeSchedule.WeeklyWorkHours> newHours = new HashMap<String, WeeklyWorkHours>();
    for (Teams.UserTimeKey userTimeKey : origHours.keySet()) {
      newHours.put(userTimeKey.toString(), origHours.get(userTimeKey));
    }
    return newHours;
  }

  protected static Map<String,List<IssueTree>> createMapFromAssigneeKeyStringToUserIssues
    (Map<Teams.UserTimeKey,List<IssueTree>> origIssues) {

    Map<String,List<IssueTree>> newIssues = new HashMap<String,List<IssueTree>>();
    for (Teams.UserTimeKey assigneeKey : origIssues.keySet()) {
      newIssues.put(assigneeKey.toString(), origIssues.get(assigneeKey));
    }
    return newIssues;
  }

}
