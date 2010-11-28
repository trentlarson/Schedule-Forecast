<%@ page
  import="java.io.PrintWriter"
  import="java.text.SimpleDateFormat"
  import="java.util.*"
  import="com.trentlarson.forecast.core.actions.TimeScheduleAction"
  import="com.trentlarson.forecast.core.dao.Team"
  import="com.trentlarson.forecast.core.scheduling.IssueDigraph"
  import="com.trentlarson.forecast.core.scheduling.IssueTree"
  import="com.trentlarson.forecast.core.scheduling.Teams"
  import="com.trentlarson.forecast.core.scheduling.TimeSchedule"
  import="com.trentlarson.forecast.core.scheduling.TimeScheduleCreatePreferences"
  import="com.trentlarson.forecast.core.scheduling.TimeScheduleDisplayPreferences"
  import="com.trentlarson.forecast.core.scheduling.TimeScheduleLoader"
  import="com.trentlarson.forecast.core.scheduling.TimeScheduleWriter"
  import="com.trentlarson.forecast.core.scheduling.TimeScheduleModifyWriter"
  import="com.trentlarson.forecast.core.scheduling.TimeCompleteBar"

import="org.hibernate.*"
import="org.hibernate.cfg.*"
import="com.trentlarson.forecast.core.scheduling.TeamHoursUtil"
%>

<head>
<title>Current Schedule Chart</title>
<script src='http://ajax.googleapis.com/ajax/libs/jquery/1.3.2/jquery.min.js' type='text/javascript'></script>
<script src='http://eucaly61-java.googlecode.com/files/wz_jsgraphics.js' type='text/javascript'></script>
<script src='gantt.js' type='text/javascript'></script>
</head>


