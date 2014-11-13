/*
    Copyright 2014 Ievgen Lukash

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

import com.fasterxml.jackson.databind.util.TokenBuffer;
import com.google.common.annotations.Beta;
import com.google.common.base.Ascii;
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
public final class MapperFactory implements ResultSetMapperFactory {

  @Override
  public boolean accepts(Class type, StatementContext ctx) {
    return MarshalingSupport.hasAssociatedMarshaler(type);
  }

  @SuppressWarnings({"unchecked", "unused"})
  @Override
  public ResultSetMapper mapperFor(final Class type, StatementContext ctx) {
    return new Mapper<Object>(MarshalingSupport.getMarshalerFor(type));
  }

  private static class Mapper<T> implements ResultSetMapper<T> {
    private final Marshaler<T> marshaler;

    public Mapper(Marshaler<T> marshaler) {
      this.marshaler = marshaler;
    }

    @SuppressWarnings("resource")
    @Override
    public T map(int index, ResultSet result, StatementContext ctx) throws SQLException {
      try {
        TokenBuffer buffer = new TokenBuffer(null, false);
        generateTokens(result, buffer);
        return marshaler.unmarshalInstance(buffer.asParser());
      } catch (IOException ex) {
        throw new ResultSetException("Unable to map result object", ex, ctx);
      }
    }

    public void generateTokens(ResultSet result, TokenBuffer buffer) throws IOException, SQLException {
      buffer.writeStartObject();
      ResultSetMetaData metaData = result.getMetaData();
      for (int j = 0; j < metaData.getColumnCount(); j++) {
        int i = j + 1;
        String name = toLowerCamel(metaData.getColumnLabel(i));
        switch (metaData.getColumnType(i)) {
        case Types.VARCHAR://$FALL-THROUGH$
        case Types.LONGVARCHAR://$FALL-THROUGH$
        case Types.CHAR: {
          String v = result.getString(i);
          if (!result.wasNull()) {
            buffer.writeFieldName(name);
            buffer.writeString(v);
          }
          break;
        }
        case Types.NVARCHAR://$FALL-THROUGH$
        case Types.NCHAR://$FALL-THROUGH$
        case Types.LONGNVARCHAR: {
          String v = result.getNString(i);
          if (!result.wasNull()) {
            buffer.writeFieldName(name);
            buffer.writeString(v);
          }
          break;
        }
        case Types.DATE://$FALL-THROUGH$
        case Types.TIME://$FALL-THROUGH$
        case Types.TIMESTAMP: {
          Date object = result.getDate(i);
          if (!result.wasNull()) {
            buffer.writeFieldName(name);
            buffer.writeObject(object);
          }
          break;
        }
        case Types.BOOLEAN://$FALL-THROUGH$
        case Types.BIT: {
          boolean v = result.getBoolean(i);
          if (!result.wasNull()) {
            buffer.writeFieldName(name);
            buffer.writeBoolean(v);
          }
          break;
        }
        case Types.INTEGER://$FALL-THROUGH$
        case Types.TINYINT: {
          int v = result.getInt(i);
          if (!result.wasNull()) {
            buffer.writeFieldName(name);
            buffer.writeNumber(v);
          }
          break;
        }
        case Types.BIGINT: {
          long v = result.getLong(i);
          if (!result.wasNull()) {
            buffer.writeFieldName(name);
            buffer.writeNumber(v);
          }
          break;
        }
        case Types.DECIMAL: {
          BigDecimal v = result.getBigDecimal(i);
          if (!result.wasNull()) {
            buffer.writeFieldName(name);
            buffer.writeNumber(v);
          }
          break;
        }
        case Types.NUMERIC://$FALL-THROUGH$
        case Types.REAL://$FALL-THROUGH$
        case Types.FLOAT://$FALL-THROUGH$
        case Types.DOUBLE: {
          double v = result.getDouble(i);
          if (!result.wasNull()) {
            buffer.writeFieldName(name);
            buffer.writeNumber(v);
          }
          break;
        }
        case Types.NULL: {
          buffer.writeFieldName(name);
          buffer.writeNull();
          break;
        }
        default:
          @Nullable
          Object object = result.getObject(i);
          if (object != null) {
            buffer.writeFieldName(name);
            buffer.writeObject(object);
          }
        }
      }
      buffer.writeEndObject();
    }

    private String toLowerCamel(String name) {
      return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, Ascii.toUpperCase(name));
    }

    @Override
    public String toString() {
      return MapperFactory.class.getSimpleName() + ".mapperFor(" + marshaler.getExpectedType() + ")";
    }
  }
}
