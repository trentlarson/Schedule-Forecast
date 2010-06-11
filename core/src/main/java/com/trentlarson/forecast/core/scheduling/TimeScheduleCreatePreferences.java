package com.trentlarson.forecast.core.scheduling;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class TimeScheduleCreatePreferences {
  public final int timeWithoutEstimate;
  public final Calendar startCal;
  public final double timeMultiplier;
  /**
     Note that this will adjust the time to midnight (morning).
   */
  public TimeScheduleCreatePreferences(int timeWithoutEstimate_, Calendar startCal_, double timeMultiplier_) {
    this.timeWithoutEstimate = timeWithoutEstimate_;
    this.startCal = (Calendar) startCal_.clone();
    adjustCalToMidnight();
    this.timeMultiplier = timeMultiplier_;
  }
  /**
     Note that this will adjust the time to midnight (morning).
   */
  public TimeScheduleCreatePreferences(int timeWithoutEstimate_, Date startTime, double timeMultiplier_) {
    this.timeWithoutEstimate = timeWithoutEstimate_;
    this.startCal = new GregorianCalendar();
    startCal.setTime(startTime);
    adjustCalToMidnight();
    this.timeMultiplier = timeMultiplier_;
  }
  /**
     Create with date of this morning at midnight.
   */
  public TimeScheduleCreatePreferences(int timeWithoutEstimate_, double timeMultiplier_) {
    this.timeWithoutEstimate = timeWithoutEstimate_;
    this.startCal = new GregorianCalendar();
    adjustCalToMidnight();
    this.timeMultiplier = timeMultiplier_;
  }
  private void adjustCalToMidnight() {
    this.startCal.set(Calendar.HOUR_OF_DAY, 0);
    this.startCal.set(Calendar.MINUTE, 0);
    this.startCal.set(Calendar.SECOND, 0);
    this.startCal.set(Calendar.MILLISECOND, 0);
  }
  public boolean equals(Object obj) {
    return timeMultiplier == ((TimeScheduleCreatePreferences) obj).getTimeMultiplier();
  }
  public Date getStartTime() {
    return startCal.getTime();
  }
  public double getTimeMultiplier() {
    return timeMultiplier;
  }
  
  
  /**
   * For our serializing convenience, so we don't muck up the enclosing class with these variabilities.
   */
  public static class Pojo {
    public int timeWithoutEstimate;
    public Calendar startCal;
    public Date startDate;
    public double timeMultiplier;
    public int getTimeWithoutEstimate() {
      return timeWithoutEstimate;
    }
    public void setTimeWithoutEstimate(int timeWithoutEstimate) {
      this.timeWithoutEstimate = timeWithoutEstimate;
    }
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
        return new TimeScheduleCreatePreferences(timeWithoutEstimate, startCal, timeMultiplier);
      } else if (startDate != null) {
        return new TimeScheduleCreatePreferences(timeWithoutEstimate, startDate, timeMultiplier);
      } else {
        return new TimeScheduleCreatePreferences(timeWithoutEstimate, timeMultiplier);
      }
    }
  }
  
}
