package com.trentlarson.forecast.core.scheduling;

import java.util.*;

public class IssueTree extends TimeSchedule.IssueWorkDetailOriginal implements Comparable {
  private String rawPerson, rawTeamName; // (REFACTOR: store a Teams.AssigneeKey instead of separate person & team ID)
  private Long rawTeamId;
  private String timePerson, timeTeamName; // (REFACTOR: store a Teams.UserTimeKey instead of separate person & team ID)
  private Long timeTeamId;
  private int spent, estimateOrig, priorityOrig;
  private Date dueDateOrig;
  private boolean resolved;
  // issues that "cannot be done until" this one
  private Set<IssueTree> dependents = new TreeSet<IssueTree>();

  public IssueTree(String key_, String summary_, String person,
                   Long teamId, String teamName, int est_,
                   int spent_, Date dueDate_, Date mustStartOnDate_, int priority_,
                   boolean resolved_) {
    super(key_,
          new Teams.UserTimeKey(teamId, person).toString(),
          summary_, est_, dueDate_,
          mustStartOnDate_, priority_);
    this.rawPerson = person;
    this.rawTeamName = teamName;
    this.rawTeamId = teamId;
    this.timePerson = person;
    this.timeTeamName = teamName;
    this.timeTeamId = teamId;
    this.spent = spent_;
    this.estimateOrig = est_;
    this.dueDateOrig = dueDate_;
    this.priorityOrig = priority_;
    this.resolved = resolved_;
    if (resolved_) {
      this.issueEstSecondsRaw = 0;
    }
  }

  public String getRawAssignedPerson() { return rawPerson; }
  public String getRawAssignedTeamName() { return rawTeamName; }
  public Long getRawAssignedTeamId() { return rawTeamId; }
  public String getAssignedTimePerson() { return timePerson; }
  public String getAssignedTimeTeamName() { return timeTeamName; }
  public Long getAssignedTimeTeamId() { return timeTeamId; }

  public Teams.AssigneeKey getRawAssigneeKey() {
    return new Teams.AssigneeKey(rawTeamId, rawPerson);
  }
  public Teams.UserTimeKey getTimeAssigneeKey() {
    return new Teams.UserTimeKey(timeTeamId, timePerson);
  }
  public String getTimeAssigneeWithTeamName() {
    return Teams.UserTimeKey.toString(timeTeamName, timePerson, false);
  }

  public int  getTimeSpent() { return spent; }
  public int  getPriorityOrig() { return priorityOrig; }
  public int getEstimateOrig() { return estimateOrig; }
  public Date getDueDateOrig() { return dueDateOrig; }
  public boolean getResolved() { return resolved; }
  public Set<TimeSchedule.IssueWorkDetail>  getSubtasks() { return subtasks; }
  public Set<IssueTree>  getDependents() { return dependents; }

  /**
     Used by loader to change the time assignment for this issue.
   */
  protected void setTimeAssigneeKey(Teams.UserTimeKey assignee) {
    this.timePerson = assignee.getUsername();
    this.timeTeamId = assignee.getTeamId();
    // REFACTOR to pull the real team name (multiple instances in these files)
    this.timeTeamName =
      IssueLoader.teamKeyFromIssueKey(assignee.getTeamId(), getKey());
    this.timeAssignee = assignee.toString();
  }

  public void setPriority(int priority_) { this.priority = priority_; }
  public void setEstimate(int estimate_) { this.issueEstSecondsRaw = estimate_; }
  public void setDueDate(Date dueDate_) { this.dueDate = dueDate_; }
  public void saveNewValues() {
    estimateOrig = issueEstSecondsRaw;
    dueDateOrig = dueDate;
    priorityOrig = priority;
  }
  public boolean equals(Object object) {
    return key.equals(((IssueTree)object).key);
  }
  public int compareTo(Object object) {
    return getKey().compareTo(((IssueTree) object).getKey());
  }
  public String toString() {
    return "IssueTree " + key;
  }

