package com.trentlarson.forecast.core.actions;

import java.lang.reflect.UndeclaredThrowableException;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.ParseException;
import java.util.Date;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;

import com.icentris.sql.SimpleSQL;
import com.trentlarson.forecast.core.helper.ForecastUtil;
import com.trentlarson.forecast.core.scheduling.IssueDigraph;
import com.trentlarson.forecast.core.scheduling.external.IssueLoader;
import com.trentlarson.forecast.core.scheduling.IssueTree;
import com.trentlarson.forecast.core.scheduling.TimeScheduleCreatePreferences;
import com.trentlarson.forecast.core.scheduling.external.TimeScheduleLoader;
import com.trentlarson.forecast.core.scheduling.TimeScheduleModifyWriter;

public class TimeScheduleAction {

  private static IssueDigraph sharedGraph = null;
  private static String changedBy = null;
  private static Date changedTime = null;
  private static String adjustmentChangedBy = null;
  private static Date adjustmentChangedTime = null;

  public static String MULTIPLIER_REQ_NAME = "time_multiplier";
  public static String ISSUE_KEY_REQ_NAME = "issueKey";
  public static String SHOW_CRITICAL_PATHS_REQ_NAME = "show_critical_paths";



  /** @return null if nobody has changed anything */
  public static String getChangedBy() {
    return changedBy;
  }
  /** @return null if nobody has changed anything */
  public static Date getChangedTime() {
    return changedTime;
  }
  /** set to the requesting user */
  protected static void setChangedBy(HttpServletRequest request) {
    setChangedByName("somebody@who.loves.you");
  }

  /** @param changedBy_ the user who has changed settings; null to clear */
  private static void setChangedByName(String changedBy_) {
    changedBy = changedBy_;
    if (changedBy == null) {
      changedTime = null;
    } else {
      changedTime = new Date();
    }
  }


  /** @return null if nobody has changed adjustment */
  public static String getAdjustmentChangedBy() {
    return adjustmentChangedBy;
  }
  /** @return null if nobody has changed adjustment */
  public static Date getAdjustmentChangedTime() {
    return adjustmentChangedTime;
  }
  /** @param changedBy_ the user who has changed adjustment; null to clear */
  protected static void setAdjustmentChangedBy(String changedBy_) {
    adjustmentChangedBy = changedBy_;
    if (adjustmentChangedBy == null) {
      adjustmentChangedTime = null;
    } else {
      adjustmentChangedTime = new Date();
    }
  }


  public static synchronized IssueDigraph regenerateGraph(TimeScheduleCreatePreferences sPrefs) {
    Connection conn = null;
    try {
      conn = ForecastUtil.getConnection();
      setSharedGraph(TimeScheduleLoader.getEntireGraph(sPrefs, conn));
    } catch (SQLException e) {
      throw new java.lang.reflect.UndeclaredThrowableException(e);
    } finally {
      try { conn.close(); } catch (Exception e) {}
    }
    return sharedGraph;
  }

  public static synchronized IssueDigraph getOrRegenerateGraph(TimeScheduleCreatePreferences sPrefs) {
    if (sharedGraph != null) {
      return sharedGraph;
    } else {
      return regenerateGraph(sPrefs);
    }
  }

  /**
     Only call this when you know there must be an existing graph.
     @return null if graph hasn't been calculated yet
  */
  public static IssueDigraph getGraph() {
    return sharedGraph;
  }

  private static synchronized void setSharedGraph(IssueDigraph graph) {
    sharedGraph = graph;
  }






  /**
     @deprecated
     Use TimeScheduleModifyWriter.formatShortDate
   */
  public static String formatShortDate(Date date) {
    return TimeScheduleModifyWriter.formatShortDate(date);
  }

  /**
     @deprecated because it will have a year of 1970
     Use date formatter with year or TimeScheduleModifyWriter.parseNewShortDate
   */
  public static Date parseShortDate(String dateStr) throws ParseException {
    return TimeScheduleModifyWriter.parseShortDate(dateStr);
  }



