package com.icentris.sql;

import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.util.Calendar;
import java.util.Map;

public class ResultSetWrapper implements ResultSet {
  private ResultSet realResultSet;

  public ResultSetWrapper(ResultSet resultSet) {
    realResultSet = resultSet;
  }



  //--------------------------------------------------------------
  // Overridden methods
  //--------------------------------------------------------------
  public boolean next() throws SQLException {
    boolean result = realResultSet.next();
    return result;
  }

  public void close() throws SQLException {
    realResultSet.close();
  }

  public boolean wasNull() throws SQLException {
    return realResultSet.wasNull();
  }

  public String getString(int columnIndex) throws SQLException {
    String result = realResultSet.getString(columnIndex);
    return result;
  }

  public boolean getBoolean(int columnIndex) throws SQLException {
    return realResultSet.getBoolean(columnIndex);
  }

  public byte getByte(int columnIndex) throws SQLException {
    return realResultSet.getByte(columnIndex);
  }

  public short getShort(int columnIndex) throws SQLException {
    return realResultSet.getShort(columnIndex);
  }

  public int getInt(int columnIndex) throws SQLException {
    return realResultSet.getInt(columnIndex);
  }

  public long getLong(int columnIndex) throws SQLException {
    return realResultSet.getLong(columnIndex);
  }

  public float getFloat(int columnIndex) throws SQLException {
    return realResultSet.getFloat(columnIndex);
  }

  public double getDouble(int columnIndex) throws SQLException {
    return realResultSet.getDouble(columnIndex);
  }

  public BigDecimal getBigDecimal(int columnIndex, int scale) throws SQLException {
    return realResultSet.getBigDecimal(columnIndex);
  }

  public byte[] getBytes(int columnIndex) throws SQLException {
    return realResultSet.getBytes(columnIndex);
  }

  public java.sql.Date getDate(int columnIndex) throws SQLException {
    return realResultSet.getDate(columnIndex);
  }

  public java.sql.Time getTime(int columnIndex) throws SQLException {
    return realResultSet.getTime(columnIndex);
  }

  public java.sql.Timestamp getTimestamp(int columnIndex) throws SQLException {
    return realResultSet.getTimestamp(columnIndex);
  }

  public java.io.InputStream getAsciiStream(int columnIndex) throws SQLException {
    return realResultSet.getAsciiStream(columnIndex);
  }

  @Deprecated
  public java.io.InputStream getUnicodeStream(int columnIndex) throws SQLException {
    return realResultSet.getUnicodeStream(columnIndex);
  }

  public java.io.InputStream getBinaryStream(int columnIndex) throws SQLException {
    return realResultSet.getBinaryStream(columnIndex);
  }


  //======================================================================
  // Methods for accessing results by column name
  //======================================================================

  public String getString(String columnName) throws SQLException {
    String result = realResultSet.getString(columnName);
    return result;
  }

  public boolean getBoolean(String columnName) throws SQLException {
    return realResultSet.getBoolean(columnName);
  }

  public byte getByte(String columnName) throws SQLException {
    return realResultSet.getByte(columnName);
  }

  public short getShort(String columnName) throws SQLException {
    return realResultSet.getShort(columnName);
  }

  public int getInt(String columnName) throws SQLException {
    return realResultSet.getInt(columnName);
  }

  public long getLong(String columnName) throws SQLException {
    return realResultSet.getLong(columnName);
  }

  public float getFloat(String columnName) throws SQLException {
    return realResultSet.getFloat(columnName);
  }

  public double getDouble(String columnName) throws SQLException {
    return realResultSet.getDouble(columnName);
  }

  public BigDecimal getBigDecimal(String columnName, int scale) throws SQLException {
    return realResultSet.getBigDecimal(columnName);
  }

  public byte[] getBytes(String columnName) throws SQLException {
    return realResultSet.getBytes(columnName);
  }

  public java.sql.Date getDate(String columnName) throws SQLException {
    return realResultSet.getDate(columnName);
  }

