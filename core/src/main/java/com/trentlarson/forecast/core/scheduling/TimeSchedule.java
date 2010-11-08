package com.trentlarson.forecast.core.scheduling;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Category;

public class TimeSchedule {


  public static final double MAX_WORKHOURS_PER_WORKDAY = 8.0;
  public static final int WORKDAYS_PER_WEEK = 5;
  public static final double TYPICAL_INDIVIDUAL_WORKHOURS_PER_WORKWEEK =
    WORKDAYS_PER_WEEK * MAX_WORKHOURS_PER_WORKDAY;
  public static final int FIRST_HOUR_OF_WORKDAY = 8;

  private static final java.text.SimpleDateFormat WEEKDAY_DATE_TIME = new java.text.SimpleDateFormat("EEE MMM dd hh:mm a");
  private static final java.text.SimpleDateFormat WEEKDAY_DATE = new java.text.SimpleDateFormat("EEE MMM dd");
  private static final java.text.SimpleDateFormat SLASH_DATE = new java.text.SimpleDateFormat("yyyy/MM/dd");
  // this has no spaces so table formatting in HTML wraps consistently (ug)
  private static final java.text.SimpleDateFormat SLASH_TIME = new java.text.SimpleDateFormat("yyyy.MM.dd.HH:mm");


  // OK, I'd like to shorten these strings (eg. to just "scheduling.Time"),
  // but then the Jira page to set logging levels doesn't work. Any ideas? TNL
  protected static final Category log4jLog = Category.getInstance("com.trentlarson.forecast.core.scheduling.TimeSchedule");
  protected static final Category fnebLog  = Category.getInstance("com.trentlarson.forecast.core.scheduling.TimeSchedule.-FNEB--"); // findNextEstBegin
  protected static final Category fdiwwLog = Category.getInstance("com.trentlarson.forecast.core.scheduling.TimeSchedule.-FDIWW-"); // futureDateInWorkWeek
  protected static final Category iaaLog   = Category.getInstance("com.trentlarson.forecast.core.scheduling.TimeSchedule.--IAA--"); // injectAndAdjust
  protected static final Category wsbLog   = Category.getInstance("com.trentlarson.forecast.core.scheduling.TimeSchedule.--WSB--"); // workSecondsBetween




  /**
     Tracks a number of work hours per week, which can be stored and
     retrieved by any point in time.
   */
  public static class WeeklyWorkHours {
    /**
       Map from start dates for each working hour rate
       to work hours available per day
       Note: it's not just a SortedMap because I need the Cloneable interface
    */
    private TreeMap<Date,Double> ranges = new TreeMap<Date,Double>();

    /** for testing */
    protected Object clone() {
      WeeklyWorkHours result = new WeeklyWorkHours();
      result.ranges = (TreeMap<Date,Double>) ranges.clone();
      return result;
    }
    /** for testing */
    protected int size() {
      return ranges.size();
    }

    public String toString() {
      return "WeeklyWorkHours " + ranges.toString();
    }
    public String toShortString() {
      StringBuilder sb = new StringBuilder();
      for (Iterator<Date> keyIter = ranges.keySet().iterator();
           keyIter.hasNext(); ) {
        Date key = keyIter.next();
        sb.append(SLASH_TIME.format(key) + "=" + ranges.get(key));
        if (keyIter.hasNext()) {
          sb.append(", ");
        }
      }
      return sb.toString();
    }
    public boolean isEmpty() {
      return ranges.size() == 0;
    }

    public Double endingHours() {
      if (ranges.size() == 0) {
        throw new IllegalStateException("Cannot access time from empty range.");
      } else {
        return ranges.get(ranges.lastKey());
      }
    }

    /**
       @return weekly hours available starting on this date
       @throws IllegalStateException if the date is before all available times
     */
    public Double retrieve(Date time) {
      if (ranges.size() == 0) {
        throw new IllegalStateException("Cannot access time " + time + " from empty range.");
      } else if (time.before(ranges.firstKey())) {
        throw new IllegalStateException("Cannot access time " + time + " from range starting " + ranges.firstKey());
      }
      if (ranges.containsKey(time)) {
        return ranges.get(time);
      } else {
        return ranges.get(ranges.headMap(time).lastKey());
      }
    }
    /**
       Simply adds this info as a record; does no sanity checking.
       If the time already exists, this value will replace it.
       This info is cloned so that modifications won't affect references.
     */
    // THIS SHOULD BE PRIVATE!  Use injectAndAdjust instead... but
    // maybe a new method that doesn't take an end time?
    protected void inject(Date startTime, Double hours) {
      ranges.put((Date)startTime.clone(), hours);
    }

  /**
     @param start time to start working (not adjusted by FIRST_HOUR_OF_WORKDAY)
     @return time when a different rate of weekly hours is available,
     or null if this rate is available forever.  More precisely:
     <ol>

     <li> return the next time a rate that is higher or lower is
     available (always the next time slice) -- unless the available
     rate for the given week is the maximum (MAX_WORKHOURS_PER_WORKDAY
     * WORKDAYS_PER_WEEK) or higher, in which case return the next
     time there is a rate that is lower (because it doesn't matter if
     they're higher)

     <li> if no such rates exist, return null

     </ol>

     @throws IllegalStateException if the date is before all available times
   */
  public Date nextRateChange(Date start) {
    Double startRate = retrieve(start);
    SortedMap<Date,Double> more = ranges.tailMap(start);
    Iterator<Date> rateIter = more.keySet().iterator();
    Date finalDate = null;
    while (rateIter.hasNext()) {
      finalDate = rateIter.next();
      double hours = retrieve(finalDate);
      // check if this rate is significantly different
      if (hours != startRate) {
        // check to see if it matters
        if (startRate < MAX_WORKHOURS_PER_WORKDAY * WORKDAYS_PER_WEEK
            || hours < MAX_WORKHOURS_PER_WORKDAY * WORKDAYS_PER_WEEK) {
          break;
        }
      }
      finalDate = null;
    }
    return finalDate;
  }
  

    /**
       Adjust all affected records by subtracting 'weeklyHourRateDiff'
       hours.  Note that this assumes that there are at least
       weeklyHourRateDiff hours in the entire range from startTime to
       endTime, and throws an exception if not.
       @throws IllegalStateException if either date is before all available times
       @throws IllegalStateException if any record would be < 0
     */
    public void injectAndAdjust(Date startTime, Date endTime,
                                double weeklyHourRateDiff) {
      if (weeklyHourRateDiff == 0.0) {
        return; // for efficiency
      }

      double startHours = retrieve(startTime);
      // sanity check
      if (startHours - weeklyHourRateDiff < 0) {
        throw new IllegalStateException("Cannot set negative hours of " + startHours + " - " + weeklyHourRateDiff + " to range of " + startTime + " thru " + endTime + ".  Ranges are: " + toShortString());
      }
      if (endTime.before(startTime)) {
        throw new IllegalStateException("Cannot adjust invalid range of " + startTime + " thru " + endTime + ".  Ranges are: " + toShortString());
      }

      // check previous
      double prevHoursNow = -1.0;

      SortedMap<Date,Double> prevMap = ranges.headMap(startTime);
      if (prevMap.size() > 0) {
        prevHoursNow = ranges.get(prevMap.lastKey());
      }
      // -- if same, remove this record
      if (prevHoursNow == startHours - weeklyHourRateDiff) {
        if (iaaLog.isDebugEnabled()) {
          iaaLog.debug("removing " + startTime);
        }        
        ranges.remove(startTime);
      } else { // -- if different, create/update this record
        if (iaaLog.isDebugEnabled()) {
          iaaLog.debug("creating " + startTime + " with "
                         + (startHours - weeklyHourRateDiff));
        }
        ranges.put(startTime, startHours - weeklyHourRateDiff);
      }

      // keep track because we may have to restore these hours later
      prevHoursNow = startHours - weeklyHourRateDiff;
      double prevHoursBeforeChange = startHours;

      // look for next time to adjust (either next rate or end)
      Date nextChange = null;
      Iterator<Date> moreIter = ranges.tailMap(startTime).keySet().iterator();
      if (moreIter.hasNext()) {
        nextChange = moreIter.next();
        // if the first one is the start time, skip it
        if (nextChange.equals(startTime)) {
          if (moreIter.hasNext()) {
            nextChange = moreIter.next();
          } else {
            nextChange = null;
          }
        }

        // repeat that while there are more rates and we're before the end time
        while (nextChange != null
               && nextChange.before(endTime)) {
          double nextRate = ranges.get(nextChange);
          if (nextRate - weeklyHourRateDiff < 0) {
            throw new IllegalStateException("At " + nextChange + ", new hours of " + nextRate + " - " + weeklyHourRateDiff + " is a negative time.  (Working from " + startTime + " to " + endTime + ".)  Ranges are: " + toShortString());
          }
          if (iaaLog.isDebugEnabled()) {
            iaaLog.debug("creating next " + nextChange + " with "
                           + (nextRate - weeklyHourRateDiff));
          }
          ranges.put(nextChange, nextRate - weeklyHourRateDiff);
          prevHoursNow = nextRate - weeklyHourRateDiff;
          prevHoursBeforeChange = nextRate;
          if (moreIter.hasNext()) {
            nextChange = moreIter.next();
          } else {
            nextChange = null;
          }
        }
      }

      // adjust the end time
      // if it already exists in our ranges...
      if (ranges.containsKey(endTime)) {
        // ... check whether it needs to be erased (if same as previous rate)
        if (ranges.get(endTime) == prevHoursNow) {
          if (iaaLog.isDebugEnabled()) {
            iaaLog.debug("removing last " + endTime + " with "
                           + prevHoursBeforeChange);
          }
          ranges.remove(endTime);
        }
      } else {
        // ... else it doesn't already exist, so we have to make one
        if (iaaLog.isDebugEnabled()) {
          iaaLog.debug("creating last " + endTime + " with "
                         + prevHoursBeforeChange);
        }
        ranges.put(endTime, prevHoursBeforeChange);
      }

      if (iaaLog.isDebugEnabled()) {
        iaaLog.debug("ranges now: " + toShortString());
      }
    }
  }















