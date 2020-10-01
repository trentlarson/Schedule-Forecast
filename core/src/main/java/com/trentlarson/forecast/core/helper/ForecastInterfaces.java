package com.trentlarson.forecast.core.helper;

import com.trentlarson.forecast.core.scheduling.IssueTree;
import com.trentlarson.forecast.core.scheduling.TimeScheduleCreatePreferencesForGsonInput;
import com.trentlarson.forecast.core.scheduling.TimeScheduleDisplayPreferences;

public class ForecastInterfaces {

  public class ScheduleAndDisplayInput {
    public IssueTree[] issues;
    public TimeScheduleCreatePreferencesForGsonInput createPreferences;
    public TimeScheduleDisplayPreferences displayPreferences;
  }
}
