package com.trentlarson.forecast.core.scheduling;

import java.util.*;
import java.text.DecimalFormat;

public class TimeCompleteBar {

  public static DecimalFormat INT_FORMATTER = new DecimalFormat("#,##0");
  public static DecimalFormat DECIMAL_FORMATTER = new DecimalFormat("#,##0.#");
  public static DecimalFormat PERCENT_FORMATTER = new DecimalFormat("##0%");

  public static DecimalFormat valueFormatter(boolean formatAsInteger) {
    if (formatAsInteger) {
      return INT_FORMATTER;
    } else {
      return DECIMAL_FORMATTER;
    }
  }


  public static class TotalAndFinished {
    int total = 0, finished = 0;
  }

  public static TotalAndFinished numIssues(IssueTree tree) {
    TotalAndFinished totals = new TotalAndFinished();
    totals.total = 1;
    if (tree.getResolved()) {
      totals.finished = 1;
    }
    for (Iterator iter = tree.getSubtasks().iterator(); iter.hasNext(); ) {
      IssueTree subtree = (IssueTree) iter.next();
      TotalAndFinished subtotals = numIssues(subtree);
      totals.total = totals.total + subtotals.total; 
      totals.finished = totals.finished + subtotals.finished; 
    }
    return totals;
  }

  public static String roadMapTable(IssueTree tree) {
    StringBuffer result = new StringBuffer();
    result.append("<!-- road map for " + tree.getKey() + " -->\n");
    result.append(tree.getKey() + "\n");
    TotalAndFinished total = numIssues(tree);
    result.append(roadMapTable("Tasks", total.finished, total.total - total.finished, true));
    result.append(roadMapTable("Hours", tree.totalTimeSpent() / 3600.0, tree.totalEstimate() / 3600.0 , false));
    return result.toString();
  }

  public static StringBuffer roadMapTable(String label, double spent, double remaining, boolean formatAsInteger) {

    DecimalFormat valueFormatter = valueFormatter(formatAsInteger);

    StringBuffer sb = new StringBuffer();
    sb.append("<br>");
    sb.append("<table>");
    sb.append("  <tr>");
    sb.append("    <td>");
    sb.append(label + ": ");
    sb.append("    </td>");
    sb.append("    <td width='100'>"); // interdependent with percent...Int values below
    sb.append(roadMapTable(spent + remaining, spent, valueFormatter));
    sb.append("    </td>");
    sb.append("    <td>");
    sb.append(" " + valueFormatter.format(spent) + " finished; " + valueFormatter.format(remaining) + " remaining");
    sb.append("    </td>");
    sb.append("  </tr>");
    sb.append("</table>");

    return sb;
  }

  public static String roadMapTable(double numTotal, double numFinished, DecimalFormat valueFormatter) {

    double percentDone = 1.0;
    if (numTotal > 0.0) {
      percentDone = numFinished / numTotal;
    }
    double percentLeft = 1.0 - percentDone;

    // these two are interdependant with the absolute TD width above
    // (I tried to put '..%' values on the TD and IMG elements below,
    //   but they render too short when the percentages were non-100, eg. 20/80)
    int percentDoneInt = (int) (percentDone * 100.0);
    int percentLeftInt = (int) (percentLeft * 100.0);

    String percentDoneStr = PERCENT_FORMATTER.format(percentDone);
    String percentLeftStr = PERCENT_FORMATTER.format(percentLeft);
    StringBuffer sb = new StringBuffer();
    sb.append("<table cellspacing=0 cellpadding=0 border=0>\n");
    sb.append("  <tr>\n");
    sb.append("    <td width='" + percentDoneInt + "' bgcolor='009900'>\n");
    sb.append("      <a title='" + valueFormatter.format(numFinished) + " = " + percentDoneStr + "'>");
    sb.append("        <img src='/images/border/spacer.gif' width='" + percentDoneInt + "' height='10' border='0'");
    sb.append("             title='" + valueFormatter.format(numFinished) + " = " + percentDoneStr + "'>");
    sb.append("      </a>");
    sb.append("    </td>\n");
    sb.append("    <td width='" + percentLeftInt + "' bgcolor='cc0000'>\n");
    sb.append("      <a title='" + valueFormatter.format(numTotal - numFinished) + " = " + percentLeftStr + "'>");
    sb.append("        <img src='/images/border/spacer.gif' width='" + percentLeftInt + "' height='10' border='0'");
    sb.append("             title='" + valueFormatter.format(numTotal - numFinished) + " = " + percentLeftStr + "'>");
    sb.append("      </a>");
    sb.append("    </td>\n");
    sb.append("  </tr>\n");
    sb.append("</table>\n");
    sb.append(percentDoneStr);
    return sb.toString();
  }

}