  private static class WorkedHoursAndRates {
    protected final Date start, end;
    protected final double numHours, hoursPerDay;
    protected WorkedHoursAndRates(Date start_, Date end_,
                                  double numHours_, double hoursPerDay_) {
      this.start = start_;
      this.end = end_;
      this.numHours = numHours_;
      this.hoursPerDay = hoursPerDay_;
    }
  }


  public static class IssueSchedule implements Comparable<IssueSchedule> {
    private final IssueWorkDetail issue;
    private final Date beginDate; // date work on this task should begin, NOT adjusted by workday hours
    private final Date endDate; // date work on this task should complete, NOT adjusted by workday hours
    private final Calendar nextEstBegin; // date the next task can begin, NOT adjusted by workday hours
    private final List<IssueSchedule> splitAroundOthers;
    private final List<WorkedHoursAndRates> hoursWorked;
    public IssueSchedule
      (IssueWorkDetail issue_, Date beginDate_, Date endDate_,
       Calendar nextEstBegin_, List<IssueSchedule> splitAroundOthers_,
       List<WorkedHoursAndRates> hoursWorked_) {

      this.issue = issue_;
      this.beginDate = beginDate_;
      this.endDate = endDate_;
      this.nextEstBegin = nextEstBegin_;
      this.splitAroundOthers = splitAroundOthers_;
      this.hoursWorked = hoursWorked_;
    }
    public int compareTo(IssueSchedule object) {
      return getBeginDate().compareTo((object).getBeginDate());
    }
    public String toString() {
      return "IssueSchedule " + issue.getKey();
    }
    public StringBuilder getHoursWorkedDescription(boolean forHtml) {
      StringBuilder sb = new StringBuilder();
      for (WorkedHoursAndRates workedRange : hoursWorked) {
        if (sb.length() > 0) {
          if (forHtml) {
            sb.append("<br>");
          } else {
            sb.append(", ");
          }
        }
        sb.append(workedRange.numHours + " hours");
        if (workedRange.numHours > 0) {
            sb.append(" (at " + workedRange.hoursPerDay + "/day)");
        } 
        sb.append(" "
                  + SLASH_TIME.format(instanceAdjustedByStartOfWorkDay(workedRange.start).getTime())
                  + "-"
                  + SLASH_TIME.format(instanceAdjustedByStartOfWorkDay(workedRange.end).getTime()));
      }
      return sb;
    }

    public IssueWorkDetail getIssue() { return issue; }
    public Date getBeginDate() { return beginDate; }
    public Calendar getBeginCal() {
      Calendar cal = new GregorianCalendar();
      cal.setTime(beginDate);
      return cal;
    }
    public Calendar getAdjustedBeginCal() {
      // REFACTOR to use instanceAdjustedByStartOfWorkDay
      Calendar beginCal = Calendar.getInstance();
      beginCal.setTime(beginDate);
      beginCal.roll(Calendar.HOUR_OF_DAY, FIRST_HOUR_OF_WORKDAY); // to calculate during day
      return beginCal;
    }
    public Date getEndDate() { return endDate; }
    public Calendar getEndCal() {
      Calendar cal = new GregorianCalendar();
      cal.setTime(endDate);
      return cal;
    }
    public Calendar getAdjustedEndCal() {
      // REFACTOR to use instanceAdjustedByStartOfWorkDay
      Calendar endCal = Calendar.getInstance();
      endCal.setTime(endDate);
      endCal.roll(Calendar.HOUR_OF_DAY, FIRST_HOUR_OF_WORKDAY); // to calculate during day
      return endCal;
    }
    public Calendar getNextEstBegin() { return nextEstBegin; }
    public Date getAdjustedNextBeginDate() {
      // REFACTOR to use instanceAdjustedByStartOfWorkDay?
      nextEstBegin.roll(Calendar.HOUR_OF_DAY, FIRST_HOUR_OF_WORKDAY); // to display during day
      Date result = nextEstBegin.getTime();
      nextEstBegin.roll(Calendar.HOUR_OF_DAY, -FIRST_HOUR_OF_WORKDAY); // to calculate from midnight
      return result;
    }
    /** @return IssueSchedule elements of items spanned by this one */
    public List<IssueSchedule> getSplitAroundOthers() {
      return splitAroundOthers;
    }
    public boolean isSplitAroundOthers() {
      return splitAroundOthers.size() > 0;
    }
    /**
       @param calStartOfDay midnight in morning of day to start checking for work to do
       @param days how many days (including weekends/holidays) to check
       @return number of seconds of work scheduled to do in that time frame
    */
    public long timeWorkedOnDays(Calendar calStartOfDay, int days, double weeklyHours) {
      Calendar calStartOfNextDay = (Calendar) calStartOfDay.clone();
      calStartOfNextDay.add(Calendar.DAY_OF_YEAR, days);
      double dailyHours = weeklyHours / WORKDAYS_PER_WEEK;
      long totalTime =
        workSecondsBetween(calStartOfDay, calStartOfNextDay, dailyHours);
      for (Iterator<IssueSchedule> schedIter = getSplitAroundOthers().iterator();
           schedIter.hasNext(); ) {
        IssueSchedule sched = schedIter.next();
        if (sched.getBeginCal().before(calStartOfNextDay)
            && sched.getEndCal().after(calStartOfDay)) {
          totalTime -=
            workSecondsBetween(sched.getBeginCal(), sched.getEndCal(), dailyHours);
        }
      }
      return totalTime;
    }
  }















  public static interface IssueWorkDetail {
    public String getKey();
    public String getTimeAssignee();
    public String getSummary();
    /** @return in seconds */
    public int getEstimate();
    /** @return in seconds */
    public int getTimeSpent();
    /** @return in seconds */
    public int totalEstimate();
    /** @return in seconds */
    public int totalTimeSpent();
    public Date getDueDate();
    public Date getMustStartOnDate();
    public int getPriority();
    public boolean getResolved();

    /** @return issues that "must be done before" this issue */
    public <T extends IssueWorkDetail> Set<T> getPrecursors();
    /** @return issues to schedule before this issue (subtasks and precursors) */
    public <T extends IssueWorkDetail> Set<T> getIssuesToScheduleFirst();
  }




  protected static class IssueWorkDetailOriginal implements IssueWorkDetail {
    // REFACTOR the key to be null by default
    protected String key = "", timeAssignee = null, summary = "";
    protected int issueEstSecondsRaw = 0;
    protected int priority = 0;
    protected Date dueDate = null, mustStartOnDate = null;
    // issues that "must be done before" this one
    protected final SortedSet<IssueWorkDetail> precursors = new TreeSet<IssueWorkDetail>();
    // issues that "are part of" this one
    protected final Set<IssueWorkDetail> subtasks = new TreeSet<IssueWorkDetail>();

    /**
       @param issueEstSecondsRaw_ estimate in seconds
     */
    public IssueWorkDetailOriginal
        (String key_, String timeAssignee_, String summary_,
         int issueEstSecondsRaw_, Date dueDate_, Date mustStartOnDate_,
         int priority_) {
      this.key = key_;
      this.timeAssignee = timeAssignee_;
      this.summary = summary_;
      this.issueEstSecondsRaw = issueEstSecondsRaw_;
      this.dueDate = dueDate_;
      this.mustStartOnDate = mustStartOnDate_;
      this.priority = priority_;
    }

    public String getKey() { return key; }
    public String getTimeAssignee() { return timeAssignee; }
    public String getSummary() { return summary; }
    /** estimated seconds remaining */
    public int getEstimate() { return issueEstSecondsRaw; }
    public int getTimeSpent() { return 0; }
    public int totalEstimate() { return issueEstSecondsRaw; }
    public int totalTimeSpent() { return 0; }
    public Date getDueDate() { return dueDate; }
    public Date getMustStartOnDate() { return mustStartOnDate; }
    public int getPriority() { return priority; }
    public boolean getResolved() { return false; }
    public Set<IssueWorkDetail> getPrecursors() { return precursors; }
    public Set<IssueWorkDetail> getSubtasks() { return subtasks; }
    public Set<IssueWorkDetail> getIssuesToScheduleFirst() {
      Set<IssueWorkDetail> result = new TreeSet<IssueWorkDetail>();
      result.addAll(precursors);
      result.addAll(subtasks);
      return result;
    }
    public String toString() {
      return "IssueWorkDetailOriginal " + getKey();
    }
  }



  /**
     Try to deprecate this since WeeklyWorkHours is used most everywhere.

     Used for input to scheduling, in a list telling what is
     available.  Implementations must use the getStartOfTimeSpan for
     Comparable interface (and 'equals' method).
   */
  public static interface HoursForTimeSpan extends Comparable<HoursForTimeSpan> {
    public Date getStartOfTimeSpan();
    public double getHoursAvailable();
  }

  public static class HoursForTimeSpanOriginal implements HoursForTimeSpan {
    Date startOfTimeSpan;
    double hoursAvailable;
    public HoursForTimeSpanOriginal(Date start, double hours) {
      if (start == null) {
        throw new IllegalStateException("Cannot supply work with null date.");
      }
      startOfTimeSpan = start;
      hoursAvailable = hours;
    }
    public Date getStartOfTimeSpan() {
      return startOfTimeSpan;
    }
    public double getHoursAvailable() {
      return hoursAvailable;
    }
    public int compareTo(HoursForTimeSpan o) {
      return getStartOfTimeSpan()
        .compareTo((o).getStartOfTimeSpan());
    }
    public boolean equals(Object o) {
      if (o instanceof HoursForTimeSpan) {
        return compareTo((HoursForTimeSpan) o) == 0;
      } else {
        return false;
      }
    }
    public String toString() {
      String[] split = SLASH_DATE.format(startOfTimeSpan).split("/");
      return
        "Span " + split[1] + "/" + split[2] + "=" + hoursAvailable;
    }
  }




