<%@ page
  import="java.util.*"
  import="java.lang.reflect.Method"
  import="java.lang.reflect.InvocationTargetException"
  import="javax.servlet.http.Cookie"
  import="java.net.URLEncoder"
  import="java.net.URLDecoder"
  import="java.sql.*"
  import="com.atlassian.jira.web.SessionKeys"
  import="com.opensymphony.user.User"
  import="com.atlassian.seraph.auth.DefaultAuthenticator"
  import="com.atlassian.jira.ManagerFactory"
  import="com.atlassian.jira.security.Permissions"
%><%!

TreeMap preferences = new TreeMap();
// useContextDataSource will force connection to the default data source
boolean useContextDataSource = true;

%><%

// allowUpdates will allow insert, update, and delete statements
boolean allowUpdates = false;
if ( request.getParameter("writable") != null ) {
  Boolean isProjectAdmin = (Boolean)request.getSession().getAttribute(SessionKeys.USER_PROJECT_ADMIN);
  if (isProjectAdmin == null) {
    isProjectAdmin = Boolean.FALSE;
    User u = (User)request.getSession().getAttribute(DefaultAuthenticator.LOGGED_IN_KEY);
    if (u != null) {
      isProjectAdmin = ManagerFactory.getPermissionManager().hasProjects(Permissions.PROJECT_ADMIN, u) ? Boolean.TRUE : Boolean.FALSE;
      request.getSession().setAttribute(SessionKeys.USER_PROJECT_ADMIN, isProjectAdmin);
    }
  }
  if ( isProjectAdmin.booleanValue() ) {
    allowUpdates = true;
  }
}

SetPreferences: {
  preferences.put("showDbMetaDataOptions",        "true");
  preferences.put("defaultDbDriver",              "oracle.jdbc.driver.OracleDriver");
//  preferences.put("defaultDbDriver",              "com.mysql.jdbc.Driver");
  if (!useContextDataSource) {
    preferences.put("defaultDbUrl",                 "jdbc:oracle:thin:@ora1.hq.icentris:1521:IC3");
//    preferences.put("defaultDbUrl",                 "jdbc:mysql://cvs2.hq.icentris/jiratest?autoReconnect=true");
    preferences.put("defaultDbUser",                "jira_test");
    preferences.put("defaultDbPass",                "test_jira");
  }
  preferences.put("defaultStartRow",              "1");
  preferences.put("defaultMaxRows",               "200");
  preferences.put("defaultSuggestedExtension",    ".txt");
  preferences.put("csv.defaultDelimiter",         ",");
  preferences.put("csv.useQuotes",                "true");
  preferences.put("csv.NULLString",               "<NULL>");
  preferences.put("sqlInsert.useQuotesAlways",    "false");
  preferences.put("jdbc.enableEscapePropcessing", "true");
  preferences.put("escapeHtmlInResults",          "true");
  preferences.put("defaultNumericSize",           "12");
//  preferences.put("allowAdd",                     "true");
//  preferences.put("allowEdit",                    "true");
  preferences.put("charLimitOnGet",               "1000");
  preferences.put("allowView",                    "false");
  preferences.put("showQueryOnView",              "false");
//  preferences.put("allowDelete",                  "true");
  preferences.put("showColumnMetaData",           "true");
  preferences.put("columnMetaOnMouseOver",        "false");
  preferences.put("columnMetaDataCloseInSeconds", "5");
  preferences.put("calculateRemainingRows",       "false");
  preferences.put("calculateQueryRunTime",        "true");
  Cookie[] cookies = request.getCookies();
  if ( cookies != null ) {
    for(int i=0; i < cookies.length; i++) {
      if ( preferences.containsKey(cookies[i].getName()) ) {
        if ( "%00".equals(cookies[i].getValue()) ) cookies[i].setValue("");
        preferences.put( cookies[i].getName(), URLDecoder.decode(cookies[i].getValue()) );
      }
    }
  }

  if ( request.getParameter("escapeHtmlInResults") != null ) {
    preferences.put("escapeHtmlInResults", "false");
  }

}
int start;
int max;
int resFormatType = 0;

String behavior = request.getParameter("behavior");
if ( behavior == null ) behavior = "executeSql";

String resultFormat = (String) request.getParameter("resultFormat");
if ( resultFormat == null ) {
  resultFormat = "html";
}
// make sure this stays right after the resultFormat initilization
String outputLocation = (String) request.getParameter("outputLocation");
if ( outputLocation == null ) {
  outputLocation = "normal";
} else if ( outputLocation.equals("fileOnClient") ) {
  if ( resultFormat.equals("html") ) {
    response.setContentType("text/saveHtml");
  } else if ( resultFormat.equals("delimited") ) {
    response.setContentType("text/delimited");
  } else if ( resultFormat.equals("excel") ) {
    response.setContentType("application/vnd.ms-excel");
    resultFormat = "delimited";
  } else if ( resultFormat.equals("sqlInsert") ) {
    response.setContentType("text/sql");
  }
}

String filePath = (String)request.getParameter("filePath");
if ( filePath == null || filePath.startsWith("output.") ) {
  String suggestedExtension = getPreference("defaultSuggestedExtension");
  if ( resultFormat.equals("html") ) {
    suggestedExtension = ".html";
  } else if ( resultFormat.equals("delimited") ) {
    suggestedExtension = ".csv";
  } else if ( resultFormat.equals("sqlInsert") ) {
    suggestedExtension = ".sql";
  }
  filePath = "output" + suggestedExtension;
}

String tableName = (String)request.getParameter("tableName");
if ( tableName == null ) {
  tableName = "";
}

String delimiter = (String)request.getParameter("delimiter");
if ( delimiter == null ) {
  delimiter = getPreference("csv.defaultDelimiter");
}

boolean useQuotes = getBooleanPreference("csv.useQuotes");
String quotes = (String)request.getParameter("quotes");
if ( quotes != null ) {
  useQuotes = "true".equals(quotes);
}

String dbdriver = (String)request.getParameter("dbdriver");
if ( dbdriver == null ) {
  dbdriver = getPreference("defaultDbDriver");
}

String dburl = (String)request.getParameter("dburl");
if ( dburl == null ) {
  dburl = getPreference("defaultDbUrl");
}

String dbuser = (String)request.getParameter("dbuser");
if ( dbuser == null ) {
  dbuser = getPreference("defaultDbUser");
}

String dbpass = (String)request.getParameter("dbpass");
if ( dbpass == null ) {
  dbpass = getPreference("defaultDbPass");
}

if ( request.getParameter("start") != null ) {
  start = Integer.parseInt(request.getParameter("start"));
} else {
  start = Integer.parseInt(getPreference("defaultStartRow"));
}
if ( request.getParameter("max") != null ) {
  max = Integer.parseInt(request.getParameter("max"));
} else {
  max = Integer.parseInt(getPreference("defaultMaxRows"));
}


String query = (String)request.getParameter("query");
if ( query == null ) {
  query = "";
} else if (!allowUpdates) {
 if(query.toLowerCase().trim().indexOf("update ") > -1) {
  throw new IllegalArgumentException("You cannot use 'Update' query ");
  
 } else if(query.toLowerCase().trim().indexOf("delete") > -1) {
  throw new IllegalArgumentException("You cannot use 'Delete' query ");
  
 } else if(query.toLowerCase().trim().indexOf("insert") > -1) {
  throw new IllegalArgumentException("You cannot use 'Insert' query ");
 }
}

String sqlError = "";

String outputOnly = (String)request.getParameter("outputOnly");
boolean showOutputOnly = false;
if ( outputOnly != null && outputOnly.equals("true") ) {
  showOutputOnly = true;
}

if ( request.getParameter("savePreferences") != null ) {
  Iterator preferenceKeys = preferences.keySet().iterator();
  while ( preferenceKeys.hasNext() ) {
    String preferenceKey = (String) preferenceKeys.next();
    if ( request.getParameter(preferenceKey) != null ) {
      preferences.put( preferenceKey, request.getParameter(preferenceKey) );
      String value = URLEncoder.encode( request.getParameter(preferenceKey) );
      if ( "".equals(value) ) value = "%00";
      Cookie newCookie = new Cookie( preferenceKey, value );
      newCookie.setMaxAge(1000000000);
      response.addCookie(newCookie);
    }
  }
}

if ( request.getParameter("editPreferences") != null &&
     request.getParameter("editPreferences").length() > 0 ) {
%>
<form method="POST">
  <input type="hidden" name="savePreferences" value="yes">
<table>
  <% Iterator preferenceKeys = preferences.keySet().iterator(); %>
  <% while ( preferenceKeys.hasNext() ) { %>
  <%   String preferenceKey = (String) preferenceKeys.next(); %>
  <%   String value = (String) preferences.get(preferenceKey); %>
  <tr>
    <td><%= preferenceKey %></td>
    <td>
    <% if ( "true".equals(value) || "false".equals(value) ) { %>
      <select name="<%= preferenceKey %>">
        <option value="true"<%= getBooleanPreference(preferenceKey) ? "selected" : "" %>>Yes</option>
        <option value="false"<%= getBooleanPreference(preferenceKey) ? "" : "selected" %>>No</option>
      </select>
    <% } else { %>
      <input type="text" name="<%= preferenceKey %>"
          value="<%= htmlEscape( value ) %>">
    <% } %>
    </td>
  </tr>
  <% } %>
</table>
<input type="submit" value="Save and Return to Sql">
<input type="reset" value="Reset">
</form>
<%
  return;
}

