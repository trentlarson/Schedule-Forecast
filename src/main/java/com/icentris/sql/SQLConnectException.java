package com.icentris.sql;

import java.sql.SQLException;

public class SQLConnectException extends SQLException {
  
  private static final long serialVersionUID = -1399752036538730299L;
  
  public SQLConnectException(SQLException e) {
    super(e.getMessage(), e.getSQLState(), e.getErrorCode());
    initCause(e);
  }
  
  public SQLConnectException(Throwable e) {
    super();
    initCause(e);
  }
  
}
