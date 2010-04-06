<%@ page
  import="java.util.*"

  import="com.icentris.jira.actions.TimeScheduleAction"
  import="com.icentris.sql.SimpleSQL"
  import="java.sql.Connection"
  import="java.sql.ResultSet"
  import="java.sql.Timestamp"
%>

<head>
<title>Company Hours</title>
</head>



<%

  int currentWeekNum = TeamHoursUtil.weekNumber(new GregorianCalendar());
  for (int selectedWeekNum = currentWeekNum; selectedWeekNum < currentWeekNum + 3; selectedWeekNum++) {

// pull all the issues for a team
// then divide up by company

      Connection conn = null;
      ResultSet rset = null;
      try {
        conn = com.icentris.jira.actions.TimeScheduleAction.getConnection();
        Set userSet = new TreeSet();
        String sql =
          "select distinct username from team_hours "
          + " where team_id = ? and ? <= start_of_week and start_of_week < ?";
        Object[] args = { team.getId(), firstWeekStart, lastWeekEnd };
        rset =
          SimpleSQL.executeQuery
            (sql, args, conn);
        while (rset.next()) {
          usersInOrder.add(rset.getString("username"));
        }
      } catch (SQLException e) {
        throw new java.lang.reflect.UndeclaredThrowableException(e);
      } finally {
        try { rset.close(); } catch (Exception e) {}
        try { conn.close(); } catch (Exception e) {}
      }

  }

%>
