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
package org.immutables.gson.stream;

import java.util.List;
import javax.ws.rs.core.Response;
import com.google.common.base.Throwables;
import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.ServiceLoader;
import java.util.Set;
import javax.annotation.Nullable;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.Provider;
import org.immutables.metainf.Metainf;

/**
 * Gson serialization provider for JAX-RS 1.0 and JAX-RS 2.0.
 */
@Provider
@Metainf.Service
@Consumes(MediaType.WILDCARD)
@Produces(MediaType.WILDCARD)
@SuppressWarnings({"resource", "unused"})
public class GsonMessageBodyProvider implements MessageBodyReader<Object>, MessageBodyWriter<Object> {

  private final Gson gson;
  private final Set<MediaType> mediaTypes;
  private final Streamer streamer;

  /**
   * Creates new provider with internally configured {@link Gson} instance,
   * and {@link MediaType#APPLICATION_JSON_TYPE application/json} media type to match.
   */
  public GsonMessageBodyProvider() {
    this(createGson(), true);
  }

  /**
   * Creates new provider with flexible setup.
   * @param gson the fully configured gson instance
   * @param allowJacksonIfAvailable allows Jackson streaming optimization if Jackson if available in
   *          classpath. Use {@code false} to disable optimization and use pure Gson.
   * @param mediaTypes the media types to match provider
   */
  public GsonMessageBodyProvider(Gson gson, boolean allowJacksonIfAvailable, MediaType... mediaTypes) {
    this.gson = gson;
    this.mediaTypes = mediaSetFrom(mediaTypes);
    this.streamer = createStreamer(allowJacksonIfAvailable);
  }

  private static Gson createGson() {
    GsonBuilder gsonBuilder = new GsonBuilder();
    for (TypeAdapterFactory factory : ServiceLoader.load(TypeAdapterFactory.class)) {
      gsonBuilder.registerTypeAdapterFactory(factory);
    }
    return gsonBuilder.create();
  }

  private static Streamer createStreamer(boolean allowJacksonIfAvailable) {
    if (allowJacksonIfAvailable) {
      try {
        return new JacksonStreamer();
      } catch (Throwable ex) {
        System.err.println(ex.toString());// FIXME just during development
        // cannot load jackson streamer class, fallback to default
      }
    }
    return new GsonStreamer();
  }

  private static Set<MediaType> mediaSetFrom(MediaType[] mediaTypes) {
    if (mediaTypes.length == 0) {
      return Collections.singleton(MediaType.APPLICATION_JSON_TYPE);
    }
    return new HashSet<MediaType>(Arrays.asList(mediaTypes));
  }

  @Override
  public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
    return mediaTypes.contains(mediaType);
  }

  @Override
  public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
    return mediaTypes.contains(mediaType);
  }

  @Override
  public long getSize(Object t, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
    return -1;
  }

  @Override
  public void writeTo(
      Object t,
      Class<?> type,
      Type genericType,
      Annotation[] annotations,
      MediaType mediaType,
      MultivaluedMap<String, Object> httpHeaders,
      OutputStream entityStream) throws IOException, WebApplicationException {
    streamer.write(gson, genericType, t, entityStream);
  }

  @Override
  public Object readFrom(
      Class<Object> type,
      Type genericType,
      Annotation[] annotations,
      MediaType mediaType,
      MultivaluedMap<String, String> httpHeaders,
      InputStream entityStream) throws IOException, WebApplicationException {
    try {
      return streamer.read(gson, genericType, entityStream);
    } catch (IOException ex) {
      // Experimental
      if (ex.getCause() instanceof RuntimeException) {
        String json = gson.toJson(new Error(ex.getCause().getMessage()));
        throw new WebApplicationException(
            Response.status(400)
                .type(mediaType)
                .entity(json)
                .build());
      }
      throw ex;
    }
  }

  static class Error {
    final String error;

    Error(String error) {
      this.error = error;
    }
  }

  private interface Streamer {
    void write(Gson gson, Type type, Object object, OutputStream stream) throws IOException;

    Object read(Gson gson, Type type, InputStream stream) throws IOException;
  }

  private static class GsonStreamer implements Streamer {
    private static final String CHARSET_NAME = "utf-8";

    @Override
    public void write(Gson gson, Type type, Object object, OutputStream stream) throws IOException {
      @Nullable JsonWriter writer = null;
      boolean wasOriginalException = false;
      try {
        writer = new JsonWriter(new BufferedWriter(new OutputStreamWriter(stream, CHARSET_NAME)));

        gson.toJson(object, type, writer);

      } catch (IOException ex) {
        wasOriginalException = true;
        throw ex;
      } catch (Exception ex) {
        wasOriginalException = true;
        throw new IOException(ex);
      } finally {
        if (writer != null) {
          try {
            // underlying stream should not be closed, just flushing
            writer.flush();
          } catch (IOException ex) {
            if (!wasOriginalException) {
              throw ex;
            }
          }
        }
      }
    }

    @Override
    public Object read(Gson gson, Type type, InputStream stream) throws IOException {
      @Nullable JsonReader reader = null;
      try {
        reader = new JsonReader(new BufferedReader(new InputStreamReader(stream, CHARSET_NAME)));

        return gson.fromJson(reader, type);

      } catch (IOException ex) {
        throw ex;
      } catch (Exception ex) {
        throw new IOException(ex);
      } finally {
        // input stream should not be closed
//        if (reader != null) {
//          try {
//            reader.close();
//          } catch (IOException ex) {
//            // ignoring io exception on reader close
//          }
//        }
      }
    }
  }

  private static class JacksonStreamer implements Streamer {
    private static final JsonFactory JSON_FACTORY = new JsonFactory()
        .disable(JsonParser.Feature.AUTO_CLOSE_SOURCE)
        .disable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);

    @Override
    public void write(Gson gson, Type type, Object object, OutputStream stream) throws IOException {
      @Nullable JsonGeneratorWriter writer = null;
      boolean wasOriginalException = false;
      try {
        writer = new JsonGeneratorWriter(JSON_FACTORY.createGenerator(stream));
        gson.toJson(object, type, writer);

      } catch (IOException ex) {
        wasOriginalException = true;
        throw ex;
      } catch (Exception ex) {
        wasOriginalException = true;
        throw new IOException(ex);
      } finally {
        if (writer != null) {
          try {
            // note that stream is not closed here as per JSON_FACTORY configuration
            writer.close();
          } catch (IOException ex) {
            if (!wasOriginalException) {
              throw ex;
            }
          }
        }
      }
    }

    @Override
    public Object read(Gson gson, Type type, InputStream stream) throws IOException {
      @Nullable JsonReader reader = null;
      try {
        reader = new JsonParserReader(JSON_FACTORY.createParser(stream));

        return gson.fromJson(reader, type);

      } catch (IOException ex) {
        throw ex;
      } catch (Exception ex) {
        throw new IOException(ex);
      } finally {
        if (reader != null) {
          try {
            // note that stream is not closed here as per JSON_FACTORY configuration
            reader.close();
          } catch (IOException ex) {
            // ignore io exception of reader close as not important
          }
        }
      }
    }
  }
}
