package com.trentlarson.forecast.core.helper;

import java.util.Calendar;
import java.util.Date;

/**
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */




/**
 * A suite of utilities surrounding the use of the
 * {@link java.util.Calendar} and {@link java.util.Date} object.
 * 
 * DateUtils contains a lot of common methods considering manipulations
 * of Dates or Calendars. Some methods require some extra explanation.
 * The truncate and round methods could be considered the Math.floor(),
 * Math.ceil() or Math.round versions for dates
 * This way date-fields will be ignored in bottom-up order.
 * As a complement to these methods we've introduced some fragment-methods.
 * With these methods the Date-fields will be ignored in top-down order.
 * Since a date without a year is not a valid date, you have to decide in what
 * kind of date-field you want your result, for instance milliseconds or days.
 * 
 *   
 *   
 *
 * @author <a href="mailto:sergek@lokitech.com">Serge Knystautas</a>
 * @author Stephen Colebourne
 * @author Janek Bogucki
 * @author <a href="mailto:ggregory@seagullsw.com">Gary Gregory</a>
 * @author Phil Steitz
 * @author Robert Scholte
 * @since 2.0
 * @version $Id: DateUtils.java 634096 2008-03-06 00:58:11Z niallp $
 * 
 * Retrieved from: http://www.java2s.com/Tutorial/Java/0040__Data-Type/Roundthisdateleavingthefieldspecifiedasthemostsignificantfield.htm
 */
public class DateUtil {
  public static final int SEMI_MONTH = Calendar.FIELD_COUNT;
  private static final int[][] fields = {
    {Calendar.MILLISECOND},
    {Calendar.SECOND},
    {Calendar.MINUTE},
    {Calendar.HOUR_OF_DAY, Calendar.HOUR},
    {Calendar.DATE, Calendar.DAY_OF_MONTH, Calendar.AM_PM 
        /* Calendar.DAY_OF_YEAR, Calendar.DAY_OF_WEEK, Calendar.DAY_OF_WEEK_IN_MONTH */
    },
    {Calendar.MONTH, DateUtil.SEMI_MONTH},
    {Calendar.YEAR},
    {Calendar.ERA}};

  /**
   * Round this date, leaving the field specified as the most
   * significant field.
   *
   * For example, if you had the datetime of 28 Mar 2002
   * 13:45:01.231, if this was passed with HOUR, it would return
   * 28 Mar 2002 14:00:00.000. If this was passed with MONTH, it
   * would return 1 April 2002 0:00:00.000.
   * 
   * For a date in a timezone that handles the change to daylight
   * saving time, rounding to Calendar.HOUR_OF_DAY will behave as follows.
   * Suppose daylight saving time begins at 02:00 on March 30. Rounding a 
   * date that crosses this time would produce the following values:
   * <ul>
   * <li>March 30, 2003 01:10 rounds to March 30, 2003 01:00</li>
   * <li>March 30, 2003 01:40 rounds to March 30, 2003 03:00</li>
   * <li>March 30, 2003 02:10 rounds to March 30, 2003 03:00</li>
   * <li>March 30, 2003 02:40 rounds to March 30, 2003 04:00</li>
   * </ul>
   * 
   * 
   * @param date  the date to work with
   * @param field  the field from <code>Calendar</code>
   *  or <code>SEMI_MONTH</code>
   * @return the rounded date (a different object)
   * @throws IllegalArgumentException if the date is <code>null</code>
   * @throws ArithmeticException if the year is over 280 million
   */
  public static Calendar round(Calendar date, int field) {
      if (date == null) {
          throw new IllegalArgumentException("The date must not be null");
      }
      Calendar rounded = (Calendar) date.clone();
      modify(rounded, field, true);
      return rounded;
  }