  /**
     sorts by start date, then priority, then due date
   */
  public static class DetailPriorityComparator implements Comparator<IssueWorkDetail> {
    public int compare(IssueWorkDetail d1, IssueWorkDetail d2) {
      // first determinant is whether there's a start date on it
      if (d1.getMustStartOnDate() == null && d2.getMustStartOnDate() != null) {
        return 1;
      } else if (d1.getMustStartOnDate() != null && d2.getMustStartOnDate() == null) {
        return -1;
      } else {
        int dateCompare = 0;
        if (d1.getMustStartOnDate() != null && d2.getMustStartOnDate() != null) {
          dateCompare = d1.getMustStartOnDate().compareTo(d2.getMustStartOnDate());
        }
        if (dateCompare != 0) {
          return dateCompare;
        } else {
          // next determinant is the priority
          if (d1.getPriority() != d2.getPriority()) {
            // lower priority comes earlier
            return 10 * (d1.getPriority() - d2.getPriority());
          // final determinant is the due-date
          } else if (d1.getDueDate() == null && d2.getDueDate() != null) {
            return 100;
          } else if (d1.getDueDate() != null && d2.getDueDate() == null) {
            return -100;
          } else {
            dateCompare = 0;
            if (d1.getDueDate() != null && d2.getDueDate() != null) {
              dateCompare = d1.getDueDate().compareTo(d2.getDueDate());
            }
            if (dateCompare != 0) {
              return dateCompare;
            } else {
              return d2.getKey().compareTo(d2.getKey());
            }
          }
        }
      }
    }
  }









  protected static Calendar instanceAdjustedByStartOfWorkDay(Calendar cal) {
    Calendar result = (Calendar) cal.clone();
    result.roll(Calendar.HOUR_OF_DAY, FIRST_HOUR_OF_WORKDAY);
    return result;
  }

  protected static Date instanceAdjustedByStartOfWorkDay(Date date) {
    Calendar resultCal = Calendar.getInstance();
    resultCal.setTime(date);
    return instanceAdjustedByStartOfWorkDay(resultCal).getTime();
  }


  private static Calendar instanceAtDayStart(Calendar thisCal) {
    Calendar newCal = (Calendar) thisCal.clone();
    newCal.set(Calendar.HOUR_OF_DAY, 0);
    newCal.set(Calendar.MINUTE, 0);
    newCal.set(Calendar.SECOND, 0);
    newCal.set(Calendar.MILLISECOND, 0);
    return newCal;
  }


  /** @return number of days into the work week by midnight this day's morning */
  private static int daysIntoWorkWeek(Calendar someday) {
    switch (someday.get(Calendar.DAY_OF_WEEK)) {
    case Calendar.SUNDAY:    return 0;
    case Calendar.MONDAY:    return 0;
    case Calendar.TUESDAY:   return 1;
    case Calendar.WEDNESDAY: return 2;
    case Calendar.THURSDAY:  return 3;
    case Calendar.FRIDAY:    return 4;
    // FIX this to throw the exception and still work on our data (test for it?)
    case Calendar.SATURDAY:  return 5;
//    case Calendar.SATURDAY:  throw new IllegalStateException("The previous date shouldn't ever be a non-weekday " + someday.get(Calendar.DAY_OF_WEEK));
    default:                 throw new IllegalStateException("The previous date shouldn't ever be a non-weekday " + someday.get(Calendar.DAY_OF_WEEK));
    }
  }

  /**
     @return Calendar for the first time after given date that falls in work-week
  */
  private static Calendar dateInWorkWeek(Date date, double dailyHours) {
    // shift the start time to be within the work week
    Calendar nextEstBegin = new GregorianCalendar();
    nextEstBegin.setTime(date);
    {
      if (nextEstBegin.get(Calendar.HOUR_OF_DAY) < FIRST_HOUR_OF_WORKDAY) {
        nextEstBegin.set(Calendar.HOUR_OF_DAY, 0);
        nextEstBegin.set(Calendar.MINUTE, 0);
      } else {
        int hour_into_workday =
          nextEstBegin.get(Calendar.HOUR_OF_DAY) - FIRST_HOUR_OF_WORKDAY;
        if (hour_into_workday < dailyHours) {
          nextEstBegin.set(Calendar.HOUR_OF_DAY, hour_into_workday);
        } else {
          nextEstBegin.add(Calendar.DAY_OF_WEEK, 1);
          nextEstBegin.set(Calendar.HOUR_OF_DAY, 0);
          nextEstBegin.set(Calendar.MINUTE, 0);
        }
      }
      nextEstBegin.set(Calendar.SECOND, 0);
      nextEstBegin.set(Calendar.MILLISECOND, 0);
      if (nextEstBegin.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY) {
        nextEstBegin.add(Calendar.DAY_OF_WEEK, 2);
      } else if (nextEstBegin.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
        nextEstBegin.add(Calendar.DAY_OF_WEEK, 1);
      }
    }
    return nextEstBegin;
  }



  /**
     @return number of seconds of work time between two dates, where
     startCal comes before endCal and both are NOT adjusted by
     FIRST_HOUR_OF_WORKDAY
  */
  private static long workSecondsBetween(Calendar startCal, Calendar endCal,
                                         double dailyHours) {

    if (dailyHours == 0.0) {
      return 0; // just for efficiency
    }

    // get the full time between the dates
    long millisecondsBetween =
      endCal.getTime().getTime() - startCal.getTime().getTime();
    long fullTimeSecondsBetween = millisecondsBetween / 1000;

    // if daylight-savings is in the middle, we'll have to adjust
    // (since our calculations are based on full-day increments)
    boolean startInDaylight =
      startCal.getTimeZone().inDaylightTime(startCal.getTime());
    boolean endInDaylight =
      endCal.getTimeZone().inDaylightTime(endCal.getTime());
    if (startInDaylight != endInDaylight) {
      if (startInDaylight) {
        // the ending is not in daylight savings, so an hour has been added
        fullTimeSecondsBetween -= 3600;
      } else {
        // the ending is in daylight savings, so an hour has been subtracted
        fullTimeSecondsBetween += 3600;
      }
    }

    // calculate any non-work seconds in between
    long numSecondsAlreadyOnFirstDay = 
      startCal.get(Calendar.HOUR_OF_DAY) * 3600L
      + startCal.get(Calendar.MINUTE) * 60L
      + startCal.get(Calendar.SECOND);

    // -- don't count after-hours (16/day)
    long totalTimePastMidnightOnFirstMorn =
      numSecondsAlreadyOnFirstDay + fullTimeSecondsBetween;
    long fullDays = totalTimePastMidnightOnFirstMorn / (24L * 3600L);
    long fullDayNonWorkSeconds =
      (long) (fullDays * (24.0 - MAX_WORKHOURS_PER_WORKDAY) * 3600.0);

    // -- don't count work if starting or ending on weekend
    long firstDaySecsToSubtract = 0;
    if (startCal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY
        || startCal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
      // remove all the time today that is usually counted for work
      firstDaySecsToSubtract =
        (long)
        (MAX_WORKHOURS_PER_WORKDAY * 3600.0 - numSecondsAlreadyOnFirstDay);
    }

    // -- don't count weekend days
    long fullWeeks = fullDays / 7;
    long fullWeekendDays = fullWeeks * (7 - WORKDAYS_PER_WEEK);
    // -- check if we are spanning any weekend days
    if (endCal.get(Calendar.DAY_OF_WEEK) < startCal.get(Calendar.DAY_OF_WEEK)) {
      // the starting time is in one week, and the end time is in the next week
      if (startCal.get(Calendar.DAY_OF_WEEK) <= Calendar.FRIDAY) {
        fullWeekendDays += 1;
      }
      if (endCal.get(Calendar.DAY_OF_WEEK) >= Calendar.MONDAY) {
        fullWeekendDays += 1;
      }
    }
    long weekendDaySecondsBetween =
      (long) (fullWeekendDays * MAX_WORKHOURS_PER_WORKDAY * 3600.0);

    // -- don't count partial time if ending on a weekend
    long secondsOnLastDayToSubtract = 0;
    if (endCal.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY
        || endCal.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY) {
      secondsOnLastDayToSubtract =
        endCal.get(Calendar.HOUR_OF_DAY) * 3600
        + endCal.get(Calendar.MINUTE) * 60
        + endCal.get(Calendar.SECOND);
    }

    long secondsOffToSubtract =
      firstDaySecsToSubtract
      + fullDayNonWorkSeconds
      + weekendDaySecondsBetween
      + secondsOnLastDayToSubtract;

    // now we can calculate result
    // note that this is where we adjust by the actual working rate
    long result = 
      (long)
      ((fullTimeSecondsBetween - secondsOffToSubtract)
       * (dailyHours / MAX_WORKHOURS_PER_WORKDAY));

    if (wsbLog.isDebugEnabled()) {
      wsbLog.debug("total hours past morning: " + (totalTimePastMidnightOnFirstMorn / 3600.0));
      wsbLog.debug("A) first-day non-work hours: " + (firstDaySecsToSubtract / 3600.0));
      wsbLog.debug("B) everyday non-work hours between: " + (fullDayNonWorkSeconds / 3600.0));
      wsbLog.debug("C) weekend day hours between: " + (weekendDaySecondsBetween / 3600.0));
      wsbLog.debug("D) last day hours: " + (secondsOnLastDayToSubtract / 3600.0));
      wsbLog.debug("E) off hours (A+B+C+D): " + (secondsOffToSubtract / 3600.0));
      wsbLog.debug("F) full time hours: " + (fullTimeSecondsBetween / 3600.0));
      wsbLog.debug("result hours (F-E * " + (100 * dailyHours / MAX_WORKHOURS_PER_WORKDAY) + "%): " + (result / 3600.0));
    }

    return result;

  }



