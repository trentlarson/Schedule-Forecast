package com.icentris.jira.helper;

//import com.atlassian.jira.ManagerFactory;
//import com.atlassian.jira.config.properties.ApplicationProperties;
//import com.atlassian.jira.issue.IssueFieldConstants;
//import com.atlassian.jira.issue.index.DocumentConstants;

public class ForecastConstants {
    /* Properties in database (accessed via ApplicationProperties).
     * I use the following naming convention for the constants
     *  PROP_OPTION_SOME_PROPERTY for boolean values (accessed via getOption/setOption)
     * and
     *  PROP_STRING_SOME_PROPERTY for string values (accessed via getString/setString)
     * and the keys use
     *  icentris.jira.option.someProperty for boolean values
     * and
     *  icentris.jira.string.someProperty for string values
     */
    public static final String PROP_OPTION_RESTRICT_TIME_LOGGING = "icentris.jira.option.restrictTimeLogging";
    public static final String PROP_STRING_ALLOWED_LOG_PERIOD = "icentris.jira.string.allowedLogPeriod";
    public static final String PROP_STRING_LOG_FUTURE_ISSUES = "icentris.jira.string.logFutureIssues";
    public static final String PROP_STRING_WORKFLOW_PROJECTS = "icentris.jira.string.workflowProjects";
    public static final String PROP_STRING_WORKFLOW_ISSUE_TYPES = "icentris.jira.string.workflowIssueTypes";
    public static final String PROP_STRING_SCHEDULE_ROLLOUT_EXCLUDE_CLIENTS = "icentris.jira.string.scheduleRolloutExcludeClients";
    public static final String PROP_STRING_DEFAULT_SYSADMIN = "icentris.jira.string.defaultSysadmin";
    public static final String PROP_STRING_DEFAULT_TESTER = "icentris.jira.string.defaultTester";

    /*
    static {
        ApplicationProperties props = ManagerFactory.getApplicationProperties();
        if ( !props.exists(PROP_OPTION_RESTRICT_TIME_LOGGING) ) {
            props.setOption(PROP_OPTION_RESTRICT_TIME_LOGGING, true);
        }
        if ( !props.exists(PROP_STRING_ALLOWED_LOG_PERIOD) ) {
            props.setString(PROP_STRING_ALLOWED_LOG_PERIOD, "current week");
        }
        if ( !props.exists(PROP_STRING_LOG_FUTURE_ISSUES) ) {
            props.setString(PROP_STRING_LOG_FUTURE_ISSUES, "ADM-139,ADM-140,ADM-141,ADM-143,ADM-656");
        }
        if ( !props.exists(PROP_STRING_WORKFLOW_PROJECTS) ) {
            props.setString(PROP_STRING_WORKFLOW_PROJECTS, "ENT,STE,VID,AIM,XAS,CTF");
        }
        if ( !props.exists(PROP_STRING_WORKFLOW_ISSUE_TYPES) ) {
            props.setString(PROP_STRING_WORKFLOW_ISSUE_TYPES, "Bug,Improvement");
        }
        if ( !props.exists(PROP_STRING_SCHEDULE_ROLLOUT_EXCLUDE_CLIENTS) ) {
            props.setString(PROP_STRING_SCHEDULE_ROLLOUT_EXCLUDE_CLIENTS, "All Clients,Icentris");
        }
        if ( !props.exists(PROP_STRING_DEFAULT_SYSADMIN) ) {
            props.setString(PROP_STRING_DEFAULT_SYSADMIN, "andreas");
        }
        if ( !props.exists(PROP_STRING_DEFAULT_TESTER) ) {
            props.setString(PROP_STRING_DEFAULT_TESTER, "kevin.homer");
        }
    }

    public static final String SUMMARY = IssueFieldConstants.SUMMARY;
    public static final String ASSIGNEE = IssueFieldConstants.ASSIGNEE;
    public static final String REPORTER = IssueFieldConstants.REPORTER;
    public static final String STATUS = IssueFieldConstants.STATUS;
    public static final String PRIORITY = IssueFieldConstants.PRIORITY;
    public static final String DUE_DATE = IssueFieldConstants.DUE_DATE;
    public static final String CREATED = IssueFieldConstants.CREATED;
    public static final String RESOLUTION = IssueFieldConstants.RESOLUTION;
    public static final String VERSIONS = IssueFieldConstants.AFFECTED_VERSIONS;
    public static final String FIX_VERSIONS = IssueFieldConstants.FIX_FOR_VERSIONS;
    public static final String TIME_ESTIMATE = IssueFieldConstants.TIME_ESTIMATE;
    public static final String TIME_SPENT = IssueFieldConstants.TIME_SPENT;
    public static final String TIME_ORIGINAL_ESTIMATE = IssueFieldConstants.TIME_ORIGINAL_ESTIMATE;
    public static final String KEY = "key";
    public static final String NAME = "name";
    public static final String PROJECT_ID = "projectId";
    public static final String ID = "id";
    public static final String FAIL = "fail";
    public static final String NO_VALUE = " ";
    public static final String TYPE = "type";
    public static final String COMPONENT = "component";
    public static final String LINK_NAME = "linkname";
    public static final String VALUE = "value";
    public static final String AUTHOR = DocumentConstants.ISSUE_AUTHOR;
    public static final String COMMENT_BODY = "commentbody";
    public static final String ALL = "all";
    public static final String PROJECT = "project";
    public static final String CUSTOM_FIELDS = "customFields";
    */
    
