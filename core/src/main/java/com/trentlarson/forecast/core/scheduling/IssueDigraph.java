package com.trentlarson.forecast.core.scheduling;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;

import com.trentlarson.forecast.core.dao.TeamHours;
import com.trentlarson.forecast.core.scheduling.TimeSchedule.IssueWorkDetail;
import com.trentlarson.forecast.core.scheduling.external.TimeScheduleLoader;

public class IssueDigraph {

  public static final Logger log4jLog = Logger.getLogger("com.trentlarson.forecast.core.scheduling.IssueDigraph");

  private Map<String,TimeSchedule.IssueSchedule<IssueTree>> issueSchedules;
  private Map<Teams.AssigneeKey,List<IssueTree>> assignedUserDetails;
  private Map<Teams.UserTimeKey,List<IssueTree>> timeUserDetails;
  private Map<Teams.AssigneeKey,Teams.UserTimeKey> assigneeToAllocatedUsers;
  private Map<Teams.UserTimeKey,TimeSchedule.WeeklyWorkHours> userWeeklyHoursAvailable;
  private Map<Teams.UserTimeKey,TimeSchedule.WeeklyWorkHours> userWeeklyHoursLeft;

  private TimeScheduleCreatePreferences prefs;
  private Date loadedDate = new Date();
  private int maxPriority = -1;
  
  /**
     @param issueSchedules_ maps issue key to IssueSchedule object
     @param timeUserDetails_ maps user key to IssueDetail List
  */
  public IssueDigraph
  (Map<String,TimeSchedule.IssueSchedule<IssueTree>> issueSchedules_,
   Map<Teams.AssigneeKey,List<IssueTree>> assignedUserDetails_,
   Map<Teams.UserTimeKey,List<IssueTree>> timeUserDetails_,
   Map<Teams.AssigneeKey,Teams.UserTimeKey> assigneeToAllocatedUsers_,
   Map<Teams.UserTimeKey,TimeSchedule.WeeklyWorkHours> userWeeklyHoursAvailable_,
   Map<Teams.UserTimeKey,TimeSchedule.WeeklyWorkHours> userWeeklyHoursLeft_,
   TimeScheduleCreatePreferences prefs_) {

    this.issueSchedules = issueSchedules_;
    this.assignedUserDetails = assignedUserDetails_;
    this.timeUserDetails = timeUserDetails_;
    this.assigneeToAllocatedUsers = assigneeToAllocatedUsers_;
    this.userWeeklyHoursAvailable = userWeeklyHoursAvailable_;
    this.userWeeklyHoursLeft = userWeeklyHoursLeft_;
    this.prefs = prefs_;
  }

  /**
     @return a Map from issue pkey to TimeSchedule.IssueSchedule object for it
  */
  public Map<String,TimeSchedule.IssueSchedule<IssueTree>> getIssueSchedules() {
    return issueSchedules;
  }
  public TimeSchedule.IssueSchedule<IssueTree> getIssueSchedule(String key) {
    return getIssueSchedules().get(key);
  }

  public IssueTree getIssueTree(String key) {
    return (IssueTree) getIssueSchedule(key).getIssue();
  }

  /**
     @return a Map from user to priority-sequential List of
     IssueWorkDetail elements originally assigned to them

     See also timeUserDetails.
   */
  public Map<Teams.AssigneeKey,List<IssueTree>> getAssignedUserDetails() {
    return assignedUserDetails;
  }

  /**
     @return a Map from user to priority-sequential List of
     IssueWorkDetail elements assigned to them for purposes of
     scheduling

     This may be different from the assignedUserDetails because the
     hour allocations often go to individuals and the team information
     is ignored.  (Sometimes it could go the other way: the time is
     allocated to a whole team and the user information is ignored,
     though that is not implemented at the time of writing this
     comment.)
  */
  public Map<Teams.UserTimeKey,List<IssueTree>> getTimeUserDetails() {
    return timeUserDetails;
  }

  /**
     @return the user which has time allotments that may be used for scheduling
  */
  public Teams.UserTimeKey getAllocatedUser(Teams.AssigneeKey assignee) {
    return assigneeToAllocatedUsers.get(assignee);
  }


