<%@ page
  import="java.util.*"
  import="com.icentris.jira.dao.Team"
  import="com.icentris.jira.dao.TeamHours"
  import="com.icentris.jira.helper.ForecastUtil"
  import="com.icentris.jira.dao.TeamHours"
  import="com.icentris.jira.scheduling.TeamHoursUtil"
  import="com.icentris.jira.scheduling.Teams"
  import="com.icentris.jira.scheduling.TimeAssignmentsByWeek"
  import="com.icentris.jira.scheduling.TimeCompleteBar"

  import="org.hibernate.*"

  import="com.icentris.jira.actions.TimeScheduleAction"
  import="com.icentris.sql.SimpleSQL"
  import="java.sql.Connection"
  import="java.sql.ResultSet"
  import="java.sql.Timestamp"
%>

<head>
<title>Team Time Schedule</title>
</head>

<h1>Team Time Schedule</h1>

<%
  int weekNum = TeamHoursUtil.weekNumber(new GregorianCalendar());
  int selectedWeekNum = weekNum;
  if (request.getParameter("week") != null) {
    selectedWeekNum = new Integer(request.getParameter("week")).intValue();
  }
  Date selectedDate = TeamHoursUtil.weekCal(selectedWeekNum).getTime();
%>

<form name="weekSelector" action="weekly-due-tasks.jsp" method="GET">
<select name="week" onChange="javascript:submit()">
  <% for (int i = weekNum - 12; i < weekNum + 2; i++) { %>
  <option value="<%= i %>" <%=(i == selectedWeekNum ? "SELECTED" : "")%>>
    Week # <%= i %> - by <%= TeamHoursUtil.weekCal(i + 1).getTime() %>
  </option>
  <% } %>
</select>

Sort by 
<a href="?week=<%=selectedWeekNum%>&SORT_BY=assignee">assignee</a>
or
<a href="?week=<%=selectedWeekNum%>&SORT_BY=dueDate">due date</a>
or
<a href="?week=<%=selectedWeekNum%>&SORT_BY=company">company</a>
or
<a href="?week=<%=selectedWeekNum%>">nothing</a>.

</form>

<%

  Session sess = TeamHoursUtil.HibernateUtil.currentSession();
  List allTeams = sess.getNamedQuery("getAllTeams").list();
  TeamHoursUtil.HibernateUtil.closeSession();

  List allIssues = TimeAssignmentsByWeek.loadWeek(selectedWeekNum);

  for (Iterator teamIter = allTeams.iterator(); teamIter.hasNext(); ) {
    Team currentTeam = (Team) teamIter.next();
    out.println("<p>");
    out.println("<h4>" + currentTeam.getName() + "</h4>");

    // now filter only for issues for this team
    List filteredIssues = new ArrayList();
    long estSeconds = 0;
    int numUnresolved = 0;
    for (Iterator issueIter = allIssues.iterator(); issueIter.hasNext(); ) {
      TimeAssignmentsByWeek.IssueInfo info = (TimeAssignmentsByWeek.IssueInfo) issueIter.next();
      if (info.getProject().equals(currentTeam.getProjectId())) {
        filteredIssues.add(info);
        estSeconds += info.getEstSeconds().longValue();
        if (!info.getResolved().booleanValue()) {
          numUnresolved++;
        }
      }
    }

    if (request.getParameter("SORT_BY") != null) {
      if (request.getParameter("SORT_BY").equals("assignee")) {
        Collections.sort(filteredIssues, new TimeAssignmentsByWeek.IssueInfoAssigneeSorter());
      } else if (request.getParameter("SORT_BY").equals("dueDate")) {
        Collections.sort(filteredIssues, new TimeAssignmentsByWeek.IssueDueDateSorter());
      } else if (request.getParameter("SORT_BY").equals("company")) {
        Collections.sort(filteredIssues, new TimeAssignmentsByWeek.IssueCompanySorter());
      }
    }


    // show issues

      // (duplicates code in scheduled-user.jsp)

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
      out.println("   <td>Company</td");
      out.println("   <td>Due Date</td");
      out.println("   <td>Priority</td");
      out.println("   <td>Total<br>Remaining</td");
      out.println(" </tr>");
      Iterator issueIter = filteredIssues.iterator();
      TimeAssignmentsByWeek.IssueInfo prevIssue = null;
      long sortedTotal = 0L;
      while (issueIter.hasNext()) {
        TimeAssignmentsByWeek.IssueInfo issue = (TimeAssignmentsByWeek.IssueInfo) issueIter.next();

        // if sorting, calculate and add row for total estimated seconds
        if (request.getParameter("SORT_BY") != null
            && prevIssue != null) {
          boolean newValue = false;
          long prevSortedTotal = 0L;
          if (request.getParameter("SORT_BY").equals("assignee")) {
            if (!issue.getAssignee().equals(prevIssue.getAssignee())) {
              newValue = true;
              prevSortedTotal = sortedTotal;
              sortedTotal = 0L;
            }
          } else if (request.getParameter("SORT_BY").equals("dueDate")) {
            if (!issue.getDueDate().equals(prevIssue.getDueDate())) {
              newValue = true;
              prevSortedTotal = sortedTotal;
              sortedTotal = 0L;
            }
          } else if (request.getParameter("SORT_BY").equals("company")) {
            if (!issue.getCompany().equals(prevIssue.getCompany())) {
              newValue = true;
              prevSortedTotal = sortedTotal;
              sortedTotal = 0L;
            }
          }
          if (newValue) {
            out.println(" <tr>");
            out.println("  <td></td>");
            out.println("  <td></td>");
            out.println("  <td></td>");
            out.println("  <td></td>");
            out.println("  <td></td>");
            out.println("  <td></td>");
            out.println("  <td></td>");
            out.println("  <td>" + TimeCompleteBar.DECIMAL_FORMATTER.format(prevSortedTotal / 3600.0) + "</td>");
            out.println(" </tr>");
          }
        }
        sortedTotal += issue.getEstSeconds();

        out.println(" <tr>");
        String strike = issue.getResolved().booleanValue() ? "<strike>" : "";
        String strikeEnd = issue.getResolved().booleanValue() ? "</strike>" : "";
        out.println("  <td><a href='/browse/" + issue.getKey() + "'>" + strike + issue.getKey() + strikeEnd + "</a></td>");
        out.println("  <td>" + TimeCompleteBar.DECIMAL_FORMATTER.format(issue.getEstSeconds().longValue() / 3600.0) + "</td>");
        out.println("  <td>" + issue.getAssignee() + "</td>");
        out.println("  <td>" + issue.getSummary() + "</td>");
        out.println("  <td>" + issue.getCompany() + "</td>");
        out.println("  <td>" + issue.getDueDate() + "</td>");
        out.println("  <td>" + issue.getPriorityName() + "</td>");
        out.println("  <td></td>");
        out.println(" </tr>");
        prevIssue = issue;
      } 
      out.println(" <tr>");
      out.println("  <td></td>");
      out.println("  <td></td>");
      out.println("  <td></td>");
      out.println("  <td></td>");
      out.println("  <td></td>");
      out.println("  <td></td>");
      out.println("  <td></td>");
      out.println("  <td>" + TimeCompleteBar.DECIMAL_FORMATTER.format(sortedTotal / 3600.0) + "</td>");
      out.println(" </tr>");
      out.println("</table>");

  }

%>

