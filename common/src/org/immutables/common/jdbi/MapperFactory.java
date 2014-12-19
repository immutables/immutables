/*
    Copyright 2014 Immutables Authors and Contributors

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.immutables.common.jdbi;

import com.google.common.collect.ObjectArrays;
import com.google.common.base.Ascii;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonGenerator;
import java.sql.Timestamp;
import java.sql.Time;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import com.google.common.annotations.Beta;
import com.google.common.base.CaseFormat;
import java.io.IOException;
import java.math.BigDecimal;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import javax.annotation.Nullable;
import org.immutables.common.marshal.Marshaler;
import org.immutables.common.marshal.internal.MarshalingSupport;
import org.skife.jdbi.v2.ResultSetMapperFactory;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.exceptions.ResultSetException;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

@Beta
public class MapperFactory implements ResultSetMapperFactory {
  private static final String COLUMN_METADATA_ATTRIBUTE = ColumnMetadata.class.getCanonicalName();

  @Override
  public final boolean accepts(Class type, StatementContext ctx) {
    return MarshalingSupport.hasAssociatedMarshaler(type);
  }

  @SuppressWarnings({"unchecked", "unused"})
  @Override
  public final ResultSetMapper mapperFor(final Class type, StatementContext ctx) {
    return new Mapper<Object>(MarshalingSupport.getMarshalerFor(type));
  }

  /**
   * Override this method in your {@link MapperFactory} subclass to hook into writing of values from
   * result set.
   * @param buffer token buffer
   * @return json generator view of provided buffer
   */
  protected JsonGenerator asGenerator(TokenBuffer buffer) {
    return buffer;
  }

  /**
   * Override this method in your {@link MapperFactory} subclass to hook into reading of values
   * during unmarshaling
   * of result set.
   * @param buffer the buffer
   * @return json parser view of provied buffer
   */
  protected JsonParser asParser(TokenBuffer buffer) {
    return buffer.asParser();
  }

  /**
   * Converts column label into name that should match. While it's overridable, in most cases
   * default conversion might suffice as it does snake-case and camel-case conversion on a best
   * effort basis.
   * @param columnLabel the column label
   * @return immutable object attribute name.
   */
  protected String columnLabelToAttributeName(String columnLabel) {
    boolean hasUnderscoreSeparator = columnLabel.indexOf('_') > 0;
    if (hasUnderscoreSeparator || Ascii.toUpperCase(columnLabel).equals(columnLabel)) {
      columnLabel = Ascii.toLowerCase(columnLabel);
    }
    return hasUnderscoreSeparator
        ? CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, columnLabel)
        : CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, columnLabel);
  }

  /**
   * Metadata is cached due to possible overhead in attribute name conversions
   * in a hope that hashtable lookup from {@link StatementContext#getAttribute(String)} is less
   * expensive. Also, same string instances will be used for attribute name on each result row.
   */
  private final class ColumnMetadata {
    final int count;
    final int[] types;
    final String[] names;

    ColumnMetadata(ResultSetMetaData metaData) throws SQLException {
      this.count = metaData.getColumnCount();
      this.types = new int[this.count];
      this.names = new String[this.count];
      collectMetadata(metaData);
    }

    private void collectMetadata(ResultSetMetaData metaData) throws SQLException {
      for (int i = 0; i < count; i++) {
        types[i] = metaData.getColumnType(i + 1);
        names[i] = columnLabelToAttributeName(metaData.getColumnLabel(i + 1));
      }
    }
  }

  private ColumnMetadata columnsFrom(ResultSetMetaData metaData, StatementContext ctx) throws SQLException {
    Object attribute = ctx.getAttribute(COLUMN_METADATA_ATTRIBUTE);
    if (attribute instanceof ColumnMetadata) {
      return (ColumnMetadata) attribute;
    }
    ColumnMetadata columnMetadata = new ColumnMetadata(metaData);
    ctx.setAttribute(COLUMN_METADATA_ATTRIBUTE, columnMetadata);
    return columnMetadata;
  }

  /**
   * Stacktrace of wrapper derived exception is almost always irrelevant,
   * while stack of cause is most important. In any case it would be
   * relatively easy to find this place.
   */
  static void makeStackTraceUseful(Exception wrapper, Exception cause) {
    wrapper.setStackTrace(ObjectArrays.concat(
        wrapper.getStackTrace()[0],
        cause.getStackTrace()));
  }

  private class Mapper<T> implements ResultSetMapper<T> {

    private final Marshaler<T> marshaler;

    public Mapper(Marshaler<T> marshaler) {
      this.marshaler = marshaler;
    }

    @SuppressWarnings("resource")
    @Override
    public T map(int index, ResultSet result, StatementContext ctx) throws SQLException {
      try {
        TokenBuffer buffer = new TokenBuffer(null, false);
        ColumnMetadata columns = columnsFrom(result.getMetaData(), ctx);
        generateTokens(result, columns, asGenerator(buffer));
        return marshaler.unmarshalInstance(asParser(buffer));
      } catch (IOException ex) {
        ResultSetException resultSetException = new ResultSetException("Unable to map result object", ex, ctx);
        makeStackTraceUseful(resultSetException, ex);
        throw resultSetException;
      }
    }

    private void generateTokens(
        ResultSet result,
        ColumnMetadata columns,
        JsonGenerator generator) throws IOException, SQLException {

      generator.writeStartObject();

      for (int j = 0; j < columns.count; j++) {
        int i = j + 1;
        String name = columns.names[j];
        switch (columns.types[j]) {
        case Types.VARCHAR://$FALL-THROUGH$
        case Types.LONGVARCHAR://$FALL-THROUGH$
        case Types.CHAR: {
          String v = result.getString(i);
          if (!result.wasNull()) {
            generator.writeFieldName(name);
            generator.writeString(v);
          }
          break;
        }
        case Types.NVARCHAR://$FALL-THROUGH$
        case Types.NCHAR://$FALL-THROUGH$
        case Types.LONGNVARCHAR: {
          String v = result.getNString(i);
          if (!result.wasNull()) {
            generator.writeFieldName(name);
            generator.writeString(v);
          }
          break;
        }
        case Types.DATE:
          Date date = result.getDate(i);
          if (!result.wasNull()) {
            generator.writeFieldName(name);
            generator.writeObject(date);
          }
          break;
        case Types.TIME:
          Time time = result.getTime(i);
          if (!result.wasNull()) {
            generator.writeFieldName(name);
            generator.writeObject(time);
          }
          break;
        case Types.TIMESTAMP: {
          Timestamp timestamp = result.getTimestamp(i);
          if (!result.wasNull()) {
            generator.writeFieldName(name);
            generator.writeObject(timestamp);
          }
          break;
        }
        case Types.BOOLEAN://$FALL-THROUGH$
        case Types.BIT: {
          boolean v = result.getBoolean(i);
          if (!result.wasNull()) {
            generator.writeFieldName(name);
            generator.writeBoolean(v);
          }
          break;
        }
        case Types.INTEGER://$FALL-THROUGH$
        case Types.TINYINT: {
          int v = result.getInt(i);
          if (!result.wasNull()) {
            generator.writeFieldName(name);
            generator.writeNumber(v);
          }
          break;
        }
        case Types.BIGINT: {
          long v = result.getLong(i);
          if (!result.wasNull()) {
            generator.writeFieldName(name);
            generator.writeNumber(v);
          }
          break;
        }
        case Types.DECIMAL: {
          BigDecimal v = result.getBigDecimal(i);
          if (!result.wasNull()) {
            generator.writeFieldName(name);
            generator.writeNumber(v);
          }
          break;
        }
        case Types.NUMERIC://$FALL-THROUGH$
        case Types.REAL://$FALL-THROUGH$
        case Types.FLOAT://$FALL-THROUGH$
        case Types.DOUBLE: {
          double v = result.getDouble(i);
          if (!result.wasNull()) {
            generator.writeFieldName(name);
            generator.writeNumber(v);
          }
          break;
        }
        case Types.NULL: {
          generator.writeFieldName(name);
          generator.writeNull();
          break;
        }
        default:
          @Nullable
          Object object = result.getObject(i);
          if (object != null) {
            generator.writeFieldName(name);
            generator.writeObject(object);
          }
        }
      }
      generator.writeEndObject();
    }

    @Override
    public String toString() {
      return MapperFactory.this.getClass().getSimpleName() + ".mapperFor(" + marshaler.getExpectedType() + ")";
    }
  }
}
