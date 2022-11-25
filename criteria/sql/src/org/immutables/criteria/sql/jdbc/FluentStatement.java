/*
 * Copyright 2022 Immutables Authors and Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.immutables.criteria.sql.jdbc;

import javax.sql.DataSource;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Date;
import java.sql.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.sql.Statement.RETURN_GENERATED_KEYS;

public class FluentStatement implements AutoCloseable {
  static final Pattern PATTERN = Pattern.compile("(?<!')(:[\\w]*)(?!')");
  private final PreparedStatement statement;

  private final Connection connection;
  private final List<String> fields = new ArrayList<>();

  public FluentStatement(final Connection connection, final String sql) throws SQLException {
    this(connection, sql, true);
  }

  public FluentStatement(final Connection connection, final String sql, final boolean keys) throws SQLException {
    final Matcher matcher = PATTERN.matcher(sql);
    while (matcher.find()) {
      fields.add(matcher.group());
    }
    final String transformed = sql.replaceAll(PATTERN.pattern(), "?");
    this.connection = connection;
    statement = keys
        ? connection.prepareStatement(transformed, RETURN_GENERATED_KEYS)
        : connection.prepareStatement(transformed);
  }

  public static FluentStatement of(final Connection connection, final String sql) throws SQLException {
    return new FluentStatement(connection, sql);
  }

  public static FluentStatement of(final DataSource ds, final String sql) throws SQLException {
    return new FluentStatement(ds.getConnection(), sql);
  }

  public static <T> List<T> convert(final ResultSet results, final Mapper<T> mapper) throws SQLException {
    final List<T> ret = new ArrayList<>();
    while (results.next()) {
      ret.add(mapper.handle(results));
    }
    return ret;
  }

  public PreparedStatement statement() {
    return statement;
  }

  public int insert(final List<List<Object>> values) throws SQLException {
    return batch(values);
  }

  public <T> List<T> list(final Mapper<T> mapper) throws SQLException {
    return convert(statement.executeQuery(), mapper);
  }

  public int batch(final List<List<Object>> values) throws SQLException {
    connection.setAutoCommit(false);
    for (final List<Object> row : values) {
      for (int i = 0; i < row.size(); i++) {
        set(i + 1, row.get(i));
      }
      statement.addBatch();
    }
    final int[] counts = statement.executeBatch();
    connection.commit();
    return Arrays.stream(counts).sum();

  }

  public int update(final List<List<Object>> values) throws SQLException {
    return batch(values);
  }

  public int update() throws SQLException {
    return statement.executeUpdate();
  }

  public int delete() throws SQLException {
    return statement.executeUpdate();
  }

  @Override
  public void close() throws SQLException {
    statement.close();
  }

  public FluentStatement set(final Map<String, Object> parameters) throws SQLException {
    assert parameters != null : "parameters cannot be null";
    for (final Map.Entry<String, Object> e : parameters.entrySet()) {
      set(e.getKey(), e.getValue());
    }
    return this;
  }

  public FluentStatement set(final int index, final byte value) throws SQLException {
    statement.setByte(index, value);
    return this;
  }

  public FluentStatement set(final int index, final short value) throws SQLException {
    statement.setShort(index, value);
    return this;
  }

  public FluentStatement set(final int index, final int value) throws SQLException {
    statement.setInt(index, value);
    return this;
  }

  public FluentStatement set(final int index, final long value) throws SQLException {
    statement.setLong(index, value);
    return this;
  }

  public FluentStatement set(final int index, final float value) throws SQLException {
    statement.setFloat(index, value);
    return this;
  }

  public FluentStatement set(final int index, final double value) throws SQLException {
    statement.setDouble(index, value);
    return this;
  }

  public FluentStatement set(final int index, final boolean value) throws SQLException {
    statement.setBoolean(index, value);
    return this;
  }

  public FluentStatement set(final int index, final String value) throws SQLException {
    statement.setString(index, value);
    return this;
  }

  public FluentStatement set(final int index, final BigDecimal value) throws SQLException {
    statement.setBigDecimal(index, value);
    return this;
  }

  public FluentStatement set(final int index, final byte[] value) throws SQLException {
    statement.setBytes(index, value);
    return this;
  }

  public FluentStatement set(final int index, final Date value) throws SQLException {
    statement.setDate(index, value);
    return this;
  }

  public FluentStatement set(final int index, final Time value) throws SQLException {
    statement.setTime(index, value);
    return this;
  }

  public FluentStatement set(final int index, final Time value, final Calendar calendar) throws SQLException {
    statement.setTime(index, value, calendar);
    return this;
  }

  public FluentStatement set(final int index, final Timestamp value) throws SQLException {
    statement.setTimestamp(index, value);
    return this;
  }

  public FluentStatement set(final int index, final Blob value) throws SQLException {
    statement.setBlob(index, value);
    return this;
  }

  public FluentStatement set(final int index, final Clob value) throws SQLException {
    statement.setClob(index, value);
    return this;
  }

  public FluentStatement set(final int index, final NClob value) throws SQLException {
    statement.setNClob(index, value);
    return this;
  }

  public FluentStatement set(final int index, final Reader value) throws SQLException {
    statement.setCharacterStream(index, value);
    return this;
  }

  public FluentStatement set(final int index, final InputStream value) throws SQLException {
    statement.setBinaryStream(index, value);
    return this;
  }

  public FluentStatement set(final int index, final URL value) throws SQLException {
    statement.setURL(index, value);
    return this;
  }

  public FluentStatement set(final String name, final Object value) throws SQLException {
    return set(getIndex(name), value);
  }

  public FluentStatement set(final int index, final Object value) throws SQLException {
    if (value instanceof Byte) {
      set(index, (byte) value);
    } else if (value instanceof Short) {
      set(index, (short) value);
    } else if (value instanceof Integer) {
      set(index, (int) value);
    } else if (value instanceof Long) {
      set(index, (long) value);
    } else if (value instanceof Float) {
      set(index, (float) value);
    } else if (value instanceof Double) {
      set(index, (double) value);
    } else if (value instanceof String) {
      set(index, (String) value);
    } else {
      statement.setObject(index, value);
    }
    return this;
  }

  private int getIndex(final String name) {
    return fields.indexOf(name) + 1;
  }

  @FunctionalInterface
  public interface Mapper<T> {
    T handle(ResultSet row) throws SQLException;
  }
}