  /**
     @return a Map from user to weekly working hours originally scheduled
  */
  public Map<Teams.UserTimeKey,TimeSchedule.WeeklyWorkHours> getUserWeeklyHoursAvailable() {
    return userWeeklyHoursAvailable;
  }

  /**
     @return a Map from user to weekly working hours that are left after scheduling
  */
  public Map<Teams.UserTimeKey,TimeSchedule.WeeklyWorkHours> getUserWeeklyHoursLeft() {
    return userWeeklyHoursLeft;
  }

  public Date getLoadedDate() {
    return loadedDate;
  }
  /**
   * Return the maximum priority found in all the issues, calculating it if this is the first call.
   * 
   * Remember: priorities are 1-based.  If no priorities exist, a -1 is returned.
   * 
   * @see IssueWorkDetail#getPriority()
   */
  public int getMaxPriority() {
    if (maxPriority == -1) {
      for (Iterator<TimeSchedule.IssueSchedule<IssueTree>> iter = getIssueSchedules().values().iterator(); iter.hasNext(); ) {
        TimeSchedule.IssueSchedule<IssueTree> schedule = iter.next();
        if (maxPriority < schedule.getIssue().getPriority()) {
          maxPriority = schedule.getIssue().getPriority();
        }
      }
    }
    return maxPriority;
  }
  public TimeScheduleCreatePreferences getTimeScheduleCreatePreferences() {
    return prefs;
  }
  public String toString() {
    return "\n<ol>\n" + treeString() + "\n</ol>";
  }

  public String treeString() {
    StringBuffer sb = new StringBuffer();
    for (Iterator<String> i = getIssueSchedules().keySet().iterator(); i.hasNext(); ) {
      String key = i.next();
      sb.append(getIssueTree(key).treeString());
    }
    return sb.toString();
  }

  /* unused (and loops through all issues when called... why did I write this?)
  public Date findMaxEndDate(Date max) {
    for (Iterator<TimeSchedule.IssueSchedule<IssueTree>> iter = getIssueSchedules().values().iterator(); iter.hasNext(); ) {
      TimeSchedule.IssueSchedule<IssueTree> schedule = iter.next();
      if (max.compareTo(schedule.getAdjustedEndCal().getTime()) < 0) {
        max = schedule.getAdjustedEndCal().getTime();
      }
    }
    return max;
  }
  */

  public void reschedule() {

    // REFACTOR with IssueDigraph.schedulesForUserIssues (lots of overlap)

    UserDetailsAndHours newTimeDetails = adjustDetailsForAssignedHours
      (getAssignedUserDetails(), getUserWeeklyHoursAvailable(),
       getTimeScheduleCreatePreferences().getStartTime());

    Map<String,List<IssueTree>> issuesFromString =
      createMapFromAssigneeKeyStringToUserIssues(newTimeDetails.timeDetails);

    // clone the hourly data (since it's modified later)
    Map<Teams.UserTimeKey,TimeSchedule.WeeklyWorkHours> weeklyHoursFromKey2 = new HashMap<Teams.UserTimeKey,TimeSchedule.WeeklyWorkHours>();
    for (Teams.UserTimeKey user : newTimeDetails.hours.keySet()) {
      weeklyHoursFromKey2
        .put(user,
             (TimeSchedule.WeeklyWorkHours) newTimeDetails.hours.get(user).clone());
    }

    Map<String,TimeSchedule.WeeklyWorkHours> weeklyHoursFromString =
      createMapFromUserTimeKeyStringToWeeklyHours(weeklyHoursFromKey2);

    Map<String,TimeSchedule.IssueSchedule<IssueTree>> newSchedules =
      TimeSchedule.schedulesForUserIssues
      (issuesFromString, weeklyHoursFromString,
       getTimeScheduleCreatePreferences().getStartTime(),
       getTimeScheduleCreatePreferences().getTimeMultiplier(),
       getTimeScheduleCreatePreferences().getReversePriority());

    this.issueSchedules = newSchedules;
    this.userWeeklyHoursAvailable = newTimeDetails.hours;
    this.userWeeklyHoursLeft = weeklyHoursFromKey2;
  }