  public java.sql.Time getTime(String columnName) throws SQLException {
    return realResultSet.getTime(columnName);
  }

  public java.sql.Timestamp getTimestamp(String columnName) throws SQLException {
    return realResultSet.getTimestamp(columnName);
  }

  public java.io.InputStream getAsciiStream(String columnName) throws SQLException {
    return realResultSet.getAsciiStream(columnName);
  }

  @Deprecated
  public java.io.InputStream getUnicodeStream(String columnName) throws SQLException {
    return realResultSet.getUnicodeStream(columnName);
  }

  public java.io.InputStream getBinaryStream(String columnName) throws SQLException {
    return realResultSet.getBinaryStream(columnName);
  }


  //=====================================================================
  // Advanced features:
  //=====================================================================

  public SQLWarning getWarnings() throws SQLException {
    return realResultSet.getWarnings();
  }

  public void clearWarnings() throws SQLException {
    realResultSet.clearWarnings();
  }

  public String getCursorName() throws SQLException {
    return realResultSet.getCursorName();
  }

  public ResultSetMetaData getMetaData() throws SQLException {
    return realResultSet.getMetaData();
  }

  public Object getObject(int columnIndex) throws SQLException {
    return realResultSet.getObject(columnIndex);
  }

  public Object getObject(String columnName) throws SQLException {
    return realResultSet.getObject(columnName);
  }

  //----------------------------------------------------------------

  public int findColumn(String columnName) throws SQLException {
    return realResultSet.findColumn(columnName);
  }


  //--------------------------JDBC 2.0-----------------------------------

  //---------------------------------------------------------------------
  // Getters and Setters
  //---------------------------------------------------------------------

  public java.io.Reader getCharacterStream(int columnIndex) throws SQLException {
    return realResultSet.getCharacterStream(columnIndex);
  }

  public java.io.Reader getCharacterStream(String columnName) throws SQLException {
    return realResultSet.getCharacterStream(columnName);
  }

  public BigDecimal getBigDecimal(int columnIndex) throws SQLException {
    return realResultSet.getBigDecimal(columnIndex);
  }

  public BigDecimal getBigDecimal(String columnName) throws SQLException {
    return realResultSet.getBigDecimal(columnName);
  }

  //---------------------------------------------------------------------
  // Traversal/Positioning
  //---------------------------------------------------------------------

  public boolean isBeforeFirst() throws SQLException {
    return realResultSet.isBeforeFirst();
  }

  public boolean isAfterLast() throws SQLException {
    return realResultSet.isAfterLast();
  }

  public boolean isFirst() throws SQLException {
    return realResultSet.isFirst();
  }

  public boolean isLast() throws SQLException {
    return realResultSet.isLast();
  }

  public void beforeFirst() throws SQLException {
    realResultSet.beforeFirst();
  }

  public void afterLast() throws SQLException {
    realResultSet.afterLast();
  }

  public boolean first() throws SQLException {
    return realResultSet.first();
  }

  public boolean last() throws SQLException {
    return realResultSet.last();
  }

  public int getRow() throws SQLException {
    return realResultSet.getRow();
  }

  public boolean absolute( int row ) throws SQLException {
    return realResultSet.absolute(row);
  }

  public boolean relative( int rows ) throws SQLException {
    return realResultSet.relative(rows);
  }

  public boolean previous() throws SQLException {
    return realResultSet.previous();
  }

  //---------------------------------------------------------------------
  // Properties
  //---------------------------------------------------------------------

  public void setFetchDirection(int direction) throws SQLException {
    realResultSet.setFetchDirection(direction);
  }

  public int getFetchDirection() throws SQLException {
    return realResultSet.getFetchDirection();
  }

  public void setFetchSize(int rows) throws SQLException {
    realResultSet.setFetchSize(rows);
  }

  public int getFetchSize() throws SQLException {
    return realResultSet.getFetchSize();
  }

  public int getType() throws SQLException {
    return realResultSet.getType();
  }

