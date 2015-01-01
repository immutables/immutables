/*
    Copyright 2013-2014 Immutables Authors and Contributors

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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonStreamContext;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import javax.annotation.Nullable;
import org.immutables.common.marshal.Marshaler;

/**
 * Marshaling support used by other utilities as well as by generated code.
 */
public final class MarshalingSupport {
  private static final String INDENT = "\n\t";

  private MarshalingSupport() {}

  @Nullable
  private static final Method addSuppressed = getAddSuppressed();

  @Nullable
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

  /**
   * Diagnosed.
   * @param exception the exception
   * @param parser the JSON parser
   * @param contextType the context type
   * @return the IO exception
   * @throws IOException Signals that an I/O or unmarshaling problem
   */
  public static IOException diagnosed(Exception exception, JsonParser parser, Class<?> contextType)
      throws IOException {
    String message = exception.toString()
        + INDENT + "using " + contextType.getCanonicalName()
        + INDENT + "path (" + buildJsonPath(parser.getParsingContext()) + ")"
        + INDENT + "token '" + parser.getText() + "'"
        + INDENT + "in " + parser.getCurrentLocation();
    IOException problem = new IOException(message, exception);
    problem.setStackTrace(new StackTraceElement[] {problem.getStackTrace()[0]});
    throw problem;
  }

  /**
   * Diagnosed.
   * @param exception the exception
   * @param generator the JSON generator
   * @param contextType the context type
   * @return the IO exception
   * @throws IOException Signals that an I/O or marshaling problem
   */
  public static IOException diagnosed(Exception exception, JsonGenerator generator, Class<?> contextType)
      throws IOException {
    String message = exception.toString()
        + INDENT + "using " + contextType.getCanonicalName()
        + INDENT + "path (" + buildJsonPath(generator.getOutputContext()) + ")";
    IOException problem = new IOException(message, exception);
    problem.setStackTrace(new StackTraceElement[] {problem.getStackTrace()[0]});
    throw problem;
  }

  private static String buildJsonPath(JsonStreamContext context) {
    StringBuilder builder = new StringBuilder();
    for (JsonStreamContext c = context; c != null; c = c.getParent()) {
      if (c.inArray()) {
        builder.insert(0, "[" + c.getCurrentIndex() + "]");
      } else if (c.inObject()) {
        builder.insert(0, "." + c.getCurrentName());
      } else if (c.inRoot()) {
        if (builder.length() > 0 && builder.charAt(0) == '.') {
          builder.deleteCharAt(0);
        }
      }
    }
    return builder.toString();
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
      return String.format("%s.%s : %s </= %s", hostType, attributeName, attributeType, super.getMessage());
    }

    @Override
    public String toString() {
      return "Cannot unmarshal " + getMessage();
    }
  }

  @SuppressWarnings("unchecked")
  public static <T> void marshalWithOneOfMarshalers(
      JsonGenerator generator,
      T instance,
      Marshaler<?>... marshalers) throws IOException {
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
  // safe resource, no need to handle buffer nor parser at this level
  @SuppressWarnings("resource")
  public static Object unmarshalWithOneOfMarshalers(
      JsonParser parser,
      String hostType,
      String attributeName,
      String attributeType,
      Marshaler<?>... marshalers) throws IOException {

    List<IOException> exceptions = Lists.newArrayListWithCapacity(marshalers.length);

    TokenBuffer buffer = new TokenBuffer(parser);
    buffer.copyCurrentStructure(parser);

    for (Marshaler<?> marshaler : marshalers) {
      try {
        JsonParser bufferParser = buffer.asParser();
        return marshaler.unmarshalInstance(bufferParser);
      } catch (IOException ex) {
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
   * Used to call from generated code.
   * Requires Jackson version 2.4.4 or up as it's dependent of fix
   * <a href="https://github.com/FasterXML/jackson-databind/issues/592">
   * https://github.com/FasterXML/jackson-databind/issues/592</a>
   * @param <T> the type of expected unmarshaled type.
   * @param marshaler the marshaler
   * @param buffer token buffer
   * @return unmarshaled instance
   * @throws IOException If JSON processing error occured.
   */
  @SuppressWarnings("resource")
  public static <T> T fromTokenBuffer(
      Marshaler<T> marshaler,
      TokenBuffer buffer) throws IOException {
    JsonParser parser = buffer.asParser();
    parser.nextToken();
    return marshaler.unmarshalInstance(parser);
  }

  @Nullable
  private static ObjectCodec findCodec(Map<String, TokenBuffer> buffers) {
    return !buffers.isEmpty()
        ? buffers.values().iterator().next().getCodec()
        : null;
  }

  public static <T> Marshaler<T> getMarshalerFor(Class<? extends T> type) {
    // unchecked: relies on how nice are marshalers contributors
    @SuppressWarnings("unchecked") @Nullable Marshaler<T> marshaler = (Marshaler<T>) Registry.marshalers.get(type);
    if (marshaler != null) {
      return marshaler;
    }
    throw new IllegalArgumentException(
        String.format(
            "Cannot find marshaler for type %s. Use @Json.Marshaled annotation to generate associated marshaler",
            type.getCanonicalName()));
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
