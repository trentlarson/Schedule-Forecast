package com.trentlarson.forecast.core.helper;

//import com.atlassian.jira.ManagerFactory;
//import com.atlassian.jira.config.properties.ApplicationProperties;
//import com.atlassian.jira.issue.issuetype.IssueType;
//import com.atlassian.jira.issue.priority.Priority;
//import com.atlassian.jira.issue.resolution.Resolution;
//import com.atlassian.jira.issue.status.Status;

//import com.atlassian.mail.Email;

//import com.trentlarson.forecast.core.dao.DAOFactory;
//import com.icentris.jira.exception.IcentrisJiraException;
//import com.icentris.jira.interfaces.UtilJiraInterface;
//import com.icentris.jira.util.IcentrisCacheUtils;


//import org.ofbiz.core.entity.GenericEntityException;
//import org.ofbiz.core.entity.GenericValue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.StringTokenizer;


public class ForecastUtil extends ForecastConstants {

    private static final DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);

    /*
    public static List<String> getPropertyValues(String propertyName) {
        return parseCommaSeparatedList(ManagerFactory.getApplicationProperties().getString(propertyName));
    }
    */

    public static List<String> parseCommaSeparatedList(String commaSeparatedList) {
        List<String> list = new ArrayList<String>();
        if (commaSeparatedList != null) {
          StringTokenizer st = new StringTokenizer(commaSeparatedList, ",");
          while ( st.hasMoreTokens() ) {
            list.add(st.nextToken().trim());
          }
        }
        return list;
    }

    public static Timestamp parseTimestamp(String dateStr) {
        try {
            return new Timestamp(dateFormat.parse(dateStr).getTime());
        } catch (ParseException e) {
            return null;
        }
    }

    public static String formatDate(Date date) {
        return dateFormat.format(date);
    }

    public static boolean isDate(String dateStr) {
        if (dateStr == null) {
            return false;
        }
        try {
            dateFormat.parse(dateStr);
            return true;
        } catch (ParseException e) {
            return false;
        }
    }
    
    /*
    public static String validateWorkLog(ApplicationProperties appProps, String timestamp, String issueKey) {
        int retVal = 0;
        Calendar current = Calendar.getInstance();
        Calendar passed = Calendar.getInstance();
        
        try {
            passed.setTime(dateFormat.parse(timestamp));
            
            String logPeriod = appProps.getString(PROP_STRING_ALLOWED_LOG_PERIOD);
            if( appProps.getOption(PROP_OPTION_RESTRICT_TIME_LOGGING) && logPeriod != null ) {
                if ( passed.getTimeInMillis() > current.getTimeInMillis() ) {
                    if ( appProps.getString(PROP_STRING_LOG_FUTURE_ISSUES).indexOf(issueKey) < 0 ) {
                        // can't log time for the future
                        retVal = 2;
                    }
                } else {
                	boolean isToday = false;
                	boolean isYesterday = false;
                    boolean isCurrentWeek = false;
                    boolean isPreviousWeek = false;
                    boolean isCurrentPayPeriod = false;
                    boolean isPreviousPayPeriod = false;
                    int currentYear = current.get(Calendar.YEAR);
                    int passedYear = passed.get(Calendar.YEAR);
                    int currentMonth = current.get(Calendar.MONTH) + 1;
                    int passedMonth = passed.get(Calendar.MONTH) + 1;
                    int currentWeek = current.get(Calendar.WEEK_OF_YEAR);
                    int passedWeek = passed.get(Calendar.WEEK_OF_YEAR);
                    int currentDayOfMonth = current.get(Calendar.DAY_OF_MONTH);
                    int passedDayOfMonth = passed.get(Calendar.DAY_OF_MONTH);
                    int passedMaxWeek = passed.getActualMaximum(Calendar.WEEK_OF_YEAR);
                    boolean isAM = current.get(Calendar.AM_PM) == Calendar.AM;
                    boolean isSaturday = current.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY;
                    boolean isSunday = current.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY;
                    boolean isMonday = current.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY;
                    
                    boolean isWeekBuffer = isSunday || (isMonday && isAM);
                    boolean isPayPeriodBuffer = 
                        (((currentDayOfMonth == 1 || currentDayOfMonth == 16) && (isSaturday || isSunday || isAM))
                         || ((currentDayOfMonth == 2 || currentDayOfMonth == 17) && (isSunday || (isMonday && isAM)))
                         || ((currentDayOfMonth == 3 || currentDayOfMonth == 18) && (isMonday && isAM)));
                    
                    if ( passedYear == currentYear ) {
                        if ( passedWeek == currentWeek ) {
                            isCurrentWeek = true;
                        } else if ( passedWeek == currentWeek - 1 ) {
                            isPreviousWeek = true;
                        }
                        if ( passedMonth == currentMonth ) {
                        	if ( passedDayOfMonth == currentDayOfMonth ) {
                        		isToday = true;
                        	} else if ( passedDayOfMonth == currentDayOfMonth - 1 ) {
                        		isYesterday = true;
                        	}
                            if ( currentDayOfMonth > 15 ) {
                                if ( passedDayOfMonth > 15 ) {
                                    isCurrentPayPeriod = true;
                                } else {
                                    isPreviousPayPeriod = true;
                                }
                            } else {
                                isCurrentPayPeriod = true;
                            }
                        } else if ( passedMonth == currentMonth - 1 
                                    && passedDayOfMonth > 15 && currentDayOfMonth < 16 ) {
                            isPreviousPayPeriod = true;
                        }
                    } else if ( passedYear == currentYear - 1 ) {
                        if ( currentWeek == 1
                             && passedWeek == passedMaxWeek ) {
                            isPreviousWeek = true;
                        }
                        if ( currentMonth == 1 && currentDayOfMonth < 16
                             && passedMonth == 12 && passedDayOfMonth > 15 ) {
                            isPreviousPayPeriod = true;
                        }
                    }
                    
                    if ( logPeriod.equals(CURRENT_DAY) ) {
                    	if ( !isToday && !(isYesterday && isAM) ) {
                    		retVal = 7;
                    	}
                    } else if ( logPeriod.equals(CURRENT_WEEK) ) {
                        if ( isCurrentWeek && isPreviousPayPeriod && !isPayPeriodBuffer ) {
                            retVal = 5;
                        } else if ( isPreviousWeek && isWeekBuffer && isPreviousPayPeriod && !isPayPeriodBuffer ) {
                            retVal = 5;
                        } else if ( isPreviousWeek && !isWeekBuffer ) {
                            retVal = 3;
                        }
                    } else if ( logPeriod.equals(PREVIOUS_WEEK) && !isCurrentWeek && !isPreviousWeek ) {
                        retVal = 4;
                    } else if ( logPeriod.equals(CURRENT_PAYPERIOD) && !isCurrentPayPeriod ) {
                        retVal = 5;
                    } else if ( logPeriod.equals(PREVIOUS_PAYPERIOD) && !isCurrentPayPeriod && !isPreviousPayPeriod ) {
                        retVal = 6;
                    }
                }
            }
        } catch (ParseException e) {
            retVal = 1;
        }

        switch(retVal) {
        case 0:
            return null;
        case 1:
            return "Invalid Date Format";
        case 2:
            return "You can only log time to a future day for one of the time-off issues listed at the bottom of the timecard report.";
        case 3: 
            return "After noon on Monday you can only log time for the current week.";
        case 4:
            return "You can only log time for the current or previous week.";
        case 5:
            return "You can only log time for the current pay period.";
        case 6:
            return "You can only log time for the current or previous pay period.";
        case 7:
        	return "You can only log time for today";
        default:
            return null;
        }
    }
    */
    
    /*
    public static Map<Long,String> getProjectMap() {
        Map<Long,String> map = new HashMap<Long,String>();
        Collection<GenericValue> projects = ManagerFactory.getProjectManager().getProjects();
        for ( GenericValue project : projects ) {
            map.put(project.getLong("id"), project.getString("name"));
        }
        return map;
    }

    public static Map<String,String> getIssueTypeMap() {
        Map<String,String> map = new HashMap<String,String>();
        Collection<IssueType> issueTypes = ManagerFactory.getConstantsManager().getAllIssueTypeObjects();
        for ( IssueType issueType : issueTypes ) {
            map.put(issueType.getId(), issueType.getName());
        }
        return map;
    }

    public static Map<String,String> getPriorityMap() {
        Map<String,String> map = new HashMap<String,String>();
        Collection<Priority> priorities = ManagerFactory.getConstantsManager().getPriorityObjects();
        for ( Priority priority : priorities ) {
            map.put(priority.getId(), priority.getName());
        }
        return map;
    }

    public static Map<String,String> getResolutionMap() {
        Map<String,String> map = new HashMap<String,String>();
        Collection<Resolution> resolutions = ManagerFactory.getConstantsManager().getResolutionObjects();
        for ( Resolution resolution : resolutions ) {
            map.put(resolution.getId(), resolution.getName());
        }
        return map;
    }

    public static Map<String,String> getStatusMap() {
        Map<String,String> map = new HashMap<String,String>();
        Collection<Status> statuses = ManagerFactory.getConstantsManager().getStatusObjects();
        for ( Status status : statuses ) {
            map.put(status.getId(), status.getName());
        }
        return map;
    }
    */

    /**
     * This method takes the value of property from file
     * icentris.properties(which is located in "WEB-INF/classes/resources"
     * directory).
     * This method converts the value of text (found in icentris.properties) into it's corresponding 
     * id and returns you the id.
     * @param propertyName
     * @return String property value
     */
    /*
      public static String getPropertyValue(String propertyName) {
      if(propertyIds.containsKey(propertyName))
      {
      return (String) propertyIds.get(propertyName);
      }
      else
      {
      return resources.get(propertyName);
      }
      }
      
      public static boolean getPropertyValueAsBoolean(String propertyName) 
      {
      return resources.getBoolean(propertyName);
      }
    */
    /**
     * This method takes the value of property from file
     * icentris.properties(which is located in "WEB-INF/classes/resources"
     * directory).This method does not convert the values in the property file 
     * into its corresponding 'id' but returns the text written in icentris.properties as it is.
     * Use it only if you want the value of key in icentris.props as it is.
     * @param propertyName
     * @return String property value
     */
    /*
      public static String getPropertyAsText(String propertyName) 
      {
      return resources.get(propertyName);
      
      }
    */
    /**
     * This method takes the value of property from file
     * icentris.properties(which is located in "WEB-INF/classes/resources"
     * directory).This method does not convert the values in the property file 
     * into its corresponding 'id' but returns the text written in icentris.properties as it is.
     * Use it only if you want the value of key in icentris.props as it is.
     * This should be called in those cases where we are expecting more than one values in property file 
     * for any key.Values are expected to be seperated with ",".
     * @param propertyName
     * @return List
     */
    /*
      public static List getMultipleValueAsText(String propertyName) 
      {
      List list = new ArrayList(10);
      String property =  resources.get(propertyName);
      StringTokenizer token = new StringTokenizer(property, PROPERTY_TOKEN);
      while (token.hasMoreTokens())
      {
      list.add(token.nextToken().trim()); 
      }
      return list;
      
      }
    */
    /**
     * This will return name of id passed according to the type(these are constants avialable in this class).
     * Ex: For finding name of user pass ForecastUtility.USER, 10000
     * @param type
     * @param id
     * @return
     */
    /*
      public static String convertIdToName(String type, String id)
      throws IcentrisJiraException 
      {
      if (id == null) 
      {
      throw new IllegalArgumentException("Id cannot be null for type " +
      type);
      }
      
      if (type.equals(ForecastUtil.USER)) 
      {
      try 
      {
      return DAOFactory.getUserDAO().getUserName(id);
      } 
      catch (java.sql.SQLException e) 
      {
      throw new IcentrisJiraException("", e);
      }
      } 
      else if (type.equals(ForecastUtil.COMPONENT)) 
      {
      return UtilJiraInterface.getComponent(new Long(id)).getString(NAME);
      } 
      else 
      {
      throw new IcentrisJiraException("Cannot convert invalid type of '" + type + "'.");
      }
      }
    */
    /**
     * Added by Ravi Reddy on 14/Dec/2004
     * @throws IcentrisJiraException
     * @throws SQLException
     * This method retrieves ids for properties from the database
     */
    /*
      public static void loadPropertyIds() 
      {
      Map customFieldMap = new HashMap();
      Map componentMap = new HashMap();
      Map userMap = new HashMap();
      Map groupMap = new HashMap();
      Map linkMap = new HashMap();
      Map defaultProject = new HashMap();
      Map defaultIssueType = new HashMap();
      Map defaultPriority = new HashMap();
      Map defaultComponent = new HashMap();
      Map hiddenCustomFieldsMap = new HashMap();
      
      try
      {
      String[] projectPropertyValues = {resources.get(DEFAULT_PROJECT_ID),
      resources.get(MAIN_WORKFLOW_PROJECT_ID_1),
      resources.get(MAIN_WORKFLOW_PROJECT_ID_2),
      resources.get(SYS_PROJECT_ID),
      resources.get(PM_PROJECT_ID),
      resources.get(QA_PROJECT_ID)
      };
      String[] projectProperty = {DEFAULT_PROJECT_ID,
      MAIN_WORKFLOW_PROJECT_ID_1,
      MAIN_WORKFLOW_PROJECT_ID_2,
      SYS_PROJECT_ID,
      PM_PROJECT_ID,
      QA_PROJECT_ID
      };
      defaultProject.put("Project", projectPropertyValues);
      propertyIds.putAll(IcentrisCacheUtils.getPropertyIds(defaultProject, "Project", projectProperty, false));
      
      String customFieldPropertyValues[] = {resources.get(CUSTOM_FIELD_NOTEWORTHYID),
      resources.get(PREVENTION_STATUS_ID),
      resources.get(EXCLUDE_CUSTOMID_VALUE1),
      resources.get(EXCLUDE_CUSTOMID_VALUE2),
      resources.get(NEWARCHITECTURE_CUSTOMFIELD_ID),
      resources.get(CUSTOM_FIELD_NOTEWORTHYID),
      resources.get(PROP_COMPANY_ID),
      resources.get(PROP_TAG_VERSION_ID)
      };
      String customFieldProperties[]     = {CUSTOM_FIELD_NOTEWORTHYID,
      PREVENTION_STATUS_ID,
      EXCLUDE_CUSTOMID_VALUE1,
      EXCLUDE_CUSTOMID_VALUE2,
      NEWARCHITECTURE_CUSTOMFIELD_ID,
      CUSTOM_FIELD_NOTEWORTHYID,
      PROP_COMPANY_ID,
      PROP_TAG_VERSION_ID
      };
      customFieldMap.put("CustomField", customFieldPropertyValues);
      propertyIds.putAll(IcentrisCacheUtils.getPropertyIds(customFieldMap, "CustomField", customFieldProperties, false));
      
      String[] componentPropertyValues  =  {resources.get(LINK_COMPONENT_STAGE_ID),
      resources.get(LINK_COMPONENT_TEST_ID),
      resources.get(LINK_COMPONENT_PROD_ID),
      resources.get(LINK_COMPONENT_PM_ID)
      };
      String[] componentProperties      =  {LINK_COMPONENT_STAGE_ID,
      LINK_COMPONENT_TEST_ID,
      LINK_COMPONENT_PROD_ID,
      LINK_COMPONENT_PM_ID
      };
      componentMap.put("Component", componentPropertyValues); 
      propertyIds.putAll(IcentrisCacheUtils.getPropertyIds(componentMap, "Component", componentProperties, true));
      
      String[] userPropertyValues   = {resources.get(LINK_USER_PROD_ID),
      resources.get(LINK_USER_TEST_ID),
      resources.get(LINK_USER_STAGE_ID),
      };
      String[] userProperties       = {LINK_USER_PROD_ID,
      LINK_USER_TEST_ID,
      LINK_USER_STAGE_ID,
      };
      userMap.put("OSUser", userPropertyValues);
      propertyIds.putAll(IcentrisCacheUtils.getPropertyIds(userMap, "OSUser", userProperties, false));
      
      String[] groupPropertyValues   =  {resources.get(LINK_USER_PM_GROUP_ID),
      resources.get(PROJECT_MANAGER_GROUPNAME),
      resources.get(LINK_USER_SYSADMIN_GROUP_ID),
      resources.get(LINK_USER_TESTER_GROUP_ID)
      };
      String[] groupProperties       =  {LINK_USER_PM_GROUP_ID,
      PROJECT_MANAGER_GROUPNAME,
      LINK_USER_SYSADMIN_GROUP_ID,
      LINK_USER_TESTER_GROUP_ID
      };
      groupMap.put("OSGroup", groupPropertyValues);
      propertyIds.putAll(IcentrisCacheUtils.getPropertyIds(groupMap, "OSGroup", groupProperties, false));
      
      String[] linkPropertyValues   =  {resources.get(LINK_TYPE_ID),
      resources.get(SUBTASK_LINK_ID),
      resources.get(RESOLVE_LINK_ID1),
      resources.get(RESOLVE_LINK_ID2),
      resources.get(RELEVANT_LINK_NAME)
      };
      String[] linkProperties     =  {LINK_TYPE_ID,
      SUBTASK_LINK_ID,
      RESOLVE_LINK_ID1,
      RESOLVE_LINK_ID2,
      RELEVANT_LINK_NAME
      };
      linkMap.put("IssueLinkType", linkPropertyValues);
      propertyIds.putAll(IcentrisCacheUtils.getPropertyIds(linkMap, "IssueLinkType", linkProperties, false));
      
      String[] issueTypePropertyValues  =  {resources.get(DEFAULT_ISSUE_TYPE_ID)};
      String[] issueTypeProperty    =  {DEFAULT_ISSUE_TYPE_ID};
      defaultIssueType.put("IssueType", issueTypePropertyValues);
      propertyIds.putAll(IcentrisCacheUtils.getPropertyIds(defaultIssueType, "IssueType", issueTypeProperty, false));
      
      String[] priorityPropertyValues   =  {resources.get(DEFAULT_PRIORITY_ID)};
      String[] priorityProperty  =  {DEFAULT_PRIORITY_ID};
      defaultPriority.put("Priority", priorityPropertyValues);
      propertyIds.putAll(IcentrisCacheUtils.getPropertyIds(defaultPriority, "Priority", priorityProperty, false));
      
      String[] defaultComponentValue    =  {resources.get(DEFAULT_COMPONENT_ID)};
      String[] defaultComponentProperty =  {DEFAULT_COMPONENT_ID};
      defaultComponent.put("Component", defaultComponentValue);
      propertyIds.putAll(IcentrisCacheUtils.getPropertyIds(defaultComponent, "Component", defaultComponentProperty, true));
      
      Object[] hiddenCustomFields   =  getMultipleValueAsText(HIDDEN_CUSTOMFIELD_IDS).toArray();  
      hiddenCustomFieldsMap.put("CustomField", hiddenCustomFields);
      
      int size = hiddenCustomFields.length;
      Object[] hiddenCustomFieldProperties = new Object[size];
      
      for(int count = 0; count < size; count++)
      {
      hiddenCustomFieldProperties[count] = HIDDEN_CUSTOMFIELD_IDS;
      }
      
      StringBuffer commaSeparated = new StringBuffer();
      for(Iterator iterator = IcentrisCacheUtils.getPropertyIds(hiddenCustomFieldsMap, "CustomField", hiddenCustomFieldProperties, false).values().iterator(); iterator.hasNext();)
      {
      commaSeparated.append(iterator.next());
      commaSeparated.append(",");
      }
      if(commaSeparated.lastIndexOf(",") > 0)
      {
      commaSeparated.deleteCharAt(commaSeparated.lastIndexOf(","));
      }
      propertyIds.put(HIDDEN_CUSTOMFIELD_IDS, commaSeparated.toString());
      
      }
      catch (GenericEntityException e)
      {
      log.error("Could not generate ids for properties: " + e.getMessage());
      }
      catch (Exception e){
      log.error("Error in loading PropertyIds", e);
      }
      catch (Throwable t)
      {
      log.error("Could not load property: " + t.getMessage());
      }
      }
    */    
    /**
     * Added by Ravi Reddy on 12/Jan/2005
     * @param date
     * @return
     * @throws IcentrisJiraException
     * This method formats the date in d/MM/yy or d/MMM/yy format into d-MMM-yyyy format
     */
    /*
      public static String formatDate(String date) throws IcentrisJiraException
      {
      String newDate = "";
      try
      {
      SimpleDateFormat format = new SimpleDateFormat("d/MMM/yy");
      SimpleDateFormat format2 = new SimpleDateFormat("d/MM/yy");
      Date formattedDate = new Date();
      if(ForecastUtil.isDate(date, format))
      {
      return dateFormatter(date, format);
      }
      else if(ForecastUtil.isDate(date, format2))
      {
      return dateFormatter(date, format2);
      }
      else 
      {
      return date;
      }
      }
      catch(ParseException parse)
      {
      log.error("Could not parse the date" + parse.getMessage());
      }
      return null;
      }
      
      private static String dateFormatter(String date, DateFormat format) throws ParseException
      {
      
      Date formattedDate = new Date();
      formattedDate = format.parse(date);
      Calendar cal  = Calendar.getInstance();
      cal.set(Calendar.YEAR, formattedDate.getYear() + 1900); // based on Date java docs 1900 has to be added to the year
      cal.set(Calendar.MONTH, formattedDate.getMonth());
      cal.set(Calendar.DATE, formattedDate.getDate());
      format = new SimpleDateFormat("d-MMM-yyyy");
      
      return format.format(cal.getTime());
      }
    */

  /*
  public static void notifyAdmin(String body) {
    List<String> adminUsers = getPropertyValues("admin.user.for.notifications");
    for (String adminUser : adminUsers) {
      try {
        String adminEmail =
          com.opensymphony.user.UserManager.getInstance().getUser(adminUser).getEmail();
        Email email = new Email(adminEmail, null, null);
        email.setSubject("Jira Admin Notification");
        email.setMimeType("text/html");
        email.setBody(body);
        try {
          new com.atlassian.mail.queue.SingleMailQueueItem(email).send();
        } catch (com.atlassian.mail.MailException e) {
          System.err.println("Got error attempting to send following content to email '" + adminEmail + "':");
          System.err.println(body);
          System.err.println("Here's the error I got:");
          e.printStackTrace();
        }
      } catch (com.opensymphony.user.EntityNotFoundException e) {
        System.err.println("Got error attempting to send following content to user '" + adminUser + "':");
        System.err.println(body);
        System.err.println("Here's the error I got:");
        e.printStackTrace();
      }
    }
  }
  */


  /**
     This is not recommended.  Jira is meant to be DB agnostic.
     Try to use hooks into the entity engine for your features.

     @return DB connection
   */
  public static Connection getConnection() throws SQLException {
	  
    /** JNDI
    */
    javax.sql.DataSource dsrc = null;
    try {
      dsrc = (javax.sql.DataSource) new javax.naming.InitialContext().lookup("java:comp/env/jdbc/JiraDS");
    } catch (javax.naming.NamingException e) {
      throw new java.lang.reflect.UndeclaredThrowableException(e);
    }
    return dsrc.getConnection();
	
	
    /** HSQL
    try {
      Class.forName("org.hsqldb.jdbcDriver");
    } catch (ClassNotFoundException e) {
      throw new java.lang.reflect.UndeclaredThrowableException(e);
    }
    return DriverManager.getConnection("jdbc:hsqldb:file:/tmp/schedule-db", "sa", "");
    */
	
	
    /** MySQL
    try {
      Class.forName("com.mysql.jdbc.Driver");
    } catch (ClassNotFoundException e) {
      throw new java.lang.reflect.UndeclaredThrowableException(e);
    }
    //return DriverManager.getConnection("jdbc:mysql://10.0.2.16:8319/jiradb?autoReconnect=true&amp;useUnicode=true&amp;characterEncoding=UTF8", "jira", "jirapass");
    //return DriverManager.getConnection("jdbc:mysql://localhost:3306/test_forecast_jira?autoReconnect=true&amp;useUnicode=true&amp;characterEncoding=UTF8", "jira", "jirapass");
    return DriverManager.getConnection("jdbc:mysql://localhost:3306/test_forecast_jira?autoReconnect=true&amp;useUnicode=true&amp;characterEncoding=UTF8", "jira", "jirapass");
    */
    
    
    /** Oracle
    javax.sql.DataSource dsrc = null;
    try {
      Class.forName("oracle.jdbc.driver.OracleDriver");
    } catch (ClassNotFoundException e) {
      throw new java.lang.reflect.UndeclaredThrowableException(e);
    } catch (javax.naming.NamingException e) {
      throw new java.lang.reflect.UndeclaredThrowableException(e);
    }
    return DriverManager.getConnection("...", "...", "...");
    */


  }

}
