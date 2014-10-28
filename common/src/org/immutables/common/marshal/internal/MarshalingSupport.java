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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.util.TokenBuffer;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.immutables.common.marshal.Marshaler;

import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * The marshaling support.
 */
public final class MarshalingSupport {
  private MarshalingSupport() {}

  public static void ensureToken(JsonToken expected, JsonToken actual, Class<?> marshaledType) {
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
   */
  public static void ensureCondition(
      boolean condition,
      String hostType,
      String attributeName,
      String attributeType,
      Object message) {
    if (!condition) {
      throw new UnmarshalMismatchException(hostType, attributeName, attributeType, message);
    }
  }

  private static class UnmarshalMismatchException extends RuntimeException {
    private final String hostType;
    private final String attributeName;
    private final String attributeType;

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

    try (TokenBuffer buffer = new TokenBuffer(parser)) {
      buffer.copyCurrentStructure(parser);

      @Nullable
      List<RuntimeException> exceptions = Lists.newArrayListWithCapacity(marshalers.length);

      for (Marshaler<?> marshaler : marshalers) {
        try (JsonParser bufferParser = buffer.asParser()) {
          bufferParser.nextToken();
          return marshaler.unmarshalInstance(bufferParser);
        } catch (RuntimeException ex) {
          exceptions.add(ex);
        }
      }

      UnmarshalMismatchException exception =
          new UnmarshalMismatchException(hostType, attributeName, attributeType, "Cannot unambigously parse");

      for (RuntimeException ex : exceptions) {
        exception.addSuppressed(ex);
      }

      throw exception;
    }
  }

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
   * @throws IOException If json processing error occured.
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
      ImmutableMap.Builder<Class<?>, Marshaler<?>> builder = ImmutableMap.builder();
      for (MarshalingContributor contributor : ServiceLoader.load(MarshalingContributor.class)) {
        contributor.putMarshalers(builder);
      }
      marshalers = builder.build();
    }
  }

}
