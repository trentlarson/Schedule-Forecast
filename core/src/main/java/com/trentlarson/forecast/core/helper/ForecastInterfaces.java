package com.trentlarson.forecast.core.helper;

import com.trentlarson.forecast.core.scheduling.IssueTree;
import com.trentlarson.forecast.core.scheduling.TimeScheduleCreatePreferences;
import com.trentlarson.forecast.core.scheduling.TimeScheduleDisplayPreferences;

public class ForecastInterfaces {

  public class ScheduleAndDisplayInput {
    public IssueTree[] issues;
    public TimeScheduleCreatePreferences createPreferences;
    public TimeScheduleDisplayPreferences displayPreferences;
  }
}