  public static IssueDigraph schedulesForUserIssues3
      (Map<Teams.AssigneeKey,List<IssueTree>> userDetails,
       Map<Teams.UserTimeKey,List<TeamHours>> userWeeklyHours,
       TimeScheduleCreatePreferences sPrefs) {

    Map<Teams.UserTimeKey, SortedSet<TimeSchedule.HoursForTimeSpan>> userSpanHours =
        teamToUserHours(userWeeklyHours);
    return schedulesForUserIssues2(userDetails, userSpanHours, sPrefs);
  }

  public static IssueDigraph schedulesForUserIssues2
      (Map<Teams.AssigneeKey,List<IssueTree>> userDetails,
       Map<Teams.UserTimeKey,SortedSet<TimeSchedule.HoursForTimeSpan>> userSpanHourSet,
       TimeScheduleCreatePreferences sPrefs) {

    Map<Teams.UserTimeKey,TimeSchedule.WeeklyWorkHours> weeklyHoursFromKey =
        weeklyHoursToRange(userSpanHourSet);
    return schedulesForUserIssues(userDetails, weeklyHoursFromKey, sPrefs);

  }

  public static class ScheduleInput {
    Map<Teams.AssigneeKey,List<IssueTree>> userDetails;
    Map<Teams.UserTimeKey,TimeSchedule.WeeklyWorkHours> weeklyHoursFromKey;
    TimeScheduleCreatePreferences sPrefs;
    public ScheduleInput(Map<Teams.AssigneeKey,List<IssueTree>> userDetails_,
                         Map<Teams.UserTimeKey,TimeSchedule.WeeklyWorkHours> weeklyHoursFromKey_,
                         TimeScheduleCreatePreferences sPrefs_) {
      userDetails = userDetails_;
      weeklyHoursFromKey = weeklyHoursFromKey_;
      sPrefs = sPrefs_;
    }
  }

  public static IssueDigraph schedulesForUserIssues(ScheduleInput input) {
    return schedulesForUserIssues(input.userDetails, input.weeklyHoursFromKey, input.sPrefs);
  }

  public static IssueDigraph schedulesForIssues(IssueTree[] issues) {
    Map<Teams.AssigneeKey,List<IssueTree>> userDetails = createUserDetails(issues);
    return schedulesForUserIssues(userDetails, null, null);
  }

  public static IssueDigraph schedulesForIssues(IssueTree[] issues, TimeScheduleCreatePreferences sPrefs) {
    Map<Teams.AssigneeKey,List<IssueTree>> userDetails = createUserDetails(issues);
    return schedulesForUserIssues(userDetails, null, sPrefs);
  }

  public static IssueDigraph schedulesForIssues(IssueTree[] issues, Map<Teams.UserTimeKey,List<TeamHours>> userWeeklyHours) {
    return schedulesForIssues(issues, userWeeklyHours, null);
  }

  public static IssueDigraph schedulesForIssues(
      IssueTree[] issues,
      Map<Teams.UserTimeKey,List<TeamHours>> userWeeklyHours,
      TimeScheduleCreatePreferences sPrefs) {
    Map<Teams.AssigneeKey,List<IssueTree>> userDetails = createUserDetails(issues);
    Map<Teams.UserTimeKey, SortedSet<TimeSchedule.HoursForTimeSpan>> userSpanHours =
        teamToUserHours(userWeeklyHours);
    Map<Teams.UserTimeKey,TimeSchedule.WeeklyWorkHours> weeklyHoursFromKey =
        weeklyHoursToRange(userSpanHours);
    return schedulesForUserIssues(userDetails, weeklyHoursFromKey, sPrefs);
  }

