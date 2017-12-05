/*
   Copyright 2015 Immutables Authors and Contributors

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
package org.immutables.mongo.types;

import com.google.gson.Gson;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.regex.Pattern;
import javax.annotation.Nullable;
import org.bson.types.Decimal128;
import org.immutables.metainf.Metainf;
import org.immutables.mongo.repository.internal.BsonReader;
import org.immutables.mongo.repository.internal.BsonWriter;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Built-in BSON type adapters. Also contains reusable delegate-adapters to easily implement own
 * wrapper if needed. Supports {@link Id}, {@link TimeInstant}, {@link Binary}, {@link Pattern}
 * types which correspond to {@code ObjectID}, {@code UTCDate}, {@code Binary}, {@code Regex} native
 * BSON types, not available in JSON.
 * <p>
 * This class also expose simple type adapters to BSON special types, so you can easily write
 * adapters for your own wrapper types.
 * @see #binaryAdapter()
 * @see #objectIdAdapter()
 * @see #objectIdAdapter()
 */
@Metainf.Service
public final class TypeAdapters implements TypeAdapterFactory {
  private static final TypeToken<Id> ID_TYPE_TOKEN = TypeToken.get(Id.class);
  private static final TypeToken<TimeInstant> TIME_INSTANT_TYPE_TOKEN = TypeToken.get(TimeInstant.class);
  private static final TypeToken<Binary> BINARY_TYPE_TOKEN = TypeToken.get(Binary.class);
  private static final TypeToken<Pattern> PATTERN_TYPE_TOKEN = TypeToken.get(Pattern.class);
  private static final TypeToken<Decimal128> DECIMAL128_TYPE_TOKEN = TypeToken.get(Decimal128.class);

