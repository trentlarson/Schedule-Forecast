package com.icentris.sql;

import java.lang.reflect.UndeclaredThrowableException;
import java.sql.SQLException;

/**
 * Since handling all the corner cases when closing out JDBC resources becomes
 * very complex, we created this Exception handler to simplify the task. To
 * understand how to use this class, it's best to understand the alternatives
 * and their drawbacks. The following is taken from <a
 * href="https://intra01.icentris.net/tiki/tiki-index.php?page=JDBC">the JDBC page in our
 * Wiki documentation</a>:
 * 
 * <hr width="90%" align="center">
 * <h2 style="background-color: #CCCCCC">Some mistakes we used to encourage</h2>
 * 
 * <b>Our old suggested finally block looked like this</b>:
 * 
 * <pre>
 *    } finally {
 *      try { if (results != null) results.close(); 
 *      } catch (SQLException e) { e.printStackTrace(); }
 *      try { if (ps != null) ps.close(); 
 *      } catch (SQLException e) { e.printStackTrace(); }
 *      try { if (connection != null) connection.close(); 
 *      } catch (SQLException e) { e.printStackTrace(); }
 *    }
 * </pre>
 * 
 * This had the following problems:
 * <ul>
 * <li>Any RuntimeException (e.g. NullPointerException--thank you Postgres) in
 * the results.close() or ps.close() methods would cause the Connection to not
 * be closed.
 * <li>Any RuntimeException? exception in the finally block would completely
 * squelch the existence of any previously thrown Exception.
 * <li>Any SQLException in the finally block would not get propagated.
 * </ul>
 * 
 * <hr width="30%" align="left">
 * <b>We also tried</b>:
 * 
 * <pre>
 *    } finally {
 *      try {
 *        results.close();
 *      } finally {
 *        try {
 *          ps.close();
 *        } finally {
 *          connection.close(); 
 *        }
 *      }
 *    }
 * </pre>
 * 
 * While that's good because it closes everything, it still has the drawbacks:
 * <ul>
 * <li>It always propegates the last exception whereas we'd prefer to propegate
 * the first.
 * <li>The last Exception would completely squelch the existence of any
 * previously thrown Exception.
 * </ul>
 * 
 * <hr width="30%" align="left">
 * <b>Some thought we should just use</b>:
 * 
 * <pre>
 *    } finally {
 *      try { if (results != null) results.close(); 
 *      } catch (Throwable e) { e.printStackTrace(); }
 *      try { if (ps != null) ps.close(); 
 *      } catch (Throwable e) { e.printStackTrace(); }
 *      try { if (connection != null) connection.close(); 
 *      } catch (Throwable e) { e.printStackTrace(); }
 *    }
 * </pre>
 * 
 * While that's good because it closes everything, it still has the drawback:
 * <ul>
 * <li>Any Exception in the finally block would never get propagated.
 * </ul>
 * 
 * <hr width="30%" align="left">
 * When we finally handeled all following goals, the code was incredibly nasty:
 * <ol>
 * <li>Make sure all connected resources (ResultSet, Statement, Connection) get
 * closed.
 * <li>Propagate Exceptions in the try block.
 * <li>If there are not Exceptions in the try block, but there are in the
 * finally, propagate those.
 * <li>If there are multiple Exceptions, print a stack trace of the secondary
 * exceptions, but propagate the first exception.
 * </ol>
 * 
 * For all these reasons, we created com.icentris.sql.SQLExceptionHandler used
 * in the examples below.
 * 
 * <h2 style="background-color: #CCCCCC">Current Suggested Approach for Esuite</h2>
 * 
 * Since the try {} finally {} blocks are so error-prone, we came up with a
 * solution that allows us to totally avoid it in many cases, and greatly
 * simplify it in others. This solution should cover 90% of our JDBC use, while
 * the valid solution at the top can be used where this is not adequate.
 * 
 * <p>
 * <b>For INSERT, UPDATE, and DELETE statements</b>:
 * 
 * <pre>
 *   String sql = . . .
 *   Object[] params = new Object[] {
 *     . . .
 *   };
 *   int rowsAffected = EsuiteSQL.executeUpdate( query, params );
 * </pre>
 * 
 * 
 * <b>For SELECT statements that should only return one row</b>:
 * 
 * <pre>
 *    String sql = . . .
 *    Object[] params = new Object[] {
 *      . . .
 *    };
 *    Object[] row = EsuiteSQL.getOneRow( query, params );
 *    String firstColumn = row[0];
 *    String secondColumn = row[1];
 *    . . .
 * </pre>
 * 
 * 
 * 
 * <b>For SELECT statements that return multiple rows</b>:
 * 
 * <pre>
 *    String sql = . . .
 *    Object[] params = new Object[] {
 *      . . .
 *    };
 *    ResultSet results = null;
 *    &lt;b&gt;SQLExeptionHandler handler = new SQLExceptionHandler();&lt;/b&gt;
 *    try {
 *      results = EsuiteSQL.executeQuery(sqlString, params);
 *      while ( results.next() ) {
 *        . . .
 *      }
 *    } catch (Throwable t) {
 *      &lt;b&gt;handler.add( t );&lt;/b&gt;
 *      return null; // would never happen, but you may need a dummy return to keep the compiler happy
 *    } finally {
 *      try { 
 *        if (results != null) results.close();
 *      } catch (Throwable t) {
 *        &lt;b&gt;handler.add( t );&lt;/b&gt;
 *      }
 *      &lt;b&gt;handler.throwFirstExceptionIfAny();&lt;/b&gt;
 *    }
 * </pre>
 * 
 * <h2 style="background-color: #CCCCCC">A Pure JDBC Option (except need for
 * SQLExceptionHandler)</h2>
 * 
 * <pre>
 *  import com.icentris.sql.SQLExceptionHandler;
 *   . . .
 *     Connection connection = null;
 *     Statement ps = null;
 *     ResultSet results = null;
 *     &lt;b&gt;SQLExeptionHandler handler = new SQLExceptionHandler();&lt;/b&gt;
 *     try {
 *       connection = . . .
 *       ps = connection.createStatement();
 *       results = ps.executeQuery( sql );
 *       while ( results.next() ) {
 *         . . .
 *       }
 *     } catch (Throwable t) {
 *       &lt;b&gt;handler.add( t );&lt;/b&gt;
 *       return null; // would never happen, but you may need a dummy return to keep the compiler happy
 *     } finally {
 *       try { 
 *         if (results != null) results.close();
 *       } catch (Throwable t) {
 *         &lt;b&gt;handler.add( t );&lt;/b&gt;
 *       }
 *       try { 
 *         if (ps != null) ps.close();
 *       } catch (Throwable t) {
 *         &lt;b&gt;handler.add( t );&lt;/b&gt;
 *       }
 *       try { 
 *         if (connection != null) connection.close();
 *       } catch (Throwable t) {
 *         &lt;b&gt;handler.add( t );&lt;/b&gt;
 *       }
 *       &lt;b&gt;handler.throwFirstExceptionIfAny();&lt;/b&gt;
 *     }
 * </pre>
 */
