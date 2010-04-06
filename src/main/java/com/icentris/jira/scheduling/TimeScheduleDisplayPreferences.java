package com.icentris.jira.scheduling;

import java.util.*;
import org.apache.log4j.Category;

public class TimeScheduleDisplayPreferences {

  private static final Category log4jLog = Category.getInstance("ic.sched.TimeScheduleDisplayPreferences");

  /** # of days to put in each slice */
  public final int timeGranularity;
  public final int timeMarker;
  public final boolean showBlocked;
  public final boolean hideDetails;
  public final boolean showResolved;
  public final boolean showChangeTools;
  /** whether to drill down into subtasks as rendering continues */
  public final boolean showHierarchically;
  /** List of users to display, each on one row; may be null */
  public final List<Teams.AssigneeKey> showUsersInOneRow;
  /** List of issue key Strings to display individually; may be empty list, but never null */
  public final List<IssueTree> showIssues;

  // NOTE: if you add more fields, then add them to the "cloning" constructor.


  public static class NoSuchIssueException extends Exception {
    public NoSuchIssueException(String message) {
      super(message);
    }
  }


  /**
     For cloning.
  */
  private TimeScheduleDisplayPreferences
    (int timeGranularity_, int timeMarker_, boolean showBlocked_,
     boolean hideDetails_, boolean showResolved_, boolean showHierarchically_,
     boolean showChangeTools_,
     List<Teams.AssigneeKey> showUsersInOneRow_,
     List<IssueTree> showIssues_) {
    this.timeGranularity = timeGranularity_;
    this.timeMarker = timeMarker_;
    this.showBlocked = showBlocked_;
    this.hideDetails = hideDetails_;
    this.showResolved = showResolved_;
    this.showChangeTools = showChangeTools_;
    this.showHierarchically = showHierarchically_;
    this.showUsersInOneRow = showUsersInOneRow_;
    this.showIssues = showIssues_;
  }

  private TimeScheduleDisplayPreferences
    (int timeGranularity_, int timeMarker_, boolean showBlocked_,
     boolean hideDetails_, boolean showResolved_, boolean showHierarchically_,
     boolean showChangeTools_) {
    this(timeGranularity_, timeMarker_, showBlocked_,
         hideDetails_, showResolved_, showHierarchically_, showChangeTools_,
         new ArrayList(), new ArrayList());
  }

  public boolean showEachUserOnOneRow() {
    return showUsersInOneRow != null && showUsersInOneRow.size() > 0;
  }


  public static TimeScheduleDisplayPreferences createForUser
    (int timeGranularity_, int timeMarker_, boolean showBlocked_,
     boolean hideDetails_, boolean showResolved_, Teams.AssigneeKey showUserAndTeam,
     boolean showChangeTools_, IssueDigraph graph) {

    TimeScheduleDisplayPreferences prefs =
      new TimeScheduleDisplayPreferences
      (timeGranularity_, timeMarker_, showBlocked_, hideDetails_, showResolved_, 
       false, showChangeTools_);

    List<IssueTree> issues = graph.getAssignedUserDetails().get(showUserAndTeam);
    if (issues == null) {
      issues = new ArrayList();
    }
    prefs.showIssues.addAll(issues);

    return prefs;
  }


  public static TimeScheduleDisplayPreferences createForUser
    (int timeGranularity_, int timeMarker_, boolean showBlocked_,
     boolean hideDetails_, boolean showResolved_, String showUser,
     boolean showChangeTools_, IssueDigraph graph) {

    TimeScheduleDisplayPreferences prefs =
      new TimeScheduleDisplayPreferences
      (timeGranularity_, timeMarker_, showBlocked_, hideDetails_, showResolved_, 
       false, showChangeTools_);

    Set<Teams.UserTimeKey> addedAlready = new TreeSet();
    for (Teams.AssigneeKey userKey : graph.getAssignedUserDetails().keySet()) {
      if (showUser.equals(userKey.getUsername())) {
        Teams.UserTimeKey timeKey = graph.getAllocatedUser(userKey);
        if (!addedAlready.contains(timeKey)) {
          prefs.showIssues.addAll(graph.getTimeUserDetails().get(timeKey));
          addedAlready.add(timeKey);
        }
      }
    }

    return prefs;
  }