<%
try {

  // set any user request parameters

  if (request.getParameter("reset") != null) {
    for (int i = 0; i < ALL_SES_NAMES.length; i++) {
      session.removeAttribute(ALL_SES_NAMES[i]);
    }
  }


  List<Team> allTeams = new ArrayList<Team>();
  Map<Long,Team> idToTeam = new HashMap();
  {
    Session sess = TeamHoursUtil.HibernateUtil.currentSession();
    Transaction tx = sess.beginTransaction();
    allTeams = sess.createQuery("from Team order by name").list();
    TeamHoursUtil.HibernateUtil.closeSession();
    for (Team team : allTeams) {
      idToTeam.put(team.getId(), team);
    }
  }




  //// Scheduling parameters

  // check and/or update the scheduling multiplier
  double time_multiplier = 1.0;
  if (request.getParameter(MULTIPLIER_REQ_NAME) != null) {
    time_multiplier = Double.parseDouble(request.getParameter(MULTIPLIER_REQ_NAME));
  }

  // check and/or update the time for issues without estimates
  int time_without_estimate = 0;
  if (request.getParameter(TIME_WITHOUT_ESTIMATE_REQ_NAME) != null) {
    time_without_estimate = Integer.parseInt(request.getParameter(TIME_WITHOUT_ESTIMATE_REQ_NAME));
  }




  //// Display parameters

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
  String[] keyArray = issueKey.split("[ ,]");
  if (issueKey.length() == 0) {
    keyArray = new String[0];
  }
  // since we're allowing both comma and space separators, must prune empty ones
  List keyList = new ArrayList();
  for (int i = 0; i < keyArray.length ; i++) {
    if (keyArray[i].length() > 0) {
      keyList.add(keyArray[i]);
    }
  }
  keyArray = (String[]) keyList.toArray(new String[keyList.size()]);


  // check and/or update the selected team
  Long show_team = (Long) session.getAttribute(SHOW_TEAM_SES_NAME);
  if (request.getParameter(SHOW_TEAM_REQ_NAME) != null
      && request.getParameter(SHOW_TEAM_REQ_NAME).length() > 0) {
    show_team = Long.valueOf(request.getParameter(SHOW_TEAM_REQ_NAME));
    session.setAttribute(SHOW_TEAM_SES_NAME, show_team);
  } else {
    show_team = null;
    session.removeAttribute(SHOW_TEAM_SES_NAME);
  }


  // check and/or update the selected user(s)
  String show_user = (String) session.getAttribute(SHOW_USER_SES_NAME);
  if (show_user == null) {
    show_user = "";
    session.setAttribute(SHOW_USER_SES_NAME, show_user);
  }
  if (request.getParameter(SHOW_USER_REQ_NAME) != null) {
    if (request.getParameter(SHOW_USER_REQ_NAME).length() > 0) {
      show_user = request.getParameter(SHOW_USER_REQ_NAME);
    } else {
      show_user = "";
    }
    session.setAttribute(SHOW_USER_SES_NAME, show_user);
  }
  // get the list if it's more than one
  String[] displayUserArray = show_user.split("[ ,]");
  if (show_user.length() == 0) {
    displayUserArray = new String[0];
  }
  // since we're allowing both comma and space separators, must prune empty ones
  List displayUserList = new ArrayList();
  for (int i = 0; i < displayUserArray.length ; i++) {
    if (displayUserArray[i].length() > 0) {
      displayUserList.add(displayUserArray[i]);
    // this by itself doesn't work to show unassigned tasks
    //} else if (displayUserArray[i].equals("unassigned")) {
    //  displayUserList.add(null);
    }
  }


  // check and/or update the selected group
  String show_group = (String) session.getAttribute(SHOW_GROUP_SES_NAME);
  if (show_group == null) {
    show_group = "";
    session.setAttribute(SHOW_GROUP_SES_NAME, show_group);
  }
  if (request.getParameter(SHOW_GROUP_REQ_NAME) != null) {
    if (request.getParameter(SHOW_GROUP_REQ_NAME).length() > 0) {
      show_group = request.getParameter(SHOW_GROUP_REQ_NAME);
    } else {
      show_group = "";
    }
    session.setAttribute(SHOW_GROUP_SES_NAME, show_group);
  }
  /** someday soon...
  if (show_group.length() > 0) {
    displayUserArray =
      (String[])
      com.trentlarson.forecast.core.dao.DAOFactory.getUserDAO().getUsersInGroup(show_group)
      .toArray(new String[0]);
  }
  **/

  // check and/or update the target due date
  Date show_due_by = (Date) session.getAttribute(SHOW_DUE_BY_SES_NAME);
  if (show_due_by == null) {
    show_due_by = null;
    session.setAttribute(SHOW_DUE_BY_SES_NAME, show_due_by);
  }
  if (request.getParameter(SHOW_DUE_BY_REQ_NAME) != null) {
    if (request.getParameter(SHOW_DUE_BY_REQ_NAME).length() > 0) {
      String show_due_by_string = request.getParameter(SHOW_DUE_BY_REQ_NAME);
      show_due_by = TimeScheduleModifyWriter.parseNewShortDate(show_due_by_string);
    } else {
      show_due_by = null;
    }
    session.setAttribute(SHOW_DUE_BY_SES_NAME, show_due_by);
  }

  // check whether to reload the data
  boolean reload_data = (request.getParameter(RELOAD_DATA_REQ_NAME) != null);

  // check whether to reschedule
  boolean reschedule = (request.getParameter(RESCHEDULE_REQ_NAME) != null);

  // check whether to show the critical paths
  boolean show_critical_paths = (request.getParameter(SHOW_CRITICAL_PATHS_REQ_NAME) != null);

  // check whether to show the resolved issues
  boolean show_resolved = (request.getParameter(SHOW_RESOLVED_REQ_NAME) != null);

  // check whether to show the users in one row
  boolean show_users_in_one_row = (request.getParameter(SHOW_USERS_IN_ONE_ROW_REQ_NAME) != null);

  // check whether to show the daily breakdown
  boolean show_schedule = (request.getParameter(SHOW_SCHEDULE_REQ_NAME) != null);

  // check whether to show the bulk changer
  boolean show_bulk_changer = (request.getParameter(SHOW_BULK_CHANGER_REQ_NAME) != null);
  if (request.getParameter(NEW_PRIORITIES_REQ_NAME) != null) {
    show_bulk_changer = true;
  }

  // check whether to show the completion measurements
  boolean show_completion = (request.getParameter(SHOW_COMPLETION_REQ_NAME) != null);


  // check and/or update the time granularity
  Integer time_granularity_sess = (Integer) session.getAttribute(TIME_GRANULARITY_SES_NAME);
  if (time_granularity_sess == null) {
    time_granularity_sess = new Integer(7);
    session.setAttribute(TIME_GRANULARITY_SES_NAME, time_granularity_sess);
  }
  int time_granularity = time_granularity_sess.intValue();
  if (request.getParameter(TIME_GRANULARITY_REQ_NAME) != null) {
    time_granularity = Integer.parseInt(request.getParameter(TIME_GRANULARITY_REQ_NAME));
    session.setAttribute(TIME_GRANULARITY_SES_NAME, Integer.valueOf(time_granularity));
  }





%>


<%

  // load the graph or other actions

  // this same default is used in scheduled-holes.jsp
  TimeScheduleCreatePreferences sPrefs =
    new TimeScheduleCreatePreferences(time_without_estimate * 60, time_multiplier);

  // record new priorities in the issues (not the DB)
  if (request.getParameter(NEW_PRIORITIES_REQ_NAME) != null) {
    TimeScheduleAction.modifyPriorities(request);
  }

  // save priorities to the DB
  if (request.getParameter(SAVE_PRIORITIES_REQ_NAME) != null) {
    TimeScheduleAction.savePriorities();
  }

  // save priorities to the DB
  if (request.getParameter(REVERT_PRIORITIES_REQ_NAME) != null) {
    TimeScheduleAction.revertPriorities();
  }

  // Why doesn't this work?
  //org.apache.log4j.Logger.getLogger(com.trentlarson.forecast.core.scheduling.TimeScheduleLoader.class).setLevel(org.apache.log4j.Level.DEBUG);

  IssueDigraph graph;
  if (reload_data) {
    graph = TimeScheduleAction.regenerateGraph(sPrefs);
  } else {
    graph = TimeScheduleAction.getOrRegenerateGraph(sPrefs);
  }

%>






<table width='100%'>
  <tr>
    <td>

<h1 style="text-align:center">Schedule Forecast</h1>


<h3>Reload Data</h3>
<form name="loader">
Charts below are based on data loaded at <%= graph.getLoadedDate() %>.
  <input type='hidden' name='<%=RELOAD_DATA_REQ_NAME%>' value='true'>
  <input type='submit' value='Reload Issue Data'>
</form>

<% if (TimeScheduleAction.getChangedTime() != null) { %>
<form>
  <font color='red'>Note:</font> The priorities or dates were modified
  at <%=TimeScheduleAction.getChangedTime()%> by <%=TimeScheduleAction.getChangedBy()%>
  and have not been saved.
  <input type='hidden' name='<%=REVERT_PRIORITIES_REQ_NAME%>' value='true'>
  <input type='submit' value='Revert to Loaded Data'>
</form>
<% } %>




<!--
<form name="reschedule">
  Multiply all estimates by
  <input type='text' size='3' name='<%=MULTIPLIER_REQ_NAME%>' value='<%=time_multiplier%>'>
  <input type='hidden' name='<%=RESCHEDULE_REQ_NAME%>' value='true'>
  <input type='submit' value='Reschedule'>
</form>

<% if (TimeScheduleAction.getAdjustmentChangedTime() != null) { %>
  <font color='red'>Note:</font> This multiplier was modified
  at <%=TimeScheduleAction.getAdjustmentChangedTime()%>
  by <%=TimeScheduleAction.getAdjustmentChangedBy()%>.
<% } %>
-->





<h3>Change Display</h3>

<form name="display">

<b>Filters</b>
<div>
  Issue Key(s) &sup1;
  <input type='text' size='9' name='<%=ISSUE_KEY_REQ_NAME%>' value='<%=session.getAttribute(ISSUE_KEY_SES_NAME)%>'>
  Team
  <select name='<%=SHOW_TEAM_REQ_NAME%>'>
    <option value=''>No particular team</option>
    <% for (Team team : allTeams) { %>
      <option value='<%= team.getId() %>' <%= show_team != null && show_team.equals(team.getId()) ? "SELECTED" : "" %>><%= team.getName() %></option>
    <% } %>
  </select>
  Assignee(s) &sup1;
  <input type='text' size='15' name='<%=SHOW_USER_REQ_NAME%>' value='<%=session.getAttribute(SHOW_USER_SES_NAME)%>'>
  <!-- someday soon...
  Group
  <input type='text' size='10' name='<%=SHOW_GROUP_REQ_NAME%>' value='<%=session.getAttribute(SHOW_GROUP_SES_NAME)%>'>
  -->
  Due Before (MM/DD)
  <input type='text' size='8' name='<%=SHOW_DUE_BY_REQ_NAME%>' value='<%= TimeScheduleAction.formatShortDate(show_due_by) %>'>

<br>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
&sup1; = space- and/or comma-separated list
</div>


<br>
<b>Format</b>
<div>
  <input type='text' size='1' name='<%=TIME_GRANULARITY_REQ_NAME%>' value='<%=session.getAttribute(TIME_GRANULARITY_SES_NAME)%>'>
  Days per slice
  <input type='checkbox' name='<%=SHOW_CRITICAL_PATHS_REQ_NAME%>' <%= (show_critical_paths ? "CHECKED" : "") %>>
  Show critical paths
  <!-- doesn't seem to work
  <input type='checkbox' name='<%=SHOW_RESOLVED_REQ_NAME%>' <%= (show_resolved ? "CHECKED" : "") %>>
  Show resolved
  -->
  <input type='checkbox' name='<%=SHOW_USERS_IN_ONE_ROW_REQ_NAME%>' <%= (show_users_in_one_row ? "CHECKED" : "") %>>
  Show users in one row
  <input type='checkbox' name='<%=SHOW_SCHEDULE_REQ_NAME%>' <%= (show_schedule ? "CHECKED" : "") %>>
  Show users' detailed schedules
  <input type='checkbox' name='<%=SHOW_BULK_CHANGER_REQ_NAME%>' <%= (show_bulk_changer ? "CHECKED" : "") %>>
  Show bulk-change tools
  <!-- ClassCastException: java.lang.String cannot be cast to com.trentlarson.forecast.core.scheduling.IssueTree
  <input type='checkbox' name='<%=SHOW_COMPLETION_REQ_NAME%>' <%= (show_completion ? "CHECKED" : "") %>>
  Show completion measurements
  -->
  <input type='submit' value='Change Display'>
</form>
<br/>
<a href="javascript:drawSubtaskLinks('source');">Show subtask lines.</a>
<a href="javascript:drawSuccessorLinks('source');">Show successor lines.</a>
<div>


<%-- This doesn't work as it probably should.

<% if (graph.getIssueSchedules().size() > 0) { %>

<h2>There are no issues to display.</h2>

<% } %>

--%>



    </td>
    <td align='right'>
      <!-- show a key to the colors -->
      <table border='1'>
        <tr>
          <td bgcolor='green'>&nbsp;</td>
          <td bgcolor='red'>&nbsp;</td>
          <td>dark = <br> working time on issue <br> (thin red = overdue date) </td>
        </tr>
        <tr>
          <td bgcolor='lightgreen'>&nbsp;</td>
          <td bgcolor='pink'>&nbsp;</td>
          <td>light = non-working time <br> covered by subtasks or <br> waiting on other issues/weekends</td>
        </tr>
        <tr>
          <td>green = <br> before <br> due date</td>
          <td>red =   <br> after <br> due date</td>
          <td>black = <br> already <br> resolved </td>
        </tr>
      </table>
    </td>
  </tr>
</table>


<div id="source">

<% if (show_bulk_changer) { %>
  <form action="?" method="post">
    <input type='hidden' name='<%=NEW_PRIORITIES_REQ_NAME%>' value='true'>
<% } %>


<%

    // print out hierarchical issue time schedule(s)

    if (keyArray.length > 0) {

      out.write("<hr>\n");
      
      TimeScheduleDisplayPreferences dPrefs;
      if (show_critical_paths) {
        dPrefs =
          TimeScheduleDisplayPreferences.createForCriticalPaths
            (time_granularity, 0, false, false, keyArray, graph);
      } else {
        dPrefs =
          TimeScheduleDisplayPreferences.createForIssues
            (time_granularity, 0, true, false, show_resolved, keyArray, show_bulk_changer, graph);
      }

      if (show_completion) {
        for (Iterator iter = dPrefs.showIssues.iterator(); iter.hasNext(); ) {
          IssueTree tree = (IssueTree) iter.next();
          out.write(TimeCompleteBar.roadMapTable(tree) + "\n");
          out.write("<br>\n");
        }
      }

      TimeScheduleWriter.writeIssueTable(graph, new PrintWriter(out), graph.getTimeScheduleCreatePreferences(), dPrefs);
    }

    /** Here's something to guard against running out of memory rendering when someone chooses too many issues.
    if (graph.getIssueSchedules().size() > 0) {
      out.println("<hr><!-- - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - - -->");
      if (graph.getIssueSchedules().size() < 1000) {
        TimeScheduleWriter.writeIssueTable(graph, new PrintWriter(out), graph.getTimeScheduleCreatePreferences(), dPrefs);
      } {
        //out.println("<h2>Sorry!  Jira runs out of memory trying to display too many issues, so we won't display all " + graph.getIssueSchedules().size() + ".  Try restricting your display selection.</h2>");
      }
    }
    **/

    // print out team graph

    if (show_team != null) {
      
      // This is a hack because TimeScheduleDisplayPreferences.createForTeam
      // currently shows the wrong data; put something like this in there.
      List<String> showIssues = new ArrayList();
      for (Teams.AssigneeKey userKey : graph.getAssignedUserDetails().keySet()) {
        if (show_team.equals(userKey.getTeamId())) {
          for (IssueTree issue : graph.getAssignedUserDetails().get(userKey)) {
            showIssues.add(issue.getKey());
          }
        }
      }
      String[] issues = showIssues.toArray(new String[0]);

      TimeScheduleDisplayPreferences dPrefs =
        TimeScheduleDisplayPreferences.createForIssues
          (time_granularity, 0, true, false, show_resolved, issues, show_bulk_changer, graph);

      out.write("<hr>\n");
      out.write("Schedule for team " + idToTeam.get(show_team).getName() + ".<br>");
      TimeScheduleWriter.writeIssueTable(graph, new PrintWriter(out), graph.getTimeScheduleCreatePreferences(), dPrefs);
    }

    // print out time schedules for each user

    if (show_users_in_one_row) {

      // a user may show on multiple teams, so find any in list of user keys
      // (duplicate code; may want to move it to TimeScheduleDisplayPreferences)
      List<Teams.AssigneeKey> userInProjects = new ArrayList();
      for (Teams.AssigneeKey userKey : graph.getAssignedUserDetails().keySet()) {
        if (userKey.getUsername() != null
            && displayUserList.contains(userKey.getUsername())) {
          userInProjects.add(userKey);
        }
      }
      Teams.AssigneeKey[] assigneeArray = 
        (Teams.AssigneeKey[]) userInProjects.toArray(new Teams.AssigneeKey[userInProjects.size()]);

      TimeScheduleDisplayPreferences dPrefs =
        TimeScheduleDisplayPreferences.createForUsers
          (time_granularity, 0, true, false, show_resolved, assigneeArray, graph);
      
      out.write("<hr>\n");
      out.write("Schedule for chosen users.<br>");
      TimeScheduleWriter.writeIssueTable(graph, new PrintWriter(out), graph.getTimeScheduleCreatePreferences(), dPrefs);

    } else { // show separate tables

      for (int userNum = 0; userNum < displayUserArray.length; userNum++) {

        String oneUser = displayUserArray[userNum];
        if (oneUser.length() > 0) {

          TimeScheduleDisplayPreferences dPrefs =
            TimeScheduleDisplayPreferences.createForUser
              (time_granularity, 0, true, false, show_resolved, oneUser, show_bulk_changer, graph);

          out.write("<hr>\n");
          out.write("Schedule for " + oneUser + ".<br>");
          TimeScheduleWriter.writeIssueTable(graph, new PrintWriter(out), graph.getTimeScheduleCreatePreferences(), dPrefs);

          if (show_schedule) {

            List schedule = new ArrayList();
            for (Teams.UserTimeKey timeKey : graph.getTimeUserDetails().keySet()) {
              if (oneUser.equals(timeKey.getUsername())) {
                List userIssueList = (List) graph.getTimeUserDetails().get(timeKey);
                for (int issueNum = 0; issueNum < userIssueList.size(); issueNum++) {
                  schedule.add
                    (graph.getIssueSchedules().get
                     (((TimeSchedule.IssueWorkDetail) userIssueList.get(issueNum)).getKey()));
                }
              }
            }

            out.write("<p>\n");
            out.write("If that were scheduled back-to-back, which dates could " + oneUser + " hit?  Not the ones below in red!\n");
            out.write("<p>\n");
            TimeSchedule.writeIssueSchedule(schedule, time_multiplier, show_resolved, new PrintWriter(out));
          }
        }
      }
    }

    // print out due-date graph
    if (show_due_by != null) {
      TimeScheduleDisplayPreferences dPrefs =
        TimeScheduleDisplayPreferences.createForDate
          (time_granularity, 0, true, false, show_resolved, show_due_by, show_bulk_changer, graph);
      out.write("<hr>\n");
      out.write("Issues due before " + show_due_by + ".<br>");
      TimeScheduleWriter.writeIssueTable(graph, new PrintWriter(out), graph.getTimeScheduleCreatePreferences(), dPrefs);
    }

%>

<% if (show_bulk_changer) { %>
    <input type='submit' value='Preview Issue Changes (Without Committing)'>
  </form>

</div><!-- source -->

<form action="?">
  <input type='hidden' name='<%=SAVE_PRIORITIES_REQ_NAME%>' value='true'>
  <input type='submit' value='Commit Issue Changes to DB (Note: You must preview changes first.)'>
</form>

<% } %>



<%

} catch (Throwable e) {
  Date errorDate = new Date();
  System.out.println("Showing user error message at " + errorDate);
  e.printStackTrace();
  %>
  <h2>Something went wrong!</h2>
  Report the following information to the Jira developers 
  and include any helpful details about what you were doing,
  then try clicking <a href="?reset=true">here</a>.
  <p>
  Error time: <%= errorDate %>
  <p>
  Error message: <%= e %>
  <%

} finally {
}

