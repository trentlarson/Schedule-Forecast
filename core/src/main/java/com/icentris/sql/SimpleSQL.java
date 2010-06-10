package com.icentris.sql;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * 
 * Think hard before using this class: it is preferred to use services with DA
 * Os to get the local database (eg. AssociateService), 
 * and use IdentityUtils to get to the InfoTrax database.
 * (Note that IdentityUtils wraps DBUtils which wraps this class.)
 * 
 * 
 * 
 * <p>SimpleSQL is a class that helps in 90% of cases to simplify JDBC usage.  While JDBC
 * is great, one large drawback kept biting us: the need to very carefully close all 
 * the connected resources (Connection's, Statement's, and ResultSet's).  By wrapping
 * jdbc updates in the single executeUpdate method, callers of SimpleSQL can greatly
 * simplify their code by only needing to open and carefully close one object, the 
 * Connection.  If an application has a consistent way of obtaining connections, even
 * the opening/closing of the Connection's can be handled for the clients by wrapping
 * SimpleSQL inside an application-specific class.
 *
 * <p>While executeQuery still returns a ResultSet which needs to be closed, this 
 * ResultSet is smart enough to close its PreparedStatement.  Again, if an application-
 * specific implementation also opens the connection, then the ResultSet is the only
 * thing that needs to be closed.
 *
 * <p>In both executeQuery and executeUpdate, an Object array of params is expected.
 * These Objects should be of the following types (these correspond to java.sql.Types):
 * <pre>
 *   java.lang.String, java.sql.Array, java.math.BigDecimal, java.sql.Blob,
 *   java.lang.Boolean, java.lang.Byte, java.lang.Byte[], java.sql.Clob, java.sql.Date,
 *   java.lang.Double, java.lang.Float, java.lang.Integer, java.lang.Long,
 *   java.sql.Ref, java.lang.Short, java.sql.Time, java.sql.Timestamp
 * </pre>
 * 
 * See the Icentris.com Tools project in SourceForge: https://sourceforge.net/projects/icentris/
 * 
 */
public class SimpleSQL {
   /**
    * Logger for this class
    */
   private static final Log logger = LogFactory.getLog(SimpleSQL.class);
   
  /**
   * Creates the PreparedStatement, sets the parameters, and executes the update for
   * you, returning the number of rows affected.
   *
   * <p>This is not a solution for all updates, just for the 90% most-common cases.
   *
   * @param sql the sql INSERT, UPDATE, or DELETE statement to be run
   * @param params the array of parameters to this statement that will
   *   replace the ? placeholders in order
   * @param connection the jdbc connection on which this statement will
   *   be run (it is your job to open/close this connection properly)
   */
  public static int executeUpdate(String sql, Object[] params, Connection connection) throws SQLException {
    if ( connection == null ) throw new IllegalArgumentException("Don't pass me a null connection!");
    PreparedStatement ps = null;
    SQLExceptionHandler handler = new SQLExceptionHandler();
    try {
      ps = connection.prepareStatement( sql );
      for ( int i=0; params != null && i < params.length; i++ ) {
        setObject( i + 1, params[i], ps );
      }
      return ps.executeUpdate();
    } catch (Throwable t) {
      handler.add( t );
      return -1; // this will never happen (I'll instead throw t with throwFirstExceptionIfAny)
    } finally {
      try {
        if ( ps != null ) ps.close();
      } catch (Throwable t) {
        handler.add( t );
      }
      handler.throwFirstExceptionIfAny();
    }
  }
  
  /**
   * Returns a ResultSet that should be carefully closed just like the connection.
   * <pre>
   *   String sqlString = . . . ;
   *   Object[] params = new Object[] {
   *     . . .
   *   };
   *   Connection connection = null;
   *   ResultSet results = null;
   *   try {
   *     connection = myGetConnectionMethod();
   *     results = SimpleSQL.executeQuery(sqlString, params, connection);
   *     while ( results.next() ) {
   *       . . .
   *     }
   *   } catch (SQLException sqle) {
   *     . . .
   *   } finally {
   *     try {
   *       results.close();
   *     } finally {
   *       connection.close(); 
   *     }
   *   }
   * </pre>
   *
   * @param sql the sql SELECT statement to be run
   * @param params the array of parameters to this statement that will
   *   replace the ? placeholders in order
   * @param connection the jdbc connection on which this statement will
   *   be run (it is your job to open/close this connection properly)
   */
  public static ResultSet executeQuery(String sql, Object[] params, Connection connection) throws SQLException {
    return executeQuery( sql, params, connection, false );
  }

  protected static ResultSet executeQuery(String sql, Object[] params, Connection connection, boolean closeConnectionsOnResultSetClose) throws SQLException {
    // max rows limit the number of records returned in a result set, 0 = unlimited
    int maxRows = 0;
    return executeQuery( sql, params, connection, closeConnectionsOnResultSetClose, maxRows);
  }

  protected static ResultSet executeQuery(String sql, Object[] params, Connection connection, boolean closeConnectionsOnResultSetClose, int maxRows) throws SQLException {
    if ( connection == null ) throw new IllegalArgumentException("Don't pass me a null connection!");
    PreparedStatement ps = connection.prepareStatement( sql );
    for ( int i=0; params != null && i < params.length; i++ ) {
      setObject( i + 1, params[i], ps );
    }

    // max rows limit the number of records returned in a result set, 0 = unlimited
    ps.setMaxRows(maxRows);

    ResultSet results = ps.executeQuery();
    SimpleSQL s = new SimpleSQL();
    return s.getResultSet(results, ps, connection, closeConnectionsOnResultSetClose);
  }

  /**
   * This will get one row of results (all the right types) for the SQL statement and clean up any DB resources.
   * 
   * @see #getOneRow(String, Object[], Connection)
   * 
   * @param driverClassName
   * @param jdbcUrl
   * @param sql for the prepared statement to run
   * @param params to insert into the SQL prepared statement
   * @return one row of results (all the right types) for the SQL statement
   * @throws SQLException
   * @throws IllegalStateException if the driverClassName is not found
   */
  public static Object[] getOneRow(String driverClassName, String jdbcUrl, String sql, Object[] params) throws SQLException {
    Connection dbConn = null;
    try {
      Class.forName(driverClassName);
      try {
        dbConn = DriverManager.getConnection(jdbcUrl);
      } catch (SQLException e) {
        throw new SQLConnectException(e);
      }
      Object[] results = SimpleSQL.getOneRow(sql, params, dbConn);
      return results;
    } catch (ClassNotFoundException e) {
      throw new IllegalStateException(e);
    } finally {
      try { if (dbConn != null) {dbConn.close();} } catch (Exception e) {} // not just SQLException because DB connect error will leave a null dbConn
    }

  }

  
  /**
   * Run an SQL statement, and get an Object array representing one row, with the correct
   * SQL types for each column.  Becuase the PreparedStatement and ResultSet are opened and closed
   * by this method, you don't have to use a careful try finally block for those (though
   * you probably still need it for your Connection).
   *
   * @param sql the sql SELECT statement to be run
   * @param params the array of parameters to this statement that will
   *   replace the ? placeholders in order
   * @param connection the jdbc connection on which this statement will
   *   be run (it is your job to open/close this connection properly)
   *
   * @return an array of Objects in order returned by the query, with types chosen based on
   *   ResultSetMetaData.  Returns null if no results found.
   *   Note: when the return type is a date, the resulting object may
   *   be a Timestamp or Date depending on the DB field and the JDBC
   *   driver.  I recommend you cast the object to a java.util.Date to
   *   cover both possibilities.
   */
  public static Object[] getOneRow(String sql, Object[] params, Connection connection) throws SQLException {
    if ( connection == null ) throw new IllegalArgumentException("Don't pass me a null connection!");
    PreparedStatement ps = null;
    ResultSet results = null;
    SQLExceptionHandler handler = new SQLExceptionHandler();
    try {
      ps = connection.prepareStatement( sql );
      for ( int i=0; params != null && i < params.length; i++ ) {
        setObject( i + 1, params[i], ps );
      }
      results = ps.executeQuery();
      if ( results.next() ) {
        ResultSetMetaData meta = results.getMetaData();
        int columnCount = meta.getColumnCount();
        Object[] row = new Object[columnCount];
        for ( int i=0; i < columnCount; i++ ) {
          //int type = meta.getColumnType( i + 1 );
          row[i] = results.getObject( i + 1 );
        }
        return row;
      } else {
        return null;
      }
    } catch (Throwable t) {
      handler.add( t );
      return null; // this will never happen (I'll instead throw t with throwFirstExceptionIfAny)
    } finally {
      try {
        if ( results != null ) results.close();
      } catch (Throwable t) {
        handler.add( t );
      }
      try {
        if ( ps != null ) ps.close(); 
      } catch (Throwable t) {
        handler.add( t );
      }
      handler.throwFirstExceptionIfAny();
    }
  }

  /**
   * All this does is check for #SQLObject objects, and call ps.setObject() with the 
   * equivalent java.sql.Types type.
   */
  protected static void setObject(int pos, Object obj, PreparedStatement ps) 
    throws SQLException
  {
    if ( ps == null ) throw new IllegalArgumentException("Don't pass in a null PreparedStatement!");
    if ( pos < 1    ) throw new IllegalArgumentException("pos must be >= 1!");
    if ( obj instanceof SQLObject ) {
      SQLObject sqlObj = (SQLObject) obj;
      ps.setObject( pos, sqlObj.getObject(), sqlObj.getSqlType() );
    } else if ( obj == null ) {
      try {
        ps.setObject( pos, obj );
      } catch (SQLException e) {
        DatabaseMetaData metaData = ps.getConnection().getMetaData();
        if ( "Oracle".equals(metaData.getDatabaseProductName()) ) {
               logger.error("setObject(int, Object, PreparedStatement)", e); //$NON-NLS-1$
          throw new IllegalStateException("Your parameter (pos: " +
            pos + ") is null.  As of Oracle jdbc driver 9.2.0.1.0, PreparedStatement.setObject() " +
            "can't handle nulls.  Wrap your String nulls with checkNull, other types with any of " +
            "the SimpleSQL.SQLObject types");
        } else {
               logger
                        .error(
                                 "setObject(int, Object, PreparedStatement) - nnNOTICE: You're probably getting an SQLException because your parameter (pos: " + pos + ") is null and you didn't wrap it with the checkNull() method or a valid SimpleSQL.SQLObject!nn", e); //$NON-NLS-1$ //$NON-NLS-2$
          throw e;
        }
      }
    } else {
      ps.setObject( pos, obj );
    }
  }

  private ResultSet getResultSet(ResultSet results, Statement statement, Connection connection, boolean closeConnectionsOnResultSetClose) {
    if ( connection == null ) throw new IllegalArgumentException("Don't pass me a null connection!");
    return new SimpleSQLResultSet(results, statement, connection, closeConnectionsOnResultSetClose);
  }

  /** Though I don't know right now why someone would override this, I'm making it protected
   * so it is an option.
   */
  protected class SimpleSQLResultSet extends ResultSetWrapper {
    private Connection connection;
    private Statement statement;
    protected boolean closeConnectionsOnResultSetClose = false;

    /** This is called inside executeQuery(), so the ResultSet returned will be smart enough
     * to close its related Statement and Connection.  In the SimpleSQL implementation, 
     * closeConnectionsOnResultSetClose is false, because we don't want to close connections
     * opened by someone else.
     */
    protected SimpleSQLResultSet(ResultSet results, Statement statement, Connection connection, boolean closeConnectionsOnResultSetClose) {
      super( results );
      this.statement = statement;
      this.connection = connection;
      this.closeConnectionsOnResultSetClose = closeConnectionsOnResultSetClose;
    }

    public void close() throws SQLException {
      SQLExceptionHandler handler = new SQLExceptionHandler();
      try {
        super.close();
      } catch (Throwable t) {
        handler.add( t );
      }
      try {
        if ( statement != null ) statement.close();
      } catch (Throwable t) {
        handler.add( t );
      }
      try {
        if ( closeConnectionsOnResultSetClose == true && connection != null ) {
          connection.close();
        }
      } catch (Throwable t) {
        handler.add( t );
      }
      handler.throwFirstExceptionIfAny();
    }
  }

  public static SQLObject checkNull(String var) {
    return new SQLObject( var, java.sql.Types.VARCHAR );
  }

  public static class SQLObject {
    private Object obj = null;
    private int sqlType = -1;

    public SQLObject(Object obj, int sqlType) {
      this.obj = obj;
      this.sqlType = sqlType;
    }

    public Object getObject() {
      return obj;
    }

    public int getSqlType() {
      return sqlType;
    }
  }

  /**
   * <P>The constant in the Java programming language, sometimes referred
   * to as a type code, that identifies the generic SQL type 
   * <code>BIT</code>.
   */
  public final static SQLObject NULL_BIT = new SQLObject(null, java.sql.Types.BIT);

  /**
   * <P>The constant in the Java programming language, sometimes referred
   * to as a type code, that identifies the generic SQL type 
   * <code>TINYINT</code>.
   */
  public final static SQLObject NULL_TINYINT = new SQLObject(null, java.sql.Types.TINYINT);

  /**
   * <P>The constant in the Java programming language, sometimes referred
   * to as a type code, that identifies the generic SQL type 
   * <code>SMALLINT</code>.
   */
  public final static SQLObject NULL_SMALLINT = new SQLObject(null, java.sql.Types.SMALLINT);

  /**
   * <P>The constant in the Java programming language, sometimes referred
   * to as a type code, that identifies the generic SQL type 
   * <code>INTEGER</code>.
   */
  public final static SQLObject NULL_INTEGER = new SQLObject(null, java.sql.Types.INTEGER);

  /**
   * <P>The constant in the Java programming language, sometimes referred
   * to as a type code, that identifies the generic SQL type 
   * <code>BIGINT</code>.
   */
  public final static SQLObject NULL_BIGINT = new SQLObject(null, java.sql.Types.BIGINT);

  /**
   * <P>The constant in the Java programming language, sometimes referred
   * to as a type code, that identifies the generic SQL type 
   * <code>FLOAT</code>.
   */
  public final static SQLObject NULL_FLOAT = new SQLObject(null, java.sql.Types.FLOAT);

  /**
   * <P>The constant in the Java programming language, sometimes referred
   * to as a type code, that identifies the generic SQL type 
   * <code>REAL</code>.
   */
  public final static SQLObject NULL_REAL = new SQLObject(null, java.sql.Types.REAL);


  /**
   * <P>The constant in the Java programming language, sometimes referred
   * to as a type code, that identifies the generic SQL type 
   * <code>DOUBLE</code>.
   */
  public final static SQLObject NULL_DOUBLE = new SQLObject(null, java.sql.Types.DOUBLE);

  /**
   * <P>The constant in the Java programming language, sometimes referred
   * to as a type code, that identifies the generic SQL type 
   * <code>NUMERIC</code>.
   */
  public final static SQLObject NULL_NUMERIC = new SQLObject(null, java.sql.Types.NUMERIC);

  /**
   * <P>The constant in the Java programming language, sometimes referred
   * to as a type code, that identifies the generic SQL type 
   * <code>DECIMAL</code>.
   */
  public final static SQLObject NULL_DECIMAL = new SQLObject(null, java.sql.Types.DECIMAL);

  /**
   * <P>The constant in the Java programming language, sometimes referred
   * to as a type code, that identifies the generic SQL type 
   * <code>CHAR</code>.
   */
  public final static SQLObject NULL_CHAR = new SQLObject(null, java.sql.Types.CHAR);

  /**
   * <P>The constant in the Java programming language, sometimes referred
   * to as a type code, that identifies the generic SQL type 
   * <code>VARCHAR</code>.
   */
  public final static SQLObject NULL_VARCHAR = new SQLObject(null, java.sql.Types.VARCHAR);

  /**
   * <P>The constant in the Java programming language, sometimes referred
   * to as a type code, that identifies the generic SQL type 
   * <code>LONGVARCHAR</code>.
   */
  public final static SQLObject NULL_LONGVARCHAR = new SQLObject(null, java.sql.Types.LONGVARCHAR);


  /**
   * <P>The constant in the Java programming language, sometimes referred
   * to as a type code, that identifies the generic SQL type 
   * <code>DATE</code>.
   */
  public final static SQLObject NULL_DATE = new SQLObject(null, java.sql.Types.DATE);

  /**
   * <P>The constant in the Java programming language, sometimes referred
   * to as a type code, that identifies the generic SQL type 
   * <code>TIME</code>.
   */
  public final static SQLObject NULL_TIME = new SQLObject(null, java.sql.Types.TIME);

  /**
   * <P>The constant in the Java programming language, sometimes referred
   * to as a type code, that identifies the generic SQL type 
   * <code>TIMESTAMP</code>.
   */
  public final static SQLObject NULL_TIMESTAMP = new SQLObject(null, java.sql.Types.TIMESTAMP);


  /**
   * <P>The constant in the Java programming language, sometimes referred
   * to as a type code, that identifies the generic SQL type 
   * <code>BINARY</code>.
   */
  public final static SQLObject NULL_BINARY = new SQLObject(null, java.sql.Types.BINARY);

  /**
   * <P>The constant in the Java programming language, sometimes referred
   * to as a type code, that identifies the generic SQL type 
   * <code>VARBINARY</code>.
   */
  public final static SQLObject NULL_VARBINARY = new SQLObject(null, java.sql.Types.VARBINARY);

  /**
   * <P>The constant in the Java programming language, sometimes referred
   * to as a type code, that identifies the generic SQL type 
   * <code>LONGVARBINARY</code>.
   */
  public final static SQLObject NULL_LONGVARBINARY = new SQLObject(null, java.sql.Types.LONGVARBINARY);

  /**
   * <P>The constant in the Java programming language, sometimes referred
   * to as a type code, that identifies the generic SQL type 
   * <code>NULL</code>.
   */
  public final static SQLObject NULL_NULL = new SQLObject(null, java.sql.Types.NULL);

  /**
   * The constant in the Java programming language that indicates
   * that the SQL type is database-specific and
   * gets mapped to a Java object that can be accessed via
   * the methods <code>getObject</code> and <code>setObject</code>.
   */
  public final static SQLObject NULL_OTHER = new SQLObject(null, java.sql.Types.OTHER);



  /**
   * The constant in the Java programming language, sometimes referred to
   * as a type code, that identifies the generic SQL type
   * <code>JAVA_OBJECT</code>.
   * @since 1.2
   */
  public final static SQLObject NULL_JAVA_OBJECT = new SQLObject(null, java.sql.Types.JAVA_OBJECT);

  /**
   * The constant in the Java programming language, sometimes referred to
   * as a type code, that identifies the generic SQL type
   * <code>DISTINCT</code>.
   * @since 1.2
   */
  public final static SQLObject NULL_DISTINCT = new SQLObject(null, java.sql.Types.DISTINCT);

  /**
   * The constant in the Java programming language, sometimes referred to
   * as a type code, that identifies the generic SQL type
   * <code>STRUCT</code>.
   * @since 1.2
   */
  public final static SQLObject NULL_STRUCT = new SQLObject(null, java.sql.Types.STRUCT);

  /**
   * The constant in the Java programming language, sometimes referred to
   * as a type code, that identifies the generic SQL type
   * <code>ARRAY</code>.
   * @since 1.2
   */
  public final static SQLObject NULL_ARRAY = new SQLObject(null, java.sql.Types.ARRAY);

  /**
   * The constant in the Java programming language, sometimes referred to
   * as a type code, that identifies the generic SQL type
   * <code>BLOB</code>.
   * @since 1.2
   */
  public final static SQLObject NULL_BLOB = new SQLObject(null, java.sql.Types.BLOB);

  /**
   * The constant in the Java programming language, sometimes referred to
   * as a type code, that identifies the generic SQL type
   * <code>CLOB</code>.
   * @since 1.2
   */
  public final static SQLObject NULL_CLOB = new SQLObject(null, java.sql.Types.CLOB);

  /**
   * The constant in the Java programming language, sometimes referred to
   * as a type code, that identifies the generic SQL type
   * <code>REF</code>.
   * @since 1.2
   */
  public final static SQLObject NULL_REF = new SQLObject(null, java.sql.Types.REF);

  /**
   * The constant in the Java programming language, somtimes referred to
   * as a type code, that identifies the generic SQL type <code>DATALINK</code>.
   *
   * @since 1.4
   */
  // Comment_next_line_to_compile_with_Java_1.3
  public final static SQLObject NULL_DATALINK = new SQLObject(null, java.sql.Types.DATALINK);

  /**
   * The constant in the Java programming language, somtimes referred to
   * as a type code, that identifies the generic SQL type <code>BOOLEAN</code>.
   *
   * @since 1.4
   */
  // Comment_next_line_to_compile_with_Java_1.3
  public final static SQLObject NULL_BOOLEAN = new SQLObject(null, java.sql.Types.BOOLEAN);

}
