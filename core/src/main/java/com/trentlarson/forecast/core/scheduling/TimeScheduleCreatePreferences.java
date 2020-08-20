package com.trentlarson.forecast.core.scheduling;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class TimeScheduleCreatePreferences {
  public final Calendar startCal;
  public final int timeWithoutEstimate;
  public final double timeMultiplier;
  /**
   * Makes the priority order descending (9 high) instead of ascending (9 low)
   */
  public final boolean reversePriority;
  /**
     Note that this will adjust the time to midnight (morning).
   */
  public TimeScheduleCreatePreferences(int timeWithoutEstimate_, Calendar startCal_, double timeMultiplier_) {
    this.startCal = (Calendar) startCal_.clone();
    adjustCalToMidnight();
    this.timeWithoutEstimate = timeWithoutEstimate_;
    this.timeMultiplier = timeMultiplier_;
    this.reversePriority = false;
  }
  /**
     Note that this will adjust the time to midnight (morning).
   */
  public TimeScheduleCreatePreferences(int timeWithoutEstimate_, Date startTime, double timeMultiplier_) {
    this.startCal = new GregorianCalendar();
    startCal.setTime(startTime);
    adjustCalToMidnight();
    this.timeWithoutEstimate = timeWithoutEstimate_;
    this.timeMultiplier = timeMultiplier_;
    this.reversePriority = false;
  }
  /**
     Create with date of this morning at midnight.
   */
  public TimeScheduleCreatePreferences(int timeWithoutEstimate_, double timeMultiplier_) {
    this.startCal = new GregorianCalendar();
    adjustCalToMidnight();
    this.timeWithoutEstimate = timeWithoutEstimate_; // I don't think we use this.
    this.timeMultiplier = timeMultiplier_;
    this.reversePriority = false;
  }
  /**
   Note that this will adjust the time to midnight (morning).
   */
  public TimeScheduleCreatePreferences(Date startTime, boolean reversePriority_) {
    this.startCal = new GregorianCalendar();
    adjustCalToMidnight();
    this.timeWithoutEstimate = 0;
    this.timeMultiplier = 1;
    this.reversePriority = reversePriority_;
  }
  public TimeScheduleCreatePreferences() {
    this(0,1);
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
  public boolean equals(Object obj) {
    return timeMultiplier == ((TimeScheduleCreatePreferences) obj).getTimeMultiplier();
  }
  public double getTimeMultiplier() {
    return timeMultiplier;
  }
  public boolean getReversePriority() {
    return reversePriority;
  }
  
  
  /**
   * For our serializing convenience, so we don't muck up the enclosing class with these variabilities.
   */
  public static class Pojo {
    public Calendar startCal;
    public Date startDate;
    public int timeWithoutEstimate;
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
    public int getTimeWithoutEstimate() {
      return timeWithoutEstimate;
    }
    public void setTimeWithoutEstimate(int timeWithoutEstimate) {
      this.timeWithoutEstimate = timeWithoutEstimate;
    }
    public double getTimeMultiplier() {
      return timeMultiplier;
    }
    public void setTimeMultiplier(double timeMultiplier) {
      this.timeMultiplier = timeMultiplier;
    }
    public TimeScheduleCreatePreferences getPrefs() {
      if (startCal != null) {
        return new TimeScheduleCreatePreferences(timeWithoutEstimate, startCal, timeMultiplier);
      } else if (startDate != null) {
        return new TimeScheduleCreatePreferences(timeWithoutEstimate, startDate, timeMultiplier);
      } else {
        return new TimeScheduleCreatePreferences(timeWithoutEstimate, timeMultiplier);
      }
    }
  }
  
}
