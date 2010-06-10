<%@ page
  import="java.util.*"
  import="java.text.*"
  import="com.trentlarson.forecast.core.dao.Team"
  import="com.trentlarson.forecast.core.dao.TeamHours"
  import="com.trentlarson.forecast.core.scheduling.TeamHoursUtil"
  import="com.trentlarson.forecast.core.scheduling.TimeAssignmentsByWeek"
  import="com.trentlarson.forecast.core.scheduling.TimeCompleteBar"

  import="com.trentlarson.forecast.core.helper.ForecastUtil"
  import="com.icentris.sql.SimpleSQL"
  import="java.sql.Connection"
  import="java.sql.ResultSet"
  import="java.sql.Timestamp"
%>

<head>
<title>User Schedule</title>
</head>

<%

  int currentWeekNum = TeamHoursUtil.weekNumber(new GregorianCalendar());
  for (int selectedWeekNum = currentWeekNum - 2; selectedWeekNum < currentWeekNum + 3; selectedWeekNum++) {
    List allIssues = TimeAssignmentsByWeek.loadWeek(selectedWeekNum);

    String username = request.getParameter("username");
    if (username=null) {
      out.println("<h2>You must log in to use this tool.</h2>");
      out.println("<p>");
      return;
    }

    // now filter only for issues for this user
    List filteredIssues = new ArrayList();
    long estSeconds = 0;
    int numUnresolved = 0;
    for (Iterator issueIter = allIssues.iterator(); issueIter.hasNext(); ) {
      TimeAssignmentsByWeek.IssueInfo info = (TimeAssignmentsByWeek.IssueInfo) issueIter.next();
      if (username.equals(info.getAssignee())) {
        filteredIssues.add(info);
        if (!info.getResolved().booleanValue()) {
          estSeconds += info.getEstSeconds().longValue();
          numUnresolved++;
        }
      }
    }

    Collections.sort(filteredIssues, new TimeAssignmentsByWeek.IssueInfoPrioritySorter());

    // show issues if there are any unresolved or if it's a current or future week
    if (numUnresolved > 0 || selectedWeekNum >= currentWeekNum) {

      out.println("<hr>");

      // (duplicates code in weekly-due-tasks.jsp)

      // retrieve time spent
      int totalTimeSpent = 0;
      if (filteredIssues.size() > 0) {
	StringBuffer issueParamSql = new StringBuffer();
	Object[] issueParams = new Object[filteredIssues.size() + 2];
	for (int i = 0; i < filteredIssues.size(); i++) {
	  if (i > 0) { issueParamSql.append(","); }
	  issueParamSql.append("?");
	  issueParams[i] = ((TimeAssignmentsByWeek.IssueInfo) filteredIssues.get(i)).getId();
	}
        issueParams[filteredIssues.size()] = 
          new Timestamp(TeamHoursUtil.weekCal(selectedWeekNum).getTime().getTime());
        issueParams[filteredIssues.size() + 1] = 
          new Timestamp(TeamHoursUtil.weekCal(selectedWeekNum + 1).getTime().getTime());
	String sql = 
	  " select sum(actionnum) as seconds "
	  + " from jiraaction ja "
	  + " where actiontype = 'worklog' "
	  + " and ja.issueid in (" + issueParamSql + ") "
	  + " and ? <= ja.created and ja.created < ? ";
	Connection conn = null;
	try {
	  conn = ForecastUtil.getConnection();
	  Object[] results = SimpleSQL.getOneRow(sql, issueParams, conn);
          if (results[0] != null) {
            totalTimeSpent = ((Number) results[0]).intValue();
          }
	} finally {
	  try { conn.close(); } catch (Exception e) {}
	}
      }

      int totalIssues = filteredIssues.size();

      out.println("<p>");
      out.println("Scheduled hours: " + TimeCompleteBar.DECIMAL_FORMATTER.format(estSeconds / 3600.0) + " remaining"); 
      out.println(" for week " + selectedWeekNum + " starting " + TeamHoursUtil.weekCal(selectedWeekNum).getTime() + ".");
      out.println(TimeCompleteBar.roadMapTable("Tasks", totalIssues - numUnresolved, numUnresolved, true));
      out.println(TimeCompleteBar.roadMapTable("Hours", totalTimeSpent / 3600.0, estSeconds / 3600.0, false));
      out.println("<p>");
      out.println("<table>\n");
      out.println(" <tr>");
      out.println("   <td>Key</td");
      out.println("   <td>Hours<br>Remaining</td");
      out.println("   <td>Assignee</td");
      out.println("   <td>Summary</td");
      out.println("   <td>Version</td");
      out.println("   <td>Due Date</td");
      out.println("   <td>Priority</td");
      out.println(" </tr>");
      for (Iterator issueIter = filteredIssues.iterator(); issueIter.hasNext(); ) {
        TimeAssignmentsByWeek.IssueInfo issue = (TimeAssignmentsByWeek.IssueInfo) issueIter.next();
        out.println(" <tr>");
        String strike = issue.getResolved().booleanValue() ? "<strike>" : "";
        String strikeEnd = issue.getResolved().booleanValue() ? "</strike>" : "";
        out.println("  <td><a href='/browse/" + issue.getKey() + "'>" + strike + issue.getKey() + strikeEnd + "</a></td>");
        out.println("  <td>" + TimeCompleteBar.DECIMAL_FORMATTER.format(issue.getEstSeconds().longValue() / 3600.0) + "</td>");
        out.println("  <td>" + issue.getAssignee() + "</td>");
        out.println("  <td>" + issue.getSummary() + "</td>");
        out.println("  <td>" + issue.getVersion() + "</td>");
        out.println("  <td>" + issue.getDueDate() + "</td>");
        out.println("  <td>" + issue.getPriorityName() + "</td>");
        out.println(" </tr>");
      }
      out.println("</table>");
    }
  }

%>