  public int getConcurrency() throws SQLException {
    return realResultSet.getConcurrency();
  }

  //---------------------------------------------------------------------
  // Updates
  //---------------------------------------------------------------------

  public boolean rowUpdated() throws SQLException {
    return realResultSet.rowUpdated();
  }

  public boolean rowInserted() throws SQLException {
    return realResultSet.rowInserted();
  }

  public boolean rowDeleted() throws SQLException {
    return realResultSet.rowDeleted();
  }

  public void updateNull(int columnIndex) throws SQLException {
    realResultSet.updateNull(columnIndex);
  }  

  public void updateBoolean(int columnIndex, boolean x) throws SQLException {
    realResultSet.updateBoolean(columnIndex, x);
  }

  public void updateByte(int columnIndex, byte x) throws SQLException {
    realResultSet.updateByte(columnIndex, x);
  }

  public void updateShort(int columnIndex, short x) throws SQLException {
    realResultSet.updateShort(columnIndex, x);
  }

  public void updateInt(int columnIndex, int x) throws SQLException {
    realResultSet.updateInt(columnIndex, x);
  }

  public void updateLong(int columnIndex, long x) throws SQLException {
    realResultSet.updateLong(columnIndex, x);
  }

  public void updateFloat(int columnIndex, float x) throws SQLException {
    realResultSet.updateFloat(columnIndex, x);
  }

  public void updateDouble(int columnIndex, double x) throws SQLException {
    realResultSet.updateDouble(columnIndex, x);
  }

  public void updateBigDecimal(int columnIndex, BigDecimal x) throws SQLException {
    realResultSet.updateBigDecimal(columnIndex, x);
  }

  public void updateString(int columnIndex, String x) throws SQLException {
    realResultSet.updateString(columnIndex, x);
  }

  public void updateBytes(int columnIndex, byte x[]) throws SQLException {
    realResultSet.updateBytes(columnIndex, x);
  }

  public void updateDate(int columnIndex, java.sql.Date x) throws SQLException {
    realResultSet.updateDate(columnIndex, x);
  }

  public void updateTime(int columnIndex, java.sql.Time x) throws SQLException {
    realResultSet.updateTime(columnIndex, x);
  }

  public void updateTimestamp(int columnIndex, java.sql.Timestamp x) throws SQLException {
    realResultSet.updateTimestamp(columnIndex, x);
  }

  public void updateAsciiStream(int columnIndex, java.io.InputStream x, int length) throws SQLException {
    realResultSet.updateAsciiStream(columnIndex, x, length);
  }

  public void updateBinaryStream(int columnIndex, java.io.InputStream x, int length) throws SQLException {
    realResultSet.updateBinaryStream(columnIndex, x, length);
  }

  public void updateCharacterStream(int columnIndex, java.io.Reader x, int length) throws SQLException {
    realResultSet.updateCharacterStream(columnIndex, x, length);
  }

  public void updateObject(int columnIndex, Object x, int scale) throws SQLException {
    realResultSet.updateObject(columnIndex, x, scale);
  }

  public void updateObject(int columnIndex, Object x) throws SQLException {
    realResultSet.updateObject(columnIndex, x);
  }

  public void updateNull(String columnName) throws SQLException {
    realResultSet.updateNull(columnName);
  }  

  public void updateBoolean(String columnName, boolean x) throws SQLException {
    realResultSet.updateBoolean(columnName, x);
  }

  public void updateByte(String columnName, byte x) throws SQLException {
    realResultSet.updateByte(columnName, x);
  }

  public void updateShort(String columnName, short x) throws SQLException {
    realResultSet.updateShort(columnName, x);
  }

  public void updateInt(String columnName, int x) throws SQLException {
    realResultSet.updateInt(columnName, x);
  }

  public void updateLong(String columnName, long x) throws SQLException {
    realResultSet.updateLong(columnName, x);
  }

  public void updateFloat(String columnName, float x) throws SQLException {
    realResultSet.updateFloat(columnName, x);
  }