    /*Report Specific constants
     * All Report names should start with REP
     * in constants.
     */
    /*
    public static final String REP_SEARCH_REQUEST = "icentris.repsearchrequest";
    public static final String REP_WATCHER = "icentris.repwatcher";
    public static final String REP_ISSUE_TYPE = "icentris.repissuetype";
    public static final String REP_WAT_UNRES_ISSUE = "icentris.repwatunresissue";
    public static final String REP_OPEN_ISSUE = "icentris.repopenissue";
    public static final String REP_PROJECTS = "icentris.reprojects";
    public static final String REP_COLUMNS = "icentris.repcolumns";
    public static final String REP_USERS = "icentris.repusers";
    public static final String REP_COMPONENT = "icentris.repcomponent";
    public static final String REP_ALL_USERS = "icentris.repallusers";
    public static final String REP_COMMENTS = "icentris.repcomments";
    public static final String REP_CUSTOMFIELD_OPTION = "icentris.repcustomfieldoption";
    public static final String REP_USER_GROUP = "icentris.repusergroup";
    public static final String REP_CHANGE_HISTORY = "icentris.repchangehistory";
    
    public static final String REP_ISSUE_WATCHERS = "icentris.repissuewatchers";
    public static final String REP_HISTORY_COMMENTS = "icentris.rephistrycomments";
    
    public static final String REP_CUSTOM_QUERY = "icentris.repcustomquery";
    
    
    //Size of reports. Make new size value like REP_COMMENTS_SIZE
    public static final String REP_WAT_UNRES_ISSUE_SIZE = "icentris.repwatunresissuesize";
    public static final String REP_OPEN_ISSUE_SIZE = "icentris.repopenissuesize";
    public static final String REP_WATCHER_SIZE = "icentris.repwatchersize";
    public static final String REP_SEARCH_REQUEST_SIZE = "icentris.repsearchrequestsize";
    public static final String REP_COMMENTS_SIZE = "icentris.repcommentssize";
    public static final String REP_CHANGE_HISTORY_SIZE = "icentris.repchangehistorysize";
    public static final String REP_CUSTOM_QUERY_SIZE = "icentris.repcustomquerysize";
    public static final String REP_HISTORY_COMMENTS_SIZE = "icentris.rephistrycommentssize";
    public static final String REP_SEARCH_FILTER = "icentris.repsearchFilter";
    
    //Issue status 
    public static final String ISSUE_RESOLVED = "5";
    public static final String ISSUE_OPENED = "1";
    public static final String ISSUE_IN_PROGRESS = "3";
    public static final String ISSUE_REOPENED = "4";
    public static final String ISSUE_CLOSED = "6";
    public static final String WATCHER = "watcher";
    public static final String WATCHER_NAME = "watchername";
    public static final String WATCHER_YES = "yes";
    public static final String WATCHER_NO = "no";
    public static final String COLUMNS = "columns";
    public static final String ADD_SIZE = "size";
    
    public static final String ISSUE_TYPE_TASK = "3";
    
    //Used in URI String
    public static final String URI_PER_PAGE = "uriperpage";
    public static final String URI_REPORT_NAME = "urireportname";
    public static final String URI_PROJECT = "uriproject";
    public static final String URI_REPORT = "urireport";
    public static final String URI_YES = "uriyes";
    public static final String URI_SHOW = "urishow";
    public static final String URI_NO = "urino";
    public static final String URI_USERS = "uriusers";
    public static final String URI_ISSUES = "uriissues";
    public static final String URI_START = "startIndex";
    public static final String URI_END = "endIndex";
    public static final String URI_SORT = "sortBy";
    public static final String URI_CHANGE = "change";
    public static final String URI_PAGE = "uripage";
    public static final String URI_PROJECT_ID = "project_Id";
    public static final String URI_COMMON_INT = "common_Int";
    public static final String URI_SIZE_REQ = "sizeRequired";
    public static final String URI_CUSTOMFIELD_ID = "customField_Id";
    public static final String URI_REPORT_SIZE = "urireportsize";
    public static final String URI_COMMON_STRING = "common_String";
    public static final String URI_JIRA_FILTER = "fromJiraFilter";
    public static final String URI_FIRST = "urifirst";
    
    public static final String TOKEN = "-";
    public static int ISSUES_PER_PAGE ;
    public static final String TAG_START = "tagStart";
    public static final String TAG_END = "tagEnd";
    public static final String TAG_COUNT = "tagCount";
    public static final String TAG_SIZE = "tagSize";
    public static final String TAG_LOOP = "tagLoop";
    public static final String ACTION_WATCH = "watch";
    public static final String ACTION_UNWATCH = "unwatch";
    public static final String ACTION_CREATE_AND_LOG = "createandlog";
    public static final String ACTION_CREATE_AND_LINK = "createandlink";
    public static final String ACTION_MAKE_FILTER = "makefilter";
    public static final String SEARCH_FILTER = "searchFilter";
    
    public static final String PROPERTY_TOKEN= ",";
    */

