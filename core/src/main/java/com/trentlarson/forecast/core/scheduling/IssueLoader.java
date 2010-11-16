package com.trentlarson.forecast.core.scheduling;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import com.icentris.sql.SimpleSQL;

public class IssueLoader {

  protected static final Logger log4jLog = Logger.getLogger("com.trentlarson.forecast.core.scheduling.IssueLoader");
  protected static final Logger issueSqlLog  = Logger.getLogger("com.trentlarson.forecast.core.scheduling.TimeScheduleLoader.IssueSQL");
  public static final String ISSUE_TO_WATCH = null; // set to some issue pkey (and enable log4jLog debugging) to print out info as that issue is loaded and analyzed


  // The source (outward) is the supertask (parent) and the destination (inward) is the subtask (child).
  public static final String LINK_SUBTASK="10002";
  // The source (outward) is the predecessor (must be done before starting dependent) and the destination (inward) is the dependent (cannot start until the predecessor).
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
  @return Map from assignee to the list of their issues needing scheduling

  Note that this loads all open issues, along with their links to
  other open issues.

  @see #loadDetails for the alternative
   */
  protected static Map<Teams.AssigneeKey,List<IssueTree>> loadAllDetails
  (Connection conn) throws SQLException {

    Map<Teams.AssigneeKey,List<IssueTree>> allUserDetails = new HashMap<Teams.AssigneeKey,List<IssueTree>>();
    ResultSet rset = null;
    try {

      ConfigData config = new ConfigData(conn);

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
        Long teamId = config.projectToTeam.get(Long.valueOf(rset.getLong("project")));
        IssueTree tree =
          new IssueTree
          (rset.getString("pkey"),
              rset.getString("summary"),
              rset.getString("assignee"),
              teamId,
              rset.getInt("timeestimate"),
              rset.getInt("timespent"),
              rset.getDate("dueDate"),
              rset.getDate("earliest_start_date"),
              config.priorities.get(rset.getString("priority")).intValue(),
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
        + " from issuelink, " + DB_ISSUE_TABLE + " a, " + DB_ISSUE_TABLE + " b"
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
        + " from issuelink, " + DB_ISSUE_TABLE + " a, " + DB_ISSUE_TABLE + " b"
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



  private static int SQL_SELECT_COUNT = 0;

  /**
  @return Map from assignee to the list of their issues needing scheduling

  Note that this only loads the project/issues/users specified,
  though it gets as complete as possible by recursively looking up
  any linked issues that may affect scheduling.

  @see #loadAllDetails for the alternative
   */
  protected static Map<Teams.AssigneeKey,List<IssueTree>> loadDetails
  (String project, String[] issueKeys, String[] users,
      Connection conn) throws SQLException {

    SQL_SELECT_COUNT = 0;

    ConfigData config = new ConfigData(conn);

    TreeSet<String> allAssignees = new TreeSet<String>();
    for (int i = 0; i < users.length; i++) {
      allAssignees.add(users[i]);
    }

    Map<String,IssueTree> visitedAlready = new HashMap<String,IssueTree>();

    if (project != null
        && project.length() > 0) {
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
          visitedAlready, config.priorities, config.projectToTeam));
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
          config.priorities, config.projectToTeam));
    }