  // safe unchecked, typecheck performed by type token equality
  @SuppressWarnings("unchecked")
  @Override
  @Nullable
  public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
    if (ID_TYPE_TOKEN.equals(type)) {
      return (TypeAdapter<T>) WRAPPED_ID_ADAPTER;
    }
    if (TIME_INSTANT_TYPE_TOKEN.equals(type)) {
      return (TypeAdapter<T>) WRAPPED_TIME_INSTANT_ADAPTER;
    }
    if (BINARY_TYPE_TOKEN.equals(type)) {
      return (TypeAdapter<T>) WRAPPED_BINARY_ADAPTER;
    }
    if (PATTERN_TYPE_TOKEN.equals(type)) {
      return (TypeAdapter<T>) PATTERN_ADAPTER;
    }
    if (DECIMAL128_TYPE_TOKEN.equals(type)) {
      return (TypeAdapter<T>) DECIMAL128_ADAPTER;
    }
    return null;
  }

  /**
   * Use this adapter to easily delagate marshaling of custom time instant wrapper.
   * @return {@code Long <==> UTCDate} adapter
   */
  public static TypeAdapter<Long> timeInstantAdapter() {
    return TIME_INSTANT_ADAPTER;
  }

  /**
   * Use this adapter to easily delagate marshaling of custom ObjectID wrapper.
   * @return {@code byte[] <==> ObjectID} adapter
   */
  public static TypeAdapter<byte[]> objectIdAdapter() {
    return OBJECT_ID_ADAPTER;
  }

  /**
   * Use this adapter to easily delagate marshaling of custom Binary wrapper.
   * @return {@code byte[] <==> Binary} adapter
   */
  public static TypeAdapter<byte[]> binaryAdapter() {
    return BINARY_ADAPTER;
  }

  /**
   * Use this adapter (not registered by factory, default Gson adapter is used otherwise) to
   * delagate marshaling of {@link BigDecimal} to {@link Decimal128} type in mongo.
   * @return {@code BigDecimal <==> Decimal128} adapter
   */
  public static TypeAdapter<BigDecimal> decimalAdapter() {
    return DECIMAL_ADAPTER;
  }

  private static final TypeAdapter<TimeInstant> WRAPPED_TIME_INSTANT_ADAPTER = new TypeAdapter<TimeInstant>() {
    @Override
    public void write(JsonWriter out, TimeInstant value) throws IOException {
      if (out instanceof BsonWriter) {
        TIME_INSTANT_ADAPTER.write(out, value.value());
      } else {
        out.value(value.toString());
      }
    }

    @Override
    public TimeInstant read(JsonReader in) throws IOException {
      return TimeInstant.of(TIME_INSTANT_ADAPTER.read(in));
    }

    @Override
    public String toString() {
      return "TypeAdapters.(TimeInstant)";
    }
  };

  private static final TypeAdapter<Id> WRAPPED_ID_ADAPTER = new TypeAdapter<Id>() {
    @Override
    public void write(JsonWriter out, Id value) throws IOException {
      if (out instanceof BsonWriter) {
        OBJECT_ID_ADAPTER.write(out, value.value());
      } else {
        out.value(value.toString());
      }
    }

    @Override
    public Id read(JsonReader in) throws IOException {
      return Id.from(OBJECT_ID_ADAPTER.read(in));
    }

    @Override
    public String toString() {
      return "TypeAdapters.(Id)";
    }
  };

  private static final TypeAdapter<Binary> WRAPPED_BINARY_ADAPTER = new TypeAdapter<Binary>() {
    @Override
    public void write(JsonWriter out, Binary value) throws IOException {
      if (out instanceof BsonWriter) {
        BINARY_ADAPTER.write(out, value.value());
      } else {
        out.value(value.toString());
      }
    }

    @Override
    public Binary read(JsonReader in) throws IOException {
      return Binary.create(BINARY_ADAPTER.read(in));
    }

    @Override
    public String toString() {
      return "TypeAdapters.(Binary)";
    }
  };

  private static final TypeAdapter<Pattern> PATTERN_ADAPTER = new TypeAdapter<Pattern>() {
    @Override
    public void write(JsonWriter out, Pattern value) throws IOException {
      if (value == null) {
        out.nullValue();
      } else if (out instanceof BsonWriter) {
        ((BsonWriter) out).value(value);
      } else {
        out.value(value.toString());
      }
    }

    @Override
    public Pattern read(JsonReader in) throws IOException {
      if (in.peek() == JsonToken.NULL) {
        in.nextNull();
        return null;
      }
      if (in instanceof BsonReader) {
        return ((BsonReader) in).nextPattern();
      }
      return Pattern.compile(in.nextString());
    }

    @Override
    public String toString() {
      return "TypeAdapters.(Pattern)";
    }
  };

  private static final TypeAdapter<Decimal128> DECIMAL128_ADAPTER = new TypeAdapter<Decimal128>() {
    @Override
    public void write(JsonWriter out, Decimal128 value) throws IOException {
      if (value == null) {
        out.nullValue();
      } else if (out instanceof BsonWriter) {
        ((BsonWriter) out).value(value);
      } else {
        out.value(value.toString());
      }
    }

    @Override
    public Decimal128 read(JsonReader in) throws IOException {
      if (in.peek() == JsonToken.NULL) {
        in.nextNull();
        return null;
      }
      if (in instanceof BsonReader) {
        return ((BsonReader) in).nextDecimal();
      }
      return Decimal128.parse(in.nextString());
    }

    @Override
    public String toString() {
      return "TypeAdapters.(Decimal128)";
    }
  };

  private static final TypeAdapter<BigDecimal> DECIMAL_ADAPTER = new TypeAdapter<BigDecimal>() {
    @Override
    public void write(JsonWriter out, BigDecimal value) throws IOException {
      checkArgument(out instanceof BsonWriter, "Should be BsonWriter, not some other JsonWriter");
      checkNotNull(value, "Value could not be null, delegate to #nullSafe() adapter if needed");
      ((BsonWriter) out).value(value);
    }

    @Override
    public BigDecimal read(JsonReader in) throws IOException {
      checkArgument(in instanceof BsonReader, "Should be BsonReader, not some other JsonReader");
      return ((BsonReader) in).nextDecimal().bigDecimalValue();
    }

    @Override
    public String toString() {
      return "TypeAdapters.decimalAdapter()";
    }
  };

  private static final TypeAdapter<Long> TIME_INSTANT_ADAPTER = new TypeAdapter<Long>() {
    @Override
    public void write(JsonWriter out, Long value) throws IOException {
      checkArgument(out instanceof BsonWriter, "Should be BsonWriter, not some other JsonWriter");
      checkNotNull(value, "Value could not be null, delegate to #nullSafe() adapter if needed");
      ((BsonWriter) out).valueTimeInstant(value);
    }

    @Override
    public Long read(JsonReader in) throws IOException {
      checkArgument(in instanceof BsonReader, "Should be BsonReader, not some other JsonReader");
      return ((BsonReader) in).nextTimeInstant();
    }

    @Override
    public String toString() {
      return "TypeAdapters.timeInstantAdapter()";
    }
  };

  private static final TypeAdapter<byte[]> OBJECT_ID_ADAPTER = new TypeAdapter<byte[]>() {
    @Override
    public void write(JsonWriter out, byte[] value) throws IOException {
      checkArgument(out instanceof BsonWriter, "Should be BsonWriter, not some other JsonWriter");
      checkNotNull(value, "Value could not be null, delegate to #nullSafe() adapter if needed");
      ((BsonWriter) out).valueObjectId(value);
    }

    @Override
    public byte[] read(JsonReader in) throws IOException {
      checkArgument(in instanceof BsonReader, "Should be BsonReader, not some other JsonReader");
      return ((BsonReader) in).nextObjectId();
    }

    @Override
    public String toString() {
      return "TypeAdapters.objectIdAdapter()";
    }
  };

  private static final TypeAdapter<byte[]> BINARY_ADAPTER = new TypeAdapter<byte[]>() {
    @Override
    public void write(JsonWriter out, byte[] value) throws IOException {
      checkArgument(out instanceof BsonWriter, "Should be BsonWriter, not some other JsonWriter");
      checkNotNull(value, "Value could not be null, delegate to #nullSafe() adapter if needed");
      ((BsonWriter) out).valueBinary(value);
    }

    @Override
    public byte[] read(JsonReader in) throws IOException {
      checkArgument(in instanceof BsonReader, "Should be BsonReader, not some other JsonReader");
      return ((BsonReader) in).nextBinary();
    }

    @Override
    public String toString() {
      return "TypeAdapters.binaryAdapter()";
    }
  };
}
