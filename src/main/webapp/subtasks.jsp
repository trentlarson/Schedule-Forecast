<%@ page
  import="java.io.PrintWriter"
  import="java.sql.Connection"
  import="java.sql.ResultSet"
  import="java.sql.SQLException"
  import="java.text.SimpleDateFormat"
  import="java.util.*"
  import="com.icentris.sql.SimpleSQL"
  import="com.trentlarson.forecast.core.scheduling.IssueTree"
  import="com.trentlarson.forecast.core.scheduling.TimeScheduleLoader"
  import="com.trentlarson.forecast.core.scheduling.TimeCompleteBar"
  import="com.trentlarson.forecast.core.helper.ForecastUtil"

  import="com.atlassian.jira.ComponentManager"
  import="com.atlassian.jira.issue.CustomFieldManager"
  import="com.atlassian.jira.issue.MutableIssue"
  import="com.atlassian.jira.issue.fields.CustomField"
  import="org.ofbiz.core.entity.GenericValue"

%>
<%@ taglib uri="jiratags" prefix="jira" %>
<jira:user id="user" />

<head>
<title>Subtask Status</title>
</head>

<%

  // exit if they aren't logged in
  if (user == null) {
    out.println("<h2>You must log in to see this report.</h2>");
    out.println("<p>");
    return;
  }


  // check and/or update the selected issue
  String issueKey = (String) session.getAttribute(ISSUE_KEY_SES_NAME);
  if (issueKey == null) {
    issueKey = "";
    session.setAttribute(ISSUE_KEY_SES_NAME, issueKey);
  }
  if (request.getParameter(ISSUE_KEY_REQ_NAME) != null) {
    issueKey = request.getParameter(ISSUE_KEY_REQ_NAME).toUpperCase();
    session.setAttribute(ISSUE_KEY_SES_NAME, issueKey);
  }
  String[] keyArray = issueKey.split("[ ]");
  if (issueKey.length() == 0) {
    keyArray = new String[0];
  }



  // check and/or update whether to show quick-create feature
  Boolean showQuickCreate = false;
  if (request.getParameter(QUICK_REQ_NAME) != null) {
    showQuickCreate = true;
  }

%>

<%
  // this quick-creates the subtasks
  for (Enumeration nameEnum = request.getParameterNames(); nameEnum.hasMoreElements(); ) {
    String name = (String) nameEnum.nextElement();
    if (name.startsWith("issueToSubtask-")
        && name.endsWith("-key")) {
      String inputPrefix = name.substring(0, name.lastIndexOf("-"));
      String parentKey = request.getParameter(inputPrefix + "-key");
      String summary = request.getParameter(inputPrefix + "-summary");
      String estHours = request.getParameter(inputPrefix + "-estHours");
      String subtractHours = request.getParameter(inputPrefix + "-subtractHours");
      if (summary.length() > 0) {
        parentKey = parentKey.toUpperCase();
        MutableIssue issue1 =
          ComponentManager.getInstance().getIssueManager().getIssueObject(parentKey);
        if (issue1 == null
            || issue1.getKey() == null) {
          out.println("No issue '" + parentKey + "' found.");
        } else {
          MutableIssue issue2 =
            ComponentManager.getInstance().getIssueFactory().cloneIssue(issue1);
          GenericValue project = issue1.getProject();

          // here's what I have to do if I simply 'store' this clone (see below)
          //long nextId = ComponentManager.getInstance().getProjectManager().getNextId(project);
          //issue2.setKey(project.getString("key") + "-" + nextId);
          //issue2.setCreated(new java.sql.Timestamp(System.currentTimeMillis()));

          issue2.setSummary(summary);
          issue2.setReporter(user);
          issue2.setDescription("");
          long estSeconds = (long) (Double.valueOf(estHours) * 3600.0);
          issue2.setOriginalEstimate(estSeconds);
          issue2.setTimeSpent((long)0.0);
          issue2.setEstimate(estSeconds);
          issue2.setAffectedVersions(issue1.getAffectedVersions());
          issue2.setComponents(issue1.getComponents());

          // Weirdness: if I 'clone' and 'store', the components don't get set
          //issue2.store();

          GenericValue issue2a = com.atlassian.jira.ManagerFactory.getIssueManager().createIssue(user, issue2);
          MutableIssue issue2b = ComponentManager.getInstance().getIssueManager().getIssueObject(issue2a.getString("key"));

          // Weirdness: if I don't loop and 'createValue', only the Tracker field is created
          CustomFieldManager fieldMgr = ComponentManager.getInstance().getCustomFieldManager();
          for (CustomField field : (List<CustomField>) fieldMgr.getCustomFieldObjects(issue1)) {
            // Ran into problems with null values; sorry; I can't remember exactly what.
            // (Note that this may not work for fields that have multiple values.)
            if (field.getValue(issue2b) == null
                && field.getValue(issue1) != null) {
              field.createValue(issue2b, field.getValue(issue1));
            }
          }
          

          // now subtract from parent's hours if that was selected
          if (subtractHours != null) {
            long parentEstSeconds = Math.max(0, issue1.getEstimate() - estSeconds);
            issue1.setEstimate(parentEstSeconds);
            issue1.store();
          }

          // create link from parent to child
          Long linkType = Long.valueOf(TimeScheduleLoader.LINK_SUBTASK);
          ComponentManager.getInstance().getIssueLinkManager()
            .createIssueLink(issue1.getId(), issue2b.getId(), linkType, new Long(0), user);

        }
      }
    }
  }