  //-----------------------------------------------------------------------
  /**
   * Internal calculation method.
   * 
   * @param val  the calendar
   * @param field  the field constant
   * @param round  true to round, false to truncate
   * @throws ArithmeticException if the year is over 280 million
   */
  private static void modify(Calendar val, int field, boolean round) {
      if (val.get(Calendar.YEAR) > 280000000) {
          throw new ArithmeticException("Calendar value too large for accurate calculations");
      }
      
      if (field == Calendar.MILLISECOND) {
          return;
      }

      // ----------------- Fix for LANG-59 ---------------------- START ---------------
      // see http://issues.apache.org/jira/browse/LANG-59
      //
      // Manually truncate milliseconds, seconds and minutes, rather than using
      // Calendar methods.

      Date date = val.getTime();
      long time = date.getTime();
      boolean done = false;

      // truncate milliseconds
      int millisecs = val.get(Calendar.MILLISECOND);
      if (!round || millisecs < 500) {
          time = time - millisecs;
      }
      if (field == Calendar.SECOND) {
          done = true;
      }

      // truncate seconds
      int seconds = val.get(Calendar.SECOND);
      if (!done && (!round || seconds < 30)) {
          time = time - (seconds * 1000L);
      }
      if (field == Calendar.MINUTE) {
          done = true;
      }

      // truncate minutes
      int minutes = val.get(Calendar.MINUTE);
      if (!done && (!round || minutes < 30)) {
          time = time - (minutes * 60000L);
      }

      // reset time
      if (date.getTime() != time) {
          date.setTime(time);
          val.setTime(date);
      }
      // ----------------- Fix for LANG-59 ----------------------- END ----------------

      boolean roundUp = false;
      for (int i = 0; i < fields.length; i++) {
          for (int j = 0; j < fields[i].length; j++) {
              if (fields[i][j] == field) {
                  //This is our field... we stop looping
                  if (round && roundUp) {
                      if (field == DateUtil.SEMI_MONTH) {
                          //This is a special case that's hard to generalize
                          //If the date is 1, we round up to 16, otherwise
                          //  we subtract 15 days and add 1 month
                          if (val.get(Calendar.DATE) == 1) {
                              val.add(Calendar.DATE, 15);
                          } else {
                              val.add(Calendar.DATE, -15);
                              val.add(Calendar.MONTH, 1);
                          }
                      } else {
                          //We need at add one to this field since the
                          //  last number causes us to round up
                          val.add(fields[i][0], 1);
                      }
                  }
                  return;
              }
          }
          //We have various fields that are not easy roundings
          int offset = 0;
          boolean offsetSet = false;
          //These are special types of fields that require different rounding rules
          switch (field) {
              case DateUtil.SEMI_MONTH:
                  if (fields[i][0] == Calendar.DATE) {
                      //If we're going to drop the DATE field's value,
                      //  we want to do this our own way.
                      //We need to subtrace 1 since the date has a minimum of 1
                      offset = val.get(Calendar.DATE) - 1;
                      //If we're above 15 days adjustment, that means we're in the
                      //  bottom half of the month and should stay accordingly.
                      if (offset >= 15) {
                          offset -= 15;
                      }
                      //Record whether we're in the top or bottom half of that range
                      roundUp = offset > 7;
                      offsetSet = true;
                  }
                  break;
              case Calendar.AM_PM:
                  if (fields[i][0] == Calendar.HOUR_OF_DAY) {
                      //If we're going to drop the HOUR field's value,
                      //  we want to do this our own way.
                      offset = val.get(Calendar.HOUR_OF_DAY);
                      if (offset >= 12) {
                          offset -= 12;
                      }
                      roundUp = offset > 6;
                      offsetSet = true;
                  }
                  break;
          }
          if (!offsetSet) {
              int min = val.getActualMinimum(fields[i][0]);
              int max = val.getActualMaximum(fields[i][0]);
              //Calculate the offset from the minimum allowed value
              offset = val.get(fields[i][0]) - min;
              //Set roundUp if this is more than half way between the minimum and maximum
              roundUp = offset > ((max - min) / 2);
          }
          //We need to remove this field
          if (offset != 0) {
              val.set(fields[i][0], val.get(fields[i][0]) - offset);
          }
      }
      throw new IllegalArgumentException("The field " + field + " is not supported");

  }
}