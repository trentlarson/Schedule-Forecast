<%@ page
  import="java.util.*"
  import="java.text.*"
  import="org.hibernate.*"
  import="org.hibernate.cfg.*"
  import="com.trentlarson.forecast.core.dao.Team"
  import="com.trentlarson.forecast.core.dao.TeamHours"
  import="com.trentlarson.forecast.core.scheduling.TeamHoursUtil"
  import="com.trentlarson.forecast.core.scheduling.Teams"
  import="com.trentlarson.forecast.core.scheduling.*"
%>

<head>
<title>Time Allocation</title>
</head>


<h1> Working Hours Available </h1>

Note that any individual who doesn't show in this list will get the
default of <%=TimeSchedule.TYPICAL_INDIVIDUAL_WORKHOURS_PER_WORKWEEK%>
hours per week (total for all their work on all teams).


<%
int firstWeek = TeamHoursUtil.weekNumber();
int numWeeks = 4;
%>


<%
// Process form submission
if (CHANGE_HOURS_COMMAND.equals(request.getParameter(COMMAND_PARAM_NAME))) {

  java.sql.Connection conn = null;
  java.sql.ResultSet rset = null;
  try {
    conn = com.trentlarson.forecast.core.helper.ForecastUtil.getConnection();
    String eraseSql =
      "delete from team_hours where start_of_week >= ?";
    Object[] args = { new java.sql.Timestamp(TeamHoursUtil.weekCal(firstWeek).getTime().getTime()) };
    rset = com.icentris.sql.SimpleSQL.executeQuery(eraseSql, args, conn);
  } finally {
    try { rset.close(); } catch (Exception e2) {}
    try { conn.close(); } catch (Exception e2) {}
  }
  
  for (Enumeration paramIter = request.getParameterNames(); paramIter.hasMoreElements(); ) {
    String paramName = (String) paramIter.nextElement();
    if (paramName.startsWith("teamHoursEntry")
        && request.getParameter(paramName).length() > 0) {

      int teamIdPos = paramName.indexOf(".teamId-");
      int usernamePos = paramName.indexOf(".username-");
      int weekNumPos = paramName.indexOf(".weekNum-");

      String teamIdStr = paramName.substring(teamIdPos + ".teamId-".length(), usernamePos);
      Long teamId = (teamIdStr.equals("null") || teamIdStr.equals("")) ? null : Long.valueOf(teamIdStr);

      String usernameStr = paramName.substring(usernamePos + ".username-".length(), weekNumPos);
      String username = (usernameStr.equals("null") || usernameStr.equals("")) ? null : usernameStr;

      String weekNumStr = paramName.substring(weekNumPos + ".weekNum-".length());
      int weekNum = Integer.valueOf(weekNumStr).intValue();

      String hoursStr = request.getParameter(paramName);
      Double hours = new Double(hoursStr);

      try {
        conn = com.trentlarson.forecast.core.helper.ForecastUtil.getConnection();
        // Oracle
        //String createSql = "insert into team_hours values (team_hours_seq.nextval, ?, ?, ?, ?, ?, ?)";
        // MySql
        String createSql = "insert into team_hours values (null, ?, ?, ?, ?, ?, ?)";
        Object[] args = {
          new java.sql.Timestamp(System.currentTimeMillis()),
          new java.sql.Timestamp(System.currentTimeMillis()),
          teamId,
          username,
          new java.sql.Timestamp(TeamHoursUtil.weekCal(weekNum).getTime().getTime()),
          hours
        };
        int result = com.icentris.sql.SimpleSQL.executeUpdate(createSql, args, conn);
        if (result != 1) {
          System.out.println("WARNING: no entries were updated; expected 1 for args: " + Arrays.asList(args));
        }
      } finally {
        try { rset.close(); } catch (Exception e2) {}
        try { conn.close(); } catch (Exception e2) {}
      }
    }
  }
}

