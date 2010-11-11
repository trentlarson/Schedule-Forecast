package com.trentlarson.forecast.core.scheduling;

import java.io.IOException;
import java.io.Writer;
import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class TimeScheduleModifyWriter {

  public static SimpleDateFormat DATE_FORMATTER = new SimpleDateFormat("MM/dd");
  public static DecimalFormat ESTIMATE_FORMATTER = new DecimalFormat("###0.0##");

  protected static void writeChangeTools(IssueTree detail, int maxPriority, Writer out)
    throws IOException {

    if (!detail.getResolved()) {
      out.write("<br>\n");
      out.write("<table>\n");
      out.write("<tr>\n");
      for (int priority = 1; priority <= maxPriority; priority++) {
        out.write("<td>");
        out.write
          ("<input type='radio' name='"
           + makePriorityCheckboxName(detail.getKey())
           + "' value='" + (priority) + "' "
           + (detail.getPriority() == (priority + 1) ? "CHECKED" : "")
           + ">");
        if (priority == (int) maxPriority / 2) {
          out.write("" + priority);
        }
        out.write("</td>\n");
      }
      out.write("</tr>\n");
      out.write("</table>\n");
      out.write("<br>\n");
      out.write
        ("start <input name='" + makeStartDateInputName(detail.getKey())
         + "' size='5' value='" + formatShortDate(detail.getMustStartOnDate())
         + "' READONLY>\n");
      out.write
        ("due <input name='" + makeDueDateInputName(detail.getKey())
         + "' size='5' value='" + formatShortDate(detail.getDueDate())
         + "'>\n");
      out.write
        ("estimate <input name='" + makeEstimateInputName(detail.getKey())
         + "' size='3' value='" + formatEstimateForTypicalDay(detail.getEstimate())
         + "'>d\n");
    }
  }

  public static String makePriorityCheckboxName(String key) {
    return "priority_" + key;
  }

  public static String makeStartDateInputName(String key) {
    return "start_date_" + key;
  }

  public static String makeDueDateInputName(String key) {
    return "due_date_" + key;
  }

  public static String makeEstimateInputName(String key) {
    return "estimate_" + key;
  }

  /**
     @param estSeconds in terms of seconds
     @return estimate in terms of days
  */
  public static String formatEstimateForTypicalDay(int estSeconds) {
    return
      ESTIMATE_FORMATTER.format
      (estSeconds / (3600.0 * TimeSchedule.MAX_WORKHOURS_PER_WORKDAY));
  }

  /**
     @param estDays in terms of days
     @return estimate in terms of seconds
  */
  public static int parseEstimateFromTypicalDay(String estDays) {
    return (int) (Double.valueOf(estDays).doubleValue()
                  * 3600.0 * TimeSchedule.MAX_WORKHOURS_PER_WORKDAY);
  }

  public static String formatShortDate(Date date) {
    if (date != null) {
      return DATE_FORMATTER.format(date);
    } else {
      return "";
    }
  }

  /**
     @deprecated because it will have a year of 1970
     Use date formatter with year or parseNewShortDate
   */
  public static Date parseShortDate(String dateStr) throws ParseException {
    if (dateStr.length() > 0) {
      return DATE_FORMATTER.parse(dateStr);
    } else {
      return null;
    }
  }

  /**
     @return null if dateStr is "", otherwise the next date in the
     future that matches this "MM/DD" format
   */
  public static Date parseNewShortDate(String dateStr) throws ParseException {
    if (dateStr.length() > 0) {
      Date date = DATE_FORMATTER.parse(dateStr);
      Calendar todayCal = new GregorianCalendar();
      Calendar selectCal = new GregorianCalendar();
      selectCal.setTime(date);
      selectCal.set(Calendar.YEAR, todayCal.get(Calendar.YEAR));
      if (selectCal.before(todayCal)) {
        selectCal.add(Calendar.YEAR, 1);
      }
      return selectCal.getTime();
    } else {
      return null;
    }
  }


}
