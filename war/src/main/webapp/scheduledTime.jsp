
<%@ page
  import="com.icentris.sql.SimpleSQL"
  import="java.sql.*"
  import="java.util.*"
%>

<head>
<title>Workload Report</title>
</head>

<h1> Currently Assigned Workload </h1>


<%
  if (request.getParameter("username") != null) {
    username = request.getParameter("username");
  } else {
    String userS = (String) session.getAttribute("username");
    if (userS != null) {
      username = userS;
    }
  }
  if (username == null) {
    out.println("<h2>You must log in to see this report.</h2>");
    out.println("<p>");
    return;
  }
  session.setAttribute("username", username);

  float multiplier = 2;
  if (request.getParameter(MULTIPLIER_REQ_NAME) != null) {
    multiplier = new Float(request.getParameter(MULTIPLIER_REQ_NAME)).floatValue();
  } else {
    Float multF = (Float) session.getAttribute(MULTIPLIER_SES_NAME);
    if (multF != null) {
      multiplier = multF.floatValue();
    }
  }
  session.setAttribute(MULTIPLIER_SES_NAME, new Float(multiplier));
%>

<!--
<form method="get" action="scheduledTime.jsp">
  <input name="username" type="text" length="30" value="<%= username %>">
  <input type="submit" value="Change User">
</form>
-->

<p>


<%

    javax.sql.DataSource dsrc = null;
    try {
      
      dsrc = (javax.sql.DataSource) new javax.naming.InitialContext().lookup("java:comp/env/jdbc/JiraDS");
    } catch (javax.naming.NamingException e) {
      throw new java.lang.reflect.UndeclaredThrowableException(e);
    }

    Connection conn = null;
    ResultSet rset = null;

    try {
      conn = dsrc.getConnection();
      String timeSql;
      Object[] args;

      timeSql = "select sum(timeestimate) / (3600) as hours from jiraissue where assignee = ? and resolution is null";
      args = new Object[]{ username };
      rset = SimpleSQL.executeQuery(timeSql, args, conn);
      if (rset.next()) {
        long estimate = rset.getLong("hours");
%>


<h2>Total</h2>

You have <%=(int) (estimate / 40)%> weeks, <%=(int) ((estimate % 40) / 8)%> days, <%=(estimate % 8.0)%> hours of total, unfinished work.

<p>

<%
      }




      timeSql = "select sum(timeestimate) / (3600) as hours from jiraissue where assignee = ? and resolution is null and duedate is not null";
      args = new Object[]{ username };
      rset = SimpleSQL.executeQuery(timeSql, args, conn);
      if (rset.next()) {
        float estimate = rset.getFloat("hours");

%>

<h2>Scheduled</h2>

You have <%=(int) (estimate / 40)%> weeks, <%=(int) ((estimate % 40) / 8)%> days, <%=(estimate % 8.0)%> hours of work scheduled with due dates.

<p>

<%

      }



      out.println("If that were scheduled back-to-back, which dates could you hit?  Not the ones below in red!");
      out.println("<p>");
      out.println("<table border='1'>");
      out.println("<tr>");
      out.println("<td>issue</td>");
      out.println("<td>reporter</td>");
      out.println("<td>summary</td>");
      out.println("<td>estimate</td>");
      out.println("<td>finish date</td>");
      out.println("<td>due date</td>");
      out.println("</tr>");

      Iterator schedules = createIssueSchedules(username, multiplier).iterator();
      while (schedules.hasNext()) {
        IssueSchedule schedule = (IssueSchedule) schedules.next();
        IssueWorkDetail detail = schedule.issue;

        out.println("<tr>");
        out.println("<td><a href='/secure/ViewIssue.jspa?key=" + detail.key + "'>" + detail.key + "</a></td>");
        out.println("<td>" + detail.reporter + "</td>");
        out.println("<td>" + detail.summary + "</td>");

        out.println("<td>");
        int issueEst = (int) (detail.issueEstRaw * multiplier);
        if (issueEst == 0) { out.println("<font color='orange'>"); }
        out.println((issueEst / 28800) + "d " + ((issueEst % 28800) / 3600.0) + "h");
        if (issueEst == 0) { out.println("</font>"); }
        out.println("</td>");

        out.println("<td>");
        if (schedule.endDate.after(detail.dueDate)) { out.println("<font color='red'>"); }
        out.println(WEEKDAY_DATE_TIME.format(schedule.getAdjustedNextBeginDate()));
        if (schedule.endDate.after(detail.dueDate)) { out.println("</font>"); }
        out.println("</td>");

        out.println("<td>");
        if (schedule.endDate.after(detail.dueDate)) { out.println("<font color='red'>"); }
        out.println(WEEKDAY_DATE.format(detail.dueDate));
        if (schedule.endDate.after(detail.dueDate)) { out.println("</font>"); }
        out.println("</td>");
        out.println("</tr>");
      }
      out.println("</table>");

    } finally {
      try { rset.close(); } catch (Exception e2) {}
      try { conn.close(); } catch (Exception e2) {}
    }


      out.println("<BR>");
      out.println("<form action='scheduledTime.jsp'>");
      out.println("(This assumes an " + HOURS_IN_DAY + " hour workday starting at " + FIRST_HOUR_OF_WORKDAY + " without a break; tasks are scheduled in order of due date;");
      out.println(" and time is multiplied by <input name='" + MULTIPLIER_REQ_NAME + "' size='2' value='" + multiplier + "'><input type='submit' value='(change)'> to account for unexpected stuff.)");
      out.println("</form>");
