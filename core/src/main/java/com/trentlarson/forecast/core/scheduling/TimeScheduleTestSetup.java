package com.trentlarson.forecast.core.scheduling;

import java.sql.Connection;
import java.text.SimpleDateFormat;

import com.icentris.sql.SimpleSQL;
import com.trentlarson.forecast.core.helper.ForecastUtil;

public class TimeScheduleTestSetup {
  
  private static final SimpleDateFormat SLASH_DATE = new SimpleDateFormat("yyyy/MM/dd");
  
  private static final String projectSql =     "insert into project (id, pname, pkey) values (?, ?, ?)";
  private static final String componentSql =   "insert into component (id, project, cname) values (?, ?, ?)";
  private static final String issueSql =       "insert into jiraissue (id, pkey, project, assignee, summary, priority, resolution, timeestimate) values (?, ?, ?, ?, ?, ?, ?, ?)";
  private static final String nodeAssocSql =   "insert into nodeassociation (source_node_id, source_node_entity, sink_node_id, sink_node_entity, association_type) values (?, ?, ?, ?, ?)";
  private static final String teamSql =        "insert into team (id, name, project_id) values (?, ?, ?)";
  private static final String teamHoursSql =   "insert into team_hours (id, team_id, username, hours_available, start_of_week) values (?, ?, ?, ?, ?)";
  
  public static final long proj1Num = 1;
  public static final long proj2Num = proj1Num + 1;
  public static final long proj1Id = idFromProjectNum(proj1Num);
  public static final long proj2Id = idFromProjectNum(proj2Num);
  public static final long team1Id = proj1Id + 0;
  public static final long team2Id = proj1Id + 1;
  
  public static final long proj3Num = proj2Num + 1;
  public static final long proj4Num = proj3Num + 1;
  public static final long proj3Id = idFromProjectNum(proj3Num);
  public static final long proj4Id = idFromProjectNum(proj4Num);
  public static final long team3Id = proj3Id + 0;
  public static final long team4Id = proj3Id + 1;
  
  

    
  private static long idFromProjectNum(long num) {
    return 20000 + (num * 100);
  }

  public static void main(String args[]) throws Exception {
    
    Connection conn = ForecastUtil.getConnection();
    
    // set up one team who has to do all the work
    
    createTwoProjectsOneEmpty(proj1Num, conn);
    
    SimpleSQL.executeUpdate(teamSql, new Object[]{ team1Id, "Dilbert's Posse on " + proj1Id, proj1Id }, conn);
    SimpleSQL.executeUpdate(teamHoursSql, new Object[]{ proj1Id + 0, team1Id, null, 30, SLASH_DATE.parse("2000/01/01") }, conn);
    
    SimpleSQL.executeUpdate(teamSql, new Object[]{ team2Id, "Dogbert's Brood on " + proj2Id, proj2Id }, conn);
    SimpleSQL.executeUpdate(teamHoursSql, new Object[]{ proj1Id + 1, team2Id, null, 30, SLASH_DATE.parse("2000/01/01") }, conn);
    
    
    
    // now get teams to do different parts of the project
    createTwoProjectsOneEmpty(proj3Num, conn);
    
    SimpleSQL.executeUpdate(teamSql, new Object[]{ team3Id, "Dilbert's Posse on " + proj3Id, proj3Id }, conn);
    SimpleSQL.executeUpdate(teamHoursSql, new Object[]{ proj3Id + 0, team3Id, null, 30, SLASH_DATE.parse("2000/01/01") }, conn);
    
    SimpleSQL.executeUpdate(teamSql, new Object[]{ team4Id, "Dogbert's Brood on " + proj3Id, proj3Id }, conn);
    SimpleSQL.executeUpdate(teamHoursSql, new Object[]{ proj3Id + 1, team4Id, null, 30, SLASH_DATE.parse("2000/01/01") }, conn);
    
    
    

  }
  
