package org.immutables.common.web;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import org.immutables.common.marshal.Marshaler;
import org.immutables.common.marshal.internal.MarshalingSupport;

@Provider
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class MarshaledMessageBodyProvider implements MessageBodyReader<Object>, MessageBodyWriter<Object> {
  public static final JsonFactory jsonFactory = new JsonFactory();

  final LoadingCache<Class<?>, Marshaler<?>> marshalerCache = CacheBuilder.newBuilder()
      .build(new CacheLoader<Class<?>, Marshaler<?>>() {
        @Override
        public Marshaler<?> load(Class<?> type) throws Exception {
          return MarshalingSupport.loadMarshalerFor(type);
        }
      });

  @Override
  public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
    if (MarshalingSupport.hasAssociatedMarshaler(type)) {
      marshalerCache.refresh(type);
      return true;
    }
    return false;
  }

  @Override
  public Object readFrom(
      Class<Object> type,
      Type genericType,
      Annotation[] annotations,
      MediaType mediaType,
      MultivaluedMap<String, String> httpHeaders,
      InputStream entityStream) throws IOException, WebApplicationException {

    try (JsonParser parser = jsonFactory.createParser(entityStream)) {
      Marshaler<?> marshaler = marshalerCache.getUnchecked(type);
      parser.nextToken();
      return marshaler.unmarshalInstance(parser);
    } catch (IOException e) {
      throw new WebApplicationException(e, Response.Status.BAD_REQUEST);
    }
  }

  @Override
  public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
    if (MarshalingSupport.hasAssociatedMarshaler(type)) {
      marshalerCache.refresh(type);
      return true;
    }
    return false;
  }

  @Override
  public long getSize(Object o, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
    return -1;
  }

  @Override
  public void writeTo(
      Object o,
      Class<?> actualType,
      Type genericType,
      Annotation[] annotations,
      MediaType mediaType,
      MultivaluedMap<String, Object> httpHeaders,
      OutputStream entityStream) throws IOException, WebApplicationException {

    if (genericType instanceof Class<?>) {
      Class<?> declaredType = (Class<?>) genericType;
      try (JsonGenerator generator = jsonFactory.createGenerator(entityStream)) {
        @SuppressWarnings("unchecked")
        Marshaler<Object> marshaler =
            (Marshaler<Object>) marshalerCache.getUnchecked(declaredType);

        marshaler.marshalInstance(generator, o);
      }
    }
  }
}