  public void updateDouble(String columnName, double x) throws SQLException {
    realResultSet.updateDouble(columnName, x);
  }

  public void updateBigDecimal(String columnName, BigDecimal x) throws SQLException {
    realResultSet.updateBigDecimal(columnName, x);
  }

  public void updateString(String columnName, String x) throws SQLException {
    realResultSet.updateString(columnName, x);
  }

  public void updateBytes(String columnName, byte x[]) throws SQLException {
    realResultSet.updateBytes(columnName, x);
  }

  public void updateDate(String columnName, java.sql.Date x) throws SQLException {
    realResultSet.updateDate(columnName, x);
  }

  public void updateTime(String columnName, java.sql.Time x) throws SQLException {
    realResultSet.updateTime(columnName, x);
  }

  public void updateTimestamp(String columnName, java.sql.Timestamp x) throws SQLException {
    realResultSet.updateTimestamp(columnName, x);
  }

  public void updateAsciiStream(String columnName, java.io.InputStream x, int length) throws SQLException {
    realResultSet.updateAsciiStream(columnName, x, length);
  }

  public void updateBinaryStream(String columnName, java.io.InputStream x, int length) throws SQLException {
    realResultSet.updateBinaryStream(columnName, x, length);
  }

  public void updateCharacterStream(String columnName, java.io.Reader reader, int length) throws SQLException {
    realResultSet.updateCharacterStream(columnName, reader, length);
  }

  public void updateObject(String columnName, Object x, int scale) throws SQLException {
    realResultSet.updateObject(columnName, x, scale);
  }

  public void updateObject(String columnName, Object x) throws SQLException {
    realResultSet.updateObject(columnName, x);
  }

  public void insertRow() throws SQLException {
    realResultSet.insertRow();
  }

  public void updateRow() throws SQLException {
    realResultSet.updateRow();
  }

  public void deleteRow() throws SQLException {
    realResultSet.deleteRow();
  }

  public void refreshRow() throws SQLException {
    realResultSet.refreshRow();
  }

  public void cancelRowUpdates() throws SQLException {
    realResultSet.cancelRowUpdates();
  }

  public void moveToInsertRow() throws SQLException {
    realResultSet.moveToInsertRow();
  }

  public void moveToCurrentRow() throws SQLException {
    realResultSet.moveToCurrentRow();
  }

  public Statement getStatement() throws SQLException {
    return realResultSet.getStatement();
  }

  public Ref getRef(int i) throws SQLException {
    return realResultSet.getRef(i);
  }

  public Blob getBlob(int i) throws SQLException {
    return realResultSet.getBlob(i);
  }

  public Clob getClob(int i) throws SQLException {
    return realResultSet.getClob(i);
  }

  public Array getArray(int i) throws SQLException {
    return realResultSet.getArray(i);
  }

  public Ref getRef(String colName) throws SQLException {
    return realResultSet.getRef(colName);
  }

  public Blob getBlob(String colName) throws SQLException {
    return realResultSet.getBlob(colName);
  }

  public Clob getClob(String colName) throws SQLException {
    return realResultSet.getClob(colName);
  }

  public Array getArray(String colName) throws SQLException {
    return realResultSet.getArray(colName);
  }

  public java.sql.Date getDate(int columnIndex, Calendar cal) throws SQLException {
    return realResultSet.getDate(columnIndex,cal);
  }

  public java.sql.Date getDate(String columnName, Calendar cal) throws SQLException {
    return realResultSet.getDate(columnName,cal);
  }

  public java.sql.Time getTime(int columnIndex, Calendar cal) throws SQLException {
    return realResultSet.getTime(columnIndex,cal);
  }

  public java.sql.Time getTime(String columnName, Calendar cal) throws SQLException {
    return realResultSet.getTime(columnName,cal);
  }

  public java.sql.Timestamp getTimestamp(int columnIndex, Calendar cal) throws SQLException {
    return realResultSet.getTimestamp(columnIndex,cal);
  }

