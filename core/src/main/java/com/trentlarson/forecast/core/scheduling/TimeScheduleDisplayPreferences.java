package com.trentlarson.forecast.core.scheduling;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import org.apache.log4j.Logger;

public class TimeScheduleDisplayPreferences {

  private static final Logger log4jLog = Logger.getLogger("ic.sched.TimeScheduleDisplayPreferences");

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
  public final List<String> showIssues;

  // NOTE: if you add more fields, then add them to the "cloning" constructor.




  /**
     For cloning.
  */
  private TimeScheduleDisplayPreferences
    (int timeGranularity_, int timeMarker_, boolean showBlocked_,
     boolean hideDetails_, boolean showResolved_, boolean showHierarchically_,
     boolean showChangeTools_,
     List<Teams.AssigneeKey> showUsersInOneRow_,
     List<String> showIssues_) {
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
         new ArrayList<Teams.AssigneeKey>(), new ArrayList<String>());
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
    for (IssueTree issue : issues) {
      prefs.showIssues.add(issue.getKey());
    }

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

    Set<Teams.UserTimeKey> addedAlready = new TreeSet<Teams.UserTimeKey>();
    for (Teams.AssigneeKey userKey : graph.getAssignedUserDetails().keySet()) {
      if ((showUser == null
           && userKey.getUsername() == null) 
          ||
          (showUser != null
           && showUser.equals(userKey.getUsername()))) {
        Teams.UserTimeKey timeKey = graph.getAllocatedUser(userKey);
        if (!addedAlready.contains(timeKey)) {
          for (IssueTree issue : graph.getTimeUserDetails().get(timeKey)) {
            prefs.showIssues.add(issue.getKey());
          }
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

    Set<Teams.UserTimeKey> addedAlready = new TreeSet<Teams.UserTimeKey>();
    for (Teams.AssigneeKey userKey : graph.getAssignedUserDetails().keySet()) {
      if (showTeamId.equals(userKey.getTeamId())) {
        Teams.UserTimeKey timeKey = graph.getAllocatedUser(userKey);
        if (!addedAlready.contains(timeKey)) {
          for (IssueTree issue : graph.getTimeUserDetails().get(timeKey)) {
            prefs.showIssues.add(issue.getKey());
          }
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
      for (IssueTree issue : graph.getAssignedUserDetails().get(showUsersInOneRow_[useri])) {
        prefs.showIssues.add(issue.getKey());
      }
    }

    return prefs;
  }


  public static TimeScheduleDisplayPreferences createForIssues
    (int timeGranularity_, int timeMarker_, boolean showBlocked_,
     boolean hideDetails_, boolean showResolved_, String[] issueKeys,
     boolean showChangeTools_, IssueDigraph graph) {
    TimeScheduleDisplayPreferences prefs =
      new TimeScheduleDisplayPreferences
      (timeGranularity_, timeMarker_, showBlocked_, hideDetails_, showResolved_, 
       true, showChangeTools_);

    for (int keyi = 0; keyi < issueKeys.length; keyi++) {
      if (graph.getIssueSchedule(issueKeys[keyi]) != null) {
        prefs.showIssues.add(issueKeys[keyi]);
      }
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
        prefs.showIssues.add(issue.getKey());
      }
    }

    return prefs;
  }

  public static TimeScheduleDisplayPreferences createForCriticalPath
    (int timeGranularity_, int timeMarker_, boolean hideDetails_, 
     boolean showChangeTools_, String issueKey, IssueDigraph graph) {
    
    TimeScheduleDisplayPreferences prefs =
      new TimeScheduleDisplayPreferences
      (timeGranularity_, timeMarker_, false, hideDetails_, false, 
          false, showChangeTools_);

    // just show the issues in the critical path
    IssueTree issue = graph.getIssueTree(issueKey);
    for (IssueTree nextIssue : TimeScheduleSearch.criticalPathFor(issue, graph).collectPostOrderButPreOrderForSubtasks()) {
      prefs.showIssues.add(nextIssue.getKey());
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
       && (showIssues.indexOf(issue.getKey()) > -1
           // or we're showing the full hierarchy
           || (showHierarchically)));// && !shownAlready.contains(issue.getKey()))));
    if (log4jLog.isDebugEnabled()) {
      log4jLog.debug("Display " + issue.getKey() + " = " + displayOK 
                     + ": (showResolved = " + showResolved
                     + " || issue is unresolved = " + (!issue.getResolved())
                     + " || issue has unresolved subtasks = " + !issue.allSubtasksResolved()
                     + ") && (it's an issue to show = " + (showIssues.indexOf(issue.getKey()) > -1)
                     + " || showHierarchically = " + showHierarchically
                     + ")");
    }
    return displayOK;
  }
  
  
  
  /**
   * For our serializing convenience, so we don't muck up the enclosing class with these variabilities.
   */
  public static class Pojo {
    public int timeGranularity;
    public int timeMarker;
    public boolean showBlocked;
    public boolean hideDetails;
    public boolean showResolved;
    public boolean showChangeTools;
    public boolean showHierarchically;
    public List<Teams.AssigneeKey> showUsersInOneRow = new ArrayList<Teams.AssigneeKey>();
    public List<String> showIssues = new ArrayList<String>();
    public int getTimeGranularity() {
      return timeGranularity;
    }
    public void setTimeGranularity(int timeGranularity) {
      this.timeGranularity = timeGranularity;
    }
    public int getTimeMarker() {
      return timeMarker;
    }
    public void setTimeMarker(int timeMarker) {
      this.timeMarker = timeMarker;
    }
    public boolean isShowBlocked() {
      return showBlocked;
    }
    public void setShowBlocked(boolean showBlocked) {
      this.showBlocked = showBlocked;
    }
    public boolean isHideDetails() {
      return hideDetails;
    }
    public void setHideDetails(boolean hideDetails) {
      this.hideDetails = hideDetails;
    }
    public boolean isShowResolved() {
      return showResolved;
    }
    public void setShowResolved(boolean showResolved) {
      this.showResolved = showResolved;
    }
    public boolean isShowChangeTools() {
      return showChangeTools;
    }
    public void setShowChangeTools(boolean showChangeTools) {
      this.showChangeTools = showChangeTools;
    }
    public boolean isShowHierarchically() {
      return showHierarchically;
    }
    public void setShowHierarchically(boolean showHierarchically) {
      this.showHierarchically = showHierarchically;
    }
    public List<Teams.AssigneeKey> getShowUsersInOneRow() {
      return showUsersInOneRow;
    }
    public void setShowUsersInOneRow(List<Teams.AssigneeKey> showUsersInOneRow) {
      this.showUsersInOneRow = showUsersInOneRow;
    }
    public List<String> getShowIssues() {
      return showIssues;
    }
    public void setShowIssues(List<String> showIssues) {
      this.showIssues = showIssues;
    }
    public TimeScheduleDisplayPreferences getPrefs() {
      return new TimeScheduleDisplayPreferences(timeGranularity, timeMarker, showBlocked, hideDetails, showResolved, showHierarchically, showChangeTools, showUsersInOneRow, showIssues);
    }
  }
  
}