if ( showOutputOnly == false ) {
String title = htmlEscape(query);
if ( title.length() > 100 ) {
  title = title.substring(0,100) + " . . .";
}
if ( title.length() > 0 ) {
  title = " - \"" + title + "\"";
}

%>
<HTML>
<HEAD>
  <TITLE>SQL Query<%= title %></TITLE>
</HEAD>

<script language="javascript">
//get method has a problem if query is too big
function checkLength(){
  <% if ( getIntPreference("charLimitOnGet") >= 0 ) { %>
  if(document.sqlQueryForm.query.value.length > <%= getIntPreference("charLimitOnGet") %>){
    document.sqlQueryForm.action = window.location.pathname;
    document.sqlQueryForm.method = "post";
  }
  <% } %>
}
function changeOutputLocation(){
  var i = document.sqlQueryForm.outputLocation.selectedIndex;
  var outputLocation = document.sqlQueryForm.outputLocation[i].value;
  if ( outputLocation == "newWindow" ) {
    document.sqlQueryForm.outputOnly.value = "true";
    document.sqlQueryForm.target = "outputWindow";
  } else if ( outputLocation == "fileOnClient" ) {
    document.sqlQueryForm.outputOnly.value = "true";
    document.sqlQueryForm.target = "";
  } else {
    document.sqlQueryForm.outputOnly.value = "false";
    document.sqlQueryForm.target = "";
  }
  document.sqlQueryForm.submit();
}

</script>

<BODY onload="document.sqlQueryForm.query.focus();">

<H1> SQL Queries </H1>
<%= (allowUpdates) ? "(writable!)" : "(read-only)" %>
<br>
<br>

<table>
<form method="POST">
  <tr>
    <td valign="top">
  <input type="hidden" name="editPreferences" value="yes">
<INPUT TYPE="submit" accessKey="p" VALUE="Edit Preferences"><br>
</form>
<script language="Javascript">
  function getBehavior(behavior) {
    document.sqlQueryForm.behavior.value = behavior;
    document.sqlQueryForm.action = '<%= getCoreUrl(request) %>';
    document.sqlQueryForm.method = 'GET';
    document.sqlQueryForm.submit();

  }

  function postBehavior(behavior) {
    document.sqlQueryForm.behavior.value = behavior;
    document.sqlQueryForm.action = '<%= getCoreUrl(request) %>';
    document.sqlQueryForm.method = 'POST';
    document.sqlQueryForm.submit();

  }
</script>
<FORM name="sqlQueryForm" onSubmit="return checkLength()" <%= outputLocation.equals("newWindow") ? "target=\"outputWindow\"" : "" %>>
<% if ( allowUpdates ) { %>
  <input type="hidden" name="writable" value="true">
<% } %>
<% if ( outputLocation.equals("newWindow") ||
        outputLocation.equals("fileOnClient") ) {
%>
  <input type="hidden" name="outputOnly" value="true">
<% } else { %>
  <input type="hidden" name="outputOnly" value="false">
<% } %>
  <input type="hidden" name="behavior" value="<%= behavior %>">
<% if (!useContextDataSource) { %>
  dbdriver: <INPUT TYPE=text SIZE=50 NAME=dbdriver VALUE="<%= dbdriver %>"><br>
  dburl: <INPUT TYPE=text SIZE=50 NAME=dburl VALUE="<%= dburl %>"><br>
  dbuser: <INPUT TYPE=text SIZE=8 NAME=dbuser VALUE="<%= dbuser %>">
  dbpass: <INPUT TYPE=password SIZE=8 NAME=dbpass VALUE="<%= dbpass %>"><BR>
<% } %>
Start Row: <INPUT TYPE=text SIZE=5 NAME=start VALUE="<%= start %>">
Max Rows: <INPUT TYPE=text SIZE=5 NAME=max VALUE="<%= max %>"><br>
    </td>
    <td valign="top">
Output Location: <select name="outputLocation" onChange="changeOutputLocation()">
  <option value="normal" <%= outputLocation.equals("normal") ? "selected" : "" %>>
    Normal
  <option value="newWindow" <%= outputLocation.equals("newWindow") ? "selected" : "" %>>
    Separate Window
  <option value="fileOnServer" <%= outputLocation.equals("fileOnServer") ? "selected" : "" %>>
    File On Server
  <option value="fileOnClient" <%= outputLocation.equals("fileOnClient") ? "selected" : "" %>>
    File On Client
</select>
<% if ( outputLocation.equals("fileOnServer") ) { %>
File Name: <INPUT TYPE=text NAME="filePath" SIZE="30" VALUE="<%= filePath %>">
<% } %>
<br>
Result Format: <select name="resultFormat" onChange="this.form.submit()">
  <option value="html" <%= resultFormat.equals("html") ? "selected" : "" %>>
    Html
  <option value="delimited" <%= resultFormat.equals("delimited") ? "selected" : "" %>>
    Delimited Text
  <option value="excel" <%= resultFormat.equals("excel") ? "selected" : "" %>>
    Excel Spreadsheet
  <option value="sqlInsert" <%= resultFormat.equals("sqlInsert") ? "selected" : "" %>>
    SQL Insert Syntax
</select>
<br>
<% if ( resultFormat.equals("delimited") ) { %>
<br>
Delimiter: <INPUT TYPE=text NAME="delimiter" SIZE="5" VALUE="<%= delimiter %>">
<br>
Surround values in quotes:
<select name="quotes">
  <option value="true"<%= useQuotes ? "selected" : "" %>>Yes</option>
  <option value="false"<%= useQuotes ? "" : "selected" %>>No</option>
</select>
<% } else if ( resultFormat.equals("sqlInsert") ) { %>
Insert Table: <INPUT TYPE=text NAME="tableName" SIZE="30" VALUE="<%= tableName %>">
<% } %>
    </td>
    <td>
Render HTML: <input type="checkbox" name="escapeHtmlInResults" <%= ( getBooleanPreference("escapeHtmlInResults") ? "" : "checked") %>/>
    </td>
  </tr>
</table>
<br>
<TEXTAREA NAME=query COLS=70 ROWS=10><%= htmlEscape(query) %></TEXTAREA>
<BR>
<INPUT TYPE=submit accessKey="e" VALUE="Execute SQL" onClick="this.form.metaDataMethod.value='';document.sqlQueryForm.behavior.value='executeSql'">
<% if ( getBooleanPreference("showDbMetaDataOptions") == true ) { %>
<%
  String metaDataMethod = (String)request.getParameter("metaDataMethod");
  if ( metaDataMethod == null ) metaDataMethod = "";
%>
<input type="hidden" name="metaDataMethod" value="<%= metaDataMethod %>">
<input type="button" onClick="this.form.metaDataMethod.value='getTables';getBehavior('showDbMetaData')" accessKey="l" value="List Tables">
<input type="button" onClick="this.form.metaDataMethod.value='';getBehavior('showDbMetaData')" accessKey="m" value="Db Meta Data">
  <% if ( ! "showDbMetaData".equals(behavior) ) { %>
</form>
  <% } %>
<% } %>
<br>For helpful SQL hints, <a href="http://tiki.icentris.com/tiki/tiki-index.php?page=JiraSqlHints">see the page in Tiki</a>.
<br><br>

<FONT SIZE=-1>
<%
}
    out.flush();
    if ( "showDbMetaData".equals(behavior) ) {
      Connection connection = null;
      ResultSet results = null;
      try {
        Class.forName(dbdriver);
        if (useContextDataSource) {
          javax.sql.DataSource dsrc = null;
          try {
            dsrc = (javax.sql.DataSource) new javax.naming.InitialContext().lookup("java:comp/env/jdbc/JiraDS");
          } catch (javax.naming.NamingException e) {
            throw new java.lang.reflect.UndeclaredThrowableException(e);
          }
          connection = dsrc.getConnection();
        } else {
//        DriverManager.registerDriver( (Driver) Class.forName(dbdriver).newInstance() );
          connection = DriverManager.getConnection(dburl,dbuser,dbpass);
        }
        DatabaseMetaData dbMetaData = connection.getMetaData();
        String metaDataMethod = (String)request.getParameter("metaDataMethod");
        String[] passedArgs = request.getParameterValues("args");
        String showCreateTable = (String)request.getParameter("showCreateTable");
        if ( metaDataMethod != null && metaDataMethod.length() > 0 ) {
          boolean tables = false;
          out.println("<input type=\"hidden\" name=\"showCreateTable\" value=\"\">");
          if ( metaDataMethod.equals("getTables") ) {
            if ( passedArgs == null || passedArgs.length == 0 ) {
              passedArgs = new String[4];
              passedArgs[0] = connection.getCatalog(); 
            }
            tables = true;
          } else if ( metaDataMethod.equals("getColumns") && passedArgs != null && passedArgs.length > 2 ) {
            out.println("Run <a href=\"javascript:document.sqlQueryForm.behavior.value='showDbMetaData';" +
              "document.sqlQueryForm.metaDataMethod.value='getPrimaryKeys';" +
              "document.sqlQueryForm.args[2].value='" + passedArgs[2] + "';" +
              "document.sqlQueryForm.submit()\">getPrimaryKeys</a><br>\n");
            out.println(" (<a href=\"javascript:" +
              "document.sqlQueryForm.showCreateTable.value='yes';" +
              "document.sqlQueryForm.metaDataMethod.value='';" +
              "document.sqlQueryForm.args[2].value='" + passedArgs[2] + "';" +
              "getBehavior('showDbMetaData')\">Create Statement for " + passedArgs[2] + "</a>)<br>");
          }
          Method[] methods = dbMetaData.getClass().getMethods();
          Method method = null;
          for (int i=0; i < methods.length; i++ ) {
            if ( metaDataMethod.equals(methods[i].getName()) ) {
              method = methods[i];
              break;
            }
          }
          if ( method == null ) {
            out.println("Method DatabaseMetaData." + metaDataMethod + " not found!");
            return;
          }
          Class[] paramTypes = method.getParameterTypes();
          StringBuffer args = new StringBuffer();
          boolean allObjects = true;
          boolean allStrings = true;
          for (int j=0; j < paramTypes.length; j++ ) {
            if ( paramTypes[j].equals(String.class) ) {
              args.append( "String" );
              args.append("  <input type=\"text\" name=\"args\" ");
              int fieldSize = 5;
              if ( passedArgs != null && passedArgs.length > j ) {
                args.append("value=\"" + passedArgs[j] + "\"");
                if ( passedArgs[j] != null && passedArgs[j].length() > 5 ) {
                  if ( passedArgs[j].length() > 20 ) {
                    fieldSize = 20;
                  } else {
                    fieldSize = passedArgs[j].length();
                  }
                }
              }
              args.append(" size=\"" + fieldSize + "\">");
            } else if ( paramTypes[j].equals(Boolean.TYPE) ) {
              args.append("boolean");
              args.append("  <select name=\"args\"><option>true<option");
              if ( passedArgs != null && passedArgs.length > j && "false".equals(passedArgs[j]) ) {
                args.append(" selected");
              }
              args.append(">false</select>");
            // anything but primitives can have "null" passed
            } else if ( ! paramTypes[j].isPrimitive() ) {
              args.append( "<font color=\"red\">" + paramTypes[j].getName() + "</font>" );
              args.append("  <input type=\"hidden\" name=\"args\" value=\"null\">");
              allStrings = false;
            } else {
              args.append( paramTypes[j].getName() );
              allObjects = false;
              allStrings = false;
            }
            if ( passedArgs != null && passedArgs.length > j && "null".equals(passedArgs[j]) ) {
              passedArgs[j] = null;
            }
            if ( j < (paramTypes.length - 1) ) {
              args.append( ", " );
            }
          }
          // let's not crash if we have too many arg parameters in the url
          if ( passedArgs != null &&
               passedArgs.length > paramTypes.length )
          {
            ArrayList tmpArgs = new ArrayList(paramTypes.length);
            for (int j=0; j < paramTypes.length; j++ ) {
              tmpArgs.add( passedArgs[j] );
            }
            passedArgs = (String[]) tmpArgs.toArray(new String[0]);
          }
          Object[] invokeArgs = null;
          if ( paramTypes == null || paramTypes.length == 0 ) {
            out.println("Results of <font color=blue>DatabaseMetaData." + metaDataMethod + "(" + args + ")</font><br>\n");
            invokeArgs = new Object[0];
          } else if ( allObjects == false ) {
            out.println("<font color=red>Sorry, Sql.jsp can't yet help you with DatabaseMetaData." + metaDataMethod + "(" + args + ")</font><br>\n");
            return;
          } else if ( passedArgs == null ) {
            out.println("Supply args for <font color=blue>DatabaseMetaData." + metaDataMethod + "(" + args + ")</font>");
            out.println("  <input type=\"submit\" value=\"Run Method\" onClick=\"document.sqlQueryForm.metaDataMethod.value='" + metaDataMethod + "';getBehavior('showDbMetaData')\"><br>");
          } else {
            out.println("Results of <font color=blue>DatabaseMetaData." + metaDataMethod + "(" + args + ")</font>");
            out.println("  <input type=\"submit\" value=\"Rerun Method\" onClick=\"document.sqlQueryForm.metaDataMethod.value='" + metaDataMethod + "';getBehavior('showDbMetaData')\"><br>");
            invokeArgs = new Object[passedArgs.length];
            for (int j=0; j < paramTypes.length; j++ ) {
              if ( paramTypes[j].equals(String.class) ) {
                invokeArgs[j] = passedArgs[j];
              } else if ( paramTypes[j].equals(Boolean.TYPE) ) {
                invokeArgs[j] = new Boolean(passedArgs[j]);
              } else {
                invokeArgs[j] = null;
              }
            }
          }
          if ( allStrings == false ) {
            out.println("<font color=\"red\">(I can't have you specify args in red because they're not java.lang.String's)</font>");
          }
          out.println("</form>");
          if ( invokeArgs != null ) {
            results = (ResultSet) method.invoke(dbMetaData, invokeArgs);
          } else {
            return;
          }
          htmlResults(connection, getCoreUrl(request), results, max, start, tables, out);
        } else if ( showCreateTable != null && showCreateTable.length() > 0 ) {
          String catalog = connection.getCatalog();
          String metaDataTable = null;
          if ( passedArgs != null &&
               passedArgs.length > 2 &&
               passedArgs[2] != null &&
               ! "null".equals(passedArgs[2]) )
          {
            metaDataTable = passedArgs[2];
          }
          if ( metaDataTable == null ) {
            out.println("<font color=red>Couldn't find args[2] to know which table to do create statement for, there must be a bug in Sql.jsp code.</font>");
            return;
          }
          results = dbMetaData.getColumns(catalog, null, metaDataTable, null);
          out.println("<pre>");
          out.println("CREATE TABLE " + metaDataTable);
          boolean first = true;
          boolean nonstandard = false;
          while ( results.next() ) {
            String type = getDisplayType( results.getInt("DATA_TYPE") );
            if ( type == null ) {
              type = "<font color=red>" + results.getString("TYPE_NAME") + "</font>";
              nonstandard = true;
            }
            if ( first == true ) {
              out.print("( " + results.getString("COLUMN_NAME"));
              first = false;
            } else {
              out.print(", " + results.getString("COLUMN_NAME"));
            }
            out.print(" " + type);
            boolean hasColumnSize = false;
            switch ( results.getInt("DATA_TYPE") ) {
              case Types.CHAR         : hasColumnSize = true; break;
              case Types.NUMERIC      : hasColumnSize = true; break;
              case Types.VARCHAR      : hasColumnSize = true; break;
            }
            if ( hasColumnSize == true ) {
              if ( ! "-1".equals(results.getString("COLUMN_SIZE")) ) {
                out.print("(" + results.getString("COLUMN_SIZE") + ")");
              } else if ( results.getInt("DATA_TYPE") == Types.NUMERIC && getPreference("defaultNumericSize").length() > 0 ) {
                out.print("(" + getPreference("defaultNumericSize") + ")");
              }
            }
            if ( results.getInt("NULLABLE") == 0 ) {
              out.print(" NOT NULL");
            }
            out.print("\n");
          }
          out.println(")</pre>");
          if ( nonstandard == true ) {
            out.print("<font color=red>Data types in red are not JDBC standard</font><br>");
          }
        } else {
          Method[] methods = (new Object()).getClass().getMethods();
          HashSet objectMethods = new HashSet();
          // first find all methods in Object so we can ignore them
          for ( int i=0; i < methods.length; i++ ) {
            // all methods that require no args
            objectMethods.add( methods[i].getName() );
          }
          methods = dbMetaData.getClass().getMethods();
          // so it will be sorted, put output in tree map
          TreeMap methodOutputMap = new TreeMap();
          for ( int i=0; i < methods.length; i++ ) {
            // only methods not in java.lang.Object
            if ( objectMethods.contains(methods[i].getName()) ) continue;
            String methodName = methods[i].getName() + "(??)";
            if ( "java.sql.ResultSet".equals(methods[i].getReturnType().getName()) ) {
              methodOutputMap.put( methods[i].getName(), "<a href=\"javascript:document.sqlQueryForm.behavior.value='showDbMetaData';document.sqlQueryForm.metaDataMethod.value='" + methods[i].getName() + "';document.sqlQueryForm.submit();\">Click for Results</a>");
              continue;
            }

            try {
              // all methods that require no args
              if ( methods[i].getParameterTypes().length == 0 ) {
                methodName = methods[i].getName() + "()";
                  methodOutputMap.put( methodName, methods[i].invoke(dbMetaData, new Object[0]) );
              // methods that require args
              } else {
                if ( "deletesAreDetected".equals(methods[i].getName()) ||
                     "insertsAreDetected".equals(methods[i].getName()) ||
                     "othersDeletesAreVisible".equals(methods[i].getName()) ||
                     "othersInsertsAreVisible".equals(methods[i].getName()) ||
                     "othersUpdatesAreVisible".equals(methods[i].getName()) ||
                     "ownDeletesAreVisible".equals(methods[i].getName()) ||
                     "ownInsertsAreVisible".equals(methods[i].getName()) ||
                     "ownUpdatesAreVisible".equals(methods[i].getName()) ||
                     "supportsResultSetType".equals(methods[i].getName()) ||
                     "updatesAreDetected".equals(methods[i].getName()) )
                {
                  methodName = methods[i].getName() + "(int resultTypes)";
                  StringBuffer sb = new StringBuffer("<pre>");

                  Object output = methods[i].invoke(dbMetaData, new Object[] { new Integer(ResultSet.TYPE_FORWARD_ONLY) });
                  sb.append(methods[i].getName() + "(ResultSet.TYPE_FORWARD_ONLY)=[<font color=\"orange\">" +
                    output + "</font>]\n");

                  output = methods[i].invoke(dbMetaData, new Object[] { new Integer(ResultSet.TYPE_SCROLL_INSENSITIVE) });
                  sb.append(methods[i].getName() + "(ResultSet.TYPE_SCROLL_INSENSITIVE)=[<font color=\"orange\">" +
                    output + "</font>]\n");

                  output = methods[i].invoke(dbMetaData, new Object[] { new Integer(ResultSet.TYPE_SCROLL_SENSITIVE) });
                  sb.append(methods[i].getName() + "(ResultSet.TYPE_SCROLL_SENSITIVE)=[<font color=\"orange\">" +
                    output + "</font>]\n");

                  sb.append("</pre>");
                  methodOutputMap.put( methodName, sb.toString() );
                } else if ( "supportsResultSetConcurrency".equals(methods[i].getName()) ) {
                  methodName = methods[i].getName() + "(int resultTypes, int concurTypes)";
                  StringBuffer sb = new StringBuffer("<pre>");

                  Object output = methods[i].invoke(dbMetaData, 
                    new Object[] { new Integer(ResultSet.TYPE_FORWARD_ONLY), new Integer(ResultSet.CONCUR_READ_ONLY) });
                  sb.append(methods[i].getName() + "(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_READ_ONLY)=[<font color=\"orange\">" +
                    output + "</font>]\n");

                  output = methods[i].invoke(dbMetaData, 
                    new Object[] { new Integer(ResultSet.TYPE_SCROLL_INSENSITIVE), new Integer(ResultSet.CONCUR_READ_ONLY) });
                  sb.append(methods[i].getName() + "(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY)=[<font color=\"orange\">" +
                    output + "</font>]\n");

                  output = methods[i].invoke(dbMetaData, 
                    new Object[] { new Integer(ResultSet.TYPE_SCROLL_SENSITIVE), new Integer(ResultSet.CONCUR_READ_ONLY) });
                  sb.append(methods[i].getName() + "(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_READ_ONLY)=[<font color=\"orange\">" +
                    output + "</font>]\n");

                  output = methods[i].invoke(dbMetaData, 
                    new Object[] { new Integer(ResultSet.TYPE_FORWARD_ONLY), new Integer(ResultSet.CONCUR_UPDATABLE) });
                  sb.append(methods[i].getName() + "(ResultSet.TYPE_FORWARD_ONLY, ResultSet.CONCUR_UPDATABLE)=[<font color=\"orange\">" +
                    output + "</font>]\n");

                  output = methods[i].invoke(dbMetaData, 
                    new Object[] { new Integer(ResultSet.TYPE_SCROLL_INSENSITIVE), new Integer(ResultSet.CONCUR_UPDATABLE) });
                  sb.append(methods[i].getName() + "(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_UPDATABLE)=[<font color=\"orange\">" +
                    output + "</font>]\n");

                  output = methods[i].invoke(dbMetaData, 
                    new Object[] { new Integer(ResultSet.TYPE_SCROLL_SENSITIVE), new Integer(ResultSet.CONCUR_UPDATABLE) });
                  sb.append(methods[i].getName() + "(ResultSet.TYPE_SCROLL_SENSITIVE, ResultSet.CONCUR_UPDATABLE)=[<font color=\"orange\">" +
                    output + "</font>]\n");

                  sb.append("</pre>");
                  methodOutputMap.put( methodName, sb.toString() );
                } else if ( "supportsTransactionIsolationLevel".equals(methods[i].getName()) ) {
                  methodName = methods[i].getName() + "(int connectionTypes)";
                  StringBuffer sb = new StringBuffer("<pre>");

                  Object output = methods[i].invoke(dbMetaData, new Object[] { new Integer(Connection.TRANSACTION_NONE) });
                  sb.append(methods[i].getName() + "(Connection.TRANSACTION_NONE)=[<font color=\"orange\">" +
                    output + "</font>]\n");

                  output = methods[i].invoke(dbMetaData, new Object[] { new Integer(Connection.TRANSACTION_READ_COMMITTED) });
                  sb.append(methods[i].getName() + "(Connection.TRANSACTION_READ_COMMITTED)=[<font color=\"orange\">" +
                    output + "</font>]\n");

                  output = methods[i].invoke(dbMetaData, new Object[] { new Integer(Connection.TRANSACTION_READ_UNCOMMITTED) });
                  sb.append(methods[i].getName() + "(Connection.TRANSACTION_READ_UNCOMMITTED)=[<font color=\"orange\">" +
                    output + "</font>]\n");

                  output = methods[i].invoke(dbMetaData, new Object[] { new Integer(Connection.TRANSACTION_REPEATABLE_READ) });
                  sb.append(methods[i].getName() + "(Connection.TRANSACTION_REPEATABLE_READ)=[<font color=\"orange\">" +
                    output + "</font>]\n");

                  output = methods[i].invoke(dbMetaData, new Object[] { new Integer(Connection.TRANSACTION_SERIALIZABLE) });
                  sb.append(methods[i].getName() + "(Connection.TRANSACTION_SERIALIZABLE)=[<font color=\"orange\">" +
                    output + "</font>]\n");

                  sb.append("</pre>");
                  methodOutputMap.put( methodName, sb.toString() );
                } else if ( "supportsConvert".equals(methods[i].getName()) ) {
                  methodName = methods[i].getName() + "(int sqlType, int sqlType)";
                  StringBuffer sb = new StringBuffer("<pre>");

                  int[] types = new int[] { Types.ARRAY, Types.BIGINT, Types.BINARY, Types.BIT, Types.BLOB,
                    Types.CHAR, Types.CLOB, Types.DATE, Types.DECIMAL, Types.DISTINCT, Types.DOUBLE, Types.FLOAT,
                    Types.INTEGER, Types.JAVA_OBJECT, Types.LONGVARBINARY, Types.LONGVARCHAR, Types.NULL,
                    Types.NUMERIC, Types.OTHER, Types.REAL, Types.REF, Types.SMALLINT, Types.STRUCT,
                    Types.TIME, Types.TIMESTAMP, Types.TINYINT, Types.VARBINARY, Types.VARCHAR };
                  String[] typeNames = new String[] { "ARRAY", "BIGINT", "BINARY", "BIT", "BLOB",
                    "CHAR", "CLOB", "DATE", "DECIMAL", "DISTINCT", "DOUBLE", "FLOAT",
                    "INTEGER", "JAVA_OBJECT", "LONGVARBINARY", "LONGVARCHAR", "NULL",
                    "NUMERIC", "OTHER", "REAL", "REF", "SMALLINT", "STRUCT",
                    "TIME", "TIMESTAMP", "TINYINT", "VARBINARY", "VARCHAR" };
                  for (int j=0; j < types.length; j++ ) {
                    for (int k=0; k < types.length; k++ ) {
                      Object output = methods[i].invoke(dbMetaData, new Object[] { new Integer(types[j]), new Integer(types[k]) });
                      sb.append(methods[i].getName() + "(Types." + typeNames[j] + ", Types." + typeNames[k] + ")=[<font color=\"orange\">" +
                        output + "</font>]\n");
                    }
                  }
                  sb.append("</pre>");
                  methodOutputMap.put( methodName, sb.toString() );
                } else {
                  Class[] paramTypes = methods[i].getParameterTypes();
                  StringBuffer args = new StringBuffer();
                  boolean allStrings = true;
                  for (int j=0; j < paramTypes.length; j++ ) {
                    args.append( paramTypes[j].getName() );
                    if ( j < (paramTypes.length - 1) ) {
                      args.append( ", " );
                    }
                    if ( ! "java.lang.String".equals(paramTypes[j].getName()) ) {
                      allStrings = false;
                    }
                  }
                  methodName = methods[i].getName() + "(" + args + ")";
                  if ( allStrings == true ) {
                    StringBuffer sb = new StringBuffer("<pre>");

                    sb.append("</pre>");
                    //methodOutputMap.put( methodName, sb.toString() );
                    methodOutputMap.put( methodName, "<font color=\"red\">Sql.jsp can't yet help you with this method</font>" );
                  } else {
                    methodOutputMap.put( methodName, "<font color=\"red\">Sql.jsp can't yet help you with this method</font>" );
                  }
                }
              }
            } catch (Throwable e) {
              if ( e instanceof InvocationTargetException ) {
                e = ((InvocationTargetException) e).getTargetException();
              }
              methodOutputMap.put( methodName,
                "<font color=red>" + e.toString() + "</font>" );
            }
          }
          out.println("<table border=1>");
          out.println("  <tr>");
          out.println("    <td align=center><big><b>java.sql.DatabaseMetaData method</b></big></td>");
          out.println("    <td><big><b>Method Output</b></big></td>");
          out.println("  </tr>");
          Iterator methodNames = methodOutputMap.keySet().iterator();
          for ( int row=1; methodNames.hasNext(); row++ ) {
            String tdColor = "#FFFFFF";
            if ( row % 2 == 0 ) {
              tdColor = "#DDDDDD";
            }
            String methodName   = (String) methodNames.next();
            out.println("  <tr>");
            out.println("    <td bgcolor=\"" + tdColor + "\" valign=\"top\"><b>" + methodName + "</b></td>");
            out.println("    <td bgcolor=\"" + tdColor + "\">&nbsp;" + methodOutputMap.get(methodName) + "</td>");
            out.println("  </tr>");
          }
          out.println("</table>");
        }
      } catch (Throwable e) {
        if ( e instanceof InvocationTargetException ) {
          e = ((InvocationTargetException) e).getTargetException();
        }
        if ( e instanceof SQLException ) {
          System.err.println("SQLException.getErrorCode()=[" + ((SQLException) e).getErrorCode() + "]");
        }
        e.printStackTrace();
        sqlError = e.toString();
      } finally {
        try { results.close();    } catch (Exception e) {}
        try { connection.close(); } catch (Exception e) {}
      }
    } else if ( "editRecord".equals(behavior) ||
                "viewRecord".equals(behavior) ||
                "deleteRecord".equals(behavior) ) {
      Connection connection = null;
      PreparedStatement statement = null;
      ResultSet results = null;
      try {
        Class.forName(dbdriver);
        if (useContextDataSource) {
          javax.sql.DataSource dsrc = null;
          try {
            dsrc = (javax.sql.DataSource) new javax.naming.InitialContext().lookup("java:comp/env/jdbc/JiraDS");
          } catch (javax.naming.NamingException e) {
            throw new java.lang.reflect.UndeclaredThrowableException(e);
          }
          connection = dsrc.getConnection();
        } else {
//        DriverManager.registerDriver( (Driver) Class.forName(dbdriver).newInstance() );
          connection = DriverManager.getConnection(dburl,dbuser,dbpass);
        }
        String[] whereColumns = request.getParameterValues("whereColumns");
        String[] whereValues = request.getParameterValues("whereValues");
        StringBuffer sql = new StringBuffer("SELECT * FROM " + tableName + " ");
        ArrayList sqlParams = new ArrayList();
        for ( int i=0; whereColumns != null && i < whereColumns.length; i++ ) {
          if ( i == 0 ) {
            sql.append("\nWHERE ");
          } else {
            sql.append("\n  AND ");
          }
          String value = request.getParameter("whereValue." + whereColumns[i]);
          if ( value != null ) {
            sql.append(whereColumns[i] + " = ?");
            sqlParams.add( value );
          } else {
            sql.append(whereColumns[i] + " IS NULL");
          }
        }
        statement = connection.prepareStatement(sql.toString());
        statement.setEscapeProcessing(false);
        for ( int i=0; i < sqlParams.size(); i++ ) {
          String value = (String) sqlParams.get(i);
          statement.setString( i + 1, value );
        }
        if ( getBooleanPreference("showQueryOnView") == true ) {
          out.write("<P>Results of [<font color=blue><pre>\n" + htmlEscape(statement.toString()) + "</pre></font>]\n");
        }
        results = statement.executeQuery();
        ResultSetMetaData resultsMeta = results.getMetaData();
        int colCount = resultsMeta.getColumnCount();
        boolean hadResults = false;
        while ( results.next() ) {
          hadResults = true;
          out.println("<form method=\"POST\">\n");
          out.println("<table border=1>\n");
          for ( int i=1; i <= colCount; i++ ) {
            out.println("  <tr>\n");
            out.println("    <td><b>" + resultsMeta.getColumnLabel(i) + "</b></td>\n");
            if ( "editRecord".equals(behavior) ) {
              out.println("    <td><input type=\"text\" name=\"" + resultsMeta.getColumnLabel(i) + "\" value=\"" + htmlEscape(results.getString(i)) + "\"></td>\n");
            } else if ( "viewRecord".equals(behavior) ) {
              String value = htmlEscape(results.getString(i));
              if ( value == null ) {
                value = "<font color=\"orange\">null</font>";
              }
              out.println("    <td>" + value + "</td>\n");
            }
            out.println("  </tr>\n");
          }
          if ( "editRecord".equals(behavior) ) {
            out.println("  <tr>\n");
            out.println("    <td colspan=\"2\"><input type=\"submit\" value=\"Save\"></td>\n");
            out.println("  </tr>\n");
          }
          out.println("</table>\n");
          out.println("</form>\n");
        }
        if ( hadResults == false ) {
          if ( getBooleanPreference("showQueryOnView") == true ) {
            out.write("<P><font color=red>No matching record found!</font>\n");
          } else {
            out.write("<P><font color=red>Found no record using query=[</font><font color=blue><pre>\n" + htmlEscape(statement.toString()) + "</pre></font><font color=red>]!</font>\n");
          }
        }
      } catch ( SQLException e ) {
        if ( statement != null ) {
          System.err.println("ERROR: [Sql.jsp] Error while executing " + statement + ":");
        }
        e.printStackTrace();
        sqlError = e.toString();
      } finally {
        try { results.close();    } catch (Exception e) {}
        try { connection.close(); } catch (Exception e) {}
      }
    } else if ( ! query.equals("") ) {
      Connection connection = null;
      try {
        Class.forName(dbdriver);
        if (useContextDataSource) {
          javax.sql.DataSource dsrc = null;
          try {
            dsrc = (javax.sql.DataSource) new javax.naming.InitialContext().lookup("java:comp/env/jdbc/JiraDS");
          } catch (javax.naming.NamingException e) {
            throw new java.lang.reflect.UndeclaredThrowableException(e);
          }
          connection = dsrc.getConnection();
        } else {
//        DriverManager.registerDriver( (Driver) Class.forName(dbdriver).newInstance() );
          connection = DriverManager.getConnection(dburl,dbuser,dbpass);
        }
        Statement statement = connection.createStatement();
        boolean enableEscapeProcessing = getBooleanPreference("jdbc.enableEscapeProcessing");
        statement.setEscapeProcessing(enableEscapeProcessing);
        // if I find no ; at the end of a line, this is probably not a batch of sql statements
        if ( query.indexOf(";\r\n") == -1 ) {
          query = query.trim();
          // remove possible trailing semicolon
          if ( query.endsWith(";") ) query = query.substring(0, query.length() - 1);
          // I can't figure out remaining rows if I've limited my cursor
          if ( getBooleanPreference("calculateRemainingRows") == false ) {
            statement.setMaxRows( max + start );
          }
          long startTime = System.currentTimeMillis();
          boolean haveResults = statement.execute(query);

          // if this was a "SELECT" statement it will have results
          if ( haveResults ) {
            ResultSet myResult = statement.getResultSet();
            if ( outputLocation.equals("fileOnServer") ) {
              java.net.URL docRootUrl = application.getResource("/");
              String docRootPath = docRootUrl.getFile();
              java.io.FileWriter fileWriter = new java.io.FileWriter(docRootPath + filePath);
              formatResults(connection, request.getRequestURI(), myResult, max, start, request, fileWriter);
              sqlError = "File " + filePath + " created on server.";
            } else {
              if ( getBooleanPreference("calculateQueryRunTime") == true ) {
                if ( resultFormat == null || resultFormat.equals("html") ) {
                  float runTimeSeconds = (System.currentTimeMillis() - startTime) / 1000f;
                  out.write("<p><font color=\"green\"><b>Query took " + runTimeSeconds + " seconds</b></font></p>");
                }
              }
              formatResults(connection, request.getRequestURI(), myResult, max, start, request, out);
            }
          // if this was not a "SELECT" statement, it will simply tell me how many rows were affected
          } else {
            out.write("<P>Query [<FONT COLOR=blue><PRE>\n" + htmlEscape(query) + "</PRE></FONT>] Executed");
            if ( getBooleanPreference("calculateQueryRunTime") == true ) {
              float runTimeSeconds = (System.currentTimeMillis() - startTime) / 1000f;
              out.write(" in " + runTimeSeconds + " seconds");
            }
            int rowsAffected = statement.getUpdateCount();
            if ( rowsAffected != -1 ) out.write(". <B>" + rowsAffected + "</B> Rows Affected");
            out.write("</P>");
          }

        // found a ; at the end of a line, this is probably a batch of statements
        } else {
          out.write("<P>Batch of Queries Executed.</P>");
          // expect the separation between statements to be a ; at the end of a line
          int begin = 0;
          int end = query.indexOf( ";\r\n", begin );
          ArrayList sqlStatements = new ArrayList();
          boolean useBatch = false;
          long totalRunTime = 0;
          while ( end != -1 ) {
            String sqlStatement = query.substring( begin, end ).trim();
            if ( useBatch == true ) {
              try {
                statement.addBatch( sqlStatement );
              } catch (SQLException e) {
                useBatch = false;
                System.err.println("ERROR: [Sql.jsp] Error while calling addBatch().  I assume " +
                  statement.getClass().getName() + " does not support addBatch(), so I'll try executeUpdate().");
              }
            }
            if ( useBatch == false ) {
              try {
                if ( sqlStatement.length() >= 6 &&
                     "SELECT".equalsIgnoreCase(sqlStatement.substring(0,6)) )
                {
                  long startTime = System.currentTimeMillis();
                  ResultSet results = statement.executeQuery( sqlStatement );
                  long runTime = System.currentTimeMillis() - startTime;
                  totalRunTime += runTime;
                  float runTimeSeconds = runTime / 1000f;
                  out.write("<P>Query [<FONT COLOR=blue><PRE>\n" + htmlEscape(sqlStatement) + "</PRE></FONT>] ");
                  if ( getBooleanPreference("calculateQueryRunTime") == true ) {
                    out.write("took " + runTimeSeconds + " seconds and ");
                  }
                  out.write("Has Results:</P>");
                  out.flush();
                  formatResults(connection, request.getRequestURI(), results, max, start, request, out);
                  out.flush();
                } else {
                  long startTime = System.currentTimeMillis();
                  int rowsAffected = statement.executeUpdate( sqlStatement );
                  out.write("<P>Query [<FONT COLOR=blue><PRE>\n" + htmlEscape(sqlStatement) + "</PRE></FONT>] Executed");
                  if ( getBooleanPreference("calculateQueryRunTime") == true ) {
                    long runTime = System.currentTimeMillis() - startTime;
                    totalRunTime += runTime;
                    float runTimeSeconds = runTime / 1000f;
                    out.write(" in " + runTimeSeconds + " seconds");
                  }
                  out.write(".  <B>" + rowsAffected + "</B> Rows Affected</P>");
                  out.flush();
                }
              } catch (SQLException e) {
                out.write("<P>Error [<FONT color=red>" + e.getMessage() + "</FONT>] occured while executing query [<PRE>\n" + htmlEscape(sqlStatement) + "</PRE>]");
              }
            }
            sqlStatements.add( sqlStatement );
            // start looking after the last ";"
            // the reason I don't do "+ 3" is to avoid begin being a position outside the query
            begin = end + 2;
            // see if there are any more
            end = query.indexOf( ";\r\n", begin );
            if ( end == -1 && begin < query.length() ) {
              // look at whatever's left after the last ; at the end of a line
              String leftover = query.substring( begin );
              leftover = leftover.trim();
              // if there's more than just whitespace left, assume it's another sql query
              if ( leftover.length() > 1 ) {
                end = query.length();
                // strip off last semicolon if there is one
                if ( leftover.endsWith(";") ) end--;
              }
            }
          }
          if ( useBatch == true ) {
            int[] rowsAffected = new int[0];
            try {
              rowsAffected = statement.executeBatch();
            } catch (BatchUpdateException e) {
              e.printStackTrace();
              SQLException sqle = e.getNextException();
              do {
                sqle.printStackTrace();
              } while ( (sqle = sqle.getNextException()) != null );
              rowsAffected = e.getUpdateCounts();
            }
            if ( rowsAffected.length != sqlStatements.size() ) {
              out.write("<P><FONT COLOR=red>Something weird happened, the number of rowsAffectedCount's returned by executeBatch() is <B>" + rowsAffected.length + "</B> when there were actually <B>" + sqlStatements.size() + "</B> statements entered</FONT></P>");
            } else {
              for ( int i = 0; i < sqlStatements.size(); i++ ) {
                out.write("<P>Query [<FONT COLOR=blue><PRE>\n" + htmlEscape((String)sqlStatements.get(i)) + "</PRE></FONT>] Executed.  <B>" + rowsAffected[i] + "</B> Rows Affected</P>");
              }
            }
          } else {
            out.write("<P><B>" + sqlStatements.size() + "</B> Statements Executed");
            if ( getBooleanPreference("calculateQueryRunTime") == true ) {
              float totalRunTimeSeconds = totalRunTime / 1000f;
              out.write(" in " + totalRunTimeSeconds + " seconds");
            }
            out.write(".</P>");
          }
        }
      } catch ( Exception e ) {
        if ( e instanceof SQLException ) {
          System.err.println("SQLException.getErrorCode()=[" + ((SQLException) e).getErrorCode() + "]");
        }
        e.printStackTrace();
        sqlError = e.toString();
      } finally {
        if ( connection != null ) connection.close();
      }
    }
