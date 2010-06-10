<%@ page
  import="java.util.*"
  import="java.text.*"
%>

<head>
<title>Scheduling Changes</title>
</head>


<%

  // load helper data
  Map<Integer,String> projectIdToName = new TreeMap();
  SortedMap<String,Integer> projectNameToId = new TreeMap();

  Map<Integer,String> priorityIdToName = new TreeMap();
  SortedMap<String,Integer> priorityNameToId = new TreeMap();

  java.sql.Connection conn = null;
  java.sql.ResultSet rset = null;
  try {
    conn = com.trentlarson.forecast.core.helper.ForecastUtil.getConnection();

    // load projects
    String projectSql = "select * from project";
    rset = com.icentris.sql.SimpleSQL.executeQuery(projectSql, new Object[0], conn);
    while (rset.next()) {
      projectIdToName.put(Integer.valueOf(rset.getInt("id")), rset.getString("pname"));
      projectNameToId.put(rset.getString("pname"), Integer.valueOf(rset.getInt("id")));
    }
    rset.close();

    // load priorities
    String prioritySql = "select * from priority order by sequence";
    rset = com.icentris.sql.SimpleSQL.executeQuery(prioritySql, new Object[0], conn);
    while (rset.next()) {
      priorityIdToName.put(Integer.valueOf(rset.getInt("id")), rset.getString("pname"));
      priorityNameToId.put(rset.getString("pname"), Integer.valueOf(rset.getInt("id")));
    }
    rset.close();

  } finally {
    try { rset.close(); } catch (Exception e2) {}
    try { conn.close(); } catch (Exception e2) {}
  }


  // get selection info
  String maxPriority = request.getParameter("priority");
  if (maxPriority == null) {
    maxPriority = "6";
  }
  String[] selectedProjectIds = request.getParameterValues("projects");
  if (selectedProjectIds == null
      || selectedProjectIds.length == 0) {
    selectedProjectIds = new String[]{ "10240" };
  }
  Calendar selectedCal;
  if (request.getParameter("date") == null) {
    selectedCal = Calendar.getInstance();
    selectedCal.set(Calendar.DAY_OF_WEEK, Calendar.FRIDAY);
    selectedCal.add(Calendar.WEEK_OF_YEAR, -1);
    selectedCal.set(Calendar.HOUR_OF_DAY, 0);
    selectedCal.set(Calendar.MINUTE, 0);
    selectedCal.set(Calendar.SECOND, 0);
    selectedCal.set(Calendar.MILLISECOND, 0);
  } else {
    selectedCal = getMostRecent(request.getParameter("date"));
  }


%>

<form>

All changes to

priority rank
<select name='priority'>
<% for (String name : priorityNameToId.keySet()) { %>
  <% boolean selected = priorityNameToId.get(name) == Integer.valueOf(maxPriority).intValue(); %>
  <option value='<%=priorityNameToId.get(name)%>' <%= (selected ? "SELECTED" : "") %>>
    <%=name%>
  </option>
<% } %>
</select>
or higher (up to 0),

for projects
<select name='projects' multiple size='5'>
<% List selectedProjectIdList = Arrays.asList(selectedProjectIds); %>
<% for (String name : projectNameToId.keySet()) { %>
  <% boolean selected = selectedProjectIdList.contains(projectNameToId.get(name).toString()); %>
  <option value='<%=projectNameToId.get(name)%>' <%= (selected ? "SELECTED" : "") %>>
    <%=name%>
  </option>
<% } %>
</select>

since
<input type='text' name='date' size='6' value='<%=MMDD.format(selectedCal.getTime())%>'>

<input type='submit' value='Filter'/>

</form>