  public static void modifyPriorities(HttpServletRequest request) {
    IssueDigraph workingGraph = getGraph();
    boolean oneChanged = false;
    for (Iterator<String> issueIter = workingGraph.getIssueSchedules().keySet().iterator();
         issueIter.hasNext(); ) {
      String issueKey = issueIter.next();
      IssueTree detail = workingGraph.getIssueTree(issueKey);
      if (!detail.getResolved()
          &&
          request.getParameter
          (TimeScheduleModifyWriter.makePriorityCheckboxName(detail.getKey()))
          != null) {
        // change priority if it has changed
        int newPriority =
          Integer.valueOf
          (request.getParameter
           (TimeScheduleModifyWriter.makePriorityCheckboxName(detail.getKey())))
          .intValue();
        if (newPriority != detail.getPriority()) {
          detail.setPriority(newPriority);
          oneChanged = true;
        }
        // change estimate if it has changed
        int newEstimate =
          TimeScheduleModifyWriter.parseEstimateFromTypicalDay
          (request.getParameter
           (TimeScheduleModifyWriter.makeEstimateInputName(detail.getKey())));
        if (newEstimate != detail.getEstimate()) {
          detail.setEstimate(newEstimate);
          oneChanged = true;
        }
        // change due date if it has changed
        try {
          String origDateStr =
            TimeScheduleModifyWriter.formatShortDate(detail.getDueDate());
          if (!origDateStr
              .equals(request.getParameter
                      (TimeScheduleModifyWriter.makeDueDateInputName(detail.getKey())))) {
            Date newDueDate =
              TimeScheduleModifyWriter.parseNewShortDate
              (request.getParameter
               (TimeScheduleModifyWriter.makeDueDateInputName(detail.getKey())));
            detail.setDueDate(newDueDate);
            oneChanged = true;
          }
        } catch (ParseException e) {
          System.err.println("We should be notifying the user of their error!");
          e.printStackTrace();
        }
      }
    }
    if (oneChanged) {
      workingGraph.reschedule();
      setChangedBy(request);
    }
  }

  public static void revertPriorities() {
    IssueDigraph workingGraph = getGraph();
    boolean oneChanged = false;
    for (Iterator<String> issueIter = workingGraph.getIssueSchedules().keySet().iterator();
         issueIter.hasNext(); ) {
      String issueKey = issueIter.next();
      IssueTree detail = workingGraph.getIssueTree(issueKey);
      if (!detail.getResolved()) {
        // change back if the priority has changed
        if (detail.getPriorityOrig() != detail.getPriority()) {
          detail.setPriority(detail.getPriorityOrig());
          oneChanged = true;
        }
        // change back if the estimate has changed
        if (detail.getEstimateOrig() != detail.getEstimate()) {
          detail.setEstimate(detail.getEstimateOrig());
          oneChanged = true;
        }
        // change back if the due date has changed
        if ((detail.getDueDateOrig() == null && detail.getDueDate() != null)
            || (detail.getDueDateOrig() != null && detail.getDueDate() == null)
            || (detail.getDueDateOrig() != null && detail.getDueDate() != null
                && !detail.getDueDateOrig().equals(detail.getDueDate()))) {
          detail.setDueDate(detail.getDueDateOrig());
          oneChanged = true;
        }
      }
    }
    if (oneChanged) {
      workingGraph.reschedule();
      setChangedByName(null);
    }
  }

