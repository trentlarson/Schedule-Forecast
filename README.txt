________________________________________________________________________________

Note that I updated core but I haven't tested with jira-plugin or war


For tests:
- Run the TimeScheduleTests unit test, eg. in core:
mvn package; java -classpath target/forecast-core-0.1-SNAPSHOT.jar:lib/commons-logging-1.1.1.jar:lib/hibernate-3.2.6.ga.jar:lib/hsqldb-1.8.0.jar:lib/log4j-1.2.15.jar com.trentlarson.forecast.core.scheduling.TimeScheduleTests > target/gantt-test2.html
- Compare the output with src/test/resources/gantt-test.html
- Redirect the jira-test-db.sql into a test DB, eg: 
mysql -u jira --password=jirapass  -D test_forecast_jira < ~/dev/scheduling/Schedule-Forecast/core/src/test/resources/jira-test-db.sql 
- Run the TimeScheduleSetup, eg. in core:
mvn package; java -classpath target/forecast-core-0.1-SNAPSHOT.jar:lib/commons-logging-1.1.1.jar:lib/hibernate-3.2.6.ga.jar:lib/hsqldb-1.8.0.jar:lib/log4j-1.2.15.jar:lib/mysql-connector-java-8.0.21.jar com.trentlarson.forecast.core.scheduling.external.TimeScheduleTestSetup
- Run the TimeScheduleTests integration test, eg. in core:
mvn package; java -classpath target/forecast-core-0.1-SNAPSHOT.jar:lib/commons-logging-1.1.1.jar:lib/hibernate-3.2.6.ga.jar:lib/hsqldb-1.8.0.jar:lib/log4j-1.2.15.jar:lib/mysql-connector-java-8.0.21.jar com.trentlarson.forecast.core.scheduling.external.TimeScheduleIntegrationTests > target/gantt-test-db.html
- Compare the output with src/test/resources/gantt-test-db.html

________________________________________________________________________________

To develop with the Atlassian SDK:
- Install it.
- Go to the project home directory (with pom.xml) and run: atlas-run

The servlet part of the plugin will be available here:
http://localhost:9090/plugins/servlet/schedule-forecast

It takes JSON parameters "cPrefs" and "dPrefs", which you'll have to
read the code of ScheduleForecastServlet.java to understand.

The servlet works in a v2 approach (installing to
plugins/installed-plugins), but the JSPs won't work.  To get gantt.jsp
to work, install the v1 jar (into WEB-INF/lib) and then copy all the
other required jars (embedded inside it in the META-INF/lib directory)
into the WEB-INF/lib as well.

________________________________________________________________________________


If you run this and point to a Jira DB, you'll get some reports showing project completion based on the due dates and priorities of the issues.

To deploy:
- Get tools: Maven; a J2EE server eg. Tomcat
- Create the TEAM and TEAM_HOURS tables; see Team*.hbm.xml
- Put DB settings in hibernate.cfg.xml & ForecastUtil.java
- Build the war with: mvn package
-- Choose your tool:
-- For standalone: Copy the target/forecast.war file into the war directory, eg. tomcat/webapps/
-- For Jira: copy the forecast jars and the core/lib jars (except logging) into atlassian-jira/WEB-INF/lib, and war/src/main/webapp/gantt.jsp file into someplace you'll remember under atlassian-jira/, eg. secure/views/company
- You'll want to change the time-per-day to match this plugin, under Administration -> Time Tracking (see timeestimate SQL below)


Helpful hints for Eclipse:
- Run this inside the forecast directory: mvn eclipse:eclipse
- Put the 'forecast' directory inside another one (I use 'forecast').
- In Eclipse:
-- switch to that as the workspace
-- import 'General -> Existing Files'
-- choose the folder that contains 'forecast'
-- in 'Window' -> 'Preferences' -> 'Java' -> 'Build Path' -> 'Classpath Variables'
   add 'M2_REPO' to point to your $HOME/.m2/repository directory

To use:
- hours-per-week is enabled by default in IssueLoader.java; make
    sure to create that as a custom field (of type number) and put the ID in there
- Browse to forecast/gantt.jsp, eg. http://localhost:8080/forecast/gantt.jsp



________________________________________________________________________________

Places to clean up:
- allow hibernate, which may have been the thing that locked the JIRA startup
- ... and then you can allow it in the gantt.jsp
- split wicket from war project(?)

