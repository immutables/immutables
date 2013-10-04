package org.immutables.common.marshal;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.io.IOException;
import java.io.StringWriter;
import org.immutables.annotation.GenerateMarshaler;
import org.immutables.common.marshal.internal.MarshalingSupport;

/**
 * Contains convenient methods for marshaling and unmarshaling documents annotated with
 * {@link GenerateMarshaler} to and from standard textual JSON.
 * <p>
 * You can avoid using this class in favour of using Marshalers directly due to the fact of using
 * static marshaler {@link LoadingCache cache} (need to note that marshalers are very lightweight
 * and cache have {@link CacheBuilder#weakKeys() weak keys}). Nevertheless, this class provides easy
 * to use static methods as could serve aas how one could use marshalers in a more complex
 * situations.
 */
public final class Marshaling {
  private Marshaling() {}

  private static final JsonFactory JSON_FACTORY = new JsonFactory();

  private static final LoadingCache<Class<?>, Marshaler<Object>> MARSHALER_CACHE =
      CacheBuilder.newBuilder()
          .weakKeys()
          .build(new CacheLoader<Class<?>, Marshaler<Object>>() {
            @Override
            public Marshaler<Object> load(Class<?> type) throws Exception {
              return MarshalingSupport.loadMarshalerFor(type);
            }
          });

  /**
   * Marshal object to JSON.
   * @param object the object
   * @return JSON string
   */
  public static String toJson(Object object) {
    Marshaler<Object> marshaler = MARSHALER_CACHE.getUnchecked(object.getClass());
    StringWriter writer = new StringWriter();
    try (JsonGenerator generator = JSON_FACTORY.createGenerator(writer)) {
      marshaler.marshalInstance(generator, object);
    } catch (IOException ex) {
      throw Throwables.propagate(ex);
    }
    return writer.toString();
  }

  /**
   * Unmarshal object from JSON by specifying expected type.
   * @param <T> the exected type
   * @param json JSON string
   * @param expectedType the expected type class
   * @return the unmarshaled instance
   */
  public static <T> T fromJson(String json, Class<T> expectedType) {
    Marshaler<Object> marshaler = MARSHALER_CACHE.getUnchecked(expectedType);
    try (JsonParser parser = JSON_FACTORY.createParser(json)) {
      parser.nextToken();
      return expectedType.cast(marshaler.unmarshalInstance(parser));
    } catch (IOException ex) {
      throw Throwables.propagate(ex);
    }
  }
}
