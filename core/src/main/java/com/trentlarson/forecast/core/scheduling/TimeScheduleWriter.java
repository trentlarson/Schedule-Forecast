package com.trentlarson.forecast.core.scheduling;

import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Category;

import com.trentlarson.forecast.core.actions.TimeScheduleAction;
import com.trentlarson.forecast.core.scheduling.external.IssueLoader;

/**
Assumptions:
- There is no assignee with a 3-letter name.  We make 3-letter assignee names out of project names.
- The subtask link ID is hard-coded.
 */
public class TimeScheduleWriter {

  @SuppressWarnings("deprecation")
  public static final Category log4jLog = Category.getInstance(TimeScheduleWriter.class);


  /**
     @return the next date marking the time period given by the
     dPrefs.timeMarker interval (or null if the interval is 0)
  */
  private static Calendar createTimeMarker(int timePeriod, Calendar startCal) {
    // figure out where to put the time marker
    Calendar timeMarker = null;
    if (timePeriod > 0) {
      timeMarker = (Calendar)startCal.clone();
      if (timePeriod == Calendar.WEEK_OF_MONTH
          || timePeriod == Calendar.WEEK_OF_YEAR) {
        timeMarker.set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY);
        // if that moved it into the past, move it up a week
        if (Calendar.getInstance().after(timeMarker)) {
          timeMarker.add(Calendar.DAY_OF_WEEK, 7);
        }
      } else if (timePeriod == Calendar.MONTH) {
        timeMarker.set(Calendar.DAY_OF_MONTH, 1);
        // if that moved it into the past, move it up a month
        if (Calendar.getInstance().after(timeMarker)) {
          timeMarker.add(Calendar.MONTH, 1);
        }
      } else {
        System.err.println("The value of " + timePeriod + " is not a recognized time period.  Not putting a marker.");
      }
    }
    return timeMarker;
  }
  private static void incrementTimeMarker(int timePeriod, Calendar timeMarker) {
    if (timeMarker != null) {
      if (timePeriod == Calendar.WEEK_OF_MONTH
          || timePeriod == Calendar.WEEK_OF_YEAR) {
        timeMarker.add(Calendar.DAY_OF_WEEK, 7);
      } else if (timePeriod == Calendar.MONTH) {
        timeMarker.add(Calendar.MONTH, 1);
      }
    }
  }



  /**
     @param issueKeys List of issue keys to display
     @param graph IssueDigraph for all issues to be displayed
     @param startDate date to start scheduling
     @param dPrefs display preferences
     @return the finish date for each priority (index in array == priority - 1)
  */
  private static Date[] priorityCompleteDates
    (List<String> issueKeys, IssueDigraph graph,
     Date startDate, TimeScheduleDisplayPreferences dPrefs) {

    // report when priority levels are done
    Map<Integer,Date> maxDateForPriority = new HashMap<Integer,Date>();
    for (String issueKey : issueKeys) {
      IssueTree detail = graph.getIssueTree(issueKey);
      detail.setPriorityCompleteDates(maxDateForPriority, graph, dPrefs);
    }
    int maxPriority = graph.getMaxPriority();
    Date[] priorityDates = new Date[maxPriority > -1 ? maxPriority : 0];
    for (int i = 1; i <= graph.getMaxPriority(); i++) { // priority numbers are 1-based
      if (maxDateForPriority.get(new Integer(i)) == null) {
        priorityDates[i - 1] = startDate;
      } else {
        priorityDates[i - 1] = maxDateForPriority.get(new Integer(i));
      }
    }
    return priorityDates;
  }

  public static void writeIssueTable
    (IssueDigraph graph, Writer out,
     TimeScheduleCreatePreferences sPrefs, TimeScheduleDisplayPreferences dPrefs)
    throws IOException {

    out.write("<table cellspacing='0'>\n");
    out.write("<tbody>\n");

    // calculate priority complete dates
    Date[] priorityDates =
      priorityCompleteDates(dPrefs.showIssues, graph, sPrefs.getStartTime(), dPrefs);
    // find the maximum date for this display
    Date maxEndDate = sPrefs.getStartTime();
    for (int i = 0; i < priorityDates.length; i++) {
      if (priorityDates[i].after(maxEndDate)) {
        maxEndDate = priorityDates[i];
      }
    }

    // headers
    int numDays = writeHeaderInfo(out, sPrefs.getStartTime(), maxEndDate, priorityDates, dPrefs);

    // finally, the actual schedule
    if (dPrefs.showEachUserOnOneRow()) {
      for (int i = 0; i < dPrefs.showUsersInOneRow.size(); i++) {
        Teams.AssigneeKey userKey = dPrefs.showUsersInOneRow.get(i);
        TimeSchedule.WeeklyWorkHours userWeeklyHours =
          graph.getUserWeeklyHoursAvailable()
          .get(Teams.UserTimeKey.fromString(userKey.toString()));
        writeUserRow
          (userKey, userWeeklyHours,
           graph.getAssignedUserDetails().get(userKey),
           graph.getIssueSchedules(), numDays, maxEndDate, out,
           sPrefs.getStartTime(), dPrefs);
      }
    } else {
      
      Set<String> shownAlready = new HashSet<String>();
      for (int i = 0; i < dPrefs.showIssues.size(); i++) {
        IssueTree tree = graph.getIssueTree(dPrefs.showIssues.get(i));

        int maxDist = 0;

        // display predecessors of the main issue
        if (dPrefs.showHierarchically) {
          List<TimeScheduleSearch.DependencyBranch<IssueTree>> predBranches = TimeScheduleSearch.findPredecessorBranches(tree);

          // to figure out the indentation, we need the maximum distance from the issue
          for (Iterator<TimeScheduleSearch.DependencyBranch<IssueTree>> predBranch = predBranches.iterator(); predBranch.hasNext(); ) {
            TimeScheduleSearch.DependencyBranch<IssueTree> branch = predBranch.next();
            if (branch.getLongestDistanceFromTarget() > maxDist) {
              maxDist = branch.getLongestDistanceFromTarget();
            }
          }

          // no need to display dependents of predecessors
          TimeScheduleDisplayPreferences dPrefs2 = dPrefs.cloneButShowBlocked(false);

          // now write out those predecessors
          log4jLog.debug("Writing predecessors: stopAtTargetDistance=true; showBlocked=" + dPrefs2.showBlocked);
          for (Iterator<TimeScheduleSearch.DependencyBranch<IssueTree>> predBranch = predBranches.iterator(); predBranch.hasNext(); ) {
            TimeScheduleSearch.DependencyBranch<IssueTree> branch = predBranch.next();
            // show each node in the branch, except the last since it's first in a later branch
            for (int branchNum = 0; branchNum < branch.getBranchList().size() - 1; branchNum++) {
              IssueTree issueDetail = branch.getBranchList().get(branchNum);
              IssueTree predTree = graph.getIssueTree(issueDetail.getKey());
              int thisDist = branch.getLongestDistanceFromTarget() - branchNum;
              writeIssueRows
                (predTree, graph.getUserWeeklyHoursAvailable(),
                 graph.getIssueSchedules(), maxEndDate, priorityDates.length,
                 maxDist - thisDist, - thisDist,
                 false, out, sPrefs.getStartTime(), dPrefs2, shownAlready, true);
            }
          }
        }

        // display the requested issues (and successors)
        log4jLog.debug("Writing issue: stopAtTargetDistance=false; showBlocked=" + dPrefs.showBlocked);

        writeIssueRows
          (tree, graph.getUserWeeklyHoursAvailable(),
           graph.getIssueSchedules(), maxEndDate, priorityDates.length, maxDist, 0,
           false, out, sPrefs.getStartTime(), dPrefs, shownAlready, false);
      }
    }
    out.write("</tbody>\n");
    out.write("</table>\n");
    out.flush();
  }


  /** @return count of days, including first and last date if partials, but at minimum 1 */
  /** unused
  private static int daysBetweenDates(Date date1, Date date2) {
    return Math.max(1, (int) ((date2.getTime() - date1.getTime()) / (24 * 60 * 60 * 1000)));
  }
  **/

  /** @return the number of days to display
   */
  private static int writeHeaderInfo(Writer out, Date startTime, Date maxEndDate, Date[] priorityDates, TimeScheduleDisplayPreferences dPrefs)
    throws IOException {

    // first, the header rows

    // the month letters
    out.write("  <tr>\n");

    // -- issue detail column
    out.write("    <td>\n");
    out.write("    </td>\n");

    // -- header label column
    out.write("    <td>\n");
    out.write("      Month\n");
    out.write("    </td>\n");

    // -- first overdue and marker column
    out.write("    <td>\n");
    out.write("    </td>\n");

    Calendar calStartOfDay = Calendar.getInstance();
    calStartOfDay.setTime(startTime);
    calStartOfDay.set(Calendar.HOUR_OF_DAY, 0);
    calStartOfDay.set(Calendar.MINUTE, 0);
    calStartOfDay.set(Calendar.SECOND, 0);
    calStartOfDay.set(Calendar.MILLISECOND, 0);
    Calendar timeMarker = createTimeMarker(dPrefs.timeMarker, calStartOfDay);
    Calendar monthMarker = (Calendar) calStartOfDay.clone();
    monthMarker.set(Calendar.DATE, 1);
    SimpleDateFormat monthOf = new SimpleDateFormat("MMM");
    int numDays = 0;
    // (since we want to show a completion date, I'll do the change at loop start)
    calStartOfDay.add(Calendar.DAY_OF_YEAR, -dPrefs.timeGranularity);
    do {
      calStartOfDay.add(Calendar.DAY_OF_YEAR, dPrefs.timeGranularity);
      numDays++;
      // add a marker column if it's in the right time period
      if (timeMarker != null
          && (calStartOfDay.getTime().equals(timeMarker.getTime())
              || calStartOfDay.getTime().after(timeMarker.getTime()))) {
        out.write("    <td bgcolor='black'></td>\n");
        incrementTimeMarker(dPrefs.timeMarker, timeMarker);
      }
      String content = "";
      // add a month letter if it's in the right time period
      if (monthMarker != null
          && (calStartOfDay.getTime().equals(monthMarker.getTime())
              || calStartOfDay.getTime().after(monthMarker.getTime()))) {
        content = String.valueOf(monthOf.format(calStartOfDay.getTime()).charAt(0));
        monthMarker.add(Calendar.MONTH, 1);
      }
      out.write("    <td>" + content + "</td>\n");

      // -- overdue and marker column
      out.write("    <td>\n");
      out.write("    </td>\n");

    } while (calStartOfDay.getTime().before(maxEndDate));
    out.write("  </tr>\n");


    // the day numbers
    out.write("  <tr>\n");

    // -- issue detail column
    out.write("    <td>\n");
    out.write("    </td>\n");

    // -- header label column
    out.write("    <td>\n");
    out.write("      Date\n");
    out.write("    </td>\n");

    // -- first overdue and marker column
    out.write("    <td>\n");
    out.write("    </td>\n");

    calStartOfDay = Calendar.getInstance();
    calStartOfDay.setTime(startTime);
    calStartOfDay.set(Calendar.HOUR_OF_DAY, 0);
    calStartOfDay.set(Calendar.MINUTE, 0);
    calStartOfDay.set(Calendar.SECOND, 0);
    calStartOfDay.set(Calendar.MILLISECOND, 0);

    timeMarker = createTimeMarker(dPrefs.timeMarker, calStartOfDay);

    // (since we want to show a completion date, I'll do the change at loop start)
    calStartOfDay.add(Calendar.DAY_OF_YEAR, -dPrefs.timeGranularity);
    do {
      calStartOfDay.add(Calendar.DAY_OF_YEAR, dPrefs.timeGranularity);

      // add a marker column if it's in the right time period
      if (timeMarker != null
          && (calStartOfDay.getTime().equals(timeMarker.getTime())
              || calStartOfDay.getTime().after(timeMarker.getTime()))) {
        out.write("    <td bgcolor='black'></td>\n");
        incrementTimeMarker(dPrefs.timeMarker, timeMarker);
      }

      String bgcolor = "";
      if (calStartOfDay.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
          || calStartOfDay.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
        bgcolor = " bgcolor='yellow'";
      }
      String padding = calStartOfDay.get(Calendar.DAY_OF_MONTH) < 10 ? "&nbsp;" : "";
      out.write("    <td" + bgcolor + ">\n");
      if (!dPrefs.hideDetails) {
        out.write("      " + padding + calStartOfDay.get(Calendar.DAY_OF_MONTH) + "\n");
      }
      out.write("    </td>\n");

      // -- overdue and marker column
      out.write("    <td>\n");
      out.write("    </td>\n");

    } while (calStartOfDay.getTime().before(maxEndDate));
    out.write("  </tr>\n");


    calStartOfDay = Calendar.getInstance();
    calStartOfDay.setTime(startTime);
    calStartOfDay.set(Calendar.HOUR_OF_DAY, 0);
    calStartOfDay.set(Calendar.MINUTE, 0);
    calStartOfDay.set(Calendar.SECOND, 0);
    calStartOfDay.set(Calendar.MILLISECOND, 0);

    // write the dates when priorities get finished
    out.write("  <tr>\n");
    // -- issue detail column
    out.write("    <td>\n");
    out.write("    </td>\n");
    // -- header label column
    out.write("    <td>\n");
    out.write("      Pri\n");
    out.write("    </td>\n");
    // -- first overdue and marker column
    out.write("    <td>\n");
    out.write("    </td>\n");
    // (since we want to show a completion date, I'll do the change at loop start)
    calStartOfDay.add(Calendar.DAY_OF_YEAR, -dPrefs.timeGranularity);
    do {
      calStartOfDay.add(Calendar.DAY_OF_YEAR, dPrefs.timeGranularity);
      String prioritiesToday = "";
      int firstNonNullPriorityInSeq = -1; // to allow for grouping of sequential priorities
      // remember: index is priority-1
      // go one further so we can gather them up
      for (int i = 0; i < priorityDates.length + 1; i++) {
        if (i < priorityDates.length
            && priorityDates[i] != null
            && priorityDates[i].before(calStartOfDay.getTime())) {
          if (firstNonNullPriorityInSeq == -1) {
            // this will be the first one in this sequence
            firstNonNullPriorityInSeq = i + 1;
          } else {
            // we must be continuing with this sequence, so do nothing
          }
          priorityDates[i] = null;
        } else {
          // this number is not one to list right now, so finish the previous sequence
          if (firstNonNullPriorityInSeq == -1) {
            // there is no previous index to list, so do nothing
          } else {
            // gotta record these new priorities
            if (prioritiesToday.length() > 0) {
              prioritiesToday += "<br>";
            }
            prioritiesToday += String.valueOf(firstNonNullPriorityInSeq);
            if (firstNonNullPriorityInSeq < i) {
              // the previous priority (i) is greater than firstNonNullPriorityInSeq
              // so show that there's a range of values
              prioritiesToday += "-" + String.valueOf(i);
            }
          }
          firstNonNullPriorityInSeq = -1;
        }
      }
      out.write("    <td>" + prioritiesToday + "</td>\n");

      // -- overdue and marker column
      out.write("    <td>\n");
      out.write("    </td>\n");

    } while (calStartOfDay.getTime().before(maxEndDate));
    out.write("  </tr>\n");

    return numDays;
  }


  /**
     @param detail is the issue to write next (depending on the display preferences)
     @param issueSchedules is all the issues with their scheduling detail; see IssueDigraph.getIssueSchedules()
     @param dist is predecessor (negative) or dependent (positive) distance away from target
     @param stopAtTargetDistance is whether to stop rendering issues on this branch when 0 dist is hit
  */
  private static void writeIssueRows
    (IssueTree detail,
     Map<Teams.UserTimeKey,TimeSchedule.WeeklyWorkHours> allUserWeeklyHours,
     Map<String,TimeSchedule.IssueSchedule<IssueTree>> issueSchedules,
     Date maxEndDate, int maxPriority, int indentDepth, int dist,
     boolean isSubtask, Writer out, Date startTime,
     TimeScheduleDisplayPreferences dPrefs, Set<String> shownAlready, boolean stopAtTargetDistance)
    throws IOException {

    log4jLog.debug("Writing issue row: " + detail.getKey() + ", indent " + indentDepth + ", dist " + dist);
    if (dPrefs.displayIssue(detail)) {

      TimeSchedule.IssueSchedule<IssueTree> schedule = issueSchedules.get(detail.getKey());

      // write this row
      String prefix = "", postfix = "";
      Date issueStartTime = detail.findMinDateOfSubs(issueSchedules, null);
      Date issueEndTime = schedule.getAdjustedEndCal().getTime();
      if (detail.getResolved()) {
        prefix = "<strike>";
        postfix = "</strike>";
      }

      TimeSchedule.WeeklyWorkHours userWeeklyHours = 
        allUserWeeklyHours.get(detail.getTimeAssigneeKey());
      if (log4jLog.isDebugEnabled()
          && detail.getKey().equals(IssueLoader.ISSUE_TO_WATCH)) {
        log4jLog.debug("About to render time info for " + IssueLoader.ISSUE_TO_WATCH + " with hash " + detail.hashCode() + "; assignee " + detail.getTimeAssigneeKey() + " has hours " + userWeeklyHours);
      }

      out.write("  <tr>\n");

      // issue detail column
      out.write("    <td>\n");
      // -- indent it to the right depth
      for (int i = 0; i < indentDepth; i++ ) {
        out.write("      <ul>\n");
      }
      if (isSubtask) {
        out.write("        <li>\n");
      }
      out.write("        " + prefix + "<a href='/secure/ViewIssue.jspa?key="
                + detail.getKey() + "'>" + detail.getKey() + "</a>\n");
      if (!dPrefs.hideDetails) {
        out.write("        <br>\n");
        out.write("        " + detail.getSummary() + " -- "
                  + detail.getTimeAssigneeKey().toString() + postfix + "\n");
      }
      if (dPrefs.showChangeTools) {
        TimeScheduleModifyWriter.writeChangeTools(detail, maxPriority, out);
      }
      if (isSubtask) {
        out.write("        </li>\n");
      }
      // -- indent it to the right depth
      for (int i = 0; i < indentDepth; i++ ) {
        out.write("      </ul>\n");
      }
      out.write("   </td>\n");

      // header label column
      out.write("   <td>\n");
      out.write("   </td>\n");


      // put the due time at night on the due date
      Date dueDate = null;
      if (detail.getDueDate() != null) {
        dueDate = new Date(detail.getDueDate().getTime() + (24 * 60 * 60 * 1000));
      }

      // calculate the beginning and ending of the first time slice
      Calendar calStartOfDay = Calendar.getInstance();
      calStartOfDay.setTime(startTime);
      calStartOfDay.set(Calendar.HOUR_OF_DAY, 0);
      calStartOfDay.set(Calendar.MINUTE, 0);
      calStartOfDay.set(Calendar.SECOND, 0);
      calStartOfDay.set(Calendar.MILLISECOND, 0);
      Calendar calStartOfNextDay = (Calendar) calStartOfDay.clone();
      calStartOfNextDay.add(Calendar.DAY_OF_YEAR, dPrefs.timeGranularity);

      // first overdue and marker column
      String firstMarkerColoring = "";
      if (dueDate != null
          && (calStartOfDay.getTime().after(dueDate)
              || calStartOfDay.getTime().equals(dueDate))) {
        firstMarkerColoring = " bgcolor='red'";
      }
      out.write("   <td" + firstMarkerColoring + ">\n");
      out.write("   </td>\n");



      // scheduled columns, coloring the column for this issue

      Calendar timeMarker = createTimeMarker(dPrefs.timeMarker, calStartOfDay);

      // -- loop and fill in all the days
      // (since we want to show a completion date, I'll do the change at loop start)
      calStartOfDay.add(Calendar.DAY_OF_YEAR, -dPrefs.timeGranularity);
      calStartOfNextDay.add(Calendar.DAY_OF_YEAR, -dPrefs.timeGranularity);

      do {
        calStartOfDay.add(Calendar.DAY_OF_YEAR, dPrefs.timeGranularity);
        calStartOfNextDay.add(Calendar.DAY_OF_YEAR, dPrefs.timeGranularity);

        double weeklyHours = userWeeklyHours.retrieve(calStartOfDay.getTime());

        // add a marker row if it's in the right time period
        if (timeMarker != null
            && (calStartOfDay.getTime().equals(timeMarker.getTime())
                || calStartOfDay.getTime().after(timeMarker.getTime()))) {
          out.write("   <td bgcolor='black'></td>\n");
          incrementTimeMarker(dPrefs.timeMarker, timeMarker);
        }
        
        
        
        
        
        //// Make some markers for lines drawn to link issues together.
        
        // If this is the end of the issue time...
        // ... create a relationship link for successors.
        String successorRels = "";
        if ((calStartOfDay.getTime().before(issueEndTime)
             && calStartOfNextDay.getTime().after(issueEndTime))
            || calStartOfNextDay.getTime().equals(issueEndTime)) {
          // for each of the successor issues, create 'rel' links (for a visual pointer if it precedes other issues)
          for (IssueTree issueAfter : detail.getDependents()) {
              successorRels += "<span rel='" + issueAfter.getKey() + "-start' type='successor' style='display:block'></span>";
          }
        }
        
        // If this is the start of the issue time...
        // ... create link to refocus or show critical path on the issue.
        String domMarker = "";
        String focusIssueLink = "";
        if (calStartOfDay.getTime().equals(issueStartTime)
            || (calStartOfDay.getTime().before(issueStartTime)
                && calStartOfNextDay.getTime().after(issueStartTime))) {
          // set the start-point DOM ID (for a visual pointer if it follows a previous issue)
          domMarker = "<span id='" + detail.getKey() + "-start' style='display:block'></span>";
          String title = detail.getKey() + ": " + detail.getSummary() + " -- " + detail.getTimeAssigneeKey().toString();
          focusIssueLink = "<a href='?" + TimeScheduleAction.ISSUE_KEY_REQ_NAME + "=" + detail.getKey() + "' style='color:black' title='Focus on " + title + "'>+</a>";
          if (!dPrefs.hideDetails) {
            focusIssueLink += "<br/>";
          }
          focusIssueLink += "<a href='?" + TimeScheduleAction.ISSUE_KEY_REQ_NAME + "=" + detail.getKey() + "&" + TimeScheduleAction.SHOW_CRITICAL_PATHS_REQ_NAME + "=on' style='color:black' title='Critical Path for " + title + "'>?</a>";
        }
        
        // If this is the middle of the issue time...
        // ... create a relationship link for the subtasks.
        String subtaskRels = "";
        long halfwayMillis = issueStartTime.getTime() + ((issueEndTime.getTime() - issueStartTime.getTime()) / 2);
        if (halfwayMillis == calStartOfDay.getTime().getTime()
            || (calStartOfDay.getTime().getTime() < halfwayMillis
                && halfwayMillis < calStartOfNextDay.getTime().getTime())) {
            // set the midpoint DOM ID (for a visual pointer if it's a subtask of a master issue)
            domMarker += "<span id='" + detail.getKey() + "-middle' style='display:block'></span>";
            
            // for each of the subtask issues, create 'rel' links (for a visual pointer if it masters other issues)
            for (IssueTree subtask : detail.getSubtasks()) {
                subtaskRels += "<span rel='" + subtask.getKey() + "-middle' type='subtask' style='display:block'></span>";
            }
        }
        
        /** alternatively, to display at the start of the issue, change that '-middle' just above to '-start', remove the 'for' loop after it, and...
        // if this is the start of the issue time and there are any subtask issues then create those 'rel' links (for a visual pointer if it masters other issues)
        if (calStartOfDay.getTime().equals(issueStartTime)
                || (calStartOfDay.getTime().before(issueStartTime)
                    && calStartOfNextDay.getTime().after(issueStartTime))) {
          for (IssueWorkDetail subtask : detail.getSubtasks()) {
              subtaskRels += "<span rel='" + subtask.getKey() + "-start' type='subtask' style='display:block'></span>";
          }
        }
        **/
        
        
        
        
        
        // set color if it's in the period
        String coloring = "";
        if (calStartOfNextDay.getTime().after(issueStartTime)
            && calStartOfDay.getTime().before(issueEndTime)) {
          String color;
          boolean actualScheduledTime = 
            schedule.getAdjustedBeginCal().before(calStartOfNextDay)
            && schedule.getAdjustedEndCal().after(calStartOfDay);
          // check that this actual scheduled time isn't overlapped by another one
          if (actualScheduledTime) {
            actualScheduledTime =
              schedule.timeWorkedOnDays(calStartOfDay, dPrefs.timeGranularity, weeklyHours) > 0;
          }
          if (detail.getResolved()) {
            // it's resolved
            color = "black";
          } else if (dueDate != null
                     && calStartOfNextDay.getTime().after(dueDate)
                     && dueDate.before(issueEndTime)) {
            // it's overdue
            if (actualScheduledTime) {
              color = "red";
            } else {
              color = "pink";
            }
          } else {
            // it fits in planning
            if (actualScheduledTime) {
              color = "green";
            } else {
              color = "lightgreen";
            }
          }
          coloring = " bgcolor='" + color + "'";
        }
        out.write("   <td" + coloring + ">" + focusIssueLink + domMarker + subtaskRels + successorRels + "</td>\n");

        // -- overdue and marker column
        String markerColoring = coloring;
        if (dueDate != null
            && (calStartOfNextDay.getTime().equals(dueDate)
                || (calStartOfDay.getTime().before(dueDate)
                    && (calStartOfNextDay.getTime().after(dueDate)
                        || !calStartOfDay.getTime().before(maxEndDate))))) {
          // show a line to mark that this is where the issue is due
          markerColoring = " bgcolor='red'";
        }
        out.write("   <td" + markerColoring + "></td>\n");

      } while (calStartOfDay.getTime().before(maxEndDate));
      out.write("  </tr>\n");
      //out.write("  <tr><td></td></tr>\n"); // put some spacing between rows
      out.flush();
      shownAlready.add(detail.getKey());
    }

    boolean goDeeper =
      dPrefs.showHierarchically && (!stopAtTargetDistance || dist != 0);
    if (goDeeper) {
      // write all subtask and dependent issue rows
      for (Iterator<IssueTree> i = detail.getSubtasks().iterator(); i.hasNext(); ) {
        IssueTree subIssue = i.next();
        writeIssueRows
          (subIssue, allUserWeeklyHours, issueSchedules, maxEndDate, maxPriority,
           indentDepth + 1, dist,
           true, out, startTime, dPrefs, shownAlready, stopAtTargetDistance);
      }
      if (dPrefs.showBlocked) {
        for (Iterator<IssueTree> i = detail.getDependents().iterator(); i.hasNext(); ) {
          IssueTree current = i.next();
          writeIssueRows
            (current, allUserWeeklyHours, issueSchedules, maxEndDate, maxPriority,
             indentDepth + 1, dist + 1,
             false, out, startTime, dPrefs, shownAlready, stopAtTargetDistance);
        }
      }
    }
  }



  private static void writeUserRow
    (Teams.AssigneeKey userKey,
     TimeSchedule.WeeklyWorkHours userWeeklyHours,
     List<IssueTree> details, Map<String,TimeSchedule.IssueSchedule<IssueTree>> schedules, int numDays, Date maxEndDate,
     Writer out, Date startTime, TimeScheduleDisplayPreferences dPrefs)
    throws IOException {

    // loop through details and fill a color for each date
    String[] dateColors = new String[numDays];
    Arrays.fill(dateColors, "");
    for (Iterator<IssueTree> issuei = details.iterator(); issuei.hasNext(); ) {
      IssueTree issue = issuei.next();
      // only change a color if it's an unresolved issue with time left
      if (!issue.getResolved()
          && issue.getEstimate() > 0) {

        TimeSchedule.IssueSchedule<IssueTree> schedule =
          (TimeSchedule.IssueSchedule<IssueTree>) schedules.get(issue.getKey());

        // calculate the beginning and ending of the first time slice
        Calendar calStartOfDay = Calendar.getInstance();
        calStartOfDay.setTime(startTime);
        calStartOfDay.set(Calendar.HOUR_OF_DAY, 0);
        calStartOfDay.set(Calendar.MINUTE, 0);
        calStartOfDay.set(Calendar.SECOND, 0);
        calStartOfDay.set(Calendar.MILLISECOND, 0);
        Calendar calStartOfNextDay = (Calendar) calStartOfDay.clone();
        calStartOfNextDay.add(Calendar.DAY_OF_YEAR, dPrefs.timeGranularity);

        // loop through dates until the end of the issue and fill in any colors
        // (since we want to show a completion date, do change at loop start)
        calStartOfDay.add(Calendar.DAY_OF_YEAR, -dPrefs.timeGranularity);
        calStartOfNextDay.add(Calendar.DAY_OF_YEAR, -dPrefs.timeGranularity);
        int dateNum = -1;

        do {
          calStartOfDay.add(Calendar.DAY_OF_YEAR, dPrefs.timeGranularity);
          calStartOfNextDay.add(Calendar.DAY_OF_YEAR, dPrefs.timeGranularity);
          dateNum++;

          double weeklyHours = userWeeklyHours.retrieve(calStartOfDay.getTime());

          if (calStartOfNextDay.getTime().after(schedule.getBeginDate())) {
            boolean actualScheduledTime = 
              schedule.getAdjustedBeginCal().before(calStartOfNextDay)
              && schedule.getAdjustedEndCal().after(calStartOfDay);
            // check that this actual scheduled time isn't overlapped by another one
            if (actualScheduledTime) {
              actualScheduledTime =
                schedule.timeWorkedOnDays(calStartOfDay, dPrefs.timeGranularity, weeklyHours) > 0;
            }
            Date dueDate = null;
            if (issue.getDueDate() != null) {
              dueDate = new Date(issue.getDueDate().getTime() + (24 * 60 * 60 * 1000));
            }
            if (dueDate != null
                && calStartOfNextDay.getTime().after(dueDate)
                && dueDate.before(schedule.getEndDate())) {
              // it's overdue
              if (actualScheduledTime) {
                dateColors[dateNum] = " bgcolor='red'";
              }
            } else {
              // it fits in planning
              if (actualScheduledTime) {
                // only change the color if there's not already one
                // (if it's already red, then we want to keep it red)
                if (dateColors[dateNum].length() == 0) {
                  dateColors[dateNum] = " bgcolor='green'";
                }
              }
            }
          }

        } while (calStartOfNextDay.getTime().before(schedule.getEndDate())
                 && calStartOfDay.getTime().before(maxEndDate));
      }
    }

    // now render this row
    out.write("  <tr>\n");
    // -- issue detail column
    out.write("    <td>\n");
    out.write("     " + userKey + "\n");
    out.write("    </td>\n");

    // -- header label column
    out.write("    <td>\n");
    out.write("    </td>\n");

    // -- first overdue and marker column
    out.write("    <td>\n");
    out.write("    </td>\n");
    for (int dateNum = 0; dateNum < numDays; dateNum++) {
      out.write("<td" + dateColors[dateNum] + "></td>");
      // -- overdue and marker column
      out.write("<td" + dateColors[dateNum] + "></td>");
    }
    out.write("  </tr>\n");

  }

}