if (ADD_USER_COMMAND.equals(request.getParameter(COMMAND_PARAM_NAME))) {
  String usernameStr = request.getParameter(ADD_USER_NAME_INPUT);
  String teamIdStr = request.getParameter(ADD_USER_TEAM_INPUT);
  Long teamId = null;
  if (!teamIdStr.equals("null")) {
    teamId = Long.valueOf(teamIdStr);
  }
  if (usernameStr.length() == 0) {
    if (teamId == null) {
      throw new IllegalStateException("You cannot make general 'unassigned' hours (ie. without a team).");
    }
  }
      String username = usernameStr.equals("") ? null : usernameStr;
      java.sql.Connection conn = null;
      try {
        conn = com.trentlarson.forecast.core.helper.ForecastUtil.getConnection();
        // Oracle
        //String createSql = "insert into team_hours values (team_hours_seq.nextval, ?, ?, ?, ?, ?, ?)";
        // MySql
        String createSql = "insert into team_hours values (null, ?, ?, ?, ?, ?, ?)";
        Object[] args = {
          new java.sql.Timestamp(System.currentTimeMillis()),
          new java.sql.Timestamp(System.currentTimeMillis()),
          teamId,
          username,
          new java.sql.Timestamp(TeamHoursUtil.weekCal(firstWeek).getTime().getTime()),
          TimeSchedule.TYPICAL_INDIVIDUAL_WORKHOURS_PER_WORKWEEK
        };
        int result = com.icentris.sql.SimpleSQL.executeUpdate(createSql, args, conn);
        if (result != 1) {
          System.out.println("WARNING: no entries were updated; expected 1 for args " + Arrays.asList(args));
        }
      } finally {
        try { conn.close(); } catch (Exception e2) {}
      }
}
%>



<%
List<Team> allTeams = TeamHoursUtil.getTeams();
Map<Long,String> projectNames = TeamHoursUtil.getProjects();
Map<Long,Team> teamMap = new HashMap();
for (Team team : allTeams) {
  teamMap.put(team.getId(), team);
}
%>


<form method='post'>

<%
Map<Teams.UserTimeKey,SortedSet<TimeSchedule.HoursForTimeSpan>> userWeeklyHours = 
  loadUserWeeklyHours(new Date());

  out.println("<table>");
  out.println("  <tr>");
  out.println("    <td></td>");
  out.println("    <td>Previously</td>");
  for (int i = firstWeek; i < firstWeek + numWeeks; i++) {
    out.println("    <td>" + dayFormatter.format(TeamHoursUtil.weekCal(i).getTime()) + "</td>");
  }
  out.println("    <td>That rate continues...</td>");
  out.println("  </tr>");
  for (Teams.UserTimeKey teamUserId : userWeeklyHours.keySet()) {
    SortedSet<TimeSchedule.HoursForTimeSpan> hourSet =
      userWeeklyHours.get(teamUserId);
    out.println("  <tr>");
    out.println("    <td>");
    out.println("      " + teamUserId);
    if (teamUserId.getTeamId() != null 
        && teamUserId.getTeamId() != 0
        && teamMap.get(teamUserId.getTeamId()).getProjectId() != null) {
      out.println(projectNames.get(teamMap.get(teamUserId.getTeamId()).getProjectId()));
    } else {
      out.println("(any team)");
    }
    out.println("      " + (teamUserId.getUsername() == null ? "(unassigned)" : ""));
    out.println("    </td>");
    Iterator<TimeSchedule.HoursForTimeSpan> spanIter = hourSet.iterator();
    TimeSchedule.HoursForTimeSpan currentSpan = 
      (spanIter.hasNext() ? spanIter.next() : null);
    String previousHours = "";
    if (currentSpan != null) {
      if (currentSpan.getStartOfTimeSpan()
          .before(TeamHoursUtil.weekCal(firstWeek).getTime())) {
        previousHours = String.valueOf(currentSpan.getHoursAvailable());
        currentSpan = (spanIter.hasNext() ? spanIter.next() : null);
      }
    }
    out.println("    <td align='right'>" + previousHours + "</td>");
    TimeSchedule.HoursForTimeSpan nextSpan =
      (spanIter.hasNext() ? spanIter.next() : null);
    for (int eachWeek = firstWeek; eachWeek < firstWeek + numWeeks; eachWeek++) {
      Double newHours = null;
      if (currentSpan != null
          && currentSpan.getStartOfTimeSpan()
             .equals(TeamHoursUtil.weekCal(eachWeek).getTime())) {
        newHours = currentSpan.getHoursAvailable();
        currentSpan = nextSpan;
        nextSpan = (spanIter.hasNext() ? spanIter.next() : null);
      }
      out.print("    <td align='right'>");
      out.print(hourEntryInput(teamUserId, eachWeek, newHours));
      out.println("</td>");
    }
    out.println("<td>");
    if (currentSpan != null) {
      out.println("until " + dayFormatter.format(currentSpan.getStartOfTimeSpan().getTime()));
    } else {
      out.println("forever");
    }
    out.println("</td>");    
    out.println("  </tr>");
  }
  out.println("</table>");
%>

  <input type='hidden' name='command' value='<%=CHANGE_HOURS_COMMAND%>'>
  <input type='submit' value='Commit these changes.'>
</form>