if ( showOutputOnly == false ) {
  %></FONT><%
}
if ( ! outputLocation.startsWith("fileOn") ) {
  %><P><FONT COLOR=red><PRE><%= htmlEscape(sqlError) %></PRE></FONT></P><%
  %><script>window.focus();</script><%
}
if ( showOutputOnly == false ) {
  %></BODY><%
  %></HTML><%
}
%><%!
  public void formatResults(Connection connection, String requestUri, ResultSet resultSet, int max, int start, HttpServletRequest request, java.io.Writer out)
    throws java.io.IOException
  {
    String outputLocation = (String) request.getParameter("outputLocation");
    boolean htmlEscape = true;
    if ( outputLocation != null && outputLocation.startsWith("fileOn") ) {
      htmlEscape = false;
    }
    if ( getBooleanPreference("escapeHtmlInResults") == false ) {
      htmlEscape = false;
    }
    String resultFormat = (String) request.getParameter("resultFormat");
    if ( resultFormat == null || resultFormat.equals("html") ) {
      htmlResults(connection, getCoreUrl(request), resultSet, max, start, false, out);
    } else if ( resultFormat.equals("delimited") || resultFormat.equals("excel") ) {
      String delimiter = (String) request.getParameter("delimiter");
      if ( delimiter == null || delimiter.equals("") ) {
        delimiter = getPreference("csv.defaultDelimiter");
      }
      if ( resultFormat.equals("excel") ) {
        delimiter = "\t";
      }
      boolean useQuotes = getBooleanPreference("csv.useQuotes");
      String quotes = (String)request.getParameter("quotes");
      if ( quotes != null ) {
        useQuotes = "true".equals(quotes);
      }
      delimitedResults(resultSet, max, start, delimiter, useQuotes, htmlEscape, out);
    } else if ( resultFormat.equals("sqlInsert") ) {
      String tableName = (String) request.getParameter("tableName");
      sqlInsertResults(resultSet, max, start, tableName, htmlEscape, out);
    } else {
      out.write("<font color=red>Couldn't find a valid result format.</font>");
    }
    out.flush();
  }

  public void delimitedResults(ResultSet resultSet, int max, int start, String delimiter, boolean useQuotes, boolean htmlEscape, java.io.Writer out)
    throws java.io.IOException
  {
    if ( htmlEscape == true ) out.write("<pre>\n");
    int row = 1;
    try {
      ResultSetMetaData myResultMetaData = resultSet.getMetaData();
      int colCount = myResultMetaData.getColumnCount();
      for ( int i=1; i <= colCount; i++ ) {
        if (i > 1) {
          out.write(delimiter);
        }
        if ( useQuotes == true ) {
          out.write("\"");
          if ( htmlEscape == true ) {
            out.write( htmlEscape(replace(myResultMetaData.getColumnLabel(i), "\"", "\"\"")) );
          } else {
            out.write( replace(myResultMetaData.getColumnLabel(i), "\"", "\"\"") );
          }
          out.write("\"");
        } else {
          if ( htmlEscape == true ) {
            out.write(htmlEscape(myResultMetaData.getColumnLabel(i)));
          } else {
            out.write(myResultMetaData.getColumnLabel(i));
          }
        }
      }
      out.write("\n");
      if ( start > 1 ) {
        try {
          // start one less that start, so the call to next() will
          // leave us at the right place
          resultSet.absolute( start - 1 );
          row = start;
        } catch (SQLException e) {
          e.printStackTrace();
        }
      }
      while ( resultSet.next() ) {
        if ( row < start ) {
          row++;
          continue;
        } else if ( row >= start + max ) {
          break;
        }
        for ( int i=1; i <= colCount; i++ ) {
          if (i > 1) {
            out.write(delimiter);
          }
          if (resultSet.getString(i) == null) {
            if ( htmlEscape == true ) {
              out.write(htmlEscape(getPreference("csv.NULLString")));
            } else {
              out.write(getPreference("csv.NULLString"));
            }
          } else {
            if ( useQuotes == true ) {
              out.write("\"");
              if ( htmlEscape == true ) {
                out.write( htmlEscape(replace(resultSet.getString(i), "\"", "\"\"")) );
              } else {
                out.write( replace(resultSet.getString(i), "\"", "\"\"") );
              }
              out.write("\"");
            } else {
              if ( htmlEscape == true ) {
                out.write(htmlEscape(resultSet.getString(i)));
              } else {
                out.write(resultSet.getString(i));
              }
            }
          }
        }
        out.write("\n");
        row++;
      }
      row = row - start;
    } catch ( SQLException e ) {
      e.printStackTrace();
    }
    if ( htmlEscape == true ) out.write("</pre>");
    if ( htmlEscape == true ) out.write("<font size=\"+1\"><b>" + row + "</b> rows displayed</font>\n");
    out.flush();
  }

  public void sqlInsertResults(ResultSet resultSet, int max, int start, String tableName, boolean htmlEscape, java.io.Writer out)
    throws java.io.IOException
  {
    if ( tableName == null || tableName.equals("") ) {
      tableName = "{table_name}";
      try {
        if ( resultSet.getMetaData().getTableName(1) != null &&
             resultSet.getMetaData().getTableName(1).length() > 0 ) {
          tableName = resultSet.getMetaData().getTableName(1);
        }
      } catch (Exception e) {}
    }
    if ( htmlEscape == true ) out.write("<pre>");
    int row = 1;
    String colList = "";
    try {
      ResultSetMetaData myResultMetaData = resultSet.getMetaData();
      int colCount = myResultMetaData.getColumnCount();
      for ( int i=1; i <= colCount; i++ ) {
        if (colList.length() > 0) {
          colList += ", ";
        }
        if ( htmlEscape == true ) {
          colList += htmlEscape(myResultMetaData.getColumnLabel(i));
        } else {
          colList += myResultMetaData.getColumnLabel(i);
        }
        if (tableName.length() < 1) {
          if ( htmlEscape == true ) {
            tableName = htmlEscape(myResultMetaData.getTableName(i));
          } else {
            tableName = myResultMetaData.getTableName(i);
          }
        }
      }
      colList = "INSERT INTO " + tableName + " (" + colList + ") VALUES (";
      if ( start > 1 ) {
        try {
          // start one less that start, so the call to next() will
          // leave us at the right place
          resultSet.absolute( start - 1 );
          row = start;
        } catch (SQLException e) {
          e.printStackTrace();
        }
      }
      while ( resultSet.next() ) {
        if ( row < start ) {
          row++;
          continue;
        } else if ( row >= start + max ) {
          break;
        }
        out.write(colList);
        for ( int i=1; i <= colCount; i++ ) {
          if (i > 1) {
            out.write(", ");
          }
          if (resultSet.getString(i) == null) {
            out.write("NULL");
          } else {
            if ( getBooleanPreference("sqlInsert.useQuotesAlways") != true && myResultMetaData.getPrecision(i) > 0 ) {
              // we're dealing with a nuber, no need to quote it
              if ( htmlEscape == true ) {
                out.write( htmlEscape(resultSet.getString(i)) );
              } else {
                out.write( resultSet.getString(i) );
              }
            } else {
              // better quote this value
              out.write("'");
              if ( htmlEscape == true ) {
                out.write( htmlEscape(replace(resultSet.getString(i), "'", "''")) );
              } else {
                out.write( replace(resultSet.getString(i), "'", "''") );
              }
              out.write("'");
            }
          }
        }
        out.write(");\n");
        row++;
      }
      row = row - start;
    } catch ( SQLException e ) {
      e.printStackTrace();
    }
    if ( htmlEscape == true ) out.write("</pre>");
    if ( htmlEscape == true ) out.write("<font size=\"+1\"><b>" + row + "</b> rows displayed</font>\n");
    out.flush();
  }

  int statementCount = 0;
  public void htmlResults(Connection connection, String requestUri, ResultSet resultSet, int max, int start, boolean tables, java.io.Writer out)
    throws java.io.IOException
  {
    statementCount++;
    out.write("  <table border=\"1\">");
    int row = 1;
    out.write("    <tr>\n");
    boolean hasMoreResults = true;
    try {
      ResultSetMetaData resultsMeta = resultSet.getMetaData();
      int colCount = resultsMeta.getColumnCount();
      boolean hasOnlyOneTable = true;
      String tableName = null;
      boolean hasWritableColumn = false;
      boolean showExtrasColumn = false;
      boolean showEditLink = false;
      if ( getBooleanPreference("allowView") ||
           getBooleanPreference("allowEdit") )
      {
        HashSet tableSet = new HashSet();
        for ( int i=1; i <= colCount; i++ ) {
          tableName = resultsMeta.getTableName(i);
          tableSet.add( tableName );
          if ( resultsMeta.isDefinitelyWritable(i) ) {
            hasWritableColumn = true;
          }
        }
        if ( tableSet.size() > 1 ) {
          hasOnlyOneTable = false;
        }
        if ( hasOnlyOneTable == true ) {
          showExtrasColumn = true;
        }
        if ( hasOnlyOneTable == true && hasWritableColumn == true ) {
          showEditLink = true;
        }
      }
      if ( getBooleanPreference("allowDelete") ) {
        showExtrasColumn = true;
      }
      if ( showExtrasColumn == true ) {
        out.write("    <td>&nbsp;</td>\n");
      }
      // Print out column headers
      for ( int i=1; i <= colCount; i++ ) {
        if ( getBooleanPreference("showColumnMetaData") == true ) {
          out.write("    <td style=\"cursor: pointer;\" ");
          if ( getBooleanPreference("columnMetaOnMouseOver") == true ) {
            out.write("onMouseOver");
          } else {
            out.write("onClick");
          }
          out.write("=\"document.getElementById('tip" + i + "," + statementCount + "').style.display='block';");
          if ( getIntPreference("columnMetaDataCloseInSeconds") > 0 ) {
            out.write("setTimeout('document.getElementById(\\'tip" + i + "," + statementCount + "\\').style.display=\\'none\\'', " + getIntPreference("columnMetaDataCloseInSeconds") + "000)");
          }
          out.write("\" onMouseOut=\"document.getElementById('tip" + i + "," + statementCount + "').style.display='none'\"><b>" +  htmlEscape(resultsMeta.getColumnLabel(i)) + "</b>\n");
          out.write("    <br><div id=\"tip" + i + "," + statementCount + "\" style=\"position: absolute;z-index: 2;background-color: yellow; display: none; border-style: solid; border-color: black; border-width: thin; padding:5; font-family: monospace\" nowrap>");
          out.write("table:           " + resultsMeta.getTableName(i) + "<br>\n");
          out.write("size:            " + resultsMeta.getColumnDisplaySize(i) + "<br>\n");
          out.write("type:            " + getDisplayType(resultsMeta.getColumnType(i)) + "<br>\n");
          String nullable = "not yet determined";
          switch (resultsMeta.isNullable(i)) {
            case ResultSetMetaData.columnNoNulls         : nullable = "false"; break;
            case ResultSetMetaData.columnNullable        : nullable = "true"; break;
            case ResultSetMetaData.columnNullableUnknown : nullable = "unknown"; break;
          }
          out.write("isNullable:      " + nullable + "<br>\n");
          out.write("isReadOnly:      " + resultsMeta.isReadOnly(i) + "<br>\n");
          out.write("isWritable:      " + resultsMeta.isWritable(i) + "<br>\n");
          out.write("isDefinitelyWritable:      " + resultsMeta.isDefinitelyWritable(i) + "<br>\n");
          out.write("isAutoIncrement: " + resultsMeta.isAutoIncrement(i) + "<br>\n");
          out.write("</div></td>\n");
        } else {
          out.write("<td><b>" +  htmlEscape(resultsMeta.getColumnLabel(i)) + "</b></td>\n");
        }
      }
      if ( start > 1 ) {
        try {
          if ( resultSet.getClass().getMethod("absolute", new Class[0]) != null &&
               resultSet.getClass().getMethod("absolute", new Class[0]).isAccessible() == true )
          {
            // start one less that start, so the call to next() will
            // leave us at the right place
            resultSet.absolute( start - 1 );
            row = start;
          }
        } catch (NoSuchMethodException e) {
        } catch (Throwable e) {
          e.printStackTrace();
        }
      }
      while ( (hasMoreResults = resultSet.next()) == true ) {
        if ( row < start ) {
          row++;
          continue;
        } else if ( row >= start + max ) {
          break;
        }
        out.write("    </tr>\n");
        out.flush();
        out.write("    <tr>\n");
        String tdColor = "#FFFFFF";
        if ( row % 2 == 0 ) {
          tdColor = "#DDDDDD";
        }
        if ( showExtrasColumn == true ) {
          if ( ! "".equals(tableName) ) {
            String formName = "extrasForm" + row;
            out.write("    <form action=\"" + requestUri + "\" name=\"" + formName + "\" method=\"POST\">\n");
            out.write("      <input type=\"hidden\" name=\"behavior\" value=\"\">\n");
            out.write("      <input type=\"hidden\" name=\"tableName\" value=\"" + tableName + "\">\n");
            out.write( getColumnHiddenFields(resultSet, connection) );
            out.write("    <td nowrap>\n");
            if ( showEditLink == true ) {
              out.write("<a href=\"javascript:document." + formName + ".behavior.value='editRecord';document." + formName + ".submit()\">Edit</a>\n");
            }
            if ( getBooleanPreference("allowView") ) {
              out.write("<a href=\"javascript:document." + formName + ".behavior.value='viewRecord';document." + formName + ".submit()\"> View</a>\n");
            }
            if ( getBooleanPreference("allowDelete") ) {
              out.write("<a href=\"javascript:document." + formName + ".behavior.value='deleteRecord';document." + formName + ".submit()\"> Delete</a>\n");
            }
            out.write("    </td>\n");
            out.write("    </form>\n");
          } else {
            out.write("    <td><a href=\"javascript:alert('Couldn\\'t create Edit/View/Delete links because your jdbc driver sucks and didn\\'t give me the table name')\">error</a></td>"); 
          }
        }
        for ( int i=1; i <= colCount; i++ ) {
          String value = resultSet.getString(i);
          if ( value == null ) {
            value = "<font color=\"orange\">null</font>";
          } else if ( getBooleanPreference("escapeHtmlInResults") == true ) {
            value = htmlEscape(value);
          }
          out.write("<td bgcolor=\"" + tdColor + "\"><PRE>" + value ); 
          if ( tables && i == 3 ) {
            out.write(" (<a href=\"javascript:document.sqlQueryForm.behavior.value='showDbMetaData';" +
              "document.sqlQueryForm.metaDataMethod.value='getColumns';" + 
              "document.sqlQueryForm.args[2].value='" + resultSet.getString(i) + "';" + 
              "document.sqlQueryForm.submit()\">Details</a>)");
          }
          if ( tables && i == 3 && 
               ( "TABLE".equals(resultSet.getString("TABLE_TYPE")) ||
                 resultSet.getString("TABLE_TYPE") == null) )
          {
            out.write(" (<a href=\"javascript:" +
              "document.sqlQueryForm.showCreateTable.value='yes';" +
              "document.sqlQueryForm.metaDataMethod.value='';" +
              "document.sqlQueryForm.args[2].value='" + resultSet.getString(i) + "';" +
              "getBehavior('showDbMetaData')\">Create Statement</a>)");
          }
          if ( tables && i == 3 && 
               ( "TABLE".equals(resultSet.getString("TABLE_TYPE")) ||
                 resultSet.getString("TABLE_TYPE") == null) )
          {
            out.write(" (<a href=\"javascript:" +
              "document.sqlQueryForm.query.value='SELECT * FROM " + resultSet.getString(i) + "';" +
              "getBehavior('executeSql')\">Select All</a>)");
          }
          out.write("</PRE></td>\n");
        }
        row++;
      }
      row = row - start;
    } catch ( SQLException e ) {
      e.printStackTrace();
    }
    out.write("    </tr>\n");
    out.write("  </table>\n");
    out.flush();
    out.write("<font size=\"+1\"><b>" + row + "</b> rows");
    int totalRows = -1;
    // make sure we're supposed to try this
    if ( getBooleanPreference("calculateRemainingRows") == true ) {
      try {
        // make sure we can jump to last record
        if ( resultSet.getClass().getMethod("getType", new Class[0]) != null &&
             resultSet.getClass().getMethod("getType", new Class[0]).isAccessible() == true &&
             resultSet.getType() != ResultSet.TYPE_FORWARD_ONLY )
        {
          resultSet.last();
          totalRows = resultSet.getRow();
          if ( totalRows == 1 && hasMoreResults == false ) {
            totalRows = 0;
          }
        }
      } catch (Throwable e) {
        e.printStackTrace();
        /* ==== I'd like to auto-set cookie, but for now I don't have response object ====
        // disable calculateRemainingRows so it won't show stack traces every time
        Cookie newCookie = new Cookie( "calculateRemainingRows", "false" );
        newCookie.setMaxAge(1000000000);
        response.addCookie(newCookie);
        System.out.println("WARNING: [Sql.jsp] Setting \"calculateRemainingRows\" preference to false because I erred in my attempt.  Click [Edit Preferences] button if you wish to turn \"calculateRemainingRows\" back on.");
        */
      }
    }
    if ( totalRows > row ) {
      out.write(" displayed of " + totalRows + " total");
    } else {
      out.write(" total");
    }
    out.write("</font> ");
    if ( start > 1 ) {
      int newStart = start - max;
      if ( newStart < 1 ) newStart = 1;
      out.write("[<a href=\"javascript:document.sqlQueryForm.start.value=" +
        newStart + ";document.sqlQueryForm.submit()\">Show previous " + max +
        "</a>]\n");
    }
    if ( hasMoreResults == true ) {
      out.write("[<a href=\"javascript:document.sqlQueryForm.start.value=" +
        (start + max) + ";document.sqlQueryForm.submit()\">Show next " + max +
        "</a>]\n");
    }
    out.flush();
  }

  public static String htmlEscape(String s) {
    // give them back what they gave me if they gave me a null instead of a string
    // avoids null pointer error below
    if ( s == null ) return s;

    StringBuffer sb = new StringBuffer();

    // loop through each character of the string and look at it
    for ( int i=0; i < s.length(); i++ ) {
      char c = s.charAt(i);

      // replace html-special characters with html-escaped characters
      switch(c) {
        case '&': sb.append("&amp;"); break;
        case '<': sb.append("&lt;"); break;
        case '>': sb.append("&gt;"); break;
        // double-quote
        case '\"': sb.append("&quot;"); break;
        // single-quote
        case '\'': sb.append("&#39;"); break;
        default: sb.append(c);
      }
    }
    return sb.toString();
  }

  public static String replace(String s, String find, String replace) {
    if ( s == null ) return null;
    StringBuffer sb = new StringBuffer();
    int index = -1;
    int lastIndex = 0;
    index = s.indexOf(find);
    while (index != -1) {
      sb.append( s.substring(lastIndex,index) );
      sb.append( replace );
      lastIndex = index + find.length();
      index = s.indexOf(find,lastIndex);
    }
    sb.append( s.substring(lastIndex) );
    return sb.toString();
  }

  public String getPreference(String preferenceKey) {
    return (String) preferences.get(preferenceKey);
  }

  public boolean getBooleanPreference(String preferenceKey) {
    return "true".equalsIgnoreCase( (String) preferences.get(preferenceKey) );
  }

  public int getIntPreference(String preferenceKey) {
    return Integer.parseInt( (String) preferences.get(preferenceKey) );
  }

  HashMap primaryKeysByTable = new HashMap();
  public HashSet getPrimaryKeys(String catalog, String schema, String tableName, DatabaseMetaData dbMeta) 
    throws SQLException
  {
    HashSet primaryKeys = (HashSet) primaryKeysByTable.get(tableName);
    if ( primaryKeys == null ) {
      primaryKeys = new HashSet();
      primaryKeysByTable.put(tableName, primaryKeys);
      ResultSet results = dbMeta.getPrimaryKeys(catalog, schema, tableName);
      while ( results.next() ) {
        String columnName = results.getString("COLUMN_NAME");
        primaryKeys.add(columnName);
      }
    }
    return primaryKeys;
  }

  public HashSet getPrimaryKeys(ResultSet results, Connection connection)
    throws SQLException
  {
    if ( results == null ) return new HashSet();
    String catalog = connection.getCatalog();
    DatabaseMetaData dbMeta = connection.getMetaData();
    boolean hasOnlyOneTable = true;
    ResultSetMetaData resultsMeta = results.getMetaData();
    int colCount = resultsMeta.getColumnCount();
    HashSet tableSet = new HashSet();
    String tableName = null;
    for ( int i=1; i <= colCount; i++ ) {
      tableName = resultsMeta.getTableName(i);
      tableSet.add( tableName );
    }
    if ( tableSet.size() > 1 ) {
      hasOnlyOneTable = false;
      System.out.println("WARNING: getPrimaryKeys called on a ResultSet that has more than one table involved! (tables: " + tableSet + ")");
    }
    return getPrimaryKeys(catalog, null, tableName, dbMeta);
  }

  public boolean isPrimaryKey(String catalog, String schema, String tableName, String columnName, DatabaseMetaData dbMeta) 
    throws SQLException
  {
    return getPrimaryKeys(catalog, schema, tableName, dbMeta).contains(columnName);
  }

  public HashSet getColumnSet(ResultSetMetaData resultsMeta)
    throws SQLException
  {
    HashSet columnSet = new HashSet();
    int colCount = resultsMeta.getColumnCount();
    for ( int i=1; i <= colCount; i++ ) {
      String columnName = resultsMeta.getColumnName(i);
      columnSet.add( columnName );
    }
    return columnSet;
  }

  public String getColumnHiddenFields(ResultSet results, Connection connection)
    throws SQLException
  {
    ResultSetMetaData resultsMeta = results.getMetaData();
    HashSet columnSet = getColumnSet(resultsMeta);
    HashSet primaryKeys = getPrimaryKeys(results, connection);
    Iterator iterator = primaryKeys.iterator();
    StringBuffer values = new StringBuffer();
    boolean haveAllPrimaryKeys;
    // start out true if we have some primary keys to check against
    if ( primaryKeys.size() > 0 ) {
      haveAllPrimaryKeys = true;
    } else {
      haveAllPrimaryKeys = false;
    }
    while ( iterator.hasNext() ) {
      String columnName = (String) iterator.next();
      values.append("      <input type=\"hidden\" name=\"whereColumns\" value=\"" + htmlEscape(columnName) + "\">\n");
      if ( results.getString(columnName) != null ) {
        values.append("      <input type=\"hidden\" name=\"whereValue." + htmlEscape(columnName) + "\" value=\"" + htmlEscape(results.getString(columnName)) + "\">\n");
      }
      // if we're missing one of our primary keys
      // we're going to have to use all our columns
      if ( ! columnSet.contains(iterator.next()) ) {
        haveAllPrimaryKeys = false;
        break;
      }
    }
    if ( haveAllPrimaryKeys == false ) {
      values = new StringBuffer();
      iterator = columnSet.iterator();
      while ( iterator.hasNext() ) {
        String columnName = (String) iterator.next();
        values.append("      <input type=\"hidden\" name=\"whereColumns\" value=\"" + htmlEscape(columnName) + "\">\n");
        if ( results.getString(columnName) != null ) {
          values.append("      <input type=\"hidden\" name=\"whereValue." + htmlEscape(columnName) + "\" value=\"" + htmlEscape(results.getString(columnName)) + "\">\n");
        }
      }
    }
    return values.toString();
  }

  public static String getDisplayType(int sqlType) {
    switch ( sqlType ) {
      case Types.ARRAY        : return "ARRAY";
      case Types.BIGINT       : return "BIGINT";
      case Types.BINARY       : return "BINARY";
      case Types.BIT          : return "BIT";
      case Types.BLOB         : return "BLOB";
      case Types.CHAR         : return "CHAR";
      case Types.CLOB         : return "CLOB";
      case Types.DATE         : return "DATE";
      case Types.DECIMAL      : return "DECIMAL";
      case Types.DISTINCT     : return "DISTINCT";
      case Types.DOUBLE       : return "DOUBLE";
      case Types.FLOAT        : return "FLOAT";
      case Types.INTEGER      : return "INTEGER";
      case Types.JAVA_OBJECT  : return "JAVA_OBJECT";
      case Types.LONGVARBINARY: return "LONGVARBINARY";
      case Types.LONGVARCHAR  : return "LONGVARCHAR";
      case Types.NULL         : return "NULL";
      case Types.NUMERIC      : return "NUMERIC";
      case Types.REAL         : return "REAL";
      case Types.REF          : return "REF";
      case Types.SMALLINT     : return "SMALLINT";
      case Types.STRUCT       : return "STRUCT";
      case Types.TIME         : return "TIME";
      case Types.TIMESTAMP    : return "TIMESTAMP";
      case Types.TINYINT      : return "TINYINT";
      case Types.VARBINARY    : return "VARBINARY";
      case Types.VARCHAR      : return "VARCHAR";
      default                 : return null;
    }
  }

  public static String getCoreUrl(HttpServletRequest request) {
    StringBuffer requestUri = new StringBuffer(request.getRequestURI());
    requestUri.append("?");
    String[] coreParams = {
      "dbdriver",
      "dburl",
      "dbuser",
      "dbpass",
      "start",
      "max",
      "query"
    };
    for ( int i=0; i < coreParams.length; i++ ) {
      String value = request.getParameter(coreParams[i]);
      if ( value != null ) {
        requestUri.append(coreParams[i] + "=" + URLEncoder.encode(value) + "&");
      }
    }
    return requestUri.toString();
  }
%>