  private static Calendar futureDateInWorkWeek(Calendar thisEstBegin,
                                               long secondsToAdd,
                                               double dailyHoursAvailable) {

    long numSecsAlreadyOnDay = 
      thisEstBegin.get(Calendar.HOUR_OF_DAY) * 3600
      + thisEstBegin.get(Calendar.MINUTE) * 60
      + thisEstBegin.get(Calendar.SECOND);

    long timePastThisMorning = numSecsAlreadyOnDay + secondsToAdd;

    int numEstDays = 0;
    int numEstSecsOnLastDay = (int) timePastThisMorning;
    if (dailyHoursAvailable > 0.0) {
      // number of days is number of whole increments of a day size
      numEstDays =
        (int) (secondsToAdd / (dailyHoursAvailable * 3600.0));
      // last day time is remainder
      numEstSecsOnLastDay =
        (int) (secondsToAdd % (dailyHoursAvailable * 3600.0));

      // calculate percentage of last day (to set the time on the final day)
      double percentOfLastDayAlready =
        numSecsAlreadyOnDay / (MAX_WORKHOURS_PER_WORKDAY * 3600.0);
      double percentOfLastDayToAdd =
        numEstSecsOnLastDay / (dailyHoursAvailable * 3600.0);
      double totalPercentOnLastDay =
        percentOfLastDayAlready + percentOfLastDayToAdd;
      if (totalPercentOnLastDay >= 1.0) {
        numEstDays++;
        totalPercentOnLastDay -= 1.0;
      }
      numEstSecsOnLastDay =
        (int) (totalPercentOnLastDay * MAX_WORKHOURS_PER_WORKDAY * 3600.0);
    }
    if (fdiwwLog.isDebugEnabled()) {
      fdiwwLog.debug("numSecsAlreadyOnDay in hours: " + (numSecsAlreadyOnDay / 3600.0));
      fdiwwLog.debug("timePastThisMorning in hours: " + (timePastThisMorning / 3600.0));
      fdiwwLog.debug("dailyHours: " + dailyHoursAvailable);
      fdiwwLog.debug("numEstDays: " + numEstDays);
      fdiwwLog.debug("last day hours if full time:  " + ((numEstSecsOnLastDay * dailyHoursAvailable / MAX_WORKHOURS_PER_WORKDAY) / 3600.0));
      fdiwwLog.debug("numEstSecsOnLastDay in hours: " + (numEstSecsOnLastDay / 3600.0));
    }

    Calendar nextEstBegin = instanceAtDayStart(thisEstBegin);
    if (numEstDays > 0) {
      int fullWeeks =
        (daysIntoWorkWeek(thisEstBegin) + numEstDays) / WORKDAYS_PER_WEEK;
      int weekendDays = fullWeeks * (7 - WORKDAYS_PER_WEEK);
      nextEstBegin.add(Calendar.DAY_OF_WEEK, numEstDays + weekendDays);
      if (fdiwwLog.isDebugEnabled()) {
        fdiwwLog.debug("added " + thisEstBegin.getTime() + " + "
                       + numEstDays + " days + "
                       + weekendDays + " weekends"
                       + " = " + nextEstBegin.getTime());
      }
    }
    nextEstBegin.add(Calendar.SECOND, numEstSecsOnLastDay);
    if (fdiwwLog.isDebugEnabled()) {
      fdiwwLog.info("added " + thisEstBegin.getTime() + " + "
                    + (secondsToAdd / 3600.0) + " hours"
                    + " (ie. " + numEstDays + " days, "
                    + (numEstSecsOnLastDay / 3600.0) + " last-day hours)"
                    + " = " + nextEstBegin.getTime());
    }

    if (fdiwwLog.isDebugEnabled()) {
      fdiwwLog.debug("result: " + (nextEstBegin.getTime()));
    }
    return nextEstBegin;
  }



  

  private static class NextBeginAndHoursWorked {
    protected final List<WorkedHoursAndRates> hoursWorked;
    protected final Calendar nextBegin;
    protected NextBeginAndHoursWorked(Calendar nextBegin_,
                                      List<WorkedHoursAndRates> hoursWorked_) {
      this.nextBegin = nextBegin_;
      this.hoursWorked = hoursWorked_;
    }
  }


  /**
     @param thisEstBegin time to start working (not adjusted by FIRST_HOUR_OF_WORKDAY)
     @param issueEstSeconds estimate in seconds
     @param weeklyHours tells how many available each week;
     beware, because it is modified by time needed for this task
     @return the next start date based on the start date and estimate
   */
  private static NextBeginAndHoursWorked findNextEstBegin
  (Calendar thisEstBegin, int estSeconds, WeeklyWorkHours weeklyHours) {

    Calendar nextWorkChunkBegins = (Calendar) thisEstBegin.clone();
    List<WorkedHoursAndRates> hoursWorked = new ArrayList<WorkedHoursAndRates>();
    int estSecsRemaining = estSeconds;

    do {


      double totalWorkRateAvailableThisRange =
        weeklyHours.retrieve(nextWorkChunkBegins.getTime());

      double dailyHoursAvailableThisRange = 
        Math.min(totalWorkRateAvailableThisRange / WORKDAYS_PER_WEEK,
                 MAX_WORKHOURS_PER_WORKDAY);

      if (fnebLog.isDebugEnabled()) {
        fnebLog.debug("est hours remaning: " + (estSecsRemaining / 3600.0));
        fnebLog.debug("next work begins: " + (nextWorkChunkBegins.getTime()));
        fnebLog.debug("all rates: " + (weeklyHours.toShortString()));
        fnebLog.debug("rate this range: " + totalWorkRateAvailableThisRange);
        fnebLog.debug("daily rate: " + dailyHoursAvailableThisRange);
      }

      Calendar nextChangeCal = null;
      long availableSecsToNextChange = estSecsRemaining;

      Date nextChange =
        weeklyHours.nextRateChange(nextWorkChunkBegins.getTime());
      if (nextChange != null) {
        nextChangeCal = Calendar.getInstance();
        nextChangeCal.setTime(nextChange);
        if (fnebLog.isDebugEnabled()) {
          fnebLog.debug("next rate change: " + (nextChangeCal.getTime()));
        }
        availableSecsToNextChange =
          workSecondsBetween(nextWorkChunkBegins, nextChangeCal,
                             dailyHoursAvailableThisRange);
      }

      long secondsThisRange =
        Math.min(availableSecsToNextChange, estSecsRemaining);

      if (fnebLog.isDebugEnabled()) {
        fnebLog.debug("hours available: " + (availableSecsToNextChange / 3600.0));
        fnebLog.debug("hours used this range: " + (secondsThisRange / 3600.0));
      }

      Calendar endOfThisRange = nextChangeCal;
      if (secondsThisRange > 0) {
        endOfThisRange =
          futureDateInWorkWeek(nextWorkChunkBegins, secondsThisRange,
                               dailyHoursAvailableThisRange);
        weeklyHours
          .injectAndAdjust(nextWorkChunkBegins.getTime(),
                           endOfThisRange.getTime(),
                           dailyHoursAvailableThisRange * WORKDAYS_PER_WEEK);
      } else { // secondsThisRange == 0
        if (estSecsRemaining == 0.0) {
          // just set the end date to the same as the beginning
          endOfThisRange = nextWorkChunkBegins;
        }
      }

      {
        // add to the records of time worked
        Date rangeBegin = nextWorkChunkBegins.getTime();

        if (secondsThisRange == 0
            && hoursWorked.size() > 0
            && hoursWorked.get(hoursWorked.size() - 1).numHours == 0) {
          rangeBegin = hoursWorked.get(hoursWorked.size() - 1).start;
          hoursWorked.remove(hoursWorked.size() - 1);
        }
        hoursWorked.add
          (new WorkedHoursAndRates
           (instanceAdjustedByStartOfWorkDay(rangeBegin),
            instanceAdjustedByStartOfWorkDay(endOfThisRange).getTime(),
            secondsThisRange / 3600.0,
            dailyHoursAvailableThisRange));
      }


      estSecsRemaining -= secondsThisRange;
      nextWorkChunkBegins = endOfThisRange;
      
    } while (estSecsRemaining > 0);



    if (fnebLog.isDebugEnabled()) {
      fnebLog.debug("result time: " + nextWorkChunkBegins.getTime());
    }
    return new NextBeginAndHoursWorked(nextWorkChunkBegins, hoursWorked);
  }



  private static Date previousTaskEnd(Calendar taskBegin, int estSeconds) {
    // calculate task's end time
    // if it's at a day's beginning, put the previous end on the previous day
    int endOffsetMillis = 0;
    if (taskBegin.get(Calendar.HOUR_OF_DAY) == 0 // if at day start
        && taskBegin.get(Calendar.MINUTE) == 0
        && taskBegin.get(Calendar.SECOND) == 0
        && taskBegin.get(Calendar.MILLISECOND) == 0
        && estSeconds > 0) { // and there was time for this issue (lest end predate start))
      // move end date back to end of previous workday
      endOffsetMillis += 16 * 3600 * 1000;
      if (taskBegin.get(Calendar.DAY_OF_WEEK) == Calendar.MONDAY) {
        // move end date back to end of previous Friday
        endOffsetMillis += 2 * 24 * 3600 * 1000;
      }
    }

    Date result = new Date(taskBegin.getTime().getTime() - endOffsetMillis);

    fnebLog.debug("previous end: " + result);

    return result;
  }






  /**
     Loop through non-contiguous list looking for all matches so we can
     get an accurate next-begin date.
  */
  private static Calendar checkWholeContiguousList(Calendar endOfFirstContiguousBlock, List<IssueSchedule> nonContiguousSchedules) {
    boolean stillFinding;
    do {
      stillFinding = false;
      for (Iterator<IssueSchedule> issueIter = nonContiguousSchedules.iterator(); issueIter.hasNext(); ) {
        IssueSchedule schedule = issueIter.next();
        if (schedule.getBeginDate().equals(endOfFirstContiguousBlock.getTime())) {
          // it lines up with my block of time, so shift the block
          endOfFirstContiguousBlock = schedule.getNextEstBegin();
          issueIter.remove();
          stillFinding = true;
        } else if (schedule.getBeginDate().before(endOfFirstContiguousBlock.getTime())) {
          // it won't affect any issues remaining to be schedules
          issueIter.remove();
        }
      }
    } while (stillFinding);
    return endOfFirstContiguousBlock;
  }

