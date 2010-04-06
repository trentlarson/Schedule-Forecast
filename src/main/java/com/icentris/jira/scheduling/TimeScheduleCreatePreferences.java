package com.icentris.jira.scheduling;

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
}
