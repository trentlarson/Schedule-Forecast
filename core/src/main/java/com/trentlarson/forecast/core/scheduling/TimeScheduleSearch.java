package com.trentlarson.forecast.core.scheduling;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class TimeScheduleSearch {

  /**
   * to keep track of branches when displaying precedessors of the selection
   */
  protected static class DependencyBranch {
    private List<TimeSchedule.IssueWorkDetail> precursorsInOrder;
    private boolean hasMore;
    private int shortestDistanceFromTarget;
    public DependencyBranch(List<TimeSchedule.IssueWorkDetail> precursorsInOrder_,
                            boolean hasMore_, int shortestDistanceFromTarget_) {
      this.precursorsInOrder = precursorsInOrder_;
      this.hasMore = hasMore_;
      this.shortestDistanceFromTarget = shortestDistanceFromTarget_;
    }
    /** @return the list of issues in this branch, from first to last */
    public List<TimeSchedule.IssueWorkDetail> getBranchList() {
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
  protected static List findPredecessorBranches(TimeSchedule.IssueWorkDetail tree) {
    return findPredecessorBranches2(tree, new HashSet(), 0);
  }
  /**
     @param precursorsFound Set of Strings for each issue pkey to not check
     @return List of DependencyBranch objects, with leaves first and the trunk as the last element
  */
  private static List<DependencyBranch> findPredecessorBranches2
    (TimeSchedule.IssueWorkDetail tree, Set<String> precursorsFound, int shortestDistanceFromTarget) {

    List<DependencyBranch> branchesDiscovered = new ArrayList<DependencyBranch>();
    for (TimeSchedule.IssueWorkDetail precursor : tree.getPrecursors()) {

      List<TimeSchedule.IssueWorkDetail> precursorsInBranch =
        new ArrayList<TimeSchedule.IssueWorkDetail>();
      precursorsInBranch.add(tree);

      precursorsInBranch.add(0, precursor);
      // walk backwards until we find one with <> 1 precursor
      int branchLength = 2;
      while (precursor.getPrecursors().size() == 1
             && !precursorsFound.contains(precursor.getKey())) {
        precursorsFound.add(precursor.getKey());
        branchLength++;
        precursor = (TimeSchedule.IssueWorkDetail) precursor.getPrecursors().iterator().next();
        precursorsInBranch.add(0, precursor);
      }
      // add more branches if this one has more predecessors
      if (precursor.getPrecursors().size() > 1
          && !precursorsFound.contains(precursor.getKey())) {
        List<DependencyBranch> moreBranches =
          findPredecessorBranches2(precursor, precursorsFound, shortestDistanceFromTarget + branchLength - 1);
        branchesDiscovered.addAll(moreBranches);
      }
      boolean hasMore =
        precursor.getPrecursors().size() > 0
        && precursorsFound.contains(precursor.getKey());
      precursorsFound.add(precursor.getKey());
      // now add the current one as a branch of the parent
      DependencyBranch thisBranch =
        new DependencyBranch(precursorsInBranch, hasMore, shortestDistanceFromTarget);
      branchesDiscovered.add(thisBranch);
    }
    return branchesDiscovered;
  }


}