  public static IssueDigraph schedulesForUserIssues
      (Map<Teams.AssigneeKey,List<IssueTree>> userDetails,
       Map<Teams.UserTimeKey,TimeSchedule.WeeklyWorkHours> weeklyHoursFromKey,
       TimeScheduleCreatePreferences sPrefs) {

    // (REFACTOR with IssueDigraph.reschedule (lots of overlap))

    if (weeklyHoursFromKey == null) {
      weeklyHoursFromKey = new TreeMap<Teams.UserTimeKey,TimeSchedule.WeeklyWorkHours>();
    }
    if (sPrefs == null) {
      sPrefs = new TimeScheduleCreatePreferences();
    }

    UserDetailsAndHours newTimeDetails =
        adjustDetailsForAssignedHours(userDetails, weeklyHoursFromKey,
            sPrefs.getStartTime());

    Map<String,List<IssueTree>> assigneeStringToIssues =
        createMapFromAssigneeKeyStringToUserIssues(newTimeDetails.timeDetails);

    // clone the hourly data (since it's modified later)
    Map<Teams.UserTimeKey,TimeSchedule.WeeklyWorkHours> weeklyHoursFromKey2 =
        new HashMap<Teams.UserTimeKey, TimeSchedule.WeeklyWorkHours>();
    for (Teams.UserTimeKey user : newTimeDetails.hours.keySet()) {
      weeklyHoursFromKey2
          .put(user,
              (TimeSchedule.WeeklyWorkHours) newTimeDetails.hours.get(user).clone());
    }

    Map<String,TimeSchedule.WeeklyWorkHours> weeklyHoursFromString =
        createMapFromUserTimeKeyStringToWeeklyHours(weeklyHoursFromKey2);

    Map<String,TimeSchedule.IssueSchedule<IssueTree>> schedules =
        TimeSchedule.schedulesForUserIssues
            (assigneeStringToIssues, weeklyHoursFromString,
                sPrefs.getStartTime(), sPrefs.getTimeMultiplier(), sPrefs.getReversePriority());

    // for displaying things in that priority order
    TimeSchedule.setInitialOrdering(userDetails, sPrefs.getReversePriority()); // Removing this does change the ordering in some output... not sure why.
    TimeSchedule.setInitialOrdering(newTimeDetails.timeDetails, sPrefs.getReversePriority());

    return new IssueDigraph(schedules,
        userDetails, newTimeDetails.timeDetails,
        newTimeDetails.assigneeToAllocatedUsers,
        newTimeDetails.hours, weeklyHoursFromKey2, sPrefs);
  }

  public static class UserDetailsAndHours {
    public final Map<Teams.UserTimeKey,List<IssueTree>> timeDetails;
    public final Map<Teams.AssigneeKey,Teams.UserTimeKey> assigneeToAllocatedUsers;
    public final Map<Teams.UserTimeKey,TimeSchedule.WeeklyWorkHours> hours;
    public UserDetailsAndHours(Map<Teams.AssigneeKey,Teams.UserTimeKey> assigneeToAllocatedUsers_,
                               Map<Teams.UserTimeKey,List<IssueTree>> timeDetails_,
                               Map<Teams.UserTimeKey,TimeSchedule.WeeklyWorkHours> hours_) {
      this.assigneeToAllocatedUsers = assigneeToAllocatedUsers_;
      this.timeDetails = timeDetails_;
      this.hours = hours_;
    }
    public String toString() {
      return "UserDetailsAndHours with:<br> assigneeToAllocatedUsers=" + assigneeToAllocatedUsers + ",<br> timeDetails=" + timeDetails + ",<br> hours=" + hours;
    }
  }

  /**
   Translate details info to input good for scheduling.

   @param hoursRange is used to decide exactly where to assign an
   issue.  If the hours don't exist for the user, then we assume
   that there is the default amount of work time available and we
   insert that record; the one exception is: if both the team and
   the user are set but no UserTimeKey exists, then a UserTimeKey is
   created for the user on any team (ie. where team is null).

   */
  public static UserDetailsAndHours adjustDetailsForAssignedHours
  (Map<Teams.AssigneeKey,List<IssueTree>> detailsFromAssignee,
   Map<Teams.UserTimeKey,TimeSchedule.WeeklyWorkHours> hoursRange,
   Date startDate) {

    Map<Teams.AssigneeKey,Teams.UserTimeKey> assigneeToAllocatedUsers = new HashMap<Teams.AssigneeKey, Teams.UserTimeKey>();
    Map<Teams.UserTimeKey,List<IssueTree>> newDetailsByTime = new HashMap<Teams.UserTimeKey,List<IssueTree>>();
    Map<Teams.UserTimeKey,TimeSchedule.WeeklyWorkHours> newHoursRanges = new HashMap<Teams.UserTimeKey, TimeSchedule.WeeklyWorkHours>();

    newHoursRanges.putAll(hoursRange);

    for (Teams.AssigneeKey assigneeKey : detailsFromAssignee.keySet()) {
      for (IssueTree detail : detailsFromAssignee.get(assigneeKey)) {

        // create appropriate time-allocation user key for this time assignee
        Teams.UserTimeKey userKeyForTime = detail.getTimeAssigneeKey();
        if (!newHoursRanges.containsKey(userKeyForTime)) {
          userKeyForTime = new Teams.UserTimeKey(null, userKeyForTime.getUsername());
          if (log4jLog.isDebugEnabled()) {
            log4jLog.debug("Changing time assignee of issue " + detail.getKey()
                + " with hash " + detail.hashCode()
                + " to " + userKeyForTime
                + " (from " + detail.getTimeAssigneeKey() + ").");
          }
          detail.setTimeAssigneeKey(userKeyForTime);
        }

        updateDetailAndLists(userKeyForTime, detail, newDetailsByTime, assigneeToAllocatedUsers, newHoursRanges, startDate);
      }
    }
    return new UserDetailsAndHours(assigneeToAllocatedUsers, newDetailsByTime, newHoursRanges);
  }

