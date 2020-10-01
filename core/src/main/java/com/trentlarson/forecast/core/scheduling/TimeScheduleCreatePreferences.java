package com.trentlarson.forecast.core.scheduling;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class TimeScheduleCreatePreferences {
  public final Calendar startCal;
  public final double timeMultiplier;
  /**
   * The work hours per week that is assumed for assignee if not explicitly given.
   * If not given, the default is TimeSchedule.TYPICAL_INDIVIDUAL_WORKHOURS_PER_WORKWEEK
   * @see TimeSchedule.TYPICAL_INDIVIDUAL_WORKHOURS_PER_WORKWEEK
   */
  public final double defaultAssigneeHoursPerWeek;
  /**
   * Makes the priority order descending (9 high) instead of ascending (9 low)
   */
  public final boolean reversePriority;

  public TimeScheduleCreatePreferences(
      Calendar startCal_, Double timeMultiplier_, Double defaultAssigneeHoursPerWeek_,
      Boolean reversePriority_) {
    this.startCal = startCal_ != null ? (Calendar) startCal_.clone() : new GregorianCalendar();
    adjustCalToMidnight();
    this.timeMultiplier =
        timeMultiplier_ != null ? timeMultiplier_ : 1;
    this.defaultAssigneeHoursPerWeek =
        defaultAssigneeHoursPerWeek_ != null
            ? defaultAssigneeHoursPerWeek_
            : TimeSchedule.TYPICAL_INDIVIDUAL_WORKHOURS_PER_WORKWEEK;
    this.reversePriority =
        reversePriority_ != null ? reversePriority_ : false;
  }
  /**
     Note that this will adjust the time to midnight (morning).
   */
  public TimeScheduleCreatePreferences(Calendar startCal_, double timeMultiplier_) {
    this.startCal = (Calendar) startCal_.clone();
    adjustCalToMidnight();
    this.defaultAssigneeHoursPerWeek = TimeSchedule.TYPICAL_INDIVIDUAL_WORKHOURS_PER_WORKWEEK;
    this.timeMultiplier = timeMultiplier_;
    this.reversePriority = false;
  }
  /**
     Note that this will adjust the time to midnight (morning).
   */
  public TimeScheduleCreatePreferences(Date startTime, double timeMultiplier_) {
    this.startCal = new GregorianCalendar();
    startCal.setTime(startTime);
    adjustCalToMidnight();
    this.defaultAssigneeHoursPerWeek = TimeSchedule.TYPICAL_INDIVIDUAL_WORKHOURS_PER_WORKWEEK;
    this.timeMultiplier = timeMultiplier_;
    this.reversePriority = false;
  }
  /**
     Create with date of this morning at midnight.
   */
  public TimeScheduleCreatePreferences(double timeMultiplier_) {
    this.startCal = new GregorianCalendar();
    adjustCalToMidnight();
    this.defaultAssigneeHoursPerWeek = TimeSchedule.TYPICAL_INDIVIDUAL_WORKHOURS_PER_WORKWEEK;
    this.timeMultiplier = timeMultiplier_;
    this.reversePriority = false;
  }
  /**
   Note that this will adjust the time to midnight (morning).
   */
  public TimeScheduleCreatePreferences(Date startTime, boolean reversePriority_) {
    this.startCal = new GregorianCalendar();
    startCal.setTime(startTime);
    adjustCalToMidnight();
    this.defaultAssigneeHoursPerWeek = TimeSchedule.TYPICAL_INDIVIDUAL_WORKHOURS_PER_WORKWEEK;
    this.timeMultiplier = 1;
    this.reversePriority = reversePriority_;
  }
  public TimeScheduleCreatePreferences() {
    this(null, null, null, null);
  }
  private void adjustCalToMidnight() {
    this.startCal.set(Calendar.HOUR_OF_DAY, 0);
    this.startCal.set(Calendar.MINUTE, 0);
    this.startCal.set(Calendar.SECOND, 0);
    this.startCal.set(Calendar.MILLISECOND, 0);
  }
  public Date getStartTime() {
    return startCal.getTime();
  }

  public double getTimeMultiplier() {
    return timeMultiplier;
  }
  public double getDefaultAssigneeHoursPerWeek() { return defaultAssigneeHoursPerWeek; }
  public boolean getReversePriority() {
    return reversePriority;
  }

  public boolean equals(Object obj) {
    return timeMultiplier == ((TimeScheduleCreatePreferences) obj).getTimeMultiplier();
  }

  
  /**
   * For our serializing convenience, so we don't muck up the enclosing class with these variabilities.
   */
  public static class Pojo {
    public Calendar startCal;
    public Date startDate;
    public double timeMultiplier;
    public Calendar getStartCal() {
      return startCal;
    }
    public void setStartCal(Calendar startCal) {
      this.startCal = startCal;
    }
    public Date getStartDate() {
      return startDate;
    }
    public void setStartDate(Date startDate) {
      this.startDate = startDate;
    }
    public double getTimeMultiplier() {
      return timeMultiplier;
    }
    public void setTimeMultiplier(double timeMultiplier) {
      this.timeMultiplier = timeMultiplier;
    }
    public TimeScheduleCreatePreferences getPrefs() {
      if (startCal != null) {
        return new TimeScheduleCreatePreferences(startCal, timeMultiplier);
      } else if (startDate != null) {
        return new TimeScheduleCreatePreferences(startDate, timeMultiplier);
      } else {
        return new TimeScheduleCreatePreferences(timeMultiplier);
      }
    }
  }
  
}
