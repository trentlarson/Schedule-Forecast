package com.trentlarson.forecast.core.scheduling;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TimeScheduleSearch {

  /**
   * to keep track of branches when displaying precedessors of the selection
   */
  protected static class DependencyBranch<T extends TimeSchedule.IssueWorkDetail<T>> {
    private List<T> precursorsInOrder;
    private boolean hasMore;
    private int shortestDistanceFromTarget;
    public DependencyBranch(List<T> precursorsInOrder_,
                            boolean hasMore_, int shortestDistanceFromTarget_) {
      this.precursorsInOrder = precursorsInOrder_;
      this.hasMore = hasMore_;
      this.shortestDistanceFromTarget = shortestDistanceFromTarget_;
    }
    /** @return the list of issues in this branch, from first to last */
    public List<T> getBranchList() {
      return precursorsInOrder;
    }
    /** @return the first node in this branch */
    public String getPreviousBranchPrecursor() {
      return precursorsInOrder.get(0).getKey();
    }
    /** @return the last node in this branch */
    public String getNextBranchDependant() {
      return precursorsInOrder.get(precursorsInOrder.size() - 1).getKey();
    }
    /** @return whether there were more precursors, not included because they were already identified in other branches */
    public boolean getHasMore() {
      return hasMore;
    }
    /** @return how many nodes are in this branch, including first and last */
    public int getBranchLength() {
      return precursorsInOrder.size();
    }
    /** @return how far away is the 'previous' node from the target, including it and not including target */
    public int getLongestDistanceFromTarget() {
      return shortestDistanceFromTarget + getBranchLength() - 1;
    }
    /** @return how far away is the 'next' node from the target, including it but not including target */
    public int getShortestDistanceFromTarget() {
      return shortestDistanceFromTarget;
    }
    public String toString() {
      return (getHasMore() ? "..>" : "")
        + getPreviousBranchPrecursor() + ">(" + (getBranchLength() - 2) + ")>"
        + getNextBranchDependant() + "(" + getShortestDistanceFromTarget() + ")";
    }
  }
  protected static <T extends TimeSchedule.IssueWorkDetail<T>> List<DependencyBranch<T>> findPredecessorBranches(T tree) {
    return findPredecessorBranches2(tree, new HashSet<String>(), 0);
  }
  /**
     @param precursorsFound Set of Strings for each issue pkey to not check
     @return List of DependencyBranch objects, with leaves first and the trunk as the last element
  */
  private static <T extends TimeSchedule.IssueWorkDetail<T>> List<DependencyBranch<T>> findPredecessorBranches2
    (T tree, Set<String> precursorsFound, int shortestDistanceFromTarget) {

    List<DependencyBranch<T>> branchesDiscovered = new ArrayList<DependencyBranch<T>>();
    for (T precursor : tree.getPrecursors()) {

      List<T> precursorsInBranch = new ArrayList<T>();
      precursorsInBranch.add(tree);

      precursorsInBranch.add(0, precursor);
      // walk backwards until we find one with <> 1 precursor
      int branchLength = 2;
      while (precursor.getPrecursors().size() == 1
             && !precursorsFound.contains(precursor.getKey())) {
        precursorsFound.add(precursor.getKey());
        branchLength++;
        precursor = (T) precursor.getPrecursors().iterator().next();
        precursorsInBranch.add(0, precursor);
      }
      // add more branches if this one has more predecessors
      if (precursor.getPrecursors().size() > 1
          && !precursorsFound.contains(precursor.getKey())) {
        List<DependencyBranch<T>> moreBranches =
          findPredecessorBranches2(precursor, precursorsFound, shortestDistanceFromTarget + branchLength - 1);
        branchesDiscovered.addAll(moreBranches);
      }
      boolean hasMore =
        precursor.getPrecursors().size() > 0
        && precursorsFound.contains(precursor.getKey());
      precursorsFound.add(precursor.getKey());
      // now add the current one as a branch of the parent
      DependencyBranch<T> thisBranch = new DependencyBranch<T>(precursorsInBranch, hasMore, shortestDistanceFromTarget);
      branchesDiscovered.add(thisBranch);
    }
    return branchesDiscovered;
  }



  /**
   * 
   * An issue, along with the reasons it is scheduled at this particular time.
   * 
   */
  protected static class CriticalPath {
    public final IssueTree issue;
    public boolean becauseOfstartDate = false;
    /** Any issues that immediately precede this one in priority scheduling. */
    public final List<CriticalPath> previousPriority = new ArrayList<CriticalPath>();
    /** Any issues that immediately precede this one in scheduling and are subtasks. */
    public final List<CriticalPath> subtasks = new ArrayList<CriticalPath>();
    /** Any issues that immediately precede this one in scheduling and are predecessors. */
    public final List<CriticalPath> precursors = new ArrayList<CriticalPath>();
    
    public CriticalPath(IssueTree _issue) {
      this.issue = _issue;
    }
    public String toString() {
      String result = "Critical Path for issue " + issue.getKey() + ":";
      boolean hasPath = false;
      if (becauseOfstartDate) {
        result += " has start date";
        hasPath = true;
      }
      if (previousPriority.size() > 0) {
        if (hasPath) {
          result += " &";
        }
        result += " follows in priority " + previousPriority;
        hasPath = true;
      }
      if (subtasks.size() > 0) {
        if (hasPath) {
          result += " &";
        }
        result += " follows subtask(s) " + subtasks;
        hasPath = true;
      }
      if (precursors.size() > 0) {
        if (hasPath) {
          result += " &";
        }
        result += " follows precursor(s) " + precursors;
        hasPath = true;
      }
      if (!hasPath) {
        result += " none";
      }
      return result;
    }
    
    /**
     * 
     * @return the path in post-order traversal, except for subtasks which are done in pre-order; never null
     */
    public List<IssueTree> collectPostOrderButPreOrderForSubtasks() {
      List<IssueTree> result = new ArrayList<IssueTree>();
      for (CriticalPath path : previousPriority) {
        result.addAll(path.collectPostOrderButPreOrderForSubtasks());
      }
      for (CriticalPath path : precursors) {
        result.addAll(path.collectPostOrderButPreOrderForSubtasks());
      }
      // now add the issue, after previous priorities and precursors but before subtasks
      result.add(issue);
      for (CriticalPath path : subtasks) {
        result.addAll(path.collectPostOrderButPreOrderForSubtasks());
      }
      return result;
    }
  }

  
  /**
   * 
   * @param issueKey
   * @param graph
   * @return the entire critical path for this issue (possibly including duplicate sub-trees); never null
   */
  public static CriticalPath criticalPathFor(IssueTree issue, IssueDigraph graph) {
    return criticalPathFor2(issue, graph, new ArrayList<String>());
  }
  public static CriticalPath criticalPathFor2(IssueTree issue, IssueDigraph graph, List<String> visited) {
    
    visited.add(issue.getKey());
    
    // approach: check other user tasks, predecessors, and dependents and see which end immediately before this one.
    
    CriticalPath result = new CriticalPath(issue);
    
    TimeSchedule.IssueSchedule<IssueTree> schedule = graph.getIssueSchedule(issue.getKey());
    
    // check start date
    if (issue.getMustStartOnDate() != null
        && issue.getMustStartOnDate().equals(schedule.getBeginDate())) {
      result.becauseOfstartDate = true;
    }
        
    // check for ones assigned to this user/team that are immediately preceding
    Date issueBeginDate = schedule.getAdjustedBeginCal().getTime();
    List<IssueTree> issueList = graph.getTimeUserDetails().get(issue.getTimeAssigneeKey());
    for (int previousIndex = issueList.indexOf(issue) - 1; previousIndex > -1; previousIndex--) {
      TimeSchedule.IssueSchedule<IssueTree> prevSchedule = graph.getIssueSchedule(issueList.get(previousIndex).getKey());
      if (!visited.contains(prevSchedule.getIssue().getKey())) {
        if (prevSchedule.getAdjustedNextBeginDate().equals(issueBeginDate)) {
          result.previousPriority.add(criticalPathFor2(issueList.get(previousIndex), graph, visited));
        }
      }
    }
      
    // check for subtasks that are immediately preceding
    for (IssueTree subtask : issue.getSubtasks()) {
      if (!visited.contains(subtask.getKey())) {
        if (graph.getIssueSchedule(subtask.getKey()).getAdjustedNextBeginDate().equals(issueBeginDate)) {
          result.subtasks.add(criticalPathFor2(subtask, graph, visited));
        }
      }
    }
    
    // check for precursors that are immediately preceding
    for (IssueTree precursor : issue.getPrecursors()) {
      if (!visited.contains(precursor.getKey())) {
        if (graph.getIssueSchedule(precursor.getKey()).getAdjustedNextBeginDate().equals(issueBeginDate)) {
          result.precursors.add(criticalPathFor2(precursor, graph, visited));
        }
      }
    }
    
    return result;
    
  }
  
}