    public static final String DATE_FORMAT = "d/MMM/yy";
    //public static final String PROP_COMPANY_ID = "custom.field.companyid";
    //public static final String PROP_TAG_VERSION_ID = "custom.field.tagversionid";
    public static final String LINK_COMPONENT_STAGE = "Roll to Stage";
    public static final String LINK_COMPONENT_TEST = "Testing";
    public static final String LINK_COMPONENT_PROD = "Roll to Prod";
    public static final String LINK_COMPONENT_PM = "Client Admin";
    //public static final String LINK_USER_PM_GROUP_ID = "link.user.pmgroupid";
    //public static final String LINK_USER_SYSADMIN_GROUP_ID = "link.user.sysadmingroupid";
    //public static final String LINK_USER_TESTER_GROUP_ID = "link.user.testergroupid";
    //public static final String LINK_USER_PROD_ID = "link.user.prodid";
    //public static final String LINK_USER_TEST_ID = "link.user.testid";
    //public static final String LINK_USER_STAGE_ID = "link.user.stageid";
    //public static final String EXCLUDE_CUSTOMID_VALUE1 = "exclude.customfield.value1";
    //public static final String EXCLUDE_CUSTOMID_VALUE2 = "exclude.customfield.value2";
    //public static final String RESOLVE_LINK_ID1 = "resolve.link.id1";
    //public static final String RESOLVE_LINK_ID2 = "resolve.link.id2";
    public static final String SEQUENCE_LINK_TYPE_ID = "10000";
    public static final String SUBTASK_LINK_ID = "10010";
    public static final String SYS_PROJECT_ID = "10071";
    public static final String PM_PROJECT_ID = "10073";
    public static final String QA_PROJECT_ID = "10070";