%>



<h2>Display</h2>


<script language="javascript">
function openIssuePicker(formName, linkFieldName, currentissue)
{
    var url = '/secure/popups/IssuePicker.jspa?';
    url += 'formName=' + formName + '&';
    url += 'linkFieldName=' + linkFieldName + '&';
    url += 'currentIssue=' + currentissue + '&';

    var vWinUsers = window.open(url, 'IssueSelectorPopup', 'status=no,resizable=yes,top=100,left=200,width=620,height=500,scrollbars=yes,resizable');
    vWinUsers.opener = self;
  vWinUsers.focus();
}
</script>


<script>
function toggleCollapse(name) {
  var el = document.getElementById(name);
  if ( el.style.display != 'none' ) {
    el.style.display = 'none';
  } else {
    el.style.display = '';
  }
  return false; // so browser doesn't jump to link (top of window)
}
</script>

<form name="<%=FORM_NAME%>">
  Issue Key(s) *
  <input type='text' size='9' name='<%=ISSUE_KEY_REQ_NAME%>' value='<%=session.getAttribute(ISSUE_KEY_SES_NAME)%>'>
  <a href="javascript:openIssuePicker('<%=FORM_NAME%>', '<%=ISSUE_KEY_REQ_NAME%>')"
     title="See recently viewed issues">[select issue]</a>

  <input type='checkbox' name='<%=QUICK_REQ_NAME%>' <%= showQuickCreate ? "CHECKED" : "" %>'> show quick-create
  <input type='submit'>
</form>

* = space-separated

<hr><!-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -->

<form method="POST">
<%

    if (keyArray.length > 0) {

      List issues = load(keyArray);
      for (Iterator iter = issues.iterator(); iter.hasNext(); ) {
        IssueTree tree = (IssueTree) iter.next();
        out.write(TimeCompleteBar.roadMapTable(tree));
        writeIssueRows(tree, showQuickCreate, out);
        out.write("<br>\n");
      }

    }

%>
<% if (showQuickCreate) { %>
<input type='submit' value='Create subtasks.'/>
<% } %>
</form>