  private static void createTwoProjectsOneEmpty(long projectNum, Connection conn) throws Exception {
    
    long proj1 = projectNum;
    long proj2 = projectNum + 1;
    long proj1Id = idFromProjectNum(proj1);
    long proj2Id = idFromProjectNum(proj2);
    SimpleSQL.executeUpdate(projectSql, new Object[]{ proj1Id, "Project " + proj1, "PR" + proj1 }, conn);
    SimpleSQL.executeUpdate(projectSql, new Object[]{ proj2Id, "Project " + proj2, "PR" + proj2 }, conn);
    
    SimpleSQL.executeUpdate(componentSql, new Object[]{ proj1Id + 0, proj1Id, "PHB Beauticians"}, conn);
    SimpleSQL.executeUpdate(componentSql, new Object[]{ proj1Id + 1, proj1Id, "Wally Cup Holders"}, conn);
    SimpleSQL.executeUpdate(componentSql, new Object[]{ proj1Id + 2, proj1Id, "Alice Face Straighteners"}, conn);
    SimpleSQL.executeUpdate(componentSql, new Object[]{ proj1Id + 3, proj1Id, "Alice Limb Holders"}, conn);
    SimpleSQL.executeUpdate(componentSql, new Object[]{ proj1Id + 4, proj2Id, "Catbert Form Handlers"}, conn);
    SimpleSQL.executeUpdate(componentSql, new Object[]{ proj1Id + 5, proj2Id, "Catbert Mood Regulaters"}, conn);
    SimpleSQL.executeUpdate(componentSql, new Object[]{ proj1Id + 6, proj2Id, "Dogbert Psychiatric Handlers"}, conn);
    
    long issueId = proj1Id;
    SimpleSQL.executeUpdate(issueSql, new Object[]{ ++issueId, "PR" + proj1 + "-" + issueId, proj1Id, null, "one team", 4, null, 2 * 8 * 3600 }, conn);
    SimpleSQL.executeUpdate(nodeAssocSql, new Object[]{ issueId, "Issue", proj1Id + 0, "Component", "IssueComponent"}, conn);
    
    SimpleSQL.executeUpdate(issueSql, new Object[]{ ++issueId, "PR" + proj1 + "-" + issueId, proj1Id, null, "another team", 4, null, 2 * 8 * 3600 }, conn);
    SimpleSQL.executeUpdate(nodeAssocSql, new Object[]{ issueId, "Issue", proj1Id + 1, "Component", "IssueComponent"}, conn);
    
    SimpleSQL.executeUpdate(issueSql, new Object[]{ ++issueId, "PR" + proj1 + "-" + issueId, proj1Id, null, "another team", 4, null, 2 * 8 * 3600}, conn);
    SimpleSQL.executeUpdate(nodeAssocSql, new Object[]{ issueId, "Issue", proj1Id + 1, "Component", "IssueComponent"}, conn);
    
    SimpleSQL.executeUpdate(issueSql, new Object[]{ ++issueId, "PR" + proj1 + "-" + issueId, proj1Id, null, "another team", 4, null, 2 * 8 * 3600}, conn);
    SimpleSQL.executeUpdate(nodeAssocSql, new Object[]{ issueId, "Issue", proj1Id + 1, "Component", "IssueComponent"}, conn);
    
    SimpleSQL.executeUpdate(issueSql, new Object[]{ ++issueId, "PR" + proj1 + "-" + issueId, proj1Id, null, "another team too", 4, null, 2 * 8 * 3600}, conn);
    SimpleSQL.executeUpdate(nodeAssocSql, new Object[]{ issueId, "Issue", proj1Id + 2, "Component", "IssueComponent"}, conn);
    
    SimpleSQL.executeUpdate(issueSql, new Object[]{ ++issueId, "PR" + proj1 + "-" + issueId, proj1Id, null, "another team too", 4, null, 2 * 8 * 3600}, conn);
    SimpleSQL.executeUpdate(nodeAssocSql, new Object[]{ issueId, "Issue", proj1Id + 2, "Component", "IssueComponent"}, conn);
    
    SimpleSQL.executeUpdate(issueSql, new Object[]{ ++issueId, "PR" + proj1 + "-" + issueId, proj1Id, null, "another team too", 4, null, 2 * 8 * 3600}, conn);
    SimpleSQL.executeUpdate(nodeAssocSql, new Object[]{ issueId, "Issue", proj1Id + 2, "Component", "IssueComponent"}, conn);
    
    SimpleSQL.executeUpdate(issueSql, new Object[]{ ++issueId, "PR" + proj1 + "-" + issueId, proj1Id, null, "another team three", 4, null, 2 * 8 * 3600}, conn);
    SimpleSQL.executeUpdate(nodeAssocSql, new Object[]{ issueId, "Issue", proj1Id + 3, "Component", "IssueComponent"}, conn);
    
    SimpleSQL.executeUpdate(issueSql, new Object[]{ ++issueId, "PR" + proj1 + "-" + issueId, proj1Id, null, "another team three", 4, null, 2 * 8 * 3600}, conn);
    SimpleSQL.executeUpdate(nodeAssocSql, new Object[]{ issueId, "Issue", proj1Id + 3, "Component", "IssueComponent"}, conn);
    
    SimpleSQL.executeUpdate(issueSql, new Object[]{ ++issueId, "PR" + proj1 + "-" + issueId, proj1Id, null, "another team three", 4, null, 2 * 8 * 3600}, conn);
    SimpleSQL.executeUpdate(nodeAssocSql, new Object[]{ issueId, "Issue", proj1Id + 3, "Component", "IssueComponent"}, conn);
    
  }
  
}
