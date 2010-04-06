<html>
<head>
<title>Task</title>
</head>
<body>

<%

String id = request.getParameter("id");
String assignee = request.getParameter("assignee");
String duedate = request.getParameter("duedate");
String pkey = request.getParameter("pkey");
String priority = request.getParameter("priority");
String project = request.getParameter("project");
String resolution = request.getParameter("resolution");
String summary = request.getParameter("summary");
String startdate = request.getParameter("startdate");
String timeestimate = request.getParameter("timeestimate");
String timespent = request.getParameter("timespent");

if (id != null && id.length != null) {


}

%>

<form action="">
 ID: <input type="text" name="id" value="<%= request.getParameter("id") %>" />
 assignee: <input type="text" name="assignee" value="<%= request.getParameter("assignee") %>" />
 duedate: <input type="text" name="duedate" value="<%= request.getParameter("duedate") %>" />
 pkey: <input type="text" name="pkey" value="<%= request.getParameter("pkey") %>" />
 priority: <input type="text" name="priority" value="<%= request.getParameter("priority") %>" />
 project: <input type="text" name="project" value="<%= request.getParameter("project") %>" />
 resolution: <input type="text" name="resolution" value="<%= request.getParameter("resolution") %>" />
 summary: <input type="text" name="summary" value="<%= request.getParameter("summary") %>" />
 startdate: <input type="text" name="startdate" value="<%= request.getParameter("startdate") %>" />
 timeestimate: <input type="text" name="timeestimate" value="<%= request.getParameter("timeestimate") %>" />
 timespent: <input type="text" name="timespent" value="<%= request.getParameter("timespent") %>" />
 <input type="submit" />
</form>


</body>
</html>