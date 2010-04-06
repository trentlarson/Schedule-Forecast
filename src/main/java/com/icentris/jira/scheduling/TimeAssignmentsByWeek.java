package com.icentris.jira.scheduling;

import java.util.*;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import com.icentris.sql.SimpleSQL;
import com.icentris.jira.helper.ForecastUtil;

public class TimeAssignmentsByWeek {

  /**
    @return List of IssueInfo objects for all unresolved issues due within this week

    Note that this goes from the first moment of
    TeamHoursUtil.FIRST_DAY_OF_WORK in the selected week (exclusive)
    to the first moment of that work day in the next week (inclusive).

   */
  public static List loadWeek(int selectedWeekNum) throws SQLException {
    List issues = new ArrayList();
    Connection conn = null;
    ResultSet rset = null;
    try {
      conn = ForecastUtil.getConnection();
      String issueSql =
        "select j.id, j.pkey as key, j.project, j.assignee, j.summary, "
        + " p.pname as priority_name, p.sequence as priority_seq, "
        + " j.timeestimate, j.duedate, j.resolution, cfv.stringvalue as company "
//v These lines allow for selection of version (more below).
//v       + ", v.vname as version"
        + " from issue j "
//v        + " left outer join nodeassociation na on j.id = na.source_node_id and na.association_type = 'IssueFixVersion' "
//v        + " left outer join version v on v.id = na.sink_node_id "
        + " left outer join priority p on p.id = j.priority "
        + " left outer join customfieldvalue cfv on cfv.issue = j.id and cfv.customfield = '10002'"
        + " where "
//v       + " ( "
        + " (? < duedate and duedate <= ?)";
//v        + " or (vname like ?) ) ";
      /*
 select j.id, j.pkey as key, j.project, j.assignee, j.summary, p.pname as priority_name, p.sequence as priority_seq, v.vname as version, j.timeestimate, j.duedate, j.resolution
 from issue j 
 left outer join nodeassociation na on j.id = na.source_node_id and na.association_type = 'IssueFixVersion'
 left outer join version v on v.id = na.sink_node_id 
 left outer join priority p on p.id = j.priority
 where 
 ( ('03-Apr-2006' <= duedate and duedate < '10-Apr-2006') or (vname like 'Iteration 14%') );
       */

      Timestamp weekBegin = new Timestamp(TeamHoursUtil.weekCal(selectedWeekNum).getTime().getTime());
      Timestamp nextWeekBegin = new Timestamp(TeamHoursUtil.weekCal(selectedWeekNum + 1).getTime().getTime());
      Object[] args = new Object[]{ weekBegin, nextWeekBegin};
//v      Object[] args = new Object[]{ weekBegin, nextWeekBegin, "Iteration " + selectedWeekNum + "%"};
      rset = SimpleSQL.executeQuery(issueSql, args, conn);
      while (rset.next()) {
        issues.add(new IssueInfo(rset));
      }
    } finally {
      try { rset.close(); } catch (Exception e) {}
      try { conn.close(); } catch (Exception e) {}
    }
    return issues;
  }

  public static class IssueInfo {
    public String key, assignee, summary, priorityName, company;
    public Long id, project, estSeconds;
    public Integer prioritySeq;
    public Date dueDate;
    public Boolean resolved;
    public IssueInfo(ResultSet rset) throws SQLException {
      id = new Long(rset.getLong("id"));
      key = rset.getString("key");
      project = new Long(rset.getLong("project"));
      assignee = rset.getString("assignee");
      if (assignee == null) { assignee = "unassigned"; }
      summary = rset.getString("summary");
      priorityName = rset.getString("priority_name");
      prioritySeq = new Integer(rset.getInt("priority_seq"));
      company = rset.getString("company");
//v      version = rset.getString("version");
//v      if (version == null) { version = ""; }
      estSeconds = new Long(rset.getLong("timeestimate"));
      dueDate = rset.getDate("duedate");
      resolved = new Boolean(rset.getInt("resolution") != 0);
    }
    public Long getId() { return id; }
    public String getKey() { return key; }
    public Long getProject() { return project; }
    public String getAssignee() { return assignee; }
    public String getSummary() { return summary; }
    public String getPriorityName() { return priorityName; }
    public Integer getPrioritySeq() { return prioritySeq; }
    public String getCompany() { return company; }
//v    public String getVersion() { return version; }
    public Long getEstSeconds() { return estSeconds; }
    public Date getDueDate() { return dueDate; }
    public Boolean getResolved() { return resolved; }
  }

  public static class IssueInfoAssigneeSorter implements Comparator {
    public int compare(Object o1, Object o2) {
      IssueInfo ii1 = (IssueInfo) o1;
      IssueInfo ii2 = (IssueInfo) o2;
      return
        ii1.getAssignee() == null
        ? (ii2.getAssignee() == null
           ? 0
           : -1)
        : (ii2.getAssignee() == null
           ? 1
           : ii1.getAssignee().compareTo(ii2.getAssignee()));
    }
  }

  public static class IssueInfoPrioritySorter implements Comparator {
    public int compare(Object o1, Object o2) {
      return
        ((IssueInfo) o1).getPrioritySeq()
        .compareTo(((IssueInfo) o2).getPrioritySeq());
    }
  }

  /**
     Is this also in TimeSchedule.java?
   */
  public static class IssueDueDateSorter implements Comparator {
    public int compare(Object o1, Object o2) {
      IssueInfo ii1 = (IssueInfo) o1;
      IssueInfo ii2 = (IssueInfo) o2;
      return
        ii1.getDueDate() == null
        ? (ii2.getDueDate() == null
           ? 0
           : -1)
        : (ii2.getDueDate() == null
           ? 1
           : ii1.getDueDate().compareTo(ii2.getDueDate()));
    }
  }

  public static class IssueCompanySorter implements Comparator {
    public int compare(Object o1, Object o2) {
      IssueInfo ii1 = (IssueInfo) o1;
      IssueInfo ii2 = (IssueInfo) o2;
      return ii1.getCompany().compareTo(ii2.getCompany());
    }
  }

}