<%!
  public static String LINK_SUBTASK = TimeScheduleLoader.LINK_SUBTASK;

  public static String ISSUE_KEY_SES_NAME = "issue key";
  public static String ISSUE_KEY_REQ_NAME = "issueKey";
  public static String QUICK_SES_NAME = "quick-create subtasks";
  public static String QUICK_REQ_NAME = "quick-create-subtasks";
  public static String FORM_NAME = "display";

  /**
    REFACTOR because this duplicates stuff in TimeScheduleLoader
   */
  public static List load(String[] issueKeys) throws SQLException {

    Connection conn = null;
    ResultSet rset = null;

    Map<Long,Long> projectToTeam = new HashMap<Long,Long>();
    try {
      conn = getConnection();
      String projectTeamSql = "select project_id, id from team";
      rset = SimpleSQL.executeQuery(projectTeamSql, new String[0], conn);
      while (rset.next()) {
        if (rset.getString("project_id") != null) {
          projectToTeam.put(new Long(rset.getLong("project_id")),
                            new Long(rset.getLong("id")));
        }
      }
    } finally {
      try { rset.close(); } catch (Exception e2) {}
      try { conn.close(); } catch (Exception e2) {}
    }


    List issues = new ArrayList();
    if (issueKeys.length > 0) {
      // get all the requested issues and load them into memory with the relationships
      String linkSql;
      Object[] args;

      linkSql =
        "select pkey, summary, assignee, resolution, timeestimate, timespent,"
        + " dueDate, priority.sequence as priority, project"
        + " from jiraissue, priority"
        + " where pkey in ("
        + Join(issueKeys, false, "?", ",")
        + ") and priority.id = jiraissue.priority";
      args = issueKeys;

      Map visitedAlready = new HashMap();
      try {
        conn = getConnection();
        rset = SimpleSQL.executeQuery(linkSql, args, conn);
        while (rset.next()) {
          Long teamId = projectToTeam.get(Long.valueOf(rset.getLong("project")));
          IssueTree tree =
            new IssueTree
            (rset.getString("pkey"),
             rset.getString("summary"),
             rset.getString("assignee"),
             teamId,
             TimeScheduleLoader.teamKeyFromIssueKey(teamId, rset.getString("pkey")),
             rset.getInt("timeestimate"),
             rset.getInt("timespent"),
             rset.getDate("dueDate"),
             null,
             rset.getInt("priority"),
             rset.getInt("resolution") != 0);
          issues.add(tree);
          visitedAlready.put(tree.getKey(), tree);
          buildSubtaskTree(tree, visitedAlready, conn, projectToTeam);
        }
      } finally {
        try { rset.close(); } catch (Exception e2) {}
        try { conn.close(); } catch (Exception e2) {}
      }
    }
    return issues;
  }


    private static void buildSubtaskTree(IssueTree parent, Map visitedAlready, Connection conn, Map<Long,Long> projectToTeam) throws SQLException {
        // get the subtasks
        String linkSql =
        "select b.pkey, b.summary, b.assignee, b.timeestimate,"
        + " b.timespent, b.duedate, b.resolution, b.project, "
        + " priority.sequence as priority"
        + " from issuelink, jiraissue a, jiraissue b, priority"
        + " where linktype = '" + LINK_SUBTASK + "'"
        + " and a.pkey = ? and source = a.id and b.id = destination"
        + " and priority.id = b.priority";

        Object[] args = new Object[]{ parent.getKey() };
        ResultSet rset = SimpleSQL.executeQuery(linkSql, args, conn);
        while (rset.next()) {
          if (visitedAlready.containsKey(rset.getString("pkey"))) {
            parent.addSubtask((IssueTree) visitedAlready.get(rset.getString("pkey")));
          } else {
            Long teamId = projectToTeam.get(Long.valueOf(rset.getLong("project")));
            IssueTree tree = 
              new IssueTree
              (rset.getString("pkey"),
               rset.getString("summary"),
               rset.getString("assignee"),
               teamId,
               TimeScheduleLoader.teamKeyFromIssueKey(teamId, rset.getString("pkey")),
               rset.getInt("timeestimate"),
               rset.getInt("timespent"),
               rset.getDate("duedate"),
               null,
               rset.getInt("priority"),
               rset.getInt("resolution") != 0);
            parent.addSubtask(tree);
            visitedAlready.put(tree.getKey(), tree);
          }
        }
        rset.close();

        for (Iterator iter = parent.getSubtasks().iterator(); iter.hasNext(); ) {
          IssueTree tree = (IssueTree) iter.next();
          buildSubtaskTree(tree, visitedAlready, conn, projectToTeam);
        }
      }

  private static Connection getConnection() throws SQLException {
    javax.sql.DataSource dsrc = null;
    try {
      Class.forName("oracle.jdbc.driver.OracleDriver");
      dsrc = (javax.sql.DataSource) new javax.naming.InitialContext().lookup("java:comp/env/jdbc/JiraDS");
    } catch (ClassNotFoundException e) {
      throw new java.lang.reflect.UndeclaredThrowableException(e);
    } catch (javax.naming.NamingException e) {
      throw new java.lang.reflect.UndeclaredThrowableException(e);
    }
    return dsrc.getConnection();
  }

  /** make a single string out of the values */
  private static String Join
  (String[] values, boolean include_values, String append_after_each, String separator) {
    StringBuffer sb = new StringBuffer();
    for (int i=0; i<values.length; i++) {
      if (i > 0
          && separator != null) {
        sb.append(separator);
      }
      if (include_values) {
        sb.append(values[i]);
      }
      if (append_after_each != null) {
        sb.append(append_after_each);
      }
    }
    return sb.toString();
  }

  private static void writeIssueRows(IssueTree detail, boolean showQuickCreate, java.io.Writer out) throws java.io.IOException {
      String prefix = "", postfix = "";
      if (detail.getResolved()) {
        prefix = "<strike>";
        postfix = "</strike>";
      }
      out.write("      <ul>\n");
      out.write("        <li>\n");
      out.write("        <table><tr><td>\n");
      out.write("        " + prefix + "<a href='/secure/ViewIssue.jspa?key=" + detail.getKey() + "'>" + detail.getKey() + "</a>\n");
      String divKey = detail.getKey() + System.currentTimeMillis();
      if (detail.getSubtasks().size() > 0) {
        out.write("       <a href='#' onClick='javascript:return toggleCollapse(\"" + divKey + "\")'>+</a>");
      }
      out.write("        <br>\n");
      out.write("        " + detail.getSummary());
      out.write(" - " + (detail.getEstimate() / 3600.0) + "h for " + detail.getRawAssigneeKey() + postfix + "\n");

      // now write the inputs that allow for quick subtask creation
      out.write("        </td><td>\n");
      if (showQuickCreate) {
        for (int i = 0; i < 3; i++) {
          out.write("        create subtask \n");
          String inputPrefix = "issueToSubtask-" + detail.getKey() + "-" + i;
          out.write("        <input type='hidden'   name='" + inputPrefix + "-key' value='" + detail.getKey() + "' size='8'/>\n");
          out.write("        <input type='text'     name='" + inputPrefix + "-summary' value='' size='20'/>\n");
          out.write("        <input type='text'     name='" + inputPrefix + "-estHours' value='0' size='2'/>h\n");
          out.write("        <input type='checkbox' name='" + inputPrefix + "-subtractHours'/> subtract from parent\n");
          out.write("        <br>\n");
        }
      }

      out.write("        </td></tr></table>\n");

      out.write("          <div id='" + divKey + "'>");
      for (Iterator i = detail.getSubtasks().iterator(); i.hasNext(); ) {
        writeIssueRows((IssueTree) i.next(), showQuickCreate, out);
      }
      out.write("          <div");
      out.write("        </li>\n");
      out.write("      </ul>\n");
  }

%>