  public static StringBuffer treeString(Iterator treeIter) {
    StringBuffer sb = new StringBuffer();
    for (; treeIter.hasNext(); ) {
      sb.append(((IssueTree) treeIter.next()).treeString());
    }
    return sb;
  }
  public String treeString() {
    StringBuffer preds = new StringBuffer();
    for (Iterator iter = precursors.iterator(); iter.hasNext(); ) {
      if (preds.length() > 0) {
        preds.append(",");
      }
      preds.append(((IssueTree) iter.next()).getKey());
    }
    return
      ("<li>" + (resolved ? "<strike>" : "") + key + (resolved ? "</strike>" : "")
       + " " + summary + " " + getRawAssigneeKey() + " " + spent + "+" + getEstimate()
       + " " + mustStartOnDate + " " + dueDate
       + "<br> ---- <ul>" + treeString(subtasks.iterator()) + "</ul>"
       + "<br> >>>> <ul>" + treeString(dependents.iterator()) + "</ul>"
       + "<br> <<<< " + preds
       + "</li>");
  }

  public void addSubtask(IssueTree issue) {
    subtasks.add(issue);
  }
  public void addDependent(IssueTree issue) {
    dependents.add(issue);
    issue.getPrecursors().add(this);
  }
  public void addPrecursor(IssueTree issue) {
    precursors.add(issue);
    issue.getDependents().add(this);
  }

  /** @return the maximum end date of sub- and dependent issues (including this issue) */
  public Date getBeginDate(Map issueSchedules) {
    return ((TimeSchedule.IssueSchedule) issueSchedules.get(getKey())).getAdjustedBeginCal().getTime();
  }