  /**
     @param issueDetails IssueWorkDetail objects remaining to be
     scheduled
     @param schedulesForKeys maps from issue key String to the
     IssueSchedule element for it; elements are added as they are
     successfully scheduled
     @param defaultStartDate is the beginning of some day to start
     scheduling
     @return list of IssueSchedule elements in the order they were
     created
  */
  private static <T extends IssueWorkDetail> List<IssueSchedule> createIssueSchedules
    (List<T> issueDetails,
     Map<String,WeeklyWorkHours> userWeeklyHours,
     Map<String,IssueSchedule> schedulesForKeys,
     double multiplier, Date defaultStartDate) {

    // store each IssueSchedule as it is scheduled
    List<IssueSchedule> allSchedules = new ArrayList<IssueSchedule>();
    Iterator<T> detailIter = issueDetails.iterator();

    if (detailIter.hasNext()) {

      // move past all the issues already scheduled
      IssueWorkDetail currentDetail;
      boolean detailIsScheduled;
      // using MAX_WORKHOURS shouldn't make a difference since start
      // date should be specified at the beginning of the day
      Calendar endOfFirstContiguousBlock =
        dateInWorkWeek(defaultStartDate, MAX_WORKHOURS_PER_WORKDAY);
      ArrayList<IssueSchedule> nonContiguousSchedules =
        new ArrayList<IssueSchedule>();
      int count = 0;
      // Hmmm... this is going through every issue and checking it's schedule;
      // seems like this could be optimized by storing this info from previous
      // scheduling.
      do {
        currentDetail = detailIter.next();
        detailIsScheduled = schedulesForKeys.containsKey(currentDetail.getKey());
        if (detailIsScheduled) {
          count++;
          IssueSchedule schedule = schedulesForKeys.get(currentDetail.getKey());
          if (schedule.getBeginDate().equals(endOfFirstContiguousBlock.getTime())) {
            endOfFirstContiguousBlock = schedule.getNextEstBegin();
          } else if (schedule.getBeginDate().after(endOfFirstContiguousBlock.getTime())) {
            nonContiguousSchedules.add(schedule);
          }
        }
      } while (detailIter.hasNext() && detailIsScheduled);
      log4jLog.info
        ("There are " + (issueDetails.size() - count) + " left"
         + " (" + count + "/" + issueDetails.size() + " done)"
         + " for " + currentDetail.getTimeAssignee()
         + (!detailIsScheduled ? "; next is " + currentDetail.getKey() : ""));

      if (!detailIsScheduled) {

        WeeklyWorkHours weeklyHours =
          userWeeklyHours.get(currentDetail.getTimeAssignee());

        if (weeklyHours == null) {
          throw new IllegalStateException(currentDetail.getTimeAssignee() + " does not have any hours available for scheduling issues, but they have issue " + currentDetail.getKey() + " assigned to them.  Available user schedules are: " + userWeeklyHours);
        }

        // some non-contiguous issues may match the end time
        endOfFirstContiguousBlock = 
          checkWholeContiguousList(endOfFirstContiguousBlock, nonContiguousSchedules);

        // schedule as many tasks as possible
        boolean allPrecursorsScheduled = true;

        Collections.sort(nonContiguousSchedules);
        do {

          Calendar nextEstBegin = Calendar.getInstance();
          nextEstBegin.setTime(defaultStartDate);
          if (weeklyHours.retrieve(nextEstBegin.getTime()) == 0.0) {
            nextEstBegin
              .setTime(weeklyHours.nextRateChange(nextEstBegin.getTime()));
          }

          // get the starting time based on the last precursor ending time,
          // but don't proceed if any precursors or subtasks are not scheduled
          Calendar maxEndOfPrecursors = null;
          Set<IssueWorkDetail> allToDoBefore =
            currentDetail.getIssuesToScheduleFirst();
          for (Iterator<IssueWorkDetail> preIter = allToDoBefore.iterator();
               preIter.hasNext() && allPrecursorsScheduled; ) {
            IssueWorkDetail preDetail = preIter.next();
            if (!schedulesForKeys.containsKey(preDetail.getKey())
                && !preDetail.getResolved()) {
              log4jLog.info("Postponing issue " + currentDetail.getKey() + " until " + preDetail.getKey() + " is scheduled.");
              allPrecursorsScheduled = false;
            } else if (!preDetail.getResolved()) {
              IssueSchedule schedule = schedulesForKeys.get(preDetail.getKey());
              Calendar startFromPrecursor = schedule.getNextEstBegin();
              if (maxEndOfPrecursors == null
                  || startFromPrecursor.after(maxEndOfPrecursors)) {
                maxEndOfPrecursors = startFromPrecursor;
              }
            }
          }


          if (allPrecursorsScheduled) {

//            nextEstBegin = endOfFirstContiguousBlock;

            boolean isNonContiguous = false;

            if (currentDetail.getMustStartOnDate() != null
                && currentDetail.getMustStartOnDate().after(nextEstBegin.getTime())) {
              double dailyHours =
                weeklyHours.retrieve(currentDetail.getMustStartOnDate())
                / WORKDAYS_PER_WEEK;
              nextEstBegin =
                dateInWorkWeek(currentDetail.getMustStartOnDate(), dailyHours);
              isNonContiguous = true;
            }

            // move if it must be pushed later due to precursors
            if (maxEndOfPrecursors != null
                && maxEndOfPrecursors.after(nextEstBegin)) {

              nextEstBegin = maxEndOfPrecursors;
              isNonContiguous = true;
            }

            Date thisEstBegin = nextEstBegin.getTime();
            List<IssueSchedule> splitAroundOthers = new ArrayList<IssueSchedule>();
            int issueEstSeconds = (int) (currentDetail.getEstimate() * multiplier);
            NextBeginAndHoursWorked nextAndWorked =
              new NextBeginAndHoursWorked(nextEstBegin, new ArrayList<WorkedHoursAndRates>());

            if (currentDetail.getResolved()) {
              // just mark this done with the current time
            } else {
              // calculate the beginning of the next task, using any spare time

              log4jLog.debug(currentDetail.getKey() + " est * mult: "
                             + (currentDetail.getEstimate() / 3600.0) + "h * " + multiplier);
              @SuppressWarnings("unchecked")
              ArrayList<IssueSchedule> nonContigsThatMayOverlap =
                (ArrayList<IssueSchedule>) nonContiguousSchedules.clone();

              nextAndWorked =
                findNextEstBegin(nextEstBegin, issueEstSeconds, weeklyHours);
              log4jLog.debug(currentDetail.getKey() + " end: "
                             + "+" + (issueEstSeconds / 3600.0)
                             + "h = " + nextEstBegin.getTime());
              nextEstBegin = nextAndWorked.nextBegin;
              
              {
                // this section is only for displaying possible overlaps
                for (Iterator<IssueSchedule> iter = nonContigsThatMayOverlap.iterator(); iter.hasNext(); ) {
                  IssueSchedule anotherSched = iter.next();

                  if (anotherSched.getBeginDate().after(thisEstBegin)
                      && anotherSched.getBeginDate().before(nextEstBegin.getTime())) {
                    // there's another task in the way, so add its time and recalculate
                    if (!isNonContiguous) {
                      // this is building on the initial block, so overlaps are contiguous now
                      nonContiguousSchedules.remove(anotherSched);
                    }
                    splitAroundOthers.add(anotherSched);
                  }
                }
                for (int icontig = 0; icontig < splitAroundOthers.size(); icontig++) {
                  nonContigsThatMayOverlap.remove(splitAroundOthers.get(icontig));
                }
              }

            }

            // finally we can add this record
            IssueSchedule schedule =
              new IssueSchedule
              (currentDetail,
               new Date(thisEstBegin.getTime()),
               previousTaskEnd(nextEstBegin, issueEstSeconds),
               (Calendar) nextEstBegin.clone(),
               splitAroundOthers,
               nextAndWorked.hoursWorked);
            log4jLog.info(currentDetail.getKey() + " times: " 
                           + schedule.getAdjustedBeginCal().getTime()
                           + " - " + schedule.getAdjustedEndCal().getTime());
            allSchedules.add(schedule);
            schedulesForKeys.put(currentDetail.getKey(), schedule);

            // if this was put on the first contiguous block, increase that block
            if (schedule.getBeginDate().equals(endOfFirstContiguousBlock.getTime())) {
              endOfFirstContiguousBlock = schedule.getNextEstBegin();
              for (int i = 0; i < nonContiguousSchedules.size(); i++) {
                IssueSchedule anotherSched = nonContiguousSchedules.get(i);
                if (anotherSched.getBeginDate().equals(endOfFirstContiguousBlock.getTime())) {
                  endOfFirstContiguousBlock = anotherSched.getNextEstBegin();
                }
              }
            } else if (isNonContiguous) {
              nonContiguousSchedules.add(schedule);
            }

            // ensure that the next iteration starts at end of used time (or later)
//            nextEstBegin = endOfFirstContiguousBlock;
            
          }

          if (detailIter.hasNext()) {
            currentDetail = detailIter.next();
          } else {
            currentDetail = null;
          }

        } while (currentDetail != null && allPrecursorsScheduled);

      }
    }
    return allSchedules;
  }


  protected static Set<IssueWorkDetail> findAllPrecursorsForAssignee(IssueWorkDetail issue) {
    return findAllPrecursorsForAssignee(issue, issue.getTimeAssignee(), new TreeSet<IssueWorkDetail>(), new TreeSet<String>());
  }
  /**
     @param found Set of IssueTree objects, to which issues are
     added if they are precursors owned by assignee
  */
  private static Set<IssueWorkDetail> findAllPrecursorsForAssignee
  (IssueWorkDetail issue, String assignee, Set<IssueWorkDetail> found, TreeSet<String> parents) {

    // check for loops
    if (parents.contains(issue.getKey())) {
      throw new IllegalStateException("The issue " + issue.getKey() + " has a loop in it's blocking parentage: " + parents);
    }
    parents.add(issue.getKey());

    // recursively look for ones to do first
    for (IssueWorkDetail pre : issue.getIssuesToScheduleFirst()) {
      if (pre.getTimeAssignee().equals(assignee)) {
        found.add(pre);
      }
      @SuppressWarnings("unchecked")
      TreeSet<String> copyOfParents = (TreeSet<String>) parents.clone();
      findAllPrecursorsForAssignee(pre, assignee, found, copyOfParents);
    }
    return found;
  }

  protected static <T extends IssueWorkDetail> void setInitialOrdering(Map<?,List<T>> userDetails) {
    // put them in order by due date and then by priority
    for (Iterator users = userDetails.keySet().iterator(); users.hasNext(); ) {
      Collections.sort
        ((List<IssueWorkDetail>) userDetails.get(users.next()),
         new DetailPriorityComparator());
    }

    // since dependents must follow their precursors, make sure they come later
    // (possibility of infinite loop if we created a tree with a cycle; we don't)
    for (Object user : userDetails.keySet()) {
      List<T> oneUserDetails = userDetails.get(user);
      for (int pos = 0; pos < oneUserDetails.size(); pos++) {
        // -- gather the list of precursors
        T issue = oneUserDetails.get(pos);
        Set precursors = findAllPrecursorsForAssignee(issue);
        int maxPosOfPres = pos;
        for (Iterator<IssueWorkDetail> preIter = precursors.iterator(); preIter.hasNext(); ) {
          int prePos = oneUserDetails.indexOf(preIter.next());
          if (prePos > maxPosOfPres) {
            maxPosOfPres = prePos;
          }
        }
        if (maxPosOfPres > pos) {
          log4jLog.info("Due to dependency, shifting " + issue.getKey() + " from " + pos + " to " + maxPosOfPres);
          oneUserDetails.remove(pos);
          oneUserDetails.add(maxPosOfPres, issue);
          pos--;
        }
      }
    }
  }