//System.out.println("TimeScheduleCounter " + user.getName() + " " + new Date());

%>



<%!

  public static String ISSUE_KEY_SES_NAME = "issue key";
  public static String SHOW_TEAM_SES_NAME = "show team";
  public static String SHOW_USER_SES_NAME = "show user";
  public static String SHOW_GROUP_SES_NAME = "show group";
  public static String SHOW_DUE_BY_SES_NAME = "show due by";
  public static String TIME_GRANULARITY_SES_NAME = "time granularity";
  public static String[] ALL_SES_NAMES = {
    ISSUE_KEY_SES_NAME,
    SHOW_TEAM_SES_NAME,
    SHOW_USER_SES_NAME,
    SHOW_GROUP_SES_NAME,
    SHOW_DUE_BY_SES_NAME,
    TIME_GRANULARITY_SES_NAME
  };

  public static String MULTIPLIER_REQ_NAME = TimeScheduleAction.MULTIPLIER_REQ_NAME;
  public static String ISSUE_KEY_REQ_NAME = TimeScheduleAction.ISSUE_KEY_REQ_NAME;
  public static String SHOW_TEAM_REQ_NAME = "show_team";
  public static String SHOW_USER_REQ_NAME = "show_user";
  public static String SHOW_GROUP_REQ_NAME = "show_group";
  public static String SHOW_DUE_BY_REQ_NAME = "show_due_by";
  public static String TIME_GRANULARITY_REQ_NAME = "time_granularity";
  public static String TIME_WITHOUT_ESTIMATE_REQ_NAME = "time_without_estimate";

  // booleans
  public static String RELOAD_DATA_REQ_NAME = "reload_data";
  public static String RESCHEDULE_REQ_NAME = "reschedule";
  public static String NEW_PRIORITIES_REQ_NAME = "setPriorities";
  public static String REVERT_PRIORITIES_REQ_NAME = "revert";
  public static String SAVE_PRIORITIES_REQ_NAME = "save_priorities";
  // check boxes that have to be handled each time they're sent
  public static String SHOW_CRITICAL_PATHS_REQ_NAME = TimeScheduleAction.SHOW_CRITICAL_PATHS_REQ_NAME;
  public static String SHOW_RESOLVED_REQ_NAME = "show_resolved";
  public static String SHOW_USERS_IN_ONE_ROW_REQ_NAME = "show_users_in_one_row";
  public static String SHOW_SCHEDULE_REQ_NAME = "show_schedule";
  public static String SHOW_BULK_CHANGER_REQ_NAME = "show_bulk_changer";
  public static String SHOW_COMPLETION_REQ_NAME = "show_completion";

  public static String DATA_SES_NAME = "all my data";


  /****************************************************************
   BEGIN useful classes for introspection.  Use thus:

    PojoStringAction action = new PojoStringAction();
    new PojoVisitor(action).visit("sPrefs: ", sPrefs);
    out.println(action.getBuffer().toString());

  ****************************************************************/

  public static interface VisitAction {
    public void act(String label, Object obj);
    public void push();
    public void pop();
  }
  public static class PojoStringAction implements VisitAction {
    private StringBuffer buff = new StringBuffer();
    private String indentation = "";
    public void act(String label, Object obj) {
      buff.append(indentation);
      buff.append(label + ": " + obj + " ");
      buff.append("\n");
    }
    public void push() {
      indentation = indentation + "  ";
    }
    public void pop() {
      indentation = indentation.substring(0, indentation.length() - 2);
    }
    public StringBuffer getBuffer() {
      return buff;
    }
  }
  public static class PojoVisitor {
    private VisitAction action;
    private java.util.Set visited = new java.util.HashSet();
    public PojoVisitor(VisitAction action) {
      this.action = action;
    }
    /** in order traversal */
    public void visit(String label, Object obj) {
      action.act(label, obj);

      if (obj != null
          && !obj.getClass().isPrimitive()
          && !obj.getClass().getName().equals("String")) {
        action.push();

        if (obj.getClass().isArray()) {
          for (int i = 0; i < java.lang.reflect.Array.getLength(obj); i++) {
              Object element = java.lang.reflect.Array.get(obj, i);
              boolean proceed = visited.add(element);
              if (proceed) {
                visit(String.valueOf(i), element);
              } else {
                action.act(String.valueOf(i), "(duplicate)");
              }
          }
        } else {
          java.lang.reflect.Method[] methods = obj.getClass().getMethods();
          for (int i = 0; i < methods.length; i++) {
            if (methods[i].getName().startsWith("get")
                && !methods[i].getName().equals("getClass")
                && methods[i].getParameterTypes().length == 0) {
              try {
                Object result = methods[i].invoke(obj, new Object[0]);
                boolean firstVisit = visited.add(result);
                if (methods[i].getName().equals("getBytes")) {
                  // do nothing
                } else if (firstVisit) {
                  visit(methods[i].getName(), result);
                } else if (result.getClass() == Boolean.class
                           || result.getClass() == Byte.class
                           || result.getClass() == Character.class
                           || result.getClass() == Double.class
                           || result.getClass() == Float.class
                           || result.getClass() == Integer.class
                           || result.getClass() == Long.class
                           || result.getClass() == Short.class
                           || result.getClass().getName().equals("String")) {
                  action.act(methods[i].getName(), result.toString());
                } else {
                  action.act(methods[i].getName(), (result.toString().length() < 40 ? ": " + result : "(duplicate object)"));
                }
              } catch (java.lang.IllegalAccessException e) {
                visit("IllegalAccessException", e.getMessage());
              } catch (java.lang.reflect.InvocationTargetException e) {
                visit("InvocationTargetException", e.getMessage());
              }
            }
          }
        }
        action.pop();
      }
    }
  }

  /****************************************************************
   END useful classes for introspection.
  ****************************************************************/

%>