<form action='team-hours.jsp' method='post'>
  User <input type='text' name='<%=ADD_USER_NAME_INPUT%>' value=''/>

  on
  <select name='<%=ADD_USER_TEAM_INPUT%>'>
    <option value='null'>Any team</option>
  <% for (Team team : allTeams) { %>
    <option value='<%=team.getId()%>'><%=team.getName()%></option>
  <% } %>
  </select>

  <input type='hidden' name='command' value='<%=ADD_USER_COMMAND%>'>
  <input type='submit' value='Add default time record.'>
</form>




<%!
  final DateFormat dayFormatter = DateFormat.getDateInstance(DateFormat.MEDIUM);
  final String COMMAND_PARAM_NAME = "command";
  final String ADD_USER_COMMAND = "add_user";
  final String CHANGE_HOURS_COMMAND = "change_hours";
  final String ADD_USER_NAME_INPUT = "user-name";
  final String ADD_USER_TEAM_INPUT = "user-team";

  private static String hourEntryInput
      (Teams.UserTimeKey key, int weekNum, Double hoursAvailable) {
    StringBuilder sb = new StringBuilder();
    String hoursStr = (hoursAvailable == null ? "" : String.valueOf(hoursAvailable));
    sb.append(
      "<input type='text' name='" 
      + "teamHoursEntry.teamId-" + key.getTeamId()
      + ".username-" + key.getUsername() + ".weekNum-" + weekNum
      + "' value='" 
      + (hoursAvailable == null ? "" : hoursAvailable)
      + "' size='3'>");
    return sb.toString();
  }

// @deprecated use TimeSchedule.loadUserWeeklyHours
/**
**/
private static Map<Teams.UserTimeKey,SortedSet<TimeSchedule.HoursForTimeSpan>> loadUserWeeklyHours
  (Date startDate) throws java.sql.SQLException {

  Map<Teams.UserTimeKey,SortedSet<TimeSchedule.HoursForTimeSpan>> userWeeklyHours =
    new HashMap();
  java.sql.Connection conn = null;
  java.sql.ResultSet rset = null;
  try {
/**
    Session sess = TeamHoursUtil.HibernateUtil.currentSession();
    Transaction tx = sess.beginTransaction();
    List<TeamHours> teamHours = sess.createQuery("from TeamHours order by team_id, username, start_of_week desc").list();
    TeamHoursUtil.HibernateUtil.closeSession();
    System.out.println("+++" + teamHours);
**/

      conn = com.trentlarson.forecast.core.helper.ForecastUtil.getConnection();
      String timeSql = 
        "select team_id, username, start_of_week, hours_available"
        + " from team_hours"
        + " order by team_id, username, start_of_week desc";
      rset = com.icentris.sql.SimpleSQL.executeQuery(timeSql, new Object[0], conn);

      Teams.UserTimeKey currentAssignee = null;
      // set when we don't need to go back any further in time for this assignee
      boolean doneWithCurrent = false;
      SortedSet<TimeSchedule.HoursForTimeSpan> weeklyHours = new TreeSet();
      while (rset.next()) {
        Long teamId = rset.getLong("team_id");
        if (rset.wasNull()) {
          teamId = null;
        }
        Teams.UserTimeKey nextAssignee =
          new Teams.UserTimeKey(teamId, rset.getString("username"));
        if (currentAssignee == null
            || currentAssignee.compareTo(nextAssignee) != 0) {
          currentAssignee = nextAssignee;
          doneWithCurrent = false;
          weeklyHours = new TreeSet();
          userWeeklyHours.put(currentAssignee, weeklyHours);
        }

        if (nextAssignee.compareTo(currentAssignee) == 0) {
          if (!doneWithCurrent) {
            // if the first has the same number of hours, replace it
            if (weeklyHours.size() > 0 
                && weeklyHours.first().getHoursAvailable() 
                   == rset.getDouble("hours_available")) {
              weeklyHours.remove(weeklyHours.first());
            }
            // add this time slice
            weeklyHours
              .add(new TimeSchedule.HoursForTimeSpanOriginal
                   (rset.getDate("start_of_week"),
                    rset.getDouble("hours_available")));
            // if this was before or equal to scheduleStartDate, we're finished
            if (!rset.getDate("start_of_week").after(startDate)) {
              doneWithCurrent = true;
            }
          }
        }
      }

  } finally {
    try { rset.close(); } catch (Exception e2) {}
    try { conn.close(); } catch (Exception e2) {}
  }

  return userWeeklyHours;
}

%>

<!--
Loaded time: <% out.println(userWeeklyHours.toString()); %>
-->