    // get the map of users to their tasks and load all issues for each
    @SuppressWarnings("unchecked")
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
              config.priorities, config.projectToTeam));
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
                config.priorities, config.projectToTeam));
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
  
  
  private static class ConfigData {
    public final Map<String,Integer> priorities = new HashMap<String,Integer>();
    Map<Long,Long> projectToTeam = new HashMap<Long,Long>();
    public ConfigData(Connection conn) throws SQLException {
      
      String prioritySql = "select id, sequence from priority";
      ResultSet rset = SimpleSQL.executeQuery(prioritySql, new String[0], conn);
      while (rset.next()) {
        priorities.put(rset.getString("id"), new Integer(rset.getInt("sequence")));
      }
      
      String projectTeamSql = "select project_id, id from team";
      rset = SimpleSQL.executeQuery(projectTeamSql, new String[0], conn);
      while (rset.next()) {
        if (rset.getString("project_id") != null) {
          projectToTeam.put(new Long(rset.getLong("project_id")),
              new Long(rset.getLong("id")));
        }
      }
    }
    
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
      if (log4jLog.isDebugEnabled()
          && parent.getKey().equals(ISSUE_TO_WATCH)) {
        log4jLog.debug("Added " + ISSUE_TO_WATCH + " to visitedAlready.");
      }
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
          if (log4jLog.isDebugEnabled()
              && rset.getString("pkey").equals(ISSUE_TO_WATCH)) {
            log4jLog.debug("Loading " + ISSUE_TO_WATCH + "...");
          }
          if (visitedAlready.containsKey(rset.getString("pkey"))) {
            if (log4jLog.isDebugEnabled()
                && rset.getString("pkey").equals(ISSUE_TO_WATCH)) {
              log4jLog.debug("... already in visitedAlready, so adding " + ISSUE_TO_WATCH + " to parent subtasks.");
            }
            parent.addSubtask(visitedAlready.get(rset.getString("pkey")));
          } else {
            if (log4jLog.isDebugEnabled()
                && rset.getString("pkey").equals(ISSUE_TO_WATCH)) {
              log4jLog.debug("... not in visitedAlready, so adding " + ISSUE_TO_WATCH + " to subtasksToFill.");
            }
            Long teamId = projectToTeam.get(Long.valueOf(rset.getLong("project")));
            IssueTree tree = 
              new IssueTree
              (rset.getString("pkey"),
                  rset.getString("summary"),
                  rset.getString("assignee"),
                  teamId,
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
          if (log4jLog.isDebugEnabled()
              && rset.getString("pkey").equals(ISSUE_TO_WATCH)) {
            log4jLog.debug("Loading " + ISSUE_TO_WATCH + "...");
          }
          if (visitedAlready.containsKey(rset.getString("pkey"))) {
            if (log4jLog.isDebugEnabled()
                && rset.getString("pkey").equals(ISSUE_TO_WATCH)) {
              log4jLog.debug("... already in visitedAlready, so adding " + ISSUE_TO_WATCH + " to parent dependents.");
            }
            parent.addDependent(visitedAlready.get(rset.getString("pkey")));
          } else {
            if (log4jLog.isDebugEnabled()
                && rset.getString("pkey").equals(ISSUE_TO_WATCH)) {
              log4jLog.debug("... not in visitedAlready, so adding " + ISSUE_TO_WATCH + " to dependentsToFill.");
            }
            Long teamId = projectToTeam.get(Long.valueOf(rset.getLong("project")));
            IssueTree tree =
              new IssueTree
              (rset.getString("pkey"),
                  rset.getString("summary"),
                  rset.getString("assignee"),
                  teamId,
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
          if (log4jLog.isDebugEnabled()
              && rset.getString("pkey").equals(ISSUE_TO_WATCH)) {
            log4jLog.debug("Loading " + ISSUE_TO_WATCH + "...");
          }
          if (visitedAlready.containsKey(rset.getString("pkey"))) {
            if (log4jLog.isDebugEnabled()
                && rset.getString("pkey").equals(ISSUE_TO_WATCH)) {
              log4jLog.debug("... already in visitedAlready, so adding " + ISSUE_TO_WATCH + " to parent precursors.");
            }
            parent.addPrecursor(visitedAlready.get(rset.getString("pkey")));
          } else {
            if (log4jLog.isDebugEnabled()
                && rset.getString("pkey").equals(ISSUE_TO_WATCH)) {
              log4jLog.debug("... not in visitedAlready, so adding " + ISSUE_TO_WATCH + " to precursorsToFill.");
            }
            Long teamId = projectToTeam.get(Long.valueOf(rset.getLong("project")));
            IssueTree tree =
              new IssueTree
              (rset.getString("pkey"),
                  rset.getString("summary"),
                  rset.getString("assignee"),
                  teamId,
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
      if (log4jLog.isDebugEnabled()
          && childTask.getKey().equals(ISSUE_TO_WATCH)) {
        log4jLog.debug("Checking on " + ISSUE_TO_WATCH + " as a subtask...");
      }
      if (visitedAncestors.contains(childTask.getKey())) {
        if (log4jLog.isDebugEnabled()
            && childTask.getKey().equals(ISSUE_TO_WATCH)) {
          log4jLog.debug(ISSUE_TO_WATCH + " is in visitedAncestors, so ignoring it.");
        }
        log4jLog.warn("Note that " + childTask.getKey() + " is a subtask of itself.  (Ignoring.)");
        i.remove();
      } else if (visitedAlready.containsKey(childTask.getKey())) { // since it may have been added during this loop
        if (log4jLog.isDebugEnabled()
            && childTask.getKey().equals(ISSUE_TO_WATCH)) {
          log4jLog.debug(ISSUE_TO_WATCH + " it's in visitedAlready, so ignoring it.");
        }
        parent.addSubtask(childTask);
      } else {
        if (log4jLog.isDebugEnabled()
            && childTask.getKey().equals(ISSUE_TO_WATCH)) {
          log4jLog.debug("Going to recurse on " + ISSUE_TO_WATCH + " as a subtask? " + (!childTask.getResolved()));
        }
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
      } else if (visitedAlready.containsKey(childTask.getKey())) { // since it may have been added during one of these loops
        parent.addDependent(childTask);
      } else {
        if (log4jLog.isDebugEnabled()
            && childTask.getKey().equals(ISSUE_TO_WATCH)) {
          log4jLog.debug("Going to recurse on " + ISSUE_TO_WATCH + " as a dependent? " + (!childTask.getResolved()));
        }
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
      } else if (visitedAlready.containsKey(childTask.getKey())) { // since it may have been added during one of these loops
        parent.addPrecursor(childTask);
      } else {
        if (!childTask.getResolved()) {
          if (log4jLog.isDebugEnabled()
              && childTask.getKey().equals(ISSUE_TO_WATCH)) {
            log4jLog.debug("Going to recurse on " + ISSUE_TO_WATCH + " as a precursor? " + (!childTask.getResolved()));
          }
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



	  
}