  /** remove
  protected static <T extends Object> Teams.AssigneeKey userKeyIfNotAlreadyExisting
     (Teams.AssigneeKey userKey, Map<Teams.UserTimeKey,T> userWeeklyHours) {

    Teams.UserTimeKey timeKey =
      new Teams.UserTimeKey(userKey.getTeamId(), userKey.getUsername());

    if (userKey.getUsername() != null
        && !userWeeklyHours.containsKey(timeKey)) {
      log4jLog.debug("Hours not allocated for " + userKey.getUsername() + ", so we're creating an assignee key for them.");
      return new Teams.AssigneeKey(null, userKey.getUsername());
    } else {
      return userKey;
    }
  }
  **/

  /**
   Modify the 'detail' and the maps so that detail has time assignee 'assignee'.
   */
  private static void updateDetailAndLists
  (Teams.UserTimeKey assignee,
   IssueTree detail,
   Map<Teams.UserTimeKey,List<IssueTree>> detailMap,
   Map<Teams.AssigneeKey,Teams.UserTimeKey> assigneeToAllocatedUsers,
   Map<Teams.UserTimeKey,TimeSchedule.WeeklyWorkHours> hoursRanges,
   Date startDate) {

    // add key if it doesn't already exist
    if (!hoursRanges.containsKey(assignee)) {
      log4jLog.debug("Adding default hours for " + assignee);
      TimeSchedule.WeeklyWorkHours defaultHours = new TimeSchedule.WeeklyWorkHours();
      defaultHours.inject(startDate, TimeSchedule.TYPICAL_INDIVIDUAL_WORKHOURS_PER_WORKWEEK);
      hoursRanges.put(assignee, defaultHours);
    }

    List<IssueTree> detailMapEntry;
    if (detailMap.containsKey(assignee)) {
      detailMapEntry = detailMap.get(assignee);
    } else {
      detailMapEntry = new ArrayList<IssueTree>();
      detailMap.put(assignee, detailMapEntry);
    }
    detailMapEntry.add(detail);

    if (assigneeToAllocatedUsers.containsKey(detail.getRawAssigneeKey())) {
      if (!assigneeToAllocatedUsers
          .get(detail.getRawAssigneeKey())
          .equals(assignee)) {
        throw new IllegalStateException
            ("Contact a Jira developer to fix this issue."
                + "  Each user & team combo should only have one bucket for hours, but "
                + detail.getKey()
                + " with user " + detail.getRawAssignedPerson()
                + " and team " + detail.getRawAssignedTeamId()
                + " results in 2: "
                + assigneeToAllocatedUsers.get(detail.getRawAssigneeKey())
                + " and " + assignee);
      }
    } else {
      assigneeToAllocatedUsers.put(detail.getRawAssigneeKey(), assignee);
    }

  }

