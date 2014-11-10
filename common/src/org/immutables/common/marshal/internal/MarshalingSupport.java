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
package org.immutables.common.marshal.internal;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import javax.annotation.Nullable;

import org.immutables.common.marshal.Marshaler;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.io.Closer;

/**
 * Marshaling support used by other utilities as well as by generated code.
 */
public final class MarshalingSupport {
  private MarshalingSupport() {}
  
  private static final Method addSuppressed = getAddSuppressed();

  private static Method getAddSuppressed() {
    try {
      return Throwable.class.getMethod("addSuppressed", Throwable.class);
    } catch (Throwable e) {
      return null;
    }
  }

  public static void ensureStarted(JsonParser parser) throws IOException {
    if (!parser.hasCurrentToken()) {
      parser.nextToken();
    }
  }

  public static IOException diagnose(JsonParser parser, String attribute, Object builder, Exception exception)
      throws IOException {
    String text = parser.getText();
    JsonLocation location = parser.getTokenLocation();
    String message = "";
    if (exception instanceof JsonProcessingException) {
      JsonProcessingException processingException = (JsonProcessingException) exception;
      location = processingException.getLocation();
      message += processingException.getOriginalMessage();
    } else {
      message += exception.getMessage();
    }
    message += "\n  after '" + text + "'";
    if (!attribute.isEmpty()) {
      message += "\n  field \"" + attribute + "\"";
    }
    message += "\n  with " + builder;
    message += "\n  at " + location;

    UnmarshalingProblemException problem = new UnmarshalingProblemException(message);
    problem.setStackTrace(exception.getStackTrace());
    if (exception.getCause() != null) {
      problem.initCause(exception);
    }
    throw problem;
  }

  public static void ensureToken(JsonToken expected, JsonToken actual, Class<?> marshaledType) throws IOException {
    if (expected != actual) {
      throw new UnmarshalMismatchException(marshaledType.getName(), "~", "", actual);
    }
  }

  /**
   * Ensure marshal condition.
   * @param condition the condition
   * @param hostType the host type
   * @param attributeName the attribute name
   * @param attributeType the attribute type
   * @param message the message
   * @throws IOException io exception
   */
  public static void ensureCondition(
      boolean condition,
      String hostType,
      String attributeName,
      String attributeType,
      Object message) throws IOException {
    if (!condition) {
      throw new UnmarshalMismatchException(hostType, attributeName, attributeType, message);
    }
  }

  private static class UnmarshalingProblemException extends IOException {
    UnmarshalingProblemException(String message) {
      super(message);
    }

    @Override
    public String toString() {
      return "problem during unmarshaling: " + getMessage();
    }
  }

  private static class UnmarshalMismatchException extends IOException {
    final String hostType;
    final String attributeName;
    final String attributeType;

    UnmarshalMismatchException(String hostType, String attributeName, String attributeType, Object message) {
      super(String.valueOf(message));
      this.hostType = hostType;
      this.attributeName = attributeName;
      this.attributeType = attributeType;
    }

    @Override
    public String getMessage() {
      return String.format("[%s.%s : %s] %s", hostType, attributeName, attributeType, super.getMessage());
    }

    @Override
    public String toString() {
      return "problem during unmarshaling: " + getMessage();
    }
  }

  @SafeVarargs
  @SuppressWarnings("unchecked")
  public static <T> void marshalWithOneOfMarshalers(
      JsonGenerator generator,
      T instance,
      Marshaler<? extends T>... marshalers) throws IOException {
    for (Marshaler<?> marshaler : marshalers) {
      if (marshaler.getExpectedType().isInstance(instance)) {
        ((Marshaler<Object>) marshaler).marshalInstance(generator, instance);
      }
    }
  }