public class SQLExceptionHandler {
   /**
    * Logger for this class
    */
   //private static final Log logger = LogFactory.getLog(SQLExceptionHandler.class);

   private SQLException sqle;

   private Throwable t;

   private RuntimeException re;

   /**
    * If no exception has been added yet, keeps track of this as the first
    * exception to be thrown later by throwFirstExceptionIfAny(). If an
    * exception has allready been added, checks to see if this is an
    * SQLException. If it is, adds this as a "next exception" with
    * SQLException's setNextException method. Otherwise, just prints the stack
    * trace for this exception with the expectation that the original exception
    * is the one we'll want to throw in the end.
    */
   public void add(Throwable t) {
      //logger.error(t.getMessage(), t);
      if (sqle != null && t instanceof SQLException) {
         sqle.setNextException((SQLException) t);
      } else if (sqle == null && re == null && this.t == null) {
         if (t instanceof SQLException) {
            sqle = (SQLException) t;
         } else if (t instanceof RuntimeException) {
            re = (RuntimeException) t;
         } else {
            this.t = t;
         }
      } else {
      }
   }

   /** Throws the first exception you set with add() method. */
   public void throwFirstExceptionIfAny() throws SQLException {
      if (sqle != null) {
         throw sqle;
      } else if (re != null) {
         throw re;
      } else if (t != null) {
         throw new UndeclaredThrowableException(t);
      }
   }
}