  /**
   Translate team info to input good for scheduling.
   Won't insert for same hours in an immediately succeeding time slice.
   */
  public static Map<Teams.UserTimeKey,SortedSet<TimeSchedule.HoursForTimeSpan>> teamToUserHours
  (Map<Teams.UserTimeKey,List<TeamHours>> allUserTeamHours) {

    Map<Teams.UserTimeKey,SortedSet<TimeSchedule.HoursForTimeSpan>> allUserSpanHours =
        new HashMap<Teams.UserTimeKey, SortedSet<TimeSchedule.HoursForTimeSpan>>();
    for (Iterator<Teams.UserTimeKey> users = allUserTeamHours.keySet().iterator();
         users.hasNext(); ) {
      Teams.UserTimeKey teamUser = users.next();

      SortedSet<TimeSchedule.HoursForTimeSpan> sortedHours =
          new TreeSet<TimeSchedule.HoursForTimeSpan>();
      List<TeamHours> teamHours = allUserTeamHours.get(teamUser);
      int prevHours = -1;
      for (int i = 0; i < teamHours.size(); i++) {
        TeamHours hours = teamHours.get(i);
        // only add this if it's a different weekly-hour rate
        if (hours.getHoursAvailable() != prevHours) {
          sortedHours
              .add
                  (new TimeSchedule.HoursForTimeSpanOriginal
                      (hours.getStartOfWeek(),
                          hours.getHoursAvailable()));
        }
        prevHours = hours.getHoursAvailable().intValue();
      }
      allUserSpanHours.put(teamUser, sortedHours);
    }
    return allUserSpanHours;
  }

  /**
   Translate team info to working data for scheduling.
   */
  public static Map<Teams.UserTimeKey,TimeSchedule.WeeklyWorkHours> weeklyHoursToRange
  (Map<Teams.UserTimeKey,SortedSet<TimeSchedule.HoursForTimeSpan>> usersWeeklyHours) {

    Map<Teams.UserTimeKey,TimeSchedule.WeeklyWorkHours> rangesForUsers =
        new TreeMap<Teams.UserTimeKey,TimeSchedule.WeeklyWorkHours>();

    for (Iterator<Teams.UserTimeKey> users = usersWeeklyHours.keySet().iterator();
         users.hasNext(); ) {
      Teams.UserTimeKey user = users.next();
      TimeSchedule.WeeklyWorkHours range = new TimeSchedule.WeeklyWorkHours();
      rangesForUsers.put(user, range);
      for (Iterator<TimeSchedule.HoursForTimeSpan> hoursIter =
           usersWeeklyHours.get(user).iterator();
           hoursIter.hasNext(); ) {
        TimeSchedule.HoursForTimeSpan hours = hoursIter.next();
        if (range.isEmpty()
            || range.endingHours() != hours.getHoursAvailable()) {
          range.inject(hours.getStartOfTimeSpan(),
              new Double(hours.getHoursAvailable()));
        }
      }
    }
    return rangesForUsers;

  }

  public static Map<String,TimeSchedule.WeeklyWorkHours> createMapFromUserTimeKeyStringToWeeklyHours
      (Map<Teams.UserTimeKey,TimeSchedule.WeeklyWorkHours> origHours) {

    Map<String,TimeSchedule.WeeklyWorkHours> newHours = new HashMap<String, TimeSchedule.WeeklyWorkHours>();
    for (Teams.UserTimeKey userTimeKey : origHours.keySet()) {
      newHours.put(userTimeKey.toString(), origHours.get(userTimeKey));
    }
    return newHours;
  }

  public static Map<String,List<IssueTree>> createMapFromAssigneeKeyStringToUserIssues
      (Map<Teams.UserTimeKey,List<IssueTree>> origIssues) {

    Map<String,List<IssueTree>> newIssues = new HashMap<String,List<IssueTree>>();
    for (Teams.UserTimeKey assigneeKey : origIssues.keySet()) {
      newIssues.put(assigneeKey.toString(), origIssues.get(assigneeKey));
    }
    return newIssues;
  }

  public static Map<Teams.AssigneeKey,List<IssueTree>> createUserDetails(IssueTree[] issues) {

    Map<Teams.AssigneeKey,List<IssueTree>> userDetails = new HashMap();
    for (int i = 0; i < issues.length; i++) {
      addIssue(issues[i], userDetails);
    }
    return userDetails;
  }

  /**
   Add issue to userDetails.
   */
  private static void addIssue
  (IssueTree issue,
   Map<Teams.AssigneeKey,List<IssueTree>> userDetails) {

    Teams.AssigneeKey assignee = issue.getRawAssigneeKey();
    List<IssueTree> userIssues = userDetails.get(assignee);
    if (userIssues == null) {
      userIssues = new ArrayList<IssueTree>();
      userDetails.put(assignee, userIssues);
    }
    userIssues.add(issue);
  }

}

