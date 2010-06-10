package com.trentlarson.forecast.core.scheduling;

import java.util.*;

import com.trentlarson.forecast.core.dao.TeamHours;

public class Teams {

  /**
     This represents an entity that has time allotted for their work.

     It can be 1) a specific individual with no team, or 2) a specific
     team with no individual, or 3) a specific individual on a
     specific team.  (It cannot be unassigned to both team and user.)
   */
  public static class UserTimeKey implements Comparable {
    private Long teamId;
    private String username;
    public UserTimeKey(Long teamId_, String username_) {
      /**
      // Technically, this is true; however, since teams are separate
      // from projects, someone might do this, and so let's not break.
      if (teamId_ == null && username_ == null) {
        throw new IllegalStateException("You cannot make a user-time key with neither team nor user.");
      }
      **/
      this.teamId = teamId_;
      this.username = username_;
    }
    /** @return maybe null */
    public Long getTeamId() { return teamId; }
    /** @return maybe null */
    public String getUsername() { return username; }
    /** Compares by team, then username, with nulls coming first. */
    public int compareTo(Object obj) {
      UserTimeKey objKey = (UserTimeKey) obj;
      return
          ("" 
           + (getTeamId() == null ? Character.MIN_VALUE : getTeamId())
           + (getUsername() == null ? "" : getUsername()))
          .compareTo
          (""
           + (objKey.getTeamId() == null ? Character.MIN_VALUE : objKey.getTeamId())
           + (objKey.getUsername() == null ? "" : objKey.getUsername()));
    }
    public int hashCode() {
      // see String.hashCode() in the API for the reasoning behind this
      long teamId_ = teamId == null ? 0 : teamId.longValue();
      String username_ = username == null ? "" : username;
      return
        (int) (teamId_ * Math.pow(31.0, username_.length()))
        + username_.hashCode();
    }
    public boolean equals(Object obj) {
      return compareTo(obj) == 0;
    }
    public String toString() {
      return toString(teamId, username, false);
    }
    public static String toString(Long teamId, String username, boolean sayNoTeam) {
      return toString(teamId == null ? null : String.valueOf(teamId), username, sayNoTeam);
    }
    public static String toString(String team, String username, boolean sayNoTeam) {
      return
        "User " + (username == null ? "is" : username + " on")
        + " "
        + (team == null ? (sayNoTeam ? "no Team" : "any Team") : "Team " + team);
    }
    public static UserTimeKey fromString(String assignee) {
      String username;
      if (assignee.startsWith("User is")) {
        username = null;
      } else {
        int startPos = "User ".length();
        int endPos = assignee.indexOf(" ", startPos);
        username = assignee.substring(startPos, endPos);
      }
      Long team;
      if (assignee.endsWith("no Team")) {
        team = null;
      } else if (assignee.endsWith("any Team")) {
        team = null;
      } else {
        team = new Long(assignee.substring(assignee.lastIndexOf(" ") + 1));
      }
      return new UserTimeKey(team, username);
    }
  }

  /**
     This represents a user assigned to handle an issue.
   */
  public static class AssigneeKey implements Comparable {
    private Long teamId;
    private String username;
    public AssigneeKey(Long teamId_, String username_) {
      this.teamId = teamId_;
      this.username = username_;
    }
    /** @return maybe null */
    public Long getTeamId() { return teamId; }
    /** @return maybe null */
    public String getUsername() { return username; }
    public int compareTo(Object obj) {
      AssigneeKey objKey = (AssigneeKey) obj;
      return ("" + getTeamId() + getUsername())
        .compareTo("" + objKey.getTeamId() + objKey.getUsername());
    }
    public int hashCode() {
      // see String.hashCode() in the API for the reasoning behind this
      long teamId_ = teamId == null ? 0 : teamId.longValue();
      String username_ = username == null ? "" : username;
      return
        (int) (teamId_ * Math.pow(31.0, username_.length()))
        + username_.hashCode();
    }
    public boolean equals(Object obj) {
      return compareTo(obj) == 0;
    }
    public String toString() {
      // This is partly because it's convenient in tests, but I also
      // believe that the data conversions (to/from String names) for
      // TimeSchedule functionality implicitly depends on it.
      return UserTimeKey.toString(getTeamId(), getUsername(), true);
    }
  }
  
  /**
     To hold two Map objects (usually the result of retrieveWeeks):
     - weeks: a Map from each week start Date to a List of TeamHours,
       in team-user order,
       one for each and every team-user with an entry in that range,
       with TeamHours object having 0 hours available if none existed in DB
     - users: a Map from each TeamUserKey to a List of TeamHours,
       in date order,
       one for each and every week in range
    */
  public static class WeeksAndUsers {
    private SortedMap<Date,List<TeamHours>> weeks;
    private SortedMap<UserTimeKey,List<TeamHours>> users;

    public WeeksAndUsers
      (SortedMap<Date,List<TeamHours>> weeks_,
       SortedMap<UserTimeKey,List<TeamHours>> users_) {
      this.weeks = weeks_;
      this.users = users_;
    }
    public SortedMap<Date,List<TeamHours>> getWeeks() { return weeks; }
    public SortedMap<UserTimeKey,List<TeamHours>> getUsers() { return users; }
  }


  public static class HoursUserComparator implements Comparator {
    public int compare(Object o1, Object o2) {
      TeamHours t1 = (TeamHours) o1;
      TeamHours t2 = (TeamHours) o2;
      return
        (t1.getTeamId() + (t1.getUsername() == null ? "" : t1.getUsername()))
        .compareTo(t2.getTeamId() + (t2.getUsername() == null ? "" : t2.getUsername()));
    }
  }
  public static class HoursDateComparator implements Comparator {
    public int compare(Object o1, Object o2) {
      TeamHours t1 = (TeamHours) o1;
      TeamHours t2 = (TeamHours) o2;
      return t1.getStartOfWeek().compareTo(t2.getStartOfWeek());
    }
  }

}