    public static final long TAG_VERSION_FIELD_ID = 10111L;
    public static final long SPECIFIC_CLIENT_FIELD_ID = 10002L;
    
    /*
    // Default values in icentris.properties.
    public static final String DEFAULT_PROJECT_ID = "default.project.id";
    public static final String DEFAULT_ISSUE_TYPE_ID = "default.issuetype.id";
    public static final String DEFAULT_COMPONENT_ID = "default.component.id";
    public static final String DEFAULT_PRIORITY_ID = "default.priority.id";
    public static final String ISSUE_PER_PAGE = "issue.per.page";
    
    public static final String MAIN_WORKFLOW_PROJECT_ID_1 = "main.workflow.project.id.1";
    public static final String MAIN_WORKFLOW_PROJECT_ID_2 = "main.workflow.project.id.2";
    
    // Local Help URLs
    public static final String LOCAL_ISSUE_HELPURL = "local.issues.helpURL";
    public static final String LOCAL_ISSUE_HELPTITLE = "local.issues.helpTitle";
    public static final String LOCAL_ISSUE_HELPKEY = "local.issues.helpKey";
    public static final String LOCAL_ISSUE_HELPALT = "local.issues.helpAlt";
    
    // Project Manager Group Name
    public static final String PROJECT_MANAGER_GROUPNAME="project.manager.groupName";
    
    // Values for changeLog.jsp to be taken from properties file
    
    public static final String PROJECT_CHANGELOG_ID = "project.changeLog.id";
    public static final String CUSTOM_FIELD_NOTEWORTHYID = "custom.field.noteworthyid";
    
    //Extra values
    public static final String CAUSE_MESSAGE = "causemessage";
    public static final String LINK_INWARD_TEXT = "inward";
    public static final String NULL = "null";
    
    // Prevention status id
    public static final String PREVENTION_STATUS_ID = "prevention.status.id";
    public static final String UNPREVENTABLE = "Unpreventable / Not worth preventing";
    public static final String WORK_ON_PREVENTION = "Will work on prevention later";
    public static final String PREVENTED = "Prevented";
    public static final String BUG = "1";
    public static final String TASK = "3";
    
    // Hidden CustomFields  Addedd by Ravi Reddy on 08/Dec/2004
    public static final String HIDDEN_CUSTOMFIELD_IDS = "hidden.customfield.ids";
    
    // New Architecture Customfield added by Ravi Reddy on 13/Dec/2004
    public static final String NEWARCHITECTURE_CUSTOMFIELD_ID = "newarchitecture.customfield.id";
    
    public static final String EXTRA_ISSUE_COMPANIES = "extra.issue.companies";
    */

    // time logging
    public static final String CURRENT_DAY = "current day";
    public static final String CURRENT_WEEK = "current week";
    public static final String PREVIOUS_WEEK = "current or previous week";
    public static final String CURRENT_PAYPERIOD = "current pay period";
    public static final String PREVIOUS_PAYPERIOD = "current or previous pay period";

