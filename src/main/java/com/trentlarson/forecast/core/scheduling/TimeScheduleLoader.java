package com.trentlarson.forecast.core.scheduling;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import com.icentris.sql.SimpleSQL;
import com.trentlarson.forecast.core.dao.Team;
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
  private static final Logger issueSqlLog  = Logger.getLogger("com.trentlarson.forecast.core.scheduling.TimeScheduleLoader.IssueSQL");




  // The source is the supertask and the destination is the subtask.
  public static final String LINK_SUBTASK="10002";
  // The source is the dependent and the destination is the predecessor.
  public static final String LINK_DEPENDENCY="10004";
  public static final String CUSTOM_START_DATE="10180";
  /**
  public static final String LINK_SUBTASK, LINK_DEPENDENCY, CUSTOM_START_DATE;
  public static final String PROP_LINK_SUBTASK = "icentris.jira.string.link.subtask";
  public static final String PROP_LINK_DEPENDENCY = "icentris.jira.string.link.blocking";
  public static final String PROP_START_DATE_FIELD = "icentris.jira.string.field.start-date";
  static {
    if ("true".equals(System.getProperty("no-jira-manager"))) {
      LINK_SUBTASK = "10010";
      LINK_DEPENDENCY = "10000";
      CUSTOM_START_DATE = "10180";
    } else {
      com.atlassian.jira.config.properties.ApplicationProperties props =
        com.atlassian.jira.ManagerFactory.getApplicationProperties();
      if ( !props.exists(PROP_LINK_SUBTASK) ) {
        props.setString(PROP_LINK_SUBTASK, "10010");
      }
      LINK_SUBTASK = props.getString(PROP_LINK_SUBTASK);
      if ( !props.exists(PROP_LINK_DEPENDENCY) ) {
        props.setString(PROP_LINK_DEPENDENCY, "10000");
      }
      LINK_DEPENDENCY = props.getString(PROP_LINK_DEPENDENCY);
      if ( !props.exists(PROP_START_DATE_FIELD) ) {
        props.setString(PROP_START_DATE_FIELD, "10180");
      }
      CUSTOM_START_DATE = props.getString(PROP_START_DATE_FIELD);
    }
  }
  **/



  /* These are SQL differences between my ideal DB and the Jira DB (besides "jiraissue"). */
  /** for my ideal
  private static final String DB_ISSUE_TABLE = "issue";
  private static final String DB_START_DATE_COLUMN = "startdate";
  private static final String DB_START_DATE_COLUMN_B = "issueb.startdate";
  private static final String DB_START_DATE_JOIN = "";
  */

  /** for Jira
  */
  public static final String DB_ISSUE_TABLE = "jiraissue";
  private static final String DB_START_DATE_COLUMN = "cfv.datevalue";
  private static final String DB_START_DATE_COLUMN_B = DB_START_DATE_COLUMN;
  private static final String DB_START_DATE_JOIN = 
    " left outer join customfieldvalue cfv on issueb.id = cfv.issue"
    + " and cfv.customfield = " + CUSTOM_START_DATE;






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
    return getGraphThrows("", new String[0], userArray, sPrefs, conn);
  }

  private static int SQL_SELECT_COUNT = 0;
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
      loadDetails(project, issueKeys, users, conn);
      //loadAllDetails(conn);

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

  /**
     @return Map from assignee to the list of their issues needing scheduling

     Note that this loads all open issues, along with their links to
     other open issues.
     @see #loadDetails
   */
  private static Map<Teams.AssigneeKey,List<IssueTree>> loadAllDetails
    (Connection conn) throws SQLException {

    Map<Teams.AssigneeKey,List<IssueTree>> allUserDetails = new HashMap<Teams.AssigneeKey,List<IssueTree>>();
    ResultSet rset = null;
    try {

      // load helper data: priority numbers
      Map<String,Integer> priorities = new HashMap<String,Integer>();
      String prioritySql = "select id, sequence from priority";
      rset = SimpleSQL.executeQuery(prioritySql, new String[0], conn);
      while (rset.next()) {
        priorities.put(rset.getString("id"), new Integer(rset.getInt("sequence")));
      }

      // load helper data: project IDs
      Map<Long,Long> projectToTeam = new HashMap<Long,Long>();
      String projectTeamSql = "select project_id, id from team";
      rset = SimpleSQL.executeQuery(projectTeamSql, new String[0], conn);
      while (rset.next()) {
        if (rset.getString("project_id") != null) {
          projectToTeam.put(new Long(rset.getLong("project_id")),
                            new Long(rset.getLong("id")));
        }
      }

      // load helper data: team data
      List<Team> allTeams;
      Map<Long,Team> idToTeam = new HashMap<Long, Team>();
      {
        org.hibernate.Session sess = TeamHoursUtil.HibernateUtil.currentSession();
        allTeams = sess.createQuery("from Team order by name").list();
        TeamHoursUtil.HibernateUtil.closeSession();
        for (Team team : allTeams) {
          idToTeam.put(team.getId(), team);
        }
      }

      // load all open issues
      Map<String,IssueTree> keyToIssue = new HashMap<String,IssueTree>();
      String openIssueSql =
        "select pkey, summary, assignee, resolution, timeestimate, timespent, duedate,"
        + " priority, project, "
        + " " + DB_START_DATE_COLUMN + " as earliest_start_date"
        + " from " + DB_ISSUE_TABLE + " issueb"
        + " " + DB_START_DATE_JOIN
        + " where (resolution is null or resolution = 0)";
      rset = SimpleSQL.executeQuery(openIssueSql, new Object[0], conn);
      while (rset.next()) {
        Long teamId = projectToTeam.get(Long.valueOf(rset.getLong("project")));
        IssueTree tree =
          new IssueTree
          (rset.getString("pkey"),
           rset.getString("summary"),
           rset.getString("assignee"),
           teamId,
           teamKeyFromIssueKey(teamId, rset.getString("pkey")),
           rset.getInt("timeestimate"),
           rset.getInt("timespent"),
           rset.getDate("dueDate"),
           rset.getDate("earliest_start_date"),
           priorities.get(rset.getString("priority")).intValue(),
           rset.getInt("resolution") != 0);
        keyToIssue.put(tree.getKey(), tree);

        // add to the user's list of issues
        Teams.AssigneeKey assigneeKey = 
          new Teams.AssigneeKey(tree.getRawAssignedTeamId(),
                                tree.getRawAssignedPerson());
        if (!allUserDetails.containsKey(assigneeKey)) {
          allUserDetails.put(assigneeKey, new ArrayList<IssueTree>());
        }
        allUserDetails.get(assigneeKey).add(tree);

      }


      // load subtask data
      String subtaskSql =
        "select a.pkey as super_key, b.pkey as sub_key"
        + " from issuelink, issue a, issue b"
        + " where linktype = '" + LINK_SUBTASK + "'"
        + " and source = a.id and b.id = destination"
        + " and a.resolution is null and b.resolution is null";
      rset = SimpleSQL.executeQuery(subtaskSql, new Object[0], conn);
      while (rset.next()) {
        String superKey = rset.getString("super_key");
        String subKey = rset.getString("sub_key");
        keyToIssue.get(superKey).addSubtask(keyToIssue.get(subKey));
      }


      // load blocking data
      String blockedSql =
        "select a.pkey as pre_key, b.pkey as post_key"
        + " from issuelink, issue a, issue b"
        + " where linktype = '" + LINK_DEPENDENCY + "'"
        + " and source = a.id and b.id = destination"
        + " and a.resolution is null and b.resolution is null";
      rset = SimpleSQL.executeQuery(blockedSql, new Object[0], conn);
      while (rset.next()) {
        String preKey = rset.getString("pre_key");
        String postKey = rset.getString("post_key");
        keyToIssue.get(preKey).addDependent(keyToIssue.get(postKey));
      }

    } finally {
      try { rset.close(); } catch (Exception e2) {}
    }

    return allUserDetails;
  }





  /**
     @return Map from assignee to the list of their issues needing scheduling

     Note that this only loads the project/issues/users specified,
     though it gets as complete as possible by recursively looking up
     any linked issues that may affect scheduling.
     
     @see #loadAllDetails
   */
  private static Map<Teams.AssigneeKey,List<IssueTree>> loadDetails
    (String project, String[] issueKeys, String[] users,
     Connection conn) throws SQLException {

    SQL_SELECT_COUNT = 0;

    ResultSet rset = null;

    // first load some helper data
    Map<String,Integer> priorities = new HashMap<String,Integer>();
    String prioritySql = "select id, sequence from priority";
    rset = SimpleSQL.executeQuery(prioritySql, new String[0], conn);
    while (rset.next()) {
      priorities.put(rset.getString("id"), new Integer(rset.getInt("sequence")));
    }

    Map<Long,Long> projectToTeam = new HashMap<Long,Long>();
    String projectTeamSql = "select project_id, id from team";
    rset = SimpleSQL.executeQuery(projectTeamSql, new String[0], conn);
    while (rset.next()) {
      if (rset.getString("project_id") != null) {
        projectToTeam.put(new Long(rset.getLong("project_id")),
                          new Long(rset.getLong("id")));
      }
    }


    TreeSet<String> allAssignees = new TreeSet<String>();
    for (int i = 0; i < users.length; i++) {
      allAssignees.add(users[i]);
    }

    Map<String,IssueTree> visitedAlready = new HashMap<String,IssueTree>();

    if (project.length() > 0) {
      log4jLog.debug("Will look for issues in project: " + project);
      String projectSql =
        "select issueb.pkey, summary, assignee, resolution, timeestimate, timespent,"
        + " dueDate, priority, issueb.project, "
        + " " + DB_START_DATE_COLUMN + " as earliest_start_date "
        + " from " + DB_ISSUE_TABLE + " issueb, project"
        + " " + DB_START_DATE_JOIN
        + " where project.pkey = ?"
        + " and issueb.project = project.id and issue.resolution is null";
      allAssignees.addAll
        (loadTopLevelIssues(projectSql, new Object[]{ project }, conn,
                            visitedAlready, priorities, projectToTeam));
    }


    if (issueKeys.length > 0) {
      log4jLog.debug("Will look for issues with keys: " + Arrays.asList(issueKeys));
      // get all the requested issues and load them into memory with the relationships
      String linkSql;
      Object[] args;

      linkSql =
        "select pkey, summary, assignee, resolution, timeestimate, timespent,"
        + " dueDate, priority, project, "
        + " " + DB_START_DATE_COLUMN + " as earliest_start_date"
        + " from " + DB_ISSUE_TABLE + " issueb"
        + " " + DB_START_DATE_JOIN
        + " where pkey in ("
        + Join(issueKeys, false, "?", ",")
        + ")";
      args = issueKeys;
      allAssignees.addAll
        (loadTopLevelIssues(linkSql, args, conn, visitedAlready,
                            priorities, projectToTeam));
    }

    // get the map of users to their tasks and load all issues for each
    Set<String> newAssignees = (Set<String>) allAssignees.clone();
    {
      log4jLog.debug("Will look for unassigned issues.");
      String noAssigneeSql =
        "select pkey, summary, assignee, resolution, timeestimate, timespent, duedate,"
        + " priority, project, "
        + " " + DB_START_DATE_COLUMN + " as earliest_start_date"
        + " from " + DB_ISSUE_TABLE + " issueb"
        + " " + DB_START_DATE_JOIN
        + " where assignee is null"
        + " and (resolution is null or resolution = 0)";
      newAssignees.addAll
        (loadTopLevelIssues
         (noAssigneeSql, new String[0], conn, visitedAlready,
          priorities, projectToTeam));
    }
    do {
      log4jLog.debug("Will look for issues assigned to: " + newAssignees);
      // -- get all unresolved issues from users that also must be scheduled
      String hasAssigneeSql =
        "select pkey, summary, assignee, resolution, timeestimate, timespent, duedate,"
        + " priority, project, "
        + " " + DB_START_DATE_COLUMN + " as earliest_start_date"
        + " from " + DB_ISSUE_TABLE + " issueb"
        + " " + DB_START_DATE_JOIN
        + " where assignee = ?"
        + " and (resolution is null or resolution = 0)";
      for (String assignee : newAssignees ) {
        String[] args = new String[]{ assignee };
        newAssignees.addAll
          (loadTopLevelIssues
           (hasAssigneeSql, args, conn, visitedAlready, 
            priorities, projectToTeam));
      }
      // -- repeat process for any new assignees not already searched
      for (Iterator<String> allAssigneeIter = allAssignees.iterator(); allAssigneeIter.hasNext(); ) {
        newAssignees.remove(allAssigneeIter.next());
      }
      allAssignees.addAll(newAssignees);
    } while (newAssignees.size() > 0);

    // -- now fill in the list of user issues
    Map<Teams.AssigneeKey,List<IssueTree>> allUserDetails = new HashMap<Teams.AssigneeKey,List<IssueTree>>();
    for (IssueTree node : visitedAlready.values()) {
      Teams.AssigneeKey assigneeKey = 
        new Teams.AssigneeKey(node.getRawAssignedTeamId(),
                              node.getRawAssignedPerson());
      if (!allUserDetails.containsKey(assigneeKey)) {
        allUserDetails.put(assigneeKey, new ArrayList<IssueTree>());
      }
      allUserDetails.get(assigneeKey).add(node);
    }

    log4jLog.info("Loaded " + visitedAlready.size() + " issues using " + SQL_SELECT_COUNT + " selects.");

    return allUserDetails;
  }

  /**
     @param visitedAlready Map of key String to IssueTree elements
     visited; this is modified to include all loaded issues
     @return List of (String ID for) assignees found in the loaded
     issues
  */
  private static List<String> loadTopLevelIssues
    (String sql, Object[] args, Connection conn,
     Map<String,IssueTree> visitedAlready,
     Map<String,Integer> priorities, Map<Long,Long> projectToTeam)
    throws SQLException {
    List<String> assignees = new ArrayList<String>();
    ResultSet rset = null;
    List<IssueTree> issueList = new ArrayList<IssueTree>();
    try {
      rset = SimpleSQL.executeQuery(sql, args, conn);
      SQL_SELECT_COUNT++;
      issueSqlLog.debug("Selecting issues with SQL: \n " + sql + "\n... with args: \n" + Arrays.asList(args));
      while (rset.next()) {
        Long teamId = projectToTeam.get(Long.valueOf(rset.getLong("project")));
        IssueTree tree =
          new IssueTree
          (rset.getString("pkey"),
           rset.getString("summary"),
           rset.getString("assignee"),
           teamId,
           teamKeyFromIssueKey(teamId, rset.getString("pkey")),
           rset.getInt("timeestimate"),
           rset.getInt("timespent"),
           rset.getDate("dueDate"),
           rset.getDate("earliest_start_date"),
           priorities.get(rset.getString("priority")).intValue(),
           rset.getInt("resolution") != 0);
        issueList.add(tree);
      }
      issueSqlLog.debug("Result of SQL: \n " + issueList);
    } finally {
      try { rset.close(); } catch (Exception e2) {}
    }

    for (Iterator<IssueTree> issueIter = issueList.iterator(); issueIter.hasNext(); ) {
      IssueTree tree = (IssueTree) issueIter.next();
      if (!visitedAlready.containsKey(tree.getKey())) {
        assignees.addAll(fillTree(tree, visitedAlready, conn, priorities, projectToTeam));
      }
    }
    return assignees;
  }


  /** make a single string out of the values */
  private static String Join
    (String[] values, boolean include_values, String append_after_each, String separator) {
    StringBuffer sb = new StringBuffer();
    for (int i=0; i<values.length; i++) {
      if (i > 0
          && separator != null) {
        sb.append(separator);
      }
      if (include_values) {
        sb.append(values[i]);
      }
      if (append_after_each != null) {
        sb.append(append_after_each);
      }
    }
    return sb.toString();
  }
  /**
     This will fill in the given IssueTree 'parent' with all its
     subtasks and the precursors necessary to schedule this issue (and
     their subtasks and precursors recusively), and possibly all its
     dependents.
     @return Set of (String username for) assignees found in the loaded issues
  */
  private static Set<String> fillTree
    (IssueTree parent, Map<String,IssueTree> visitedAlready,
     Connection conn, Map<String,Integer> priorities, Map<Long,Long> projectToTeam)
    throws SQLException {
    return fillTree(parent, visitedAlready, new TreeSet<String>(), new TreeSet<String>(),
                    new TreeSet<String>(), conn, priorities, projectToTeam);
  }
  /**
     @param visitedAlready is Map of issue key String to (IssueTree elements for) all trees visited so far
     @param visitedAncestors is Set of (String key for) ancestors to the parent (used to detect cycles)
     @param visitedPrecursors is Set of (String key for) precursors to the parent (used to detect cycles)
     @param visitedDependents is Set of (String key for) dependents to the parent (used to detect cycles)
     @param assignees is Set of (String username for) assignees
     @return Set of (Teams.AssigneeKey for) assignees found in the loaded issues
  */
  private static Set<String> fillTree
    (IssueTree parent, Map<String,IssueTree> visitedAlready,
     Set<String> visitedAncestors,
     Set<String> visitedPrecursors, Set<String> visitedDependents,
     Connection conn, Map<String,Integer> priorities, Map<Long,Long> projectToTeam)
    throws SQLException {

    Set<String> newAssignees = new TreeSet<String>();

    ResultSet rset = null;
    Set<IssueTree> subtasksToFill = new TreeSet<IssueTree>();
    Set<IssueTree> dependentsToFill = new TreeSet<IssueTree>();
    Set<IssueTree> precursorsToFill = new TreeSet<IssueTree>();
    try {
      String linkSql;
      Object[] args;

      visitedAlready.put(parent.getKey(), parent);
      if (parent.getRawAssignedPerson() != null) {
        newAssignees.add(parent.getRawAssignedPerson());
      }

      // get the subtasks
      {
        linkSql =
          "select issueb.pkey, issueb.summary, issueb.assignee, issueb.timeestimate,"
          + " issueb.timespent, issueb.duedate, issueb.resolution, issueb.priority, issueb.project,"
          + " " + DB_START_DATE_COLUMN_B + " as earliest_start_date"
          + " from issuelink, " + DB_ISSUE_TABLE + " a, " + DB_ISSUE_TABLE + " issueb"
          + " " + DB_START_DATE_JOIN
          + " where linktype = '" + LINK_SUBTASK + "'"
          + " and a.pkey = ? and source = a.id and issueb.id = destination";
        args = new Object[]{ parent.getKey() };
        issueSqlLog.debug("Selecting subtask issues with SQL: \n " + linkSql + "\n... with args: \n" + Arrays.asList(args));
        rset = SimpleSQL.executeQuery(linkSql, args, conn);
        SQL_SELECT_COUNT++;
        while (rset.next()) {
          if (visitedAlready.containsKey(rset.getString("pkey"))) {
            parent.addSubtask(visitedAlready.get(rset.getString("pkey")));
          } else {
            Long teamId = projectToTeam.get(Long.valueOf(rset.getLong("project")));
            IssueTree tree = 
              new IssueTree
              (rset.getString("pkey"),
               rset.getString("summary"),
               rset.getString("assignee"),
               teamId,
               teamKeyFromIssueKey(teamId, rset.getString("pkey")),
               rset.getInt("timeestimate"),
               rset.getInt("timespent"),
               rset.getDate("duedate"),
               rset.getDate("earliest_start_date"),
               priorities.get(rset.getString("priority")).intValue(),
               rset.getInt("resolution") != 0);
            subtasksToFill.add(tree);
          }
        }
        rset.close();
        issueSqlLog.debug("Result of SQL: \n " + subtasksToFill);
      }

      // get the dependents
      {
        linkSql =
          "select issueb.pkey, issueb.summary, issueb.assignee, issueb.timeestimate,"
          + " issueb.timespent, issueb.duedate, issueb.resolution, issueb.priority, issueb.project,"
          + " " + DB_START_DATE_COLUMN_B + " as earliest_start_date"
          + " from issuelink, " + DB_ISSUE_TABLE + " a, " + DB_ISSUE_TABLE + " issueb"
          + " " + DB_START_DATE_JOIN
          + " where linktype = '" + LINK_DEPENDENCY + "'"
          + " and a.pkey = ? and destination = a.id and issueb.id = source";
          // + " and (issueb.resolution is null or issueb.resolution = 0)"
        args = new Object[]{ parent.getKey() };
        issueSqlLog.debug("Selecting dependent issues with SQL: \n " + linkSql + "\n... with args: \n" + Arrays.asList(args));
        rset = SimpleSQL.executeQuery(linkSql, args, conn);
        SQL_SELECT_COUNT++;
        while (rset.next()) {
          if (visitedAlready.containsKey(rset.getString("pkey"))) {
            parent.addDependent(visitedAlready.get(rset.getString("pkey")));
          } else {
            Long teamId = projectToTeam.get(Long.valueOf(rset.getLong("project")));
            IssueTree tree =
              new IssueTree
              (rset.getString("pkey"),
               rset.getString("summary"),
               rset.getString("assignee"),
               teamId,
               teamKeyFromIssueKey(teamId, rset.getString("pkey")),
               rset.getInt("timeestimate"),
               rset.getInt("timespent"),
               rset.getDate("duedate"),
               rset.getDate("earliest_start_date"),
               priorities.get(rset.getString("priority")).intValue(),
               rset.getInt("resolution") != 0);
            dependentsToFill.add(tree);
          }
        }
        rset.close();
        issueSqlLog.debug("Result of SQL: \n " + dependentsToFill);
      }

      // get the precursors
      {
        linkSql =
          "select issueb.pkey, issueb.summary, issueb.assignee, issueb.timeestimate,"
          + " issueb.timespent, issueb.duedate, issueb.resolution, issueb.priority, issueb.project,"
          + " " + DB_START_DATE_COLUMN_B + " as earliest_start_date"
          + " from issuelink, " + DB_ISSUE_TABLE + " a, " + DB_ISSUE_TABLE + " issueb"
          + " " + DB_START_DATE_JOIN
          + " where linktype = '" + LINK_DEPENDENCY + "'"
          + " and a.pkey = ? and source = a.id and issueb.id = destination";
          // + " and (issueb.resolution is null or issueb.resolution = 0)"
        args = new Object[]{ parent.getKey() };
        issueSqlLog.debug("Selecting precursor issues with SQL: \n " + linkSql + "\n... with args: \n" + Arrays.asList(args));
        rset = SimpleSQL.executeQuery(linkSql, args, conn);
        SQL_SELECT_COUNT++;
        while (rset.next()) {
          if (visitedAlready.containsKey(rset.getString("pkey"))) {
            parent.addPrecursor(visitedAlready.get(rset.getString("pkey")));
          } else {
            Long teamId = projectToTeam.get(Long.valueOf(rset.getLong("project")));
            IssueTree tree =
              new IssueTree
              (rset.getString("pkey"),
               rset.getString("summary"),
               rset.getString("assignee"),
               teamId,
               teamKeyFromIssueKey(teamId, rset.getString("pkey")),
               rset.getInt("timeestimate"),
               rset.getInt("timespent"),
               rset.getDate("duedate"),
               rset.getDate("earliest_start_date"),
               priorities.get(rset.getString("priority")).intValue(),
               rset.getInt("resolution") != 0);
            precursorsToFill.add(tree);
          }
        }
        issueSqlLog.debug("Result of SQL: \n " + dependentsToFill);
      }

    } finally {
      try { rset.close(); } catch (Exception e2) {}
    }

    // now fill all information for children
    for (Iterator<IssueTree> i = subtasksToFill.iterator(); i.hasNext(); ) {
      IssueTree childTask = i.next();
      if (visitedAncestors.contains(childTask.getKey())) {
        log4jLog.warn("Note that " + childTask.getKey() + " is a subtask of itself.  (Ignoring.)");
        i.remove();
      } else {
        if (!childTask.getResolved()) {
          parent.addSubtask(childTask);
          TreeSet<String> childVisitedAncestors = new TreeSet<String>();
          childVisitedAncestors.add(parent.getKey());
          childVisitedAncestors.addAll(visitedAncestors);
          Set<String> moreAssignees =
            fillTree(childTask, visitedAlready, childVisitedAncestors,
                     visitedPrecursors, visitedDependents,
                     conn, priorities, projectToTeam);
          newAssignees.addAll(moreAssignees);
        }
      }
    }

    for (Iterator<IssueTree> i = dependentsToFill.iterator(); i.hasNext(); ) {
      IssueTree childTask = i.next();
      if (visitedPrecursors.contains(childTask.getKey())) {
        log4jLog.warn("Note that " + childTask.getKey() + " is a precursor to itself.  (Ignoring.)");
        i.remove();
      } else {
        if (!childTask.getResolved()) {
          parent.addDependent(childTask);
          TreeSet<String> passVisitedPrecursors = new TreeSet<String>();
          passVisitedPrecursors.add(parent.getKey());
          passVisitedPrecursors.addAll(visitedPrecursors);
          Set<String> moreAssignees =
            fillTree(childTask, visitedAlready, new TreeSet<String>(),
                     passVisitedPrecursors, new TreeSet<String>(),
                     conn, priorities, projectToTeam);
          newAssignees.addAll(moreAssignees);
        }
      }
    }

    for (Iterator<IssueTree> i = precursorsToFill.iterator(); i.hasNext(); ) {
      IssueTree childTask = i.next();
      if (visitedDependents.contains(childTask.getKey())) {
        log4jLog.warn("Note that " + childTask.getKey() + " is a dependant of itself.  (Ignoring.)");
        i.remove();
      } else {
        if (!childTask.getResolved()) {
          parent.addPrecursor(childTask);
          Set<String> passVisitedDependents = new TreeSet<String>();
          passVisitedDependents.add(parent.getKey());
          passVisitedDependents.addAll(visitedDependents);
          Set<String> moreAssignees =
            fillTree(childTask, visitedAlready, new TreeSet<String>(), new TreeSet<String>(),
                     passVisitedDependents, conn, priorities, projectToTeam);
          newAssignees.addAll(moreAssignees);
        }
      }
    }
    return newAssignees;
  }

  public static String teamKeyFromIssueKey(Long teamId, String issueKey) {
    // REFACTOR to pull the real team name (multiple instances in these files)
    return teamId == null ? null : issueKey.substring(0, issueKey.indexOf("-"));
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
      userIssuesKeyToString(newTimeDetails.timeDetails);

    // clone the hourly data (since it's modified later)
    Map<Teams.UserTimeKey,TimeSchedule.WeeklyWorkHours> weeklyHoursFromKey2 =
      new HashMap<UserTimeKey, WeeklyWorkHours>();
    for (Teams.UserTimeKey user : newTimeDetails.hours.keySet()) {
      weeklyHoursFromKey2
        .put(user,
             (TimeSchedule.WeeklyWorkHours) newTimeDetails.hours.get(user).clone());
    }

    Map<String,TimeSchedule.WeeklyWorkHours> weeklyHoursFromString =
      weeklyHoursKeyToString(weeklyHoursFromKey2);

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
    public UserDetailsAndHours(Map<Teams.UserTimeKey,List<IssueTree>> timeDetails_,
                               Map<Teams.AssigneeKey,Teams.UserTimeKey> assigneeToAllocatedUsers_,
                               Map<Teams.UserTimeKey,TimeSchedule.WeeklyWorkHours> hours_) {
      this.timeDetails = timeDetails_;
      this.assigneeToAllocatedUsers = assigneeToAllocatedUsers_;
      this.hours = hours_;
    }
  }

  /**
     Translate details info to input good for scheduling.

     @param hoursRange is used to decide exactly where to assign an
     issue.  If the hours don't exist for the user, then we assume
     that there is the default amount of work time available and we
     insert that record; the one exception is: if both the team and
     the user are set but no time record exists, then a record is
     created for the user on any team (ie. not only for the user on
     this team, ie. team is null).

     Note that each element in detailsFromAssignee may be modified to
     have an assignee that is assigned time (either existing or
     created with defaults; see previous paragraph).

     Note that hoursRange may be modified.

   */
  public static UserDetailsAndHours adjustDetailsForAssignedHours
     (Map<Teams.AssigneeKey,List<IssueTree>> detailsFromAssignee,
      Map<Teams.UserTimeKey,TimeSchedule.WeeklyWorkHours> hoursRange,
      Date startDate) {

    Map<Teams.UserTimeKey,TimeSchedule.WeeklyWorkHours> newHoursRanges = new HashMap<UserTimeKey, WeeklyWorkHours>();
    newHoursRanges.putAll(hoursRange);

    Map<Teams.UserTimeKey,List<IssueTree>> newDetailsByTime = new HashMap<Teams.UserTimeKey,List<IssueTree>>();

    Map<Teams.AssigneeKey,Teams.UserTimeKey> assigneeToAllocatedUsers = new HashMap<AssigneeKey, UserTimeKey>();

    for (Teams.AssigneeKey assigneeKey : detailsFromAssignee.keySet()) {
      for (IssueTree detail : detailsFromAssignee.get(assigneeKey)) {

        // create appropriate time-allocation user key for this time assignee
        Teams.UserTimeKey userKeyForTime =
          timeKeyIfNotAlreadyExisting(detail.getTimeAssigneeKey(), newHoursRanges);

        addTimeKeyIfNecessary(userKeyForTime, newHoursRanges, startDate);
        updateDetailAndLists(userKeyForTime, detail, newDetailsByTime,
                             assigneeToAllocatedUsers);
      }
    }
    return new UserDetailsAndHours(newDetailsByTime, assigneeToAllocatedUsers,
                                   newHoursRanges);
  }

  protected static Teams.UserTimeKey timeKeyIfNotAlreadyExisting
  (Teams.UserTimeKey userKey,
   Map<Teams.UserTimeKey,TimeSchedule.WeeklyWorkHours> userWeeklyHours) {

    if (userKey.getUsername() != null
        && !userWeeklyHours.containsKey(userKey)) {
      log4jLog.debug("Hours not allocated for " + userKey.getUsername() + ", so we're creating a time key for them.");
      return new Teams.UserTimeKey(null, userKey.getUsername());
    } else {
      return userKey;
    }
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
     Map<Teams.AssigneeKey,Teams.UserTimeKey> assigneeToAllocatedUsers) {

    if (log4jLog.isDebugEnabled()) {
      if (!assignee.equals(detail.getTimeAssigneeKey())) {
        log4jLog.debug("Changing time assignee of issue " + detail.getKey()
                       + " to " + assignee
                       + " (from " + detail.getTimeAssigneeKey() + ").");
      }
    }
    detail.setTimeAssigneeKey(assignee);

    if (!detailMap.containsKey(assignee)) {
      detailMap.put(assignee, new ArrayList<IssueTree>());
    }
    List<IssueTree> details = detailMap.get(assignee);
    details.add(detail);

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
     Check for this key in the hours range, and return the right
     IssueTree for this scheduling, creating it if it exists.
  */
  private static void addTimeKeyIfNecessary
    (Teams.UserTimeKey userKeyForTime,
     Map<Teams.UserTimeKey,TimeSchedule.WeeklyWorkHours> hoursRange,
     Date startDate) {
    if (!hoursRange.containsKey(userKeyForTime)) {
      log4jLog.debug("Adding default hours for " + userKeyForTime);
      TimeSchedule.WeeklyWorkHours defaultHours =
        new TimeSchedule.WeeklyWorkHours();
      defaultHours.inject(startDate,
                          TimeSchedule.TYPICAL_INDIVIDUAL_WORKHOURS_PER_WORKWEEK);
      hoursRange.put(userKeyForTime, defaultHours);
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

  protected static Map<String,TimeSchedule.WeeklyWorkHours> weeklyHoursKeyToString
    (Map<Teams.UserTimeKey,TimeSchedule.WeeklyWorkHours> origHours) {

    Map<String,TimeSchedule.WeeklyWorkHours> newHours = new HashMap<String, WeeklyWorkHours>();
    for (Teams.UserTimeKey userTimeKey : origHours.keySet()) {
      newHours.put(userTimeKey.toString(), origHours.get(userTimeKey));
    }
    return newHours;
  }

  protected static Map<String,List<IssueTree>> userIssuesKeyToString
    (Map<Teams.UserTimeKey,List<IssueTree>> origIssues) {

    Map<String,List<IssueTree>> newIssues = new HashMap<String,List<IssueTree>>();
    for (Teams.UserTimeKey assigneeKey : origIssues.keySet()) {
      newIssues.put(assigneeKey.toString(), origIssues.get(assigneeKey));
    }
    return newIssues;
  }

}
