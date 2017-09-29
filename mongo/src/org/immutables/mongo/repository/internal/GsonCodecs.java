package org.immutables.mongo.repository.internal;

import com.google.gson.TypeAdapter;
import org.bson.AbstractBsonReader;
import org.bson.codecs.Codec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.codecs.configuration.CodecRegistry;

import java.io.IOException;

/**
 * Codec based on type adapter.
 */
public class GsonCodecs {

  public static <T> CodecRegistry registryFor(final Class<T> type, final TypeAdapter<T> adapter, final CodecRegistry existing) {
    return new CodecRegistry() {
      @SuppressWarnings("unchecked")
      @Override
      public <X> Codec<X> get(Class<X> clazz) {
        if (type.isAssignableFrom(clazz)) {
          return (Codec<X>) new GsonCodec<>(type, adapter);
        } else {
          return existing.get(clazz);
        }
      };
    };
  }


  static class GsonCodec<T> implements Codec<T> {

    private final TypeAdapter<T> adapter;
    private final Class<T> type;

    GsonCodec(Class<T> type, TypeAdapter<T> adapter) {
      this.adapter = adapter;
      this.type = type;
    }

    @Override
    public T decode(org.bson.BsonReader reader, DecoderContext decoderContext) {
      if (!(reader instanceof AbstractBsonReader)) {
        throw new UnsupportedOperationException(String.format("Only readers of type %s supported. Yours is %s",
                AbstractBsonReader.class.getName(), reader.getClass().getName()));
      }

      try {
        return adapter.read(new BsonReader((AbstractBsonReader) reader));
      } catch (IOException e) {
        throw new RuntimeException(String.format("Couldn't encode %s", type), e);
      }
    }

    @Override
    public void encode(org.bson.BsonWriter writer, T value, EncoderContext encoderContext) {
      try {
        adapter.write(new BsonWriter(writer), value);
      } catch (IOException e) {
        throw new RuntimeException(String.format("Couldn't write value of class %s: %s", type.getName(), value),e);
      }
    }

    @Override
    public Class<T> getEncoderClass() {
      return type;
    }
  }
}