<%

  // Display the selections.


  Map<Integer,String> typeIdToName = new HashMap();
  Map<String,IssueChange> changedIssues = new HashMap();

  conn = null;
  rset = null;
  try {
    conn = com.trentlarson.forecast.core.helper.ForecastUtil.getConnection();

    // load helper data - issue types
    String typesSql = "select * from issuetype";
    rset = com.icentris.sql.SimpleSQL.executeQuery(typesSql, new Object[0], conn);
    while (rset.next()) {
      typeIdToName.put(Integer.valueOf(rset.getInt("id")), rset.getString("pname"));
    }
    rset.close();

    // load selected issue data
    if (selectedProjectIds.length == 0) {
      return; // because this would make an invalid query
    }
    String qMarks = "?";
    for (int i = 1; i < selectedProjectIds.length; i++) {
      qMarks += ", ?";
    }
    // -- first load the change records
    String changesSql =
      "select ji.pkey, ji.issuetype, "
      + " to_char(ci.oldvalue) as old_value, "
      + " to_char(ci.newvalue) as new_value, cg.author, "
      + " ji.timeestimate as est_seconds"
      + " from changeitem ci, changegroup cg, jiraissue ji"
      + " where ci.field = 'priority'"
      + " and to_char(ci.newvalue) < to_char(ci.oldvalue)"
      + " and to_char(ci.newvalue) in (select id from priority where sequence <= (select sequence from priority p2 where p2.id = ?))"
      + " and cg.id = ci.groupid"
      + " and cg.created >= ?"
      + " and ji.id = cg.issueid"
      + " and ji.project in (" + qMarks+ ")"
      + " order by cg.created desc";
    List argList = new ArrayList();
    argList.add(Integer.valueOf(maxPriority));
    argList.add(new java.sql.Timestamp(selectedCal.getTime().getTime()));
    argList.addAll(Arrays.asList(selectedProjectIds));
    Object[] args = argList.toArray();
    rset = com.icentris.sql.SimpleSQL.executeQuery(changesSql, args, conn);
    while (rset.next()) {
      if (!changedIssues.containsKey(rset.getString("pkey"))) {
        IssueChange change = new IssueChange(rset, typeIdToName, priorityIdToName);
        changedIssues.put(rset.getString("pkey"), change);
      }
    }
    rset.close();

    // -- now load the new issues
    String newIssueSql =
      "select pkey, issuetype, null as old_value, priority as new_value, "
      + " reporter as author, timeestimate as est_seconds "
      + " from jiraissue "
      + " where "
      + " priority in (select id from priority where sequence <= (select sequence from priority p2 where p2.id = ?))"
      + " and created >= ? "
      + " and project in (" + qMarks + ")";
    rset = com.icentris.sql.SimpleSQL.executeQuery(newIssueSql, args, conn);
    while (rset.next()) {
      if (!changedIssues.containsKey(rset.getString("pkey"))) {
        IssueChange change = new IssueChange(rset, typeIdToName, priorityIdToName);
        changedIssues.put(rset.getString("pkey"), change);
      }
    }
    rset.close();

    // output the results
    out.println("<table>");
      out.println("<tr>");
    out.println("  <td>Key</td>");
    out.println("  <td>Type</td>");
    out.println("  <td>Changed by</td>");
    out.println("  <td>Old</td>");
    out.println("  <td>New</td>");
    out.println("  <td>Est Hours</td>");
    out.println("</tr>");
    for (IssueChange change : changedIssues.values()) {
      out.println("<tr>");
      out.println("  <td>" + change.getKey() + "</td>");
      out.println("  <td>" + change.getType() + "</td>");
      out.println("  <td>" + change.getAuthor() + "</td>");
      out.println("  <td>" + change.getOldPriority() + "</td>");
      out.println("  <td>" + change.getNewPriority() + "</td>");
      out.println("  <td>" + change.getEstHours() + "</td>");
      out.println("</tr>");
    }
    out.println("</table>");
  } finally {
    try { rset.close(); } catch (Exception e2) {}
    try { conn.close(); } catch (Exception e2) {}
  }

  

%>

<%!

public static DateFormat MMDD = new SimpleDateFormat("MM/dd"); // date in 1970

public static Calendar getMostRecent(String monthNum1To12AndDay) throws ParseException {
  Date resultDate = MMDD.parse(monthNum1To12AndDay);
  Calendar resultCal = Calendar.getInstance();
  resultCal.setTime(resultDate);
  resultCal.set(Calendar.YEAR, Calendar.getInstance().get(Calendar.YEAR));
  if (resultCal.getTime().after(new Date())) {
    resultCal.add(Calendar.YEAR, -1);
  }
  return resultCal;
}

public class IssueChange {
  public String key, type, author, oldPriority, newPriority;
  public double estHours;
  public IssueChange(java.sql.ResultSet rset, Map<Integer,String> typeIdToName, 
    Map<Integer,String> priorityIdToName)
    throws java.sql.SQLException {

    key = rset.getString("pkey");
    type = typeIdToName.get(Integer.valueOf(rset.getInt("issuetype")));
    author = rset.getString("author");
    int oldVal = rset.getInt("old_value");
    if (rset.wasNull()) { 
      oldPriority = ""; 
    } else {
      oldPriority = priorityIdToName.get(Integer.valueOf(oldVal));
    }
    int newVal = rset.getInt("new_value");
    newPriority = priorityIdToName.get(Integer.valueOf(newVal));
    estHours = rset.getInt("est_seconds") / 3600.0;
  }
  public String getKey() { return key; }
  public String getType() { return type; }
  public String getAuthor() { return author; }
  public String getOldPriority() { return oldPriority; }
  public String getNewPriority() { return newPriority; }
  public double getEstHours() { return estHours; }
}

%>