  public static TimeScheduleDisplayPreferences createForTeam
    (int timeGranularity_, int timeMarker_, boolean showBlocked_,
     boolean hideDetails_, boolean showResolved_, Long showTeamId,
     boolean showChangeTools_, IssueDigraph graph) {
    TimeScheduleDisplayPreferences prefs =
      new TimeScheduleDisplayPreferences
      (timeGranularity_, timeMarker_, showBlocked_, hideDetails_, showResolved_, 
       false, showChangeTools_);

    Set<Teams.UserTimeKey> addedAlready = new TreeSet();
    for (Teams.AssigneeKey userKey : graph.getAssignedUserDetails().keySet()) {
      if (showTeamId.equals(userKey.getTeamId())) {
        Teams.UserTimeKey timeKey = graph.getAllocatedUser(userKey);
        if (!addedAlready.contains(timeKey)) {
          prefs.showIssues.addAll(graph.getTimeUserDetails().get(timeKey));
          addedAlready.add(timeKey);
        }
      }
    }

    return prefs;
  }

  /** This is for displaying all user data in one row. */
  public static TimeScheduleDisplayPreferences createForUsers
    (int timeGranularity_, int timeMarker_, boolean showBlocked_,
     boolean hideDetails_, boolean showResolved_,
     Teams.AssigneeKey[] showUsersInOneRow_,
     IssueDigraph graph) {
    TimeScheduleDisplayPreferences prefs =
      new TimeScheduleDisplayPreferences
      // (REFACTOR because some of these should always be false)
      (timeGranularity_, timeMarker_, showBlocked_, hideDetails_, showResolved_, 
       false, false);

    prefs.showUsersInOneRow.addAll(Arrays.asList(showUsersInOneRow_));
    for (int useri = 0; useri < showUsersInOneRow_.length; useri++) {
      prefs.showIssues.addAll(graph.getAssignedUserDetails().get(showUsersInOneRow_[useri]));
    }

    return prefs;
  }


  public static TimeScheduleDisplayPreferences createForIssues
    (int timeGranularity_, int timeMarker_, boolean showBlocked_,
     boolean hideDetails_, boolean showResolved_, String[] issueKeys,
     boolean showChangeTools_, IssueDigraph graph)
  throws NoSuchIssueException {
    TimeScheduleDisplayPreferences prefs =
      new TimeScheduleDisplayPreferences
      (timeGranularity_, timeMarker_, showBlocked_, hideDetails_, showResolved_, 
       true, showChangeTools_);

    for (int keyi = 0; keyi < issueKeys.length; keyi++) {
      if (graph.getIssueSchedule(issueKeys[keyi]) == null) {
        throw new NoSuchIssueException(issueKeys[keyi]);
      }
      prefs.showIssues.add(graph.getIssueTree(issueKeys[keyi]));
    }

    return prefs;
  }


  public static TimeScheduleDisplayPreferences createForDate
    (int timeGranularity_, int timeMarker_, boolean showBlocked_,
     boolean hideDetails_, boolean showResolved_, Date dueBefore,
     boolean showChangeTools_, IssueDigraph graph) {
    TimeScheduleDisplayPreferences prefs =
      new TimeScheduleDisplayPreferences
      (timeGranularity_, timeMarker_, showBlocked_, hideDetails_, showResolved_, 
       false, showChangeTools_);

    // use the whole graph, just check for overdue issues
    for (String treeKey : graph.getIssueSchedules().keySet()) {
      IssueTree issue = graph.getIssueTree(treeKey);
      if (issue.getDueDate() != null
          && issue.getDueDate().before(dueBefore)) {
        prefs.showIssues.add(issue);
      }
    }

    return prefs;
  }

  public TimeScheduleDisplayPreferences cloneButShowBlocked(boolean showBlocked_) {
    return new TimeScheduleDisplayPreferences
      (this.timeGranularity, this.timeMarker, showBlocked_,
       this.hideDetails, this.showResolved, this.showHierarchically,
       this.showChangeTools,
       this.showUsersInOneRow, this.showIssues);
  }


  protected boolean displayIssue(IssueTree issue) {
    // if it's resolved, we'll only show if subtasks are supposed to be shown
    boolean resolvedOK =
      showResolved
      || !issue.getResolved()
      || !issue.allSubtasksResolved();
    boolean displayOK =
      // only if
      // resolved status is right
      (resolvedOK
       // and 
       // it's one of the items in showIssues
       && (showIssues.indexOf(issue) > -1
           // or we're showing the full hierarchy
           || (showHierarchically)));// && !shownAlready.contains(issue.getKey()))));
    if (log4jLog.isDebugEnabled()) {
      log4jLog.debug("Display " + issue.getKey() + " = " + displayOK 
                     + ": (showResolved = " + showResolved
                     + " || issue is unresolved = " + (!issue.getResolved())
                     + " || issue has unresolved subtasks = " + !issue.allSubtasksResolved()
                     + ") && (it's an issue to show = " + (showIssues.indexOf(issue) > -1)
                     + " || showHierarchically = " + showHierarchically
                     + ")");
    }
    return displayOK;
  }
}