  public java.sql.Timestamp getTimestamp(String columnName, Calendar cal) throws SQLException {
    return realResultSet.getTimestamp(columnName,cal);
  }

  //-------------------------- JDBC 3.0 ----------------------------------------

  public java.net.URL getURL(int columnIndex) throws SQLException {
    return realResultSet.getURL(columnIndex);
  }

  public java.net.URL getURL(String columnName) throws SQLException {
    return getURL(columnName);
  }

  public void updateRef(int columnIndex, java.sql.Ref x) throws SQLException {
    updateRef(columnIndex, x);
  }

  public void updateRef(String columnName, java.sql.Ref x) throws SQLException {
    updateRef(columnName, x);
  }

  public void updateBlob(int columnIndex, java.sql.Blob x) throws SQLException {
    updateBlob(columnIndex, x);
  }

  public void updateBlob(String columnName, java.sql.Blob x) throws SQLException {
    updateBlob(columnName, x);
  }

  public void updateClob(int columnIndex, java.sql.Clob x) throws SQLException {
    updateClob(columnIndex, x);
  }

  public void updateClob(String columnName, java.sql.Clob x) throws SQLException {
    updateClob(columnName, x);
  }

  public void updateArray(int columnIndex, java.sql.Array x) throws SQLException {
    updateArray(columnIndex, x);
  }

  public void updateArray(String columnName, java.sql.Array x) throws SQLException {
    updateArray(columnName, x);
  }



  public int getHoldability() throws SQLException {
    return getHoldability();
  }



  public Reader getNCharacterStream(int columnIndex) throws SQLException {
    return getNCharacterStream(columnIndex);
  }



  public Reader getNCharacterStream(String columnLabel) throws SQLException {
    return getNCharacterStream(columnLabel);
  }



  public NClob getNClob(int columnIndex) throws SQLException {
    return getNClob(columnIndex);
  }



  public NClob getNClob(String columnLabel) throws SQLException {
    return getNClob(columnLabel);
  }



  public String getNString(int columnIndex) throws SQLException {
    return getNString(columnIndex);
  }



  public String getNString(String columnLabel) throws SQLException {
    return getNString(columnLabel);
  }



  public Object getObject(int arg0, Map<String, Class<?>> arg1) throws SQLException {
    return getObject(arg0, arg1);
  }



  public Object getObject(String arg0, Map<String, Class<?>> arg1) throws SQLException {
    return getObject(arg0, arg1);
  }



  public RowId getRowId(int columnIndex) throws SQLException {
    return getRowId(columnIndex);
  }



  public RowId getRowId(String columnLabel) throws SQLException {
    return null;
  }



  public SQLXML getSQLXML(int columnIndex) throws SQLException {
    return getSQLXML(columnIndex);
  }



  public SQLXML getSQLXML(String columnLabel) throws SQLException {
    return getSQLXML(columnLabel);
  }



  public boolean isClosed() throws SQLException {
    return isClosed();
  }



  public void updateAsciiStream(int columnIndex, InputStream x) throws SQLException {
    updateAsciiStream(columnIndex, x);
  }



  public void updateAsciiStream(String columnLabel, InputStream x) throws SQLException {
    updateAsciiStream(columnLabel, x);
  }



  public void updateAsciiStream(int columnIndex, InputStream x, long length) throws SQLException {
    updateAsciiStream(columnIndex, x, length);
    
  }



  public void updateAsciiStream(String columnLabel, InputStream x, long length) throws SQLException {
    updateAsciiStream(columnLabel, x, length);
  }



  public void updateBinaryStream(int columnIndex, InputStream x) throws SQLException {
    updateBinaryStream(columnIndex, x);
  }



  public void updateBinaryStream(String columnLabel, InputStream x) throws SQLException {
    updateBinaryStream(columnLabel, x);
  }



  public void updateBinaryStream(int columnIndex, InputStream x, long length) throws SQLException {
    updateBinaryStream(columnIndex, x, length);
  }



  public void updateBinaryStream(String columnLabel, InputStream x, long length) throws SQLException {
    updateBinaryStream(columnLabel, x, length);
  }