  /**
   * Support method that is used for parsing polymorphic/variant types.
   * @param parser the parser pointing to object start
   * @param hostType the host type
   * @param attributeName the attribute name
   * @param attributeType the attribute type
   * @param marshalers the marshalers to try
   * @return the object
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static Object unmarshalWithOneOfMarshalers(
      JsonParser parser,
      String hostType,
      String attributeName,
      String attributeType,
      Marshaler<?>... marshalers) throws IOException {

    Closer bufferCloser = Closer.create();
    try {
      TokenBuffer buffer = bufferCloser.register(new TokenBuffer(parser));
      buffer.copyCurrentStructure(parser);

      @Nullable
      List<Exception> exceptions = Lists.newArrayListWithCapacity(marshalers.length);

      for (Marshaler<?> marshaler : marshalers) {
        try {
          Closer parserCloser = Closer.create();
          try {
            JsonParser bufferParser = parserCloser.register(buffer.asParser());
            bufferParser.nextToken();
            return marshaler.unmarshalInstance(bufferParser);
          } catch (Throwable t) {
            throw parserCloser.rethrow(t);
          } finally {
            parserCloser.close();
          }
        } catch (Exception ex) {
          exceptions.add(ex);
        }
      }

      UnmarshalMismatchException exception =
          new UnmarshalMismatchException(hostType, attributeName, attributeType, "Cannot unambigously parse");

      for (Exception ex : exceptions) {
        if (addSuppressed != null) {
          try {
            addSuppressed.invoke(exception, ex);
          } catch (Throwable e) {
          }
        }
      }

      throw exception;
    } finally {
      bufferCloser.close();
    }
  }

  /**
   * Used from generated code to write content to token buffer.
   * @param <T> the type of expected unmarshaled type.
   * @param marshaler the marshaler
   * @param instance the instance
   * @return the token buffer
   * @throws IOException If JSON processing error occured.
   */
  public static <T> TokenBuffer toTokenBuffer(Marshaler<T> marshaler, T instance) throws IOException {
    TokenBuffer buffer = new TokenBuffer(null, false);
    marshaler.marshalInstance(buffer, instance);
    return buffer;
  }

  /**
   * Used to call from generated code as a workaround for
   * <a href="https://github.com/FasterXML/jackson-databind/issues/592">
   * https://github.com/FasterXML/jackson-databind/issues/592</a>
   * @param <T> the type of expected unmarshaled type.
   * @param marshaler the marshaler
   * @param buffers token buffers per each attribute
   * @return unmarshaled instance
   * @throws IOException If JSON processing error occured.
   */
  @SuppressWarnings("resource")
  public static <T> T fromTokenBuffers(
      Marshaler<T> marshaler,
      Map<String, TokenBuffer> buffers) throws IOException {
    TokenBuffer buffer = new TokenBuffer(null, false);
    buffer.writeStartObject();
    for (Map.Entry<String, TokenBuffer> entry : buffers.entrySet()) {
      buffer.writeFieldName(entry.getKey());
      entry.getValue().serialize(buffer);
    }
    buffer.writeEndObject();
    JsonParser parser = buffer.asParser();
    parser.nextToken();
    return marshaler.unmarshalInstance(parser);
  }

  public static <T> Marshaler<T> getMarshalerFor(Class<? extends T> type) {
    // unchecked: relies on how nice are marshalers contributors
    @SuppressWarnings("unchecked")
    @Nullable
    Marshaler<T> marshaler = (Marshaler<T>) Registry.marshalers.get(type);
    Preconditions.checkArgument(marshaler != null,
        "Cannot find marshaler for type %s. Use @Json.Marshaled annotation to generate associated marshaler", type);
    return marshaler;
  }

  public static boolean hasAssociatedMarshaler(Class<?> type) {
    return Registry.marshalers.containsKey(type);
  }

  /**
   * Lazily loaded registry of statically-extended marshalers via {@link MarshalingContributor}.
   */
  private static class Registry {
    static final ImmutableMap<Class<?>, Marshaler<?>> marshalers;

    static {
      Map<Class<?>, Marshaler<?>> map = Maps.newHashMapWithExpectedSize(1 << 6);
      for (MarshalingContributor contributor : ServiceLoader.load(MarshalingContributor.class)) {
        contributor.putMarshalers(map);
      }
      marshalers = ImmutableMap.copyOf(map);
    }
  }
}
