/*
    Copyright 2013-2014 Immutables.org authors

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
package org.immutables.common.marshal;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
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
import org.immutables.common.marshal.internal.MarshalingSupport;

/**
 * JSON marshaling JAX-RS provider for immutable classes with generated marshaler.
 * @see org.immutables.value.Value.Immutable
 * @see org.immutables.json.Json.Marshaled
 */
@Provider
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class JaxrsMessageBodyProvider implements MessageBodyReader<Object>, MessageBodyWriter<Object> {
  private final JsonFactory jsonFactory = new JsonFactory();

  @Override
  public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
    if (MarshalingSupport.hasAssociatedMarshaler(type)) {
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
      parser.nextToken();
      return MarshalingSupport.getMarshalerFor(type).unmarshalInstance(parser);
    } catch (Exception ex) {
      throw new WebApplicationException(ex, Response.Status.BAD_REQUEST);
    }
  }

  @Override
  public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
    if (MarshalingSupport.hasAssociatedMarshaler(type)) {
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

    try (JsonGenerator generator = jsonFactory.createGenerator(entityStream)) {
      MarshalingSupport.getMarshalerFor(actualType).marshalInstance(generator, o);
    }
  }
}
