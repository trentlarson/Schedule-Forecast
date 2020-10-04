package com.trentlarson.forecast.core.helper;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.stream.Collectors;

import com.trentlarson.forecast.core.scheduling.IssueDigraph;
import com.trentlarson.forecast.core.scheduling.IssueTree;
import com.trentlarson.forecast.core.scheduling.TimeScheduleCreatePreferences;
import com.trentlarson.forecast.core.scheduling.TimeScheduleCreatePreferencesForGsonInput;
import com.trentlarson.forecast.core.scheduling.TimeScheduleDisplayPreferences;

public class InputInterfaces {

  public class ScheduleAndDisplayInput {
    public IssueTree[] issues;
    public TimeScheduleCreatePreferencesForGsonInput createPreferences;
    public TimeScheduleDisplayPreferences displayPreferences;

    public ScheduleAndDisplayAfterChecks check() {

      // walk the input and link any dependents / subtasks at the top level of the list for scheduling
      Set<IssueTree> inputIssueSet = linkAllAtTopOfList(Arrays.asList(issues));
      setAllPrecursors(inputIssueSet);
      IssueTree[] inputIssues = inputIssueSet.toArray(new IssueTree[] {});

      TimeScheduleCreatePreferences cPrefs = createPreferences != null
          ? createPreferences.getTimeScheduleCreatePreferences()
          : new TimeScheduleCreatePreferences();
      // set the hourly schedule based on input hours
      IssueDigraph graph = IssueDigraph
          .schedulesForIssues(inputIssues, createPreferences.getTimeScheduleCreatePreferences());

      TimeScheduleDisplayPreferences dPrefs = displayPreferences;
      if (dPrefs == null
          || ((dPrefs.showIssues == null
          || dPrefs.showIssues.size() == 0)
          &&
          (dPrefs.showUsersInOneRow == null
              || dPrefs.showUsersInOneRow.size() == 0))) {
        // nothing is requested to be shown, so let's be nice and show all the keys
        String[] keyArray = new String[issues.length];
        String[] keys =
            Arrays.asList(issues).stream().map(i -> i.getKey())
                .collect(Collectors.toList()).toArray(keyArray);
        if (dPrefs == null) {
          // we'll guess at reasonable defaults
          dPrefs = TimeScheduleDisplayPreferences.createForIssues(
              1, 0, true, false,
              true, keys, false, false, graph
          );
        } else {
          int timeGranularity = dPrefs.timeGranularity;
          if (timeGranularity == 0) {
            timeGranularity = 1;
          }
          dPrefs = TimeScheduleDisplayPreferences.createForIssues(
              timeGranularity, dPrefs.timeMarker, dPrefs.showBlocked, dPrefs.hideDetails,
              dPrefs.showResolved, keys, dPrefs.showChangeTools, dPrefs.embedJiraLinks, graph
          );
        }
      }

      return new ScheduleAndDisplayAfterChecks(graph, dPrefs);
    }

  }

  public class ScheduleAndDisplayAfterChecks {
    public IssueDigraph graph;
    public TimeScheduleDisplayPreferences displayPreferences;
    ScheduleAndDisplayAfterChecks(IssueDigraph graph_,
                                  TimeScheduleDisplayPreferences displayPreferences_) {
      this.graph = graph_;
      this.displayPreferences = displayPreferences_;
    }
  }


  /**
   *
   * @param issues a list of issue, with dependents & subtasks embedded
   * @return a set where all issues in the tree are listed
   */
  private static Set<IssueTree> linkAllAtTopOfList(List<IssueTree> issues) {
    SortedSet<IssueTree> masterSet = new TreeSet<IssueTree>();
    Set<IssueTree> moreToCheck = new TreeSet<IssueTree>();
    moreToCheck.addAll(issues);
    linkAllAtTopOfList(masterSet, moreToCheck);
    return masterSet;
  }
  private static void linkAllAtTopOfList(SortedSet<IssueTree> masterSet, Set<IssueTree> moreToCheck) {
    for (Iterator<IssueTree> issueIter = moreToCheck.iterator(); issueIter.hasNext();) {
      IssueTree issue = issueIter.next();
      if (!masterSet.contains(issue)) {
        masterSet.add(issue);
        linkAllAtTopOfList(masterSet, issue.getDependents());
        linkAllAtTopOfList(masterSet, issue.getPrecursors());
        linkAllAtTopOfList(masterSet, issue.getSubtasks());
      }
    }
  }

  /**
   * This is to accommodate the bad design where we have both precursors & dependents stored in IssueTree.
   *
   * @param trees set of all trees, before scheduling and possibly without precursors set
   */
  private static void setAllPrecursors(Set<IssueTree> trees) {
    for (Iterator<IssueTree> treeIter = trees.iterator(); treeIter.hasNext(); ) {
      IssueTree tree = treeIter.next();
      for (Iterator<IssueTree> depIter = tree.getDependents().iterator(); depIter.hasNext(); ) {
        depIter.next().addPrecursor(tree);
        // We don't have to recurse because all trees are at the top level (I believe).
      }
    }
  }
}