    /*
    //Added by Ravi Reddy on 16/Dec/2004
    public static final String URI_ICENTRIS_FILTER = "fromIcentrisFilter";
    
    //added by Ravi Reddy on 22/Dec/2004
    public static final String REP_RESOLVE_ISSUE = "icentris.represolveissue";
    public static final String REP_RESOLVE_ISSUE_SIZE = "icentris.represolveissuesize";
    public static final String REP_ISSUE_STATUSES = "icentris.repissuestatuses";
    public static final String REP_ISSUE_STATUSES_SIZE = "icentris.repissuestatusessize";
    public static final String CHANGE_LOG = "changelog";
    
    // workflow actions added by Ravi Reddy on 24/Dec/2004
    public static final int WORKFLOW_CLOSED = 6;
    public static final int WORKFLOW_DEPLOYED_DEV = 7; 
    public static final int WORKFLOW_DEPLOYED_SATGE = 8;
    public static final int WORKFLOW_TESTED_STAGE = 9;
    public static final int WORKFLOW_ACCEPTED_STAGE = 10;
    public static final int WORKFLOW_DEPLOYED_PROD = 11;
    public static final int WORKFLOW_PROD_TESTED = 12;
    public static final int ISSUE_STATUS = 5;
    
    public static final String RESOLUTION_FIXED = "1";
    public static final String RESOLUTION_UNRESOLVED = "UNRESOLVED";
    public static final String IMPROVEMENT_ISSUE = "4";
    
    public static final String FORM_VALUE_APPROVERC = "approverc";
    public static final String FORM_VALUE_DEPLOYRC = "deployrc";
    /**
     *Non Resolved Close Action is used for the issues whose status 
     *is anything but Resolved.Issue status can be 'Open','InProgress'... 
     * 
     */
    //public static final int ACTION_WORKFLOW_CLOSE_NONRESOLVED = 002;
    /**
     * Resolved close action is for the issues whose status is 'Resolved'.
     */
    /*
    public static final int ACTION_WORKFLOW_CLOSE_RESOLVED = 701;
    
    public static final int ACTION_WORKFLOW_RESOLVED = 005;
    
    public static final String RELEVANT_LINK_NAME = "relevant.link.name";
    
    public static final String BULK_ISSUETYPE = "issueType";
    public static final String BULK_DUEDATE = "duedate";
    
    public static final String ORACLE_DATE_FORMAT = "dd-MMM-yy";
    
    public static final int DEVTESTED_ACTION_ID = 501;
    */
    public static final String TASK_SUMMARY_ALPHA_APPROVAL      = "Feature Freeze";
    public static final String TASK_SUMMARY_ALPHA_ROLLOUT       = "Go Alpha (w/ Full Stage Practice)";
    public static final String TASK_SUMMARY_ALPHA_TEST_MASTER   = "QA: Master ALPHA task";
    public static final String TASK_SUMMARY_BETA_APPROVAL       = "PM Approval to go Beta";
    public static final String TASK_SUMMARY_BETA_ROLLOUT        = "Go Beta (w/ Full Stage Practice)";
    public static final String TASK_SUMMARY_BETA_TEST_MASTER    = "QA: Master BETA task";
    public static final String TASK_SUMMARY_RC_APPROVAL         = "PM Approval to roll to RC";
    public static final String TASK_SUMMARY_RC_ROLLOUT          = "Rollout to RC";
    public static final String TASK_SUMMARY_RC_TEST_MASTER      = "QA: Master RC task";
    public static final String TASK_SUMMARY_PROD_APPROVAL       = "Final PM Approval";
    public static final String TASK_SUMMARY_PROD_ROLLOUT        = "Rollout to Production";
    public static final String TASK_SUMMARY_PROD_TEST_MASTER    = "QA: Master PROD task";
    public static final int TASK_SUMMARY_PREFIXES_PROD_ROLL_INDEX = 10;
    public static final String[] TASK_SUMMARY_PREFIXES = {"Feature Freeze", "Go Alpha", "QA: Master ALPHA",
                                                          "PM Approval", "Go Beta", "QA: Master BETA",
                                                          "PM Approval", "Rollout to RC", "QA: Master RC",
                                                          "Final PM Approval", "Rollout to Production", "QA: Master PROD"};
}