- I think there's a problem where defaultStartDate is used in the middle of scheduling in TimeSchedule.createIssueSchedules.  It seems like it's being used for later issues and not just the first one.
- in gantt-test.html, the "ranges of hourly work done" don't seem to match the start and end times

To duplicate the error with efficient loading:
- Load your DB with jira-test-render-error.sql (an admin user is trent/jirapass)
- Run TimeScheduleTests.testIntegrationDynamicLoadAssigneeProblem (and see the exception)

To duplicate the error with time scheduling:
- Load your DB with jira-test-render-error.sql (an admin user is tlarson/jirapass)
- see http://localhost:8080/secure/views/max/forecast-gantt.jsp?show_user=tlarson
  ... and notice the bad scheduling, where many begin work at the same time
  (If you have problems seeing it, try forcing this load date: Fri Dec 17 12:59:42 MST 2010)

________________________________________________________________________________
Sample SQL to adjust timeestimate fields if you've already set a bunch of estimates on 24x7 tracking:

  update jiraissue set timeestimate = 
    ((timeestimate div (7*24*3600))*(5*8*3600))
    + (((timeestimate mod (7*24*3600)) div (24*3600))*(8*3600))
    + (((timeestimate mod (7*24*3600)) mod (24*3600))
  where timeestimate div (24*3600) > 0;

________________________________________________________________________________

   LINK_SUBTASK="10010", LINK_DEPENDENCY="10000", CUSTOM_START_DATE="10180"

CUSTOMFIELDVALUE
customfield = company (10002)
issue = issue.id
stringvalue = company

create table customfieldvalue (
 customfield integer,
 issue integer,
 datevalue date,
 stringvalue varchar(255));

ISSUELINK
linktype = LINK_SUBTASK, LINK_DEPENDENCY
source, destination = issue.id

create table issuelink (
 linktype integer,
 source integer,
 destination integer);

create table issue (
 id integer,
 assignee varchar(255),
 duedate date,
 pkey varchar(255),
 priority integer,
 project integer,
 resolution integer,
 summary varchar(255),
 timeestimate integer,
 timespent integer,
 startdate date);

sequence: lower = do first
create table priority (
 id integer,
 pname varchar(63),
 sequence integer);

create table project (
 id integer,
 pkey varchar(255));

create table userbase (
 username varchar(255));


Create "team" tables; see *.hbm.xml files.




insert into userbase values ('trent');

insert into project values ('1', 'Buehner');

insert into priority values ('0', 'Highest', '0');
insert into priority values ('1', 'Med-High', '1');
insert into priority values ('2', 'Medium', '2');
insert into priority values ('3', 'Med-Low', '3');
insert into priority values ('4', 'Lowest', '4');

insert into issue values (0, 'trent', '2009-08-10', 'IT-1', 3, 1, null, 'Google App Engine', 4 * 3600, 0, null);


insert into team (id, created, updated, name, project_id) values (0, null, null, 'self', 1);

insert into team_hours (id, created, updated, team_id, username, start_of_week, hours_available) values (0, null, null, 0, 'trent', '2009-08-10', '40');






TeamHoursUtil

      rset = SimpleSQL.executeQuery("select id, pname as name from project", new Object[0], conn);

          "select distinct username from team_hours "
          + " where team_id = ? and ? <= start_of_week and start_of_week < ?";

TimeScheduleAction

            "update jiraissue set priority = (select id from priority where sequence = ?), "
            + " timeestimate = ?, duedate = ? where pkey = ?";

TimeScheduleLoader

      String sql = "select username from userbase";

        "select team_id, username, start_of_week, hours_available"
        + " from team_hours"
        + " order by team_id, username, start_of_week desc";

      String prioritySql = "select id, sequence from priority";

      String projectTeamSql = "select project_id, id from team";

        "select pkey, summary, assignee, resolution, timeestimate, timespent, duedate,"
        + " priority, project, cfv.datevalue as must_start_date"
        + " from jiraissue"
        + " left outer join customfieldvalue cfv on jiraissue.id = cfv.issue"
        + "   and cfv.customfield = " + CUSTOM_START_DATE
        + " where (resolution is null or resolution = 0)";


        "select a.pkey as super_key, b.pkey as sub_key"
        + " from issuelink, jiraissue a, jiraissue b"
        + " where linktype = '" + LINK_SUBTASK + "'"
        + " and source = a.id and b.id = destination"
        + " and a.resolution is null and b.resolution is null";

        "select a.pkey as pre_key, b.pkey as post_key"
        + " from issuelink, jiraissue a, jiraissue b"
        + " where linktype = '" + LINK_DEPENDENCY + "'"
        + " and source = a.id and b.id = destination"
        + " and a.resolution is null and b.resolution is null";

          "select b.pkey, b.summary, b.assignee, b.timeestimate,"
          + " b.timespent, b.duedate, b.resolution, b.priority, b.project,"
          + " cfv.datevalue as must_start_date"
          + " from issuelink, jiraissue a, jiraissue b"
          + " left outer join customfieldvalue cfv on b.id = cfv.issue"
          + "   and cfv.customfield = " + CUSTOM_START_DATE
          + " where linktype = '" + LINK_SUBTASK + "'"
          + " and a.pkey = ? and source = a.id and b.id = destination";

          "select b.pkey, b.summary, b.assignee, b.timeestimate,"
          + " b.timespent, b.duedate, b.resolution, b.priority, b.project,"
          + " cfv.datevalue as must_start_date"
          + " from issuelink, jiraissue a, jiraissue b"
          + " left outer join customfieldvalue cfv on b.id = cfv.issue"
          + "   and cfv.customfield = " + CUSTOM_START_DATE
          + " where linktype = '" + LINK_DEPENDENCY + "'"
          + " and a.pkey = ? and destination = a.id and b.id = source";

          "select b.pkey, b.summary, b.assignee, b.timeestimate,"
          + " b.timespent, b.duedate, b.resolution, b.priority, b.project,"
          + " cfv.datevalue as must_start_date"
          + " from issuelink, jiraissue a, jiraissue b"
          + " left outer join customfieldvalue cfv on b.id = cfv.issue"
          + "   and cfv.customfield = " + CUSTOM_START_DATE
          + " where linktype = '" + LINK_DEPENDENCY + "'"
          + " and a.pkey = ? and source = a.id and b.id = destination";




        "select jiraissue.pkey, summary, assignee, resolution, timeestimate, timespent,"
        + " dueDate, priority, jiraissue.project, cfv.datestring as must_start_date "
        + " from jiraissue, project"
        + " left outer join customfieldvalue cfv on jiraissue.id = cfv.issue"
        + "   and cfv.customfield = " + CUSTOM_START_DATE
        + " where project.pkey = ?"
        + " and jiraissue.project = project.id and jiraissue.resolution is null";

        "select pkey, summary, assignee, resolution, timeestimate, timespent,"
        + " dueDate, priority, project, cfv.datevalue as must_start_date"
        + " from jiraissue"
        + " left outer join customfieldvalue cfv on jiraissue.id = cfv.issue"
        + "   and cfv.customfield = " + CUSTOM_START_DATE
        + " where pkey in ("
        + Join(issueKeys, false, "?", ",")
        + ")";


        "select pkey, summary, assignee, resolution, timeestimate, timespent, duedate,"
        + " priority, project, cfv.datevalue as must_start_date"
        + " from jiraissue"
        + " left outer join customfieldvalue cfv on jiraissue.id = cfv.issue"
        + "   and cfv.customfield = " + CUSTOM_START_DATE
        + " where assignee is null"
        + " and (resolution is null or resolution = 0)";


        "select pkey, summary, assignee, resolution, timeestimate, timespent, duedate,"
        + " priority, project, cfv.datevalue as must_start_date"
        + " from jiraissue"
        + " left outer join customfieldvalue cfv on jiraissue.id = cfv.issue"
        + "   and cfv.customfield = " + CUSTOM_START_DATE
        + " where assignee = ?"
        + " and (resolution is null or resolution = 0)";



TimeAssignmentsByWeek

        "select j.id, j.pkey as key, j.project, j.assignee, j.summary, "
        + " p.pname as priority_name, p.sequence as priority_seq, "
        + " j.timeestimate, j.duedate, j.resolution, cfv.stringvalue as company "
//v These lines allow for selection of version (more below).
//v       + ", v.vname as version"
        + " from jiraissue j "
//v        + " left outer join nodeassociation na on j.id = na.source_node_id and na.association_type = 'IssueFixVersion' "
//v        + " left outer join version v on v.id = na.sink_node_id "
        + " left outer join priority p on p.id = j.priority "
        + " left outer join customfieldvalue cfv on cfv.issue = j.id and cfv.customfield = '10002'"
        + " where "
//v       + " ( "
        + " (? < duedate and duedate <= ?)";
//v        + " or (vname like ?) ) ";