  public void updateBlob(int columnIndex, InputStream inputStream) throws SQLException {
    updateBlob(columnIndex, inputStream);
  }



  public void updateBlob(String columnLabel, InputStream inputStream) throws SQLException {
    updateBlob(columnLabel, inputStream);
  }



  public void updateBlob(int columnIndex, InputStream inputStream, long length) throws SQLException {
    updateBlob(columnIndex, inputStream, length);
  }



  public void updateBlob(String columnLabel, InputStream inputStream, long length) throws SQLException {
    updateBlob(columnLabel, inputStream, length);
  }



  public void updateCharacterStream(int columnIndex, Reader x) throws SQLException {
    updateCharacterStream(columnIndex, x);
  }



  public void updateCharacterStream(String columnLabel, Reader reader) throws SQLException {
    updateCharacterStream(columnLabel, reader);
  }



  public void updateCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
    updateCharacterStream(columnIndex, x, length);
  }



  public void updateCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
    updateCharacterStream(columnLabel, reader, length);
  }



  public void updateClob(int columnIndex, Reader reader) throws SQLException {
    updateClob(columnIndex, reader);
  }



  public void updateClob(String columnLabel, Reader reader) throws SQLException {
    updateClob(columnLabel, reader);
  }



  public void updateClob(int columnIndex, Reader reader, long length) throws SQLException {
    updateClob(columnIndex, reader, length);
  }



  public void updateClob(String columnLabel, Reader reader, long length) throws SQLException {
    updateClob(columnLabel, reader, length);
  }



  public void updateNCharacterStream(int columnIndex, Reader x) throws SQLException {
    updateNCharacterStream(columnIndex, x);
  }



  public void updateNCharacterStream(String columnLabel, Reader reader) throws SQLException {
    updateNCharacterStream(columnLabel, reader);
  }



  public void updateNCharacterStream(int columnIndex, Reader x, long length) throws SQLException {
    updateNCharacterStream(columnIndex, x, length);
  }



  public void updateNCharacterStream(String columnLabel, Reader reader, long length) throws SQLException {
    updateNCharacterStream(columnLabel, reader, length);
  }



  public void updateNClob(int columnIndex, NClob nClob) throws SQLException {
    updateNClob(columnIndex, nClob);
  }



  public void updateNClob(String columnLabel, NClob nClob) throws SQLException {
    updateNClob(columnLabel, nClob);
  }



  public void updateNClob(int columnIndex, Reader reader) throws SQLException {
    updateNClob(columnIndex, reader);
  }



  public void updateNClob(String columnLabel, Reader reader) throws SQLException {
    updateNClob(columnLabel, reader);
  }



  public void updateNClob(int columnIndex, Reader reader, long length) throws SQLException {
    updateNClob(columnIndex, reader, length);
  }



  public void updateNClob(String columnLabel, Reader reader, long length) throws SQLException {
    updateNClob(columnLabel, reader, length);
  }



  public void updateNString(int columnIndex, String nString) throws SQLException {
    updateNString(columnIndex, nString);
  }



  public void updateNString(String columnLabel, String nString) throws SQLException {
    updateNString(columnLabel, nString);
  }



  public void updateRowId(int columnIndex, RowId x) throws SQLException {
    updateRowId(columnIndex, x);
  }



  public void updateRowId(String columnLabel, RowId x) throws SQLException {
    updateRowId(columnLabel, x);
  }



  public void updateSQLXML(int columnIndex, SQLXML xmlObject) throws SQLException {
    updateSQLXML(columnIndex, xmlObject);
  }



  public void updateSQLXML(String columnLabel, SQLXML xmlObject) throws SQLException {
    updateSQLXML(columnLabel, xmlObject);
  }



  public boolean isWrapperFor(Class<?> iface) throws SQLException {
    return isWrapperFor(iface);
  }



  public <T> T unwrap(Class<T> iface) throws SQLException {
    return unwrap(iface);
  }

}