  /**
     @param userDetails Map from username String to a List of
     IssueWorkDetail elements; it is modified to ensure that each List
     is sorted into priority order
     @param userWeeklyHours tracks the hours for each person; it will
     also be updated with the new available hours as things are
     scheduled
     @return a map from the issue key String to the IssueSchedule for
     it; plus, it has the side-effect that the userDetails Map will be
     sorted in priority order
  */
  public static <T extends IssueWorkDetail> Map<String,IssueSchedule> schedulesForUserIssues
    (Map<String,List<T>> userDetails,
     Map<String,WeeklyWorkHours> userWeeklyHours, Date startDate,
     double multiplier) {

    //log4jLog.setLevel(org.apache.log4j.Level.DEBUG);

    if (log4jLog.isDebugEnabled()) {
      log4jLog.debug("Basic scheduling info follows.");
      log4jLog.debug("userWeeklyHours: " + userWeeklyHours);
      log4jLog.debug("userDetails: " + userDetails);
    }

    setInitialOrdering(userDetails);

    // look through all the user issues, and repeat until all scheduled
    Map<String,IssueSchedule> issueSchedules =
      new HashMap<String,IssueSchedule>();
    String stillUnfinished;
    String previousUnfinishedName = null;
    int previousUnfinishedIssueNum = -1;
    int finished; // to guard against an infinite loop without progress
    do {

      do {
        stillUnfinished = null;
        finished = 0;
        for (String user : userDetails.keySet()) {
          List<T> oneUserDetails = userDetails.get(user);
          List<IssueSchedule> schedule =
            createIssueSchedules(oneUserDetails, userWeeklyHours,
                                 issueSchedules, multiplier, startDate);

          finished += schedule.size();

          // check if there are any issues still not scheduled
          if (oneUserDetails.size() > 0) {
            IssueWorkDetail lastDetail =
              oneUserDetails.get(oneUserDetails.size() - 1);
            if (!issueSchedules.containsKey(lastDetail.getKey())) {
              stillUnfinished = user;
            }
          }
        }
        log4jLog.info("Finished " + finished + " on this iteration.");

      } while (stillUnfinished != null && finished > 0);

      // The following is to try and resolve a deadlock in the last
      // person's issue.  We could do more by trying to adjust the
      // issue order for anyone who still has issues, maybe by trying
      // each at a time.

      if (stillUnfinished != null && finished == 0) {
        // deadlock in the scheduling!
        // move the precursors (to the last (problem) issue) up in their scheduling
        // -- move backward along the last person's issues to find the problem
        List<T> oneUserDetails = userDetails.get(stillUnfinished);
        int lastIssueDone;
        for (lastIssueDone = oneUserDetails.size() - 1;
             lastIssueDone > -1 // this has happened before!
             && !issueSchedules.containsKey
                   ((oneUserDetails.get(lastIssueDone)).getKey());
             lastIssueDone--) {
        }

        // -- now move that issue up in the scheduling order
        if (previousUnfinishedName != null
            && previousUnfinishedName.equals(stillUnfinished)
            && previousUnfinishedIssueNum < oneUserDetails.size()
            && previousUnfinishedIssueNum == lastIssueDone + 1) {
          // this has all the same finished info as last time!
          throw new IllegalStateException("The scheduling loop stopped making progress because we can't schedule " + oneUserDetails.get(lastIssueDone + 1));

        } else {
          // record this info to make sure we aren't looping without progress
          previousUnfinishedName = stillUnfinished;
          previousUnfinishedIssueNum = lastIssueDone + 1;

          // now grab that unfinished issue, get the precursors, and move them forward
          IssueWorkDetail unfinishedTarget = oneUserDetails.get(lastIssueDone + 1);
          shiftPrecursors(unfinishedTarget, userDetails, issueSchedules, new HashSet<String>());
        }
      }
    } while (stillUnfinished != null && finished == 0);

    return issueSchedules;
  }

  /**
     Adjust position in schedule for precursors of unfinishedTarget.
     Note that the HashSet argument was specified only because we need both Set and Cloneable.
  */
  private static <T extends IssueWorkDetail> void shiftPrecursors
    (IssueWorkDetail unfinishedTarget,
     Map<String,List<T>> userDetails,
     Map<String,IssueSchedule> issueSchedules,
     HashSet<String> visitedPrecursorKeys) {

    Set<T> precursorsToShift = unfinishedTarget.getIssuesToScheduleFirst();
    for (T precursor : precursorsToShift) {
      if (!issueSchedules.containsKey(precursor.getKey())) {

        // find this item in the assignee's list and move it up in order
        List<T> assigneeDetails = userDetails.get(precursor.getTimeAssignee());
        int firstUnscheduledPos;
        for (firstUnscheduledPos = 0;
             issueSchedules.containsKey
               ((assigneeDetails.get(firstUnscheduledPos)).getKey());
             firstUnscheduledPos++);
        log4jLog.info("To avoid deadlock, shifting " + precursor.getKey() + " from " + assigneeDetails.indexOf(precursor) + " to " + firstUnscheduledPos);
        assigneeDetails.remove(precursor);
        assigneeDetails.add(firstUnscheduledPos, precursor);
        if (visitedPrecursorKeys.add(precursor.getKey())) {
          shiftPrecursors
            (precursor, userDetails, issueSchedules,
             (HashSet<String>) visitedPrecursorKeys.clone());
        } else {
          log4jLog.warn("Warning: there's a cycle in the precursor loop for " + precursor.getKey() + "!  (Continuing anyway.)");
        }
      }
    }
  }



  /**
     @param fullSchedule contains all the IssueSchedule elements to display
  */
  public static void writeIssueSchedule
    (List<IssueSchedule> fullSchedule,
     double multiplier,
     boolean showResolved,
     java.io.Writer out)
    throws java.io.IOException {

    out.write("<table border='1'>\n");
    out.write("<tr>\n");
    out.write("<td>issue</td>\n");
    out.write("<td>assignee</td>\n");
    out.write("<td>summary</td>\n");
    out.write("<td>priority</td>\n");
    out.write("<td>start date</td>\n");
    out.write("<td>time left</td>\n");
    out.write("<td>finish date</td>\n");
    out.write("<td>due date</td>\n");
    out.write("<td>ranges of hourly work done</td>\n");
    out.write("</tr>\n");

    Iterator schedules = fullSchedule.iterator();
    while (schedules.hasNext()) {
      IssueSchedule schedule = (IssueSchedule) schedules.next();
      IssueWorkDetail detail = schedule.issue;

      if (showResolved
          || !detail.getResolved()) {

        String prefix = "", suffix = "";
        if (detail.getResolved()) {
          prefix = "<strike>";
          suffix = "</strike>";
        }

        out.write("<tr>\n");

        // key
        out.write("<td>" + prefix + "<a href='/secure/ViewIssue.jspa?key=" + detail.getKey() + "'>" + detail.getKey() + "</a>" + suffix + "</td>\n");

        // assignee
        out.write("<td>" + detail.getTimeAssignee() + "</td>\n");

        // summary
        out.write("<td>" + prefix + detail.getSummary() + suffix + "</td>\n");

        // priority
        out.write("<td>" + (detail.getPriority() - 1) + suffix + "</td>\n");

        String value;

        // start date
        out.write("<td>");
        value = WEEKDAY_DATE_TIME.format(schedule.getAdjustedBeginCal().getTime());
        if (detail.getDueDate() != null
            && schedule.endDate.after(detail.getDueDate())) {
          value = "<font color='red'>" + value + "</font>";
        }
        out.write(value);
        out.write("</td>\n");

        // remaining time
        out.write("<td>");
        out.write(prefix);
        int issueEstSeconds = (int) (detail.getEstimate() * multiplier);
        if (detail.getEstimate() == 0) { out.write("<font color='orange'>"); }
        String hours =
          String.valueOf
          ((issueEstSeconds % (MAX_WORKHOURS_PER_WORKDAY * 3600.0)) / 3600.0);
        if (hours.indexOf('.') > -1
            && hours.length() - hours.indexOf('.') > 3) {
          hours = hours.substring(0, hours.indexOf('.') + 3);
        }
        out.write
          ((int) (issueEstSeconds / (MAX_WORKHOURS_PER_WORKDAY * 3600.0)) + "d "
           + hours + "h");
        if (detail.getEstimate() == 0) { out.write("</font>"); }
        out.write(suffix);
        out.write("</td>\n");

        // finish date
        out.write("<td>");
        value = WEEKDAY_DATE_TIME.format(schedule.getAdjustedEndCal().getTime());
        if (detail.getDueDate() != null
            && schedule.endDate.after(detail.getDueDate())) {
          value = "<font color='red'>" + value + "</font>";
        }
        out.write(value);
        out.write("</td>\n");

        // due date
        out.write("<td>");
        if (detail.getDueDate() != null) {
          value = WEEKDAY_DATE.format(detail.getDueDate());
        } else {
          value = "none";
        }
        if (detail.getDueDate() != null
            && schedule.endDate.after(detail.getDueDate())) {
          value = "<font color='red'>" + value + "</font>";
        }
        out.write(value);
        out.write("</td>\n");

        // working hours
        out.write("<td>\n");
        out.write(schedule.getHoursWorkedDescription(true).toString());
        out.write("</td>\n");
      }
      out.write("</tr>\n");
    }
    out.write("</table>\n");

    out.flush();
  }






  public static void main(String[] args) throws Exception {

    //log4jLog.setLevel(org.apache.log4j.Level.DEBUG);

    java.io.PrintWriter out = null;
    try {
      out = new java.io.PrintWriter(System.out);
      if (args.length > 0) {
        out = new java.io.PrintWriter(args[0]);
      }
      outputTestResults(out);
    } finally {
      try { out.close(); } catch (Exception e) {}
    }
  }

