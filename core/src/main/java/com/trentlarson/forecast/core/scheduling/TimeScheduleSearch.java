package com.trentlarson.forecast.core.scheduling;

import java.util.ArrayList;
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


}