%>


<p>

The built-in JIRA time report is found <a href="/secure/DeveloperWorkloadReport.jspa?developer=<%=username%>">here</a>.

<p>


<%!

  private static int DAYS_IN_WEEK = 5;
  private static int HOURS_IN_DAY = 8;
  private static int FIRST_HOUR_OF_WORKDAY = 8;
  private static java.text.SimpleDateFormat WEEKDAY_DATE_TIME = new java.text.SimpleDateFormat("EEE MMM dd hh:mm a");
  private static java.text.SimpleDateFormat WEEKDAY_DATE = new java.text.SimpleDateFormat("EEE MMM dd");

  private static String MULTIPLIER_REQ_NAME = "multiplier";
  private static String MULTIPLIER_SES_NAME = "multiplier";

  // return number of days into the work week by midnight this day's morning
  private static int daysIntoWorkWeek(Calendar someday) {
    switch (someday.get(Calendar.DAY_OF_WEEK)) {
      case Calendar.SUNDAY:    throw new IllegalStateException("The previous date shouldn't ever be a non-weekday " + someday.get(Calendar.DAY_OF_WEEK));
      case Calendar.MONDAY:    return 0;
      case Calendar.TUESDAY:   return 1;
      case Calendar.WEDNESDAY: return 2;
      case Calendar.THURSDAY:  return 3;
      case Calendar.FRIDAY:    return 4;
// FIX this to throw the exception!
      case Calendar.SATURDAY:  return 5;//throw new IllegalStateException("The previous date shouldn't ever be a non-weekday " + someday.get(Calendar.DAY_OF_WEEK));
      default:                 throw new IllegalStateException("The previous date shouldn't ever be a non-weekday " + someday.get(Calendar.DAY_OF_WEEK));
    }
  }

  /** @return List of IssueSchedule objects */
  public List createIssueSchedules(String username, float multiplier) throws SQLException {

    // set the work start time
    Calendar nextEstBegin = new GregorianCalendar();
    {
      if (nextEstBegin.get(Calendar.HOUR_OF_DAY) < FIRST_HOUR_OF_WORKDAY) {
        nextEstBegin.set(Calendar.HOUR_OF_DAY, 0);
        nextEstBegin.set(Calendar.MINUTE, 0);
      } else {
        int hour_into_workday = nextEstBegin.get(Calendar.HOUR_OF_DAY) - FIRST_HOUR_OF_WORKDAY;
        if (hour_into_workday < HOURS_IN_DAY) {
          nextEstBegin.set(Calendar.HOUR_OF_DAY, hour_into_workday);
        } else {
          nextEstBegin.add(Calendar.DAY_OF_WEEK, 1);
          nextEstBegin.set(Calendar.HOUR_OF_DAY, 0);
          nextEstBegin.set(Calendar.MINUTE, 0);
        }
      }
      nextEstBegin.set(Calendar.SECOND, 0);
      nextEstBegin.set(Calendar.MILLISECOND, 0);
      if (nextEstBegin.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
        nextEstBegin.add(Calendar.DAY_OF_WEEK, 2);
      } else if (nextEstBegin.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
        nextEstBegin.add(Calendar.DAY_OF_WEEK, 1);
      }
    }

    // read issue details and store in a list
    List allSchedules = new Vector();
    {
      javax.sql.DataSource dsrc = null;
      try {
        Class.forName("com.mysql.jdbc.Driver");
        dsrc = (javax.sql.DataSource) new javax.naming.InitialContext().lookup("java:comp/env/jdbc/JiraDS");
      } catch (ClassNotFoundException e) {
        throw new java.lang.reflect.UndeclaredThrowableException(e);
      } catch (javax.naming.NamingException e) {
        throw new java.lang.reflect.UndeclaredThrowableException(e);
      }

      Connection conn = null;
      ResultSet rset = null;

      try {
        conn = dsrc.getConnection();
        String timeSql = "select * from jiraissue where assignee = ? and resolution is null and duedate is not null order by duedate asc";
        Object[] args = new Object[]{ username };
        rset = SimpleSQL.executeQuery(timeSql, args, conn);
        while (rset.next()) {
          IssueWorkDetail detail = new IssueWorkDetail(rset.getString("pkey"), rset.getString("reporter"), rset.getString("summary"), rset.getInt("timeestimate"), rset.getDate("dueDate"));

          int issueEst = (int) (detail.issueEstRaw * multiplier);
          int numSecondsAlreadyOnDay = 
            nextEstBegin.get(Calendar.HOUR_OF_DAY) * 60 * 60
            + nextEstBegin.get(Calendar.MINUTE) * 60
            + nextEstBegin.get(Calendar.SECOND);
          nextEstBegin.set(Calendar.HOUR_OF_DAY, 0);
          nextEstBegin.set(Calendar.MINUTE, 0);
          nextEstBegin.set(Calendar.SECOND, 0);
          nextEstBegin.set(Calendar.MILLISECOND, 0);
          int timePastToday = numSecondsAlreadyOnDay + issueEst;
          int numEstDays = timePastToday / (60 * 60 * HOURS_IN_DAY);
          int numEstSecsOnLastDay = timePastToday % (60 * 60 * HOURS_IN_DAY);
          if (numEstDays > 0) {
            int fullWeeks = (daysIntoWorkWeek(nextEstBegin) + numEstDays) / DAYS_IN_WEEK;
            int weekendDays = fullWeeks * 2;
            nextEstBegin.add(Calendar.DAY_OF_WEEK, numEstDays + weekendDays);
          }
          nextEstBegin.set(Calendar.SECOND, numEstSecsOnLastDay);

          int endOffset = 0;
          if (numEstSecsOnLastDay == 0) {
            // move end date back to end of previous workday
            endOffset += 16 * 3600 * 1000;
            if (nextEstBegin.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY) {
              // move end date back to end of previous Friday
              endOffset += 2 * 24 * 3600 * 1000;
            }
          }
          IssueSchedule schedule = new IssueSchedule(detail, (Calendar) nextEstBegin.clone(), new java.util.Date(nextEstBegin.getTime().getTime() - endOffset));
          allSchedules.add(schedule);
        }
      } finally {
        try { rset.close(); } catch (Exception e2) {}
        try { conn.close(); } catch (Exception e2) {}
      }
    }
    return allSchedules;
  }

  public class IssueSchedule {
    public final IssueWorkDetail issue;
    public final java.util.Calendar nextEstBegin; // date the next task can begin, NOT adjusted by FIRST_HOUR_OF_WORKDAY
    public final java.util.Date endDate; // date work on this task should complete, adjusted by workday hours
    public IssueSchedule(IssueWorkDetail issue_, java.util.Calendar nextEstBegin_, java.util.Date endDate_) {
      issue = issue_;
      nextEstBegin = nextEstBegin_;
      endDate = endDate_;
    }
    public java.util.Date getAdjustedNextBeginDate() {
      nextEstBegin.roll(Calendar.HOUR_OF_DAY, FIRST_HOUR_OF_WORKDAY); // to display during day
      java.util.Date result = nextEstBegin.getTime();
      nextEstBegin.roll(Calendar.HOUR_OF_DAY, -FIRST_HOUR_OF_WORKDAY); // to calculate from midnight
      return result;
    }
  }

  public class IssueWorkDetail {
    public final String key, reporter, summary;
    public final int issueEstRaw;
    public final java.sql.Date dueDate;
    public IssueWorkDetail(String key_, String reporter_, String summary_, int issueEstRaw_, java.sql.Date dueDate_) {
      key = key_;
      reporter = reporter_;
      summary = summary_;
      issueEstRaw = issueEstRaw_;
      dueDate = dueDate_;
    }
  }


%>
