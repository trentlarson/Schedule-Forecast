package com.trentlarson.forecast.core.scheduling;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class TimeScheduleCreatePreferencesForGsonInput {
  public final Date startDate = null;
  public final Double timeMultiplier = null;
  public final Double defaultAssigneeHoursPerWeek = null;
  public final Boolean reversePriority = null;

  public TimeScheduleCreatePreferences getTimeScheduleCreatePreferences() {
    Calendar startCal = new GregorianCalendar();
    if (this.startDate != null) {
      startCal.setTime(this.startDate);
    }
    return new TimeScheduleCreatePreferences(startCal, this.timeMultiplier, this.defaultAssigneeHoursPerWeek, this.reversePriority);
  }
}