  public static void outputTestResults(java.io.PrintWriter out) throws Exception {

    //log4jLog.setLevel(org.apache.log4j.Level.DEBUG);

    final java.text.SimpleDateFormat slashFormatter = new java.text.SimpleDateFormat("yyyy/MM/dd");
    final double multiplier = 2.0;
    final double dailyHours = MAX_WORKHOURS_PER_WORKDAY;
    final int weeklyHours = (int) (WORKDAYS_PER_WEEK * dailyHours);
    final Date startDate = SLASH_DATE.parse("2005/04/04");

    Map userDetails = new HashMap();

    List details1 = new ArrayList();
    String user = "trent--team_1";
    IssueWorkDetailOriginal test1 =
      (new IssueWorkDetailOriginal
       ("TEST-T1", user, "summary T1", 10 * 3600,
        slashFormatter.parse("2005/06/01"), null, 1));
    IssueWorkDetailOriginal test2 =
      (new IssueWorkDetailOriginal
       ("TEST-T2", user, "summary T2", 5 * 3600,
        slashFormatter.parse("2005/05/01"), null, 2));
    IssueWorkDetailOriginal test3 =
      (new IssueWorkDetailOriginal
       ("TEST-T3", user, "summary T3", 5 * 3600, null, null, 3));
    IssueWorkDetailOriginal test4 =
      (new IssueWorkDetailOriginal
       ("TEST-T4", user, "summary T4", 4 * 3600,
        slashFormatter.parse("2005/06/01"), null, 4));
    IssueWorkDetailOriginal test5 =
      (new IssueWorkDetailOriginal
       ("TEST-T5", user, "summary T5", 1 * 3600,
        slashFormatter.parse("2005/05/15"), null, 3));
    IssueWorkDetailOriginal test6 =
      (new IssueWorkDetailOriginal
       ("TEST-T6", user, "summary T6", 4 * 3600,
        slashFormatter.parse("2005/06/01"), null, 3));

    details1.add(test1);
    details1.add(test2);
    details1.add(test3);
    details1.add(test4);
    details1.add(test5);
    details1.add(test6);
    test4.getPrecursors().add(test6);

    userDetails.put(user, details1);

    List details2 = new ArrayList();
    user = "nobody--team_1";
    details2.add
      (new IssueWorkDetailOriginal
       ("TEST-n1", user, "summary n1", 4 * 3600,
        slashFormatter.parse("2005/06/01"), null, 0));

    userDetails.put(user, details2);

    setInitialOrdering(userDetails);

    out.println("<br>");
    String key1 = ((IssueWorkDetail) details1.get(1)).getKey();
    out.println((key1.equals("TEST-T2") ? "pass" : "fail")
                + " (first after simple prioritizing is TEST-T2; got " + key1 + ")");

    out.println("<br>");
    out.println((details1.indexOf(test4) > details1.indexOf(test6) ? "pass" : "fail")
                + " (a dependant issue comes after its precursor)");

    Calendar friMorn = new GregorianCalendar();
    friMorn.setTime(slashFormatter.parse("2005/04/29"));
    Calendar friEve = (Calendar) friMorn.clone();
    friEve.add(Calendar.HOUR_OF_DAY, (int) dailyHours);
    out.println("<br>");
    out.println((workSecondsBetween(friMorn, friEve, 0.0)
                 == 0.0 ? "pass" : "fail")
                + " (there are the right number of seconds with no work time;"
                + " wanted " + 0.0
                + " got " + workSecondsBetween(friMorn, friEve, 0.0) + ")");
    out.println("<br>");
    out.println((workSecondsBetween(friMorn, friEve, dailyHours)
                 == dailyHours * 3600 ? "pass" : "fail")
                + " (there are the right number of seconds in a work day;"
                + " wanted " + (dailyHours * 3600)
                + " got " + workSecondsBetween(friMorn, friEve, dailyHours) + ")");
    Calendar friNoon = (Calendar) friMorn.clone();
    friNoon.add(Calendar.HOUR_OF_DAY, (int) dailyHours / 2);
    out.println("<br>");
    out.println((workSecondsBetween(friMorn, friNoon, dailyHours / 2.0)
                 == dailyHours / 4 * 3600 ? "pass" : "fail")
                + " (there are the right number of seconds in half a half a work day;"
                + " wanted " + (dailyHours / 4 * 3600)
                + " got " + workSecondsBetween(friMorn, friNoon, dailyHours / 2.0) + ")");
out.flush();
    Calendar satEve = (Calendar) friEve.clone();
    satEve.add(Calendar.DATE, 1);
    out.println("<br>");
    out.println((workSecondsBetween(friMorn, satEve, dailyHours)
                 == dailyHours * 3600 ? "pass" : "fail")
                + " (there are the right number of seconds from Fri morn to Sat eve;"
                + " wanted " + (dailyHours * 3600)
                + " got " + workSecondsBetween(friMorn, satEve, dailyHours) + ")");
    Calendar sunMorn = (Calendar) friEve.clone();
    sunMorn.add(Calendar.DATE, 2);
    Calendar sunEve = (Calendar) satEve.clone();
    sunEve.add(Calendar.DATE, 1);
    out.println("<br>");
    out.println((workSecondsBetween(friMorn, sunEve, dailyHours)
                 == dailyHours * 3600 ? "pass" : "fail")
                + " (there are the right number of seconds from Fri morn to Sun eve;"
                + " wanted " + (dailyHours * 3600)
                + " got " + workSecondsBetween(friMorn, sunEve, dailyHours) + ")");
    Calendar monMorn = new GregorianCalendar();
    monMorn.setTime(slashFormatter.parse("2005/05/02"));
    out.println("<br>");
    out.println((workSecondsBetween(sunMorn, monMorn, dailyHours)
                 == 0 ? "pass" : "fail")
                + " (there are the right number of seconds from Sun morn to Mon morn;"
                + " wanted " + 0
                + " got " + workSecondsBetween(sunMorn, monMorn, dailyHours) + ")");
    out.println("<br>");
    out.println((workSecondsBetween(friMorn, monMorn, dailyHours)
                 == dailyHours * 3600 ? "pass" : "fail")
                + " (there are the right number of seconds from Fri morn to Mon morn;"
                + " wanted " + (dailyHours * 3600)
                + " got " + workSecondsBetween(friMorn, monMorn, dailyHours) + ")");
    Calendar monNoon = (Calendar) monMorn.clone();
    monNoon.add(Calendar.HOUR_OF_DAY, 4);
    out.println("<br>");
    out.println((workSecondsBetween(friMorn, monNoon, dailyHours)
                 == (int) (1.5 * dailyHours * 3600) ? "pass" : "fail")
                + " (there are the right number of seconds from Fri morn to Mon noon;"
                + " wanted " + ((int) (1.5 * dailyHours * 3600))
                + " got " + workSecondsBetween(friMorn, monNoon, dailyHours) + ")");
    Calendar monEve = (Calendar) monMorn.clone();
    monEve.add(Calendar.HOUR_OF_DAY, (int) dailyHours);
    out.println("<br>");
    out.println((workSecondsBetween(friMorn, monEve, dailyHours)
                 == 2 * dailyHours * 3600 ? "pass" : "fail")
                + " (there are the right number of seconds from Fri morn to Mon eve;"
                + " wanted " + (2 * dailyHours * 3600)
                + " got " + workSecondsBetween(friMorn, monEve, dailyHours) + ")");
    out.println("<br>");
    out.println((workSecondsBetween(friMorn, monNoon, dailyHours / 2.0)
                 == 1.5 * dailyHours / 2 * 3600 ? "pass" : "fail")
                + " (there are the right number of seconds from Fri morn to Mon noon"
                + " doing half time;"
                + " wanted " + (1.5 * dailyHours / 2 * 3600)
                + " got " + workSecondsBetween(friMorn, monNoon, dailyHours / 2.0) + ")");
    Calendar tuesEve = (Calendar) monEve.clone();
    tuesEve.add(Calendar.DAY_OF_WEEK, 1);
    out.println("<br>");
    out.println((workSecondsBetween(monMorn, tuesEve, dailyHours)
                 == 2 * dailyHours * 3600 ? "pass" : "fail")
                + " (there are the right number of seconds from Mon morn to Tues eve;"
                + " wanted " + (2 * dailyHours * 3600)
                + " got " + workSecondsBetween(monMorn, tuesEve, dailyHours) + ")");
    monEve.add(Calendar.WEEK_OF_YEAR, 1);
    out.println("<br>");
    out.println((workSecondsBetween(friMorn, monEve, dailyHours)
                 == 7 * dailyHours * 3600 ? "pass" : "fail")
                + " (there are the right number of seconds from Fri morn to Mon 2 eves away;"
                + " wanted " + (7 * dailyHours * 3600)
                + " got " + workSecondsBetween(friMorn, monEve, dailyHours) + ")");


    {
      //out.println("<h3>nextRateChange</h3>");

      Date first = slashFormatter.parse("2005/04/01");
      Date eighth = slashFormatter.parse("2005/04/08");
      Date fifteenth = slashFormatter.parse("2005/04/15");
      Date twentysecond = slashFormatter.parse("2005/04/22");
      Date twentyninth = slashFormatter.parse("2005/04/29");
      WeeklyWorkHours range = new WeeklyWorkHours();
      range.inject(first, 10.0);
      range.inject(eighth, 5.0);
      range.inject(fifteenth, 40.0);
      range.inject(twentysecond, 60.0);
      range.inject(twentyninth, 20.0);

      Date before = slashFormatter.parse("2005/03/31");
      out.println("<br>");
      try {
        range.nextRateChange(before);
        out.println("fail (invalid date didn't throw an exception)");
      } catch (IllegalStateException e) {
        out.println("pass (invalid date threw an exception)");
      }
      Date second = slashFormatter.parse("2005/04/02");
      out.println("<br>");
      out.println((range.nextRateChange(second).equals(eighth) ? "pass" : "fail")
                  + " (the next rate change after the 2nd is the 8th)");
      Date ninth = slashFormatter.parse("2005/04/09");
      out.println("<br>");
      out.println((range.nextRateChange(ninth).equals(fifteenth) ? "pass" : "fail")
                  + " (the next rate change after the 9th is the 15th)");
      out.println("<br>");
      out.println((range.nextRateChange(fifteenth).equals(twentyninth) ? "pass" : "fail")
                  + " (the next rate change after the 15th is the 29th)");
      out.println("<br>");
      out.println((range.nextRateChange(twentysecond).equals(twentyninth) ? "pass" : "fail")
                  + " (the next rate change after the 22nd is the 29th)");
      Date thirtieth = slashFormatter.parse("2005/04/30");
      out.println("<br>");
      out.println((range.nextRateChange(thirtieth) == null ? "pass" : "fail")
                  + " (there is no rate change after the 30th)");
    }

    {
      //out.println("<h3>injectAndAdjust pass/fails</h3>");

      Date first = slashFormatter.parse("2005/04/01");
      Date eighth = slashFormatter.parse("2005/04/08");
      Date fifteenth = slashFormatter.parse("2005/04/15");
      Date twentysecond = slashFormatter.parse("2005/04/22");
      Date twentyninth = slashFormatter.parse("2005/04/29");
      WeeklyWorkHours origRange = new WeeklyWorkHours();
      origRange.inject(first, 10.0);
      origRange.inject(eighth, 5.0);
      origRange.inject(fifteenth, 40.0);
      origRange.inject(twentysecond, 60.0);
      origRange.inject(twentyninth, 20.0);

      WeeklyWorkHours workingRange = (WeeklyWorkHours) origRange.clone();
      workingRange.injectAndAdjust(first, eighth, 8.0);
      out.println("<br>");
      out.println((workingRange.size() == 5 ? "pass" : "fail")
                  + " (adjusting the first date range doesn't affect others)");

      try {
        workingRange = (WeeklyWorkHours) origRange.clone();
        workingRange.injectAndAdjust(first, fifteenth, 6.0);
        out.println("<br>");
        out.println("fail (cannot drop any week below 0 hours)");
      } catch (IllegalStateException e) {
        out.println("<br>");
        out.println("pass (cannot drop any week below 0 hours)");
      }
    }

    {
      //out.println("<h3>findNextEstBegin</h3>");

      Date first = slashFormatter.parse("2005/04/01");
      Date fourth = slashFormatter.parse("2005/04/04");
      Date eleventh = slashFormatter.parse("2005/04/11");
      Date eighteenth = slashFormatter.parse("2005/04/18");
      Date twentyfifth = slashFormatter.parse("2005/04/25");
      Date may2 = slashFormatter.parse("2005/05/02");
      WeeklyWorkHours origRange = new WeeklyWorkHours();
      origRange.inject(fourth, 20.0);
      origRange.inject(eleventh, 5.0);
      origRange.inject(eighteenth, 40.0);
      origRange.inject(twentyfifth, 60.0);
      origRange.inject(may2, 20.0);

      WeeklyWorkHours workingRange;

      Calendar thisEstBegin = Calendar.getInstance();
      thisEstBegin.setTime(fourth);
      workingRange = (WeeklyWorkHours) origRange.clone();
      NextBeginAndHoursWorked nextAndHours = 
        findNextEstBegin(thisEstBegin, 1 * 3600, workingRange);
      Date taskEnd = previousTaskEnd(nextAndHours.nextBegin, 1 * 3600);
      int offsetMillis = (int) (nextAndHours.nextBegin.getTime().getTime() - taskEnd.getTime());
      out.println("<br>");
      out.println((offsetMillis == 0 ? "pass" : "fail")
                  + " (on the 4th, a 1-hour issue -- at 20 hours/week --"
                  + " will end immediately before the next one begins)");

      workingRange = (WeeklyWorkHours) origRange.clone();
      nextAndHours = findNextEstBegin(thisEstBegin, 16 * 3600, workingRange);
      taskEnd = previousTaskEnd(nextAndHours.nextBegin, 16 * 3600);
      Date eighth = slashFormatter.parse("2005/04/08");
      out.println("<br>");
      out.println((nextAndHours.nextBegin.getTime().equals(eighth) ? "pass" : "fail")
                  + " (on the 4th, a 16-hour issue -- at 20 hours/week --"
                  + " makes the next begin date the 8th;"
                  + " got " + nextAndHours.nextBegin.getTime() + ")");

      out.println("<br>");
      offsetMillis = (int) (nextAndHours.nextBegin.getTime().getTime() - taskEnd.getTime());
      out.println((offsetMillis == 16 * 3600 * 1000 ? "pass" : "fail")
                  + " (on the 4th, a 16-hour issue -- at 20 hours/week --"
                  + " will end the day before the next one begins;"
                  + " got " + (offsetMillis / 3600000) + " hours)");

      workingRange = (WeeklyWorkHours) origRange.clone();
      nextAndHours = findNextEstBegin(thisEstBegin, 22 * 3600, workingRange);
      Date thirteenth = slashFormatter.parse("2005/04/13");
      out.println("<br>");
      out.println((nextAndHours.nextBegin.getTime().equals(thirteenth) ? "pass" : "fail")
                  + " (on the 4th, a 22-hour issue -- at 20 then 5 hours/week --"
                  + " makes the next begin date the 13th;"
                  + " got " + nextAndHours.nextBegin.getTime() + ")");
    }


    {
      out.println("<h3>injectAndAdjust ranges</h3>");

      out.println("<table>");

      Date first = slashFormatter.parse("2005/04/01");
      Date eighth = slashFormatter.parse("2005/04/08");
      Date fifteenth = slashFormatter.parse("2005/04/15");
      Date twentysecond = slashFormatter.parse("2005/04/22");
      Date twentyninth = slashFormatter.parse("2005/04/29");
      WeeklyWorkHours origRange = new WeeklyWorkHours();
      origRange.inject(first, 10.0);
      origRange.inject(eighth, 5.0);
      origRange.inject(fifteenth, 40.0);
      origRange.inject(twentysecond, 60.0);
      origRange.inject(twentyninth, 20.0);
      out.println("  <tr>");
      out.println("    <td>original:</td>");
      out.println("    <td>" + origRange.toShortString() + "</td>");
      out.println("  </tr>");

      WeeklyWorkHours workingRange;

      workingRange = (WeeklyWorkHours) origRange.clone();
      workingRange.injectAndAdjust(first, fifteenth, 0.0);
      out.println("  <tr>");
      out.println("    <td>-0 in first 2 ranges</td>");
      out.println("    <td>" + workingRange.toShortString() + "</td>");
      out.println("  </tr>");

      workingRange = (WeeklyWorkHours) origRange.clone();
      workingRange.injectAndAdjust(first, eighth, 8.0);
      out.println("  <tr>");
      out.println("    <td>-8 in first range</td>");
      out.println("    <td>" + workingRange.toShortString() + "</td>");
      out.println("  </tr>");

      workingRange = (WeeklyWorkHours) origRange.clone();
      workingRange.injectAndAdjust(twentysecond, twentyninth, 55.0);
      workingRange.injectAndAdjust(fifteenth, twentysecond, 35.0);
      out.println("  <tr>");
      out.println("    <td>-55 in fourth range, -35 in third</td>");
      out.println("    <td>" + workingRange.toShortString() + "</td>");
      out.println("  </tr>");

      workingRange = (WeeklyWorkHours) origRange.clone();
      Date second = slashFormatter.parse("2005/04/02");
      Date sixteenth = slashFormatter.parse("2005/04/16");
      workingRange.injectAndAdjust(second, sixteenth, 4.0);
      out.println("  <tr>");
      out.println("    <td>-4 from 2nd to 16th</td>");
      out.println("    <td>" + workingRange.toShortString() + "</td>");
      out.println("  </tr>");

      workingRange = (WeeklyWorkHours) origRange.clone();
      Date tenth = slashFormatter.parse("2005/04/10");
      workingRange.injectAndAdjust(second, tenth, 5.0);
      out.println("  <tr>");
      out.println("    <td>-5 from 2nd to 10th</td>");
      out.println("    <td>" + workingRange.toShortString() + "</td>");
      out.println("  </tr>");

      workingRange = (WeeklyWorkHours) origRange.clone();
      Date twentieth = slashFormatter.parse("2005/04/20");
      workingRange.injectAndAdjust(fifteenth, twentieth, 35.0);
      out.println("  <tr>");
      out.println("    <td>-35 from 15th to 20th: </td>");
      out.println("    <td>" + workingRange.toShortString() + "</td>");
      out.println("  </tr>");

      workingRange = (WeeklyWorkHours) origRange.clone();
      workingRange.injectAndAdjust(fifteenth, twentysecond, 35.0);
      out.println("  <tr>");
      out.println("    <td>-35 from 15th to 22nd</td>");
      out.println("    <td>" + workingRange.toShortString() + "</td>");
      out.println("  </tr>");

      workingRange = (WeeklyWorkHours) origRange.clone();
      Date twenthEighth = slashFormatter.parse("2005/04/28");
      Date may2 = slashFormatter.parse("2005/05/02");
      workingRange.injectAndAdjust(twenthEighth, may2, 20.0);
      out.println("  <tr>");
      out.println("    <td>-20 from 28th to 2nd</td>");
      out.println("    <td>" + workingRange.toShortString() + "</td>");
      out.println("  </tr>");

      workingRange = (WeeklyWorkHours) origRange.clone();
      Date may22 = slashFormatter.parse("2005/05/22");
      workingRange.injectAndAdjust(may2, may22, 20.0);
      out.println("  <tr>");
      out.println("    <td>-20 from 2nd to 22nd</td>");
      out.println("    <td>" + workingRange.toShortString() + "</td>");
      out.println("  </tr>");

      out.println("</table>");

    }



    {
      out.println("<p>Showing schedule for our samples...<p>");
      List sortedDetailKeys = new ArrayList(userDetails.keySet());
      Collections.sort(sortedDetailKeys);
      for (Iterator users = sortedDetailKeys.iterator(); users.hasNext(); ) {
        Map<String,WeeklyWorkHours> userRanges =
          new HashMap<String,WeeklyWorkHours>();
        user = (String) users.next();
        WeeklyWorkHours range = new WeeklyWorkHours();
        range.inject(slashFormatter.parse("2005/04/01"), new Double(weeklyHours));
        userRanges.put(user, range);
        List schedules =
          createIssueSchedules
          ((List) userDetails.get(user), userRanges, new HashMap(), multiplier,
           slashFormatter.parse("2005/04/01"));
        writeIssueSchedule(schedules, multiplier, true, out);
      }
    }


  }
}
