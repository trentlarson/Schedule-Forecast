package com.trentlarson.forecast.core.scheduling;

import java.util.*;

import com.trentlarson.forecast.core.scheduling.TimeSchedule.IssueWorkDetail;

public class IssueTree extends TimeSchedule.IssueWorkDetailOriginal<IssueTree> implements Comparable<IssueTree> {
  private String rawPerson; // (REFACTOR: store a Teams.AssigneeKey instead of separate person & team ID)
  private Long rawTeamId;
  private String timePerson; // (REFACTOR: store a Teams.UserTimeKey instead of separate person & team ID)
  private Long timeTeamId;
  private int spent, estimateOrig, priorityOrig;
  private Date dueDateOrig;
  private boolean resolved;
  // issues that "cannot be done until" this one
  private Set<IssueTree> dependents = new TreeSet<IssueTree>();

  /**
   * 
   * @param key_
   * @param summary_
   * @param person
   * @param teamId
   * @param est_ total estimage in seconds
   * @param spent_ total time spent in seconds
   * @param maxHoursPerWeek_ max hours to spend per week; if <= 0.0, use the full estimate
   * @param dueDate_
   * @param mustStartOnDate_ earliest date to start issue (may be null)
   * @param priority_
   * @param resolved_
   */
  public IssueTree(String key_, String summary_, String person, Long teamId, 
                   int est_, int spent_, double maxHoursPerWeek_, Date dueDate_, Date mustStartOnDate_, int priority_,
                   boolean resolved_) {
    super(key_,
          new Teams.UserTimeKey(teamId, person).toString(),
          summary_, est_, maxHoursPerWeek_,
          dueDate_, mustStartOnDate_, priority_);
    this.rawPerson = person;
    this.rawTeamId = teamId;
    this.timePerson = person;
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
  public Long getRawAssignedTeamId() { return rawTeamId; }
  public String getAssignedTimePerson() { return timePerson; }
  public Long getAssignedTimeTeamId() { return timeTeamId; }

  public Teams.AssigneeKey getRawAssigneeKey() {
    return new Teams.AssigneeKey(rawTeamId, rawPerson);
  }
  public Teams.UserTimeKey getTimeAssigneeKey() {
    return new Teams.UserTimeKey(timeTeamId, timePerson);
  }

  public int  getTimeSpent() { return spent; }
  /** @see IssueWorkDetail#getPriority() */
  public int  getPriorityOrig() { return priorityOrig; }
  public int getEstimateOrig() { return estimateOrig; }
  public int getSecsPerWeek() { return secsPerWeek; }
  public Date getDueDateOrig() { return dueDateOrig; }
  public boolean getResolved() { return resolved; }
  public Set<IssueTree>  getDependents() { return dependents; }

  /**
     Used by loader to change the time assignment for this issue.
   */
  public void setTimeAssigneeKey(Teams.UserTimeKey assignee) {
    this.timePerson = assignee.getUsername();
    this.timeTeamId = assignee.getTeamId();
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
  public int compareTo(IssueTree object) {
    return getKey().compareTo(((IssueTree) object).getKey());
  }
  public String toString() {
    return "IssueTree " + key;
  }

  public static StringBuffer treeString(Iterator<IssueTree> treeIter) {
    StringBuffer sb = new StringBuffer();
    for (; treeIter.hasNext(); ) {
      sb.append((treeIter.next()).treeString());
    }
    return sb;
  }
  public String treeString() {
    StringBuffer preds = new StringBuffer();
    for (Iterator<IssueTree> iter = precursors.iterator(); iter.hasNext(); ) {
      if (preds.length() > 0) {
        preds.append(",");
      }
      preds.append((iter.next()).getKey());
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
  public Date getBeginDate(Map<String,TimeSchedule.IssueSchedule<IssueTree>> issueSchedules) {
    return ((TimeSchedule.IssueSchedule<IssueTree>) issueSchedules.get(getKey())).getAdjustedBeginCal().getTime();
  }

  /**
     @return the maximum end date of sub- and dependent issues
     (including this issue),
     or the minimum date allowed if no issues match display filtering
     @param dPrefs is the preferences for filtering whether to use
     issues (see displayIssue), or null if we want to check all issues
  */
  public Date findMaxDateOfSubsAndDeps(Map<String, TimeSchedule.IssueSchedule<IssueTree>> issueSchedules,
                                       TimeScheduleDisplayPreferences dPrefs) {
    Date max = (issueSchedules.get(getKey())).getAdjustedEndCal().getTime();            
    if (dPrefs != null
        && !dPrefs.displayIssue(this)) {
      max = new Date(Long.MIN_VALUE);
    }
    for (Iterator<IssueTree> i = subtasks.iterator(); i.hasNext(); ) {
      Date subMax = (i.next()).findMaxDateOfSubsAndDeps(issueSchedules, dPrefs);
      if (max.compareTo(subMax) < 0) { max = subMax; }
    }
    for (Iterator<IssueTree> i = dependents.iterator(); i.hasNext(); ) {
      Date subMax = (i.next()).findMaxDateOfSubsAndDeps(issueSchedules, dPrefs);
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
  public Date findMaxDateOfDeps(Map<String, TimeSchedule.IssueSchedule<IssueTree>> issueSchedules,
                                TimeScheduleDisplayPreferences dPrefs) {
    Date max = (issueSchedules.get(getKey())).getAdjustedEndCal().getTime();
    if (dPrefs != null
        && !dPrefs.displayIssue(this)) {
      max = new Date(Long.MIN_VALUE);
    }
    for (Iterator<IssueTree> i = dependents.iterator(); i.hasNext(); ) {
      Date subMax = (i.next()).findMaxDateOfSubs(issueSchedules, dPrefs);
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
  public Date findMaxDateOfSubs(Map<String, TimeSchedule.IssueSchedule<IssueTree>> issueSchedules,
                                TimeScheduleDisplayPreferences dPrefs) {
    Date max = (issueSchedules.get(getKey())).getAdjustedEndCal().getTime();
    if (dPrefs != null
        && !dPrefs.displayIssue(this)) {
      max = new Date(Long.MIN_VALUE);
    }
    for (Iterator<IssueTree> i = subtasks.iterator(); i.hasNext(); ) {
      Date subMax = (i.next()).findMaxDateOfSubs(issueSchedules, dPrefs);
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
  public Date findMinDateOfSubs(Map<String, TimeSchedule.IssueSchedule<IssueTree>> issueSchedules,
                                TimeScheduleDisplayPreferences dPrefs) {
    Date min = (issueSchedules.get(getKey())).getAdjustedBeginCal().getTime();
    if (dPrefs != null
        && !dPrefs.displayIssue(this)) {
      min = new Date(Long.MAX_VALUE);
    }
    for (Iterator<IssueTree> i = subtasks.iterator(); i.hasNext(); ) {
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
    for (Iterator<IssueTree> i = subtasks.iterator(); i.hasNext(); ) {
      IssueTree tree = i.next();
      total += tree.totalEstimate();
    }
    return total > getEstimate() ? total : getEstimate();
  }
  /** @return the time spent on this issue plus sub-issues, whichever is greater */
  public int totalTimeSpent() {
    int total = spent;
    for (Iterator<IssueTree> i = subtasks.iterator(); i.hasNext(); ) {
      total += (i.next()).totalTimeSpent();
    }
    return total;
  }

  public boolean allSubtasksResolved() {
    boolean allResolved = true;
    for (Iterator<IssueTree> i = subtasks.iterator(); i.hasNext() && allResolved; ) {
      IssueTree tree = (IssueTree) i.next();
      allResolved = tree.getResolved() && tree.allSubtasksResolved();
    }
    return allResolved;
  }

  /**
     Set each priority key in maxDateForPriority to the maximum of it's previous
     setting and end dates, looking this priority and all subtasks
     (and dependents if includeBlocked is on).

     @param maxDateForPriority is modified
     @param dPrefs is the preferences for filtering whether to use
     issues (see displayIssue), or null if we want to check all issues
   */
  protected void setPriorityCompleteDates(Map<Integer,Date> maxDateForPriority, IssueDigraph graph,
                                          TimeScheduleDisplayPreferences dPrefs) {
    TimeSchedule.IssueSchedule<IssueTree> sched = graph.getIssueSchedules().get(getKey());
    if (dPrefs == null
        || dPrefs.displayIssue(this)) {
      if (maxDateForPriority.get(getPriority()) == null
          || maxDateForPriority.get(getPriority()).before(sched.getEndDate())) {
        maxDateForPriority.put(getPriority(), sched.getEndDate());
      }
    }
    for (Iterator<IssueTree> i = getSubtasks().iterator(); i.hasNext(); ) {
      (i.next()).setPriorityCompleteDates(maxDateForPriority, graph, dPrefs);
    }
    for (Iterator<IssueTree> i = getDependents().iterator(); i.hasNext(); ) {
      (i.next()).setPriorityCompleteDates(maxDateForPriority, graph, dPrefs);
    }
  }
}

