package org.immutables.common.jdbi;

import java.math.BigDecimal;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import java.io.IOException;
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
import org.skife.jdbi.v2.tweak.ResultSetMapper;

public final class MapperFactory implements ResultSetMapperFactory {

  @Override
  public boolean accepts(Class type, StatementContext ctx) {
    return MarshalingSupport.hasAssociatedMarshaler(type);
  }

  @SuppressWarnings({ "unchecked", "unused" })
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
        generateJsonTokens(result, buffer);
        JsonParser parser = buffer.asParser();
        parser.nextToken();
        return marshaler.unmarshalInstance(parser);
      } catch (IOException ex) {
        throw new SQLException(ex);
      }
    }

    @Override
    public String toString() {
      return MapperFactory.class.getSimpleName() + ".mapperFor(" + marshaler.getExpectedType() + ")";
    }

    public void generateJsonTokens(ResultSet result, TokenBuffer buffer) throws IOException, SQLException {
      ResultSetMetaData metaData = result.getMetaData();
      for (int j = 0; j < metaData.getColumnCount(); j++) {
        int i = j + 1;
        String name = metaData.getColumnName(i);
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
  }
}