  /**
     @return the maximum end date of sub- and dependent issues
     (including this issue),
     or the minimum date allowed if no issues match display filtering
     @param dPrefs is the preferences for filtering whether to use
     issues (see displayIssue), or null if we want to check all issues
  */
  public Date findMaxDateOfSubsAndDeps(Map<String, TimeSchedule.IssueSchedule> issueSchedules,
                                       TimeScheduleDisplayPreferences dPrefs) {
    Date max = (issueSchedules.get(getKey())).getAdjustedEndCal().getTime();            
    if (dPrefs != null
        && !dPrefs.displayIssue(this)) {
      max = new Date(Long.MIN_VALUE);
    }
    for (Iterator i = subtasks.iterator(); i.hasNext(); ) {
      Date subMax = ((IssueTree) i.next()).findMaxDateOfSubsAndDeps(issueSchedules, dPrefs);
      if (max.compareTo(subMax) < 0) { max = subMax; }
    }
    for (Iterator i = dependents.iterator(); i.hasNext(); ) {
      Date subMax = ((IssueTree) i.next()).findMaxDateOfSubsAndDeps(issueSchedules, dPrefs);
      if (max.compareTo(subMax) < 0) { max = subMax; }
    }
    return max;
  }
  /**
     @return the maximum end date of dependent tasks (including this issue),
     or the minimum date allowed if no issues match display filtering
     @param dPrefs is the preferences for filtering whether to use
     issues (see displayIssue), or null if we want to check all issues
  */
  public Date findMaxDateOfDeps(Map<String, TimeSchedule.IssueSchedule> issueSchedules,
                                TimeScheduleDisplayPreferences dPrefs) {
    Date max = (issueSchedules.get(getKey())).getAdjustedEndCal().getTime();
    if (dPrefs != null
        && !dPrefs.displayIssue(this)) {
      max = new Date(Long.MIN_VALUE);
    }
    for (Iterator i = dependents.iterator(); i.hasNext(); ) {
      Date subMax = ((IssueTree) i.next()).findMaxDateOfSubs(issueSchedules, dPrefs);
      if (max.compareTo(subMax) < 0) { max = subMax; }
    }
    return max;
  }
  /**
     @return the maximum end date of sub-tasks (including this issue),
     or the minimum date allowed if no issues match display filtering
     @param dPrefs is the preferences for filtering whether to use
     issues (see displayIssue), or null if we want to check all issues
  */
  public Date findMaxDateOfSubs(Map<String, TimeSchedule.IssueSchedule> issueSchedules,
                                TimeScheduleDisplayPreferences dPrefs) {
    Date max = (issueSchedules.get(getKey())).getAdjustedEndCal().getTime();
    if (dPrefs != null
        && !dPrefs.displayIssue(this)) {
      max = new Date(Long.MIN_VALUE);
    }
    for (Iterator i = subtasks.iterator(); i.hasNext(); ) {
      Date subMax = ((IssueTree) i.next()).findMaxDateOfSubs(issueSchedules, dPrefs);
      if (max.compareTo(subMax) < 0) { max = subMax; }
    }
    return max;
  }
  /**
     @return the minimum end date of sub-tasks (including this issue),
     or the maximum date allowed if no issues match display filtering
     @param dPrefs is the preferences for filtering whether to use
     issues (see displayIssue), or null if we want to check all issues
  */
  public Date findMinDateOfSubs(Map<String, TimeSchedule.IssueSchedule> issueSchedules,
                                TimeScheduleDisplayPreferences dPrefs) {
    Date min = (issueSchedules.get(getKey())).getAdjustedBeginCal().getTime();
    if (dPrefs != null
        && !dPrefs.displayIssue(this)) {
      min = new Date(Long.MAX_VALUE);
    }
    for (Iterator i = subtasks.iterator(); i.hasNext(); ) {
      Date subMin = ((IssueTree) i.next()).findMinDateOfSubs(issueSchedules, dPrefs);
      if (min.compareTo(subMin) > 0) { min = subMin; }
    }
    return min;
  }
  /**
     @return the estimate remaining on this issue or on sub-issues,
     whichever is greater
     @param dPrefs is the preferences for filtering whether to use
     issues (see displayIssue), or null if we want to check all issues
  */
  public int totalEstimate() {
    int total = 0;
    for (Iterator i = subtasks.iterator(); i.hasNext(); ) {
      IssueTree tree = (IssueTree) i.next();
      total += tree.totalEstimate();
    }
    return total > getEstimate() ? total : getEstimate();
  }
  /** @return the time spent on this issue plus sub-issues, whichever is greater */
  public int totalTimeSpent() {
    int total = spent;
    for (Iterator i = subtasks.iterator(); i.hasNext(); ) {
      total += ((IssueTree) i.next()).totalTimeSpent();
    }
    return total;
  }

  public boolean allSubtasksResolved() {
    boolean allResolved = true;
    for (Iterator i = subtasks.iterator(); i.hasNext() && allResolved; ) {
      IssueTree tree = (IssueTree) i.next();
      allResolved = tree.getResolved() && tree.allSubtasksResolved();
    }
    return allResolved;
  }

  /**
     Set the priorityMax record to the maximum of it's previous
     setting and end dates, looking this priority and all subtasks
     (and dependents if includeBlocked is on).

     @param dPrefs is the preferences for filtering whether to use
     issues (see displayIssue), or null if we want to check all issues
   */
  protected void setPriorityCompleteDates(Date[] priorityMax, IssueDigraph graph,
                                          TimeScheduleDisplayPreferences dPrefs) {
    TimeSchedule.IssueSchedule sched =
      (TimeSchedule.IssueSchedule) graph.getIssueSchedules().get(getKey());
    if (dPrefs == null
        || dPrefs.displayIssue(this)) {
      if (priorityMax[getPriority() - 1].before(sched.getEndDate())) {
        priorityMax[getPriority() - 1] = sched.getEndDate();
      }
    }
    for (Iterator i = getSubtasks().iterator(); i.hasNext(); ) {
      ((IssueTree) i.next()).setPriorityCompleteDates(priorityMax, graph, dPrefs);
    }
    for (Iterator i = getDependents().iterator(); i.hasNext(); ) {
      ((IssueTree) i.next()).setPriorityCompleteDates(priorityMax, graph, dPrefs);
    }
  }
}