  public static void savePriorities() {
    Connection conn = null;
    StringBuffer recipients = new StringBuffer();

    recipients.append(getChangedBy());

    StringBuffer changes = new StringBuffer();
    try {
      conn = ForecastUtil.getConnection();
      IssueDigraph workingGraph = getGraph();

      // keep track of all changes to send users
      changes.append("<html>\n<body>\n");
      changes.append("You are the assignee, reporter, or watcher of one of the tasks below, ");
      changes.append("which has just been modified by " + getChangedBy() + " on the bulk scheduling page.\n");
      changes.append("<table border='1'>\n");
      changes.append("<tr><td></td><td></td><td colspan='6'>Changes (blank if no change made)</td></tr>\n");
      changes.append("<tr><td></td><td></td><td colspan='2'>Priority</td><td colspan='2'>Estimate</td><td colspan='2'>Due Date</td></tr>\n");
      changes.append("<tr><td>Key</td><td>Summary</td><td>Before</td><td>Now</td><td>Before</td><td>Now</td><td>Before</td><td>Now</td></tr>\n");

      // loop through and save issue changes
      for (Iterator<String> issueIter = workingGraph.getIssueSchedules().keySet().iterator();
           issueIter.hasNext(); ) {
        String issueKey = issueIter.next();
        IssueTree detail = workingGraph.getIssueTree(issueKey);
        if (detail.getPriorityOrig() != detail.getPriority()
            || detail.getEstimateOrig() != detail.getEstimate()
            || (detail.getDueDateOrig() == null && detail.getDueDate() != null)
            || (detail.getDueDateOrig() != null && detail.getDueDate() == null)
            || (detail.getDueDateOrig() != null && detail.getDueDate() != null
                && !detail.getDueDate().equals(detail.getDueDateOrig()))) {

          // add to the report

          // - priority
          String priorityOldStr = String.valueOf(detail.getPriorityOrig());
          String priorityNewStr = String.valueOf(detail.getPriority());

          // - estimate
          String estimateOldStr =
            TimeScheduleModifyWriter
            .formatEstimateForTypicalDay(detail.getEstimateOrig())
            + "d";
          String estimateNewStr =
            TimeScheduleModifyWriter
            .formatEstimateForTypicalDay(detail.getEstimate())
            + "d";

          // - due date; check for nulls
          String dateOldStr =
            detail.getDueDateOrig() == null ? "" : detail.getDueDateOrig().toString();
          String dateNewStr =
            detail.getDueDate() == null ? "" : detail.getDueDate().toString();

          // - show blanks if there is no change in the value
          if (priorityNewStr.equals(priorityOldStr)) {
            priorityOldStr = "";
            priorityNewStr = "";
          }
          if (estimateNewStr.equals(estimateOldStr)) {
            estimateOldStr = "";
            estimateNewStr = "";
          }
          if (dateNewStr.equals(dateOldStr)) {
            dateOldStr = "";
            dateNewStr = "";
          }

          // - show the data
          changes.append
            ("<tr><td><a href='https://intra01.icentris.net/browse/"
             + detail.getKey() + "'>" + detail.getKey() + "</td><td>"
             + detail.getSummary() + "</td><td>"
             + priorityOldStr + "</td><td>"
             + priorityNewStr + "</td><td>"
             + estimateOldStr + "</td><td>"
             + estimateNewStr + "</td><td>"
             + dateOldStr + "</td><td>"
             + dateNewStr + "</td></tr>\n");

          // now make the DB changes
          String sql =
            "update " + IssueLoader.DB_ISSUE_TABLE + " set priority = (select id from priority where sequence = ?), "
            + " timeestimate = ?, duedate = ? where pkey = ?";
          Timestamp date = null;
          if (detail.getDueDate() != null) {
            date = new Timestamp(detail.getDueDate().getTime());
          }
          Object[] args =
            { new Integer(detail.getPriority()),
              new Integer(detail.getEstimate()),
              new SimpleSQL.SQLObject(date, java.sql.Types.TIMESTAMP),
              detail.getKey() };
          SimpleSQL.executeUpdate(sql, args, conn);
          detail.saveNewValues();

          // add the assignee to list of recipients
          String recipient = detail.getRawAssignedPerson();
          if (recipient != null
              && recipients.indexOf(recipient) == -1) {
            if (recipients.length() > 0) {
              recipients.append(",");
            }
            recipients.append(recipient);
          }

          // add the watchers to the list of recipients
          /** someday soon...
          ComponentManager cm = ComponentManager.getInstance();
          Collection watchers =
            cm.getWatcherManager()
            .getCurrentWatchList
            (cm.getIssueManager().getIssue(detail.getKey()));

          ... modify the following to work with those previous 2 lines

          for (int i = 0; i < issueWatchers.size(); i++) {
            com.icentris.jira.reports.GenericValueWrapper issue =
              (com.icentris.jira.reports.GenericValueWrapper) issueWatchers.get(i);
            String watcher = issue.getString(ForecastConstants.NAME);
            try {
              recipient = UserManager.getInstance().getUser(watcher).getEmail();
              if (recipients.indexOf(recipient) == -1) {
                if (recipients.length() > 0) {
                  recipients.append(",");
                }
                recipients.append(recipient);
              }
            } catch (EntityNotFoundException e) {
              ForecastUtil.notifyAdmin("Unable to find user " + watcher+ ", who supposedly is watching issue " + issueKey + ".  We won't include their email in the recipient list.");
            }
          }

          // possible alternate means of change and notification(?):
          // ComponentManager.getIssueUpdater().doUpdate(new IssueUpdateBean(oldIssue, newIssue, ?, user);

          **/
        }
      }

      // send notifications
      changes.append("</table>\n</body>\n</html>\n");

      System.out.println("!!! Here's where we should send an email to people. !!!");
      //notify(recipients.toString(), changes.toString());

      // reset graph and changed time
      setSharedGraph(null);
      setChangedByName(null);
    } catch (SQLException e) {
      throw new UndeclaredThrowableException(e);
    } finally {
      try { conn.close(); } catch (Exception e) {}
    }
  }





  public static void modifyAdjustment(HttpServletRequest request) {
    double multiple = Double.valueOf(request.getParameter(MULTIPLIER_REQ_NAME)).doubleValue();
    IssueDigraph workingGraph = getGraph();
    TimeScheduleCreatePreferences sPrefs = getGraph().getTimeScheduleCreatePreferences();
    sPrefs = new TimeScheduleCreatePreferences(sPrefs.timeWithoutEstimate, sPrefs.startCal, multiple);
    setSharedGraph(TimeScheduleLoader.reschedule(workingGraph, sPrefs));
    setChangedBy(request);
  }


}
