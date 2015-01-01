/*
    Copyright 2014 Immutables Authors and Contributors

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
package org.immutables.marshal.gson;

import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import javax.annotation.Nullable;

public final class Routines {
  private Routines() {}

  /**
   * Fallback overload method that throws exception.
   * @param <T> the generic type
   * @param reader the reader
   * @param objectNull the null refence
   * @param expectedClass the expected class
   * @return the T instance
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static <T> T unmarshal(
      JsonReader reader,
      @Nullable Object objectNull,
      Class<?> expectedClass) throws IOException {
    throw new IOException("No marshaler can handle " + expectedClass);
  }

  /**
   * Default unmarshal for enum object.
   * <p>
   * Used in generated code via static imports method overload resolution by compiler.
   * @param <T> expected enum type
   * @param reader the reader
   * @param enumNull the enum null, always {@code null}
   * @param expectedClass the expected class
   * @return the T instance
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static <T extends Enum<T>> T unmarshal(
      JsonReader reader,
      @Nullable Enum<T> enumNull,
      Class<T> expectedClass) throws IOException {
    return Enum.valueOf(expectedClass, reader.nextString());
  }

  /**
   * Marshal key by default via {@link Object#toString()}.
   * @param object the object
   * @return the string
   */
  public static String marshalKey(Object object) {
    return object.toString();
  }

  public static void marshal(
      JsonWriter writer,
      @Nullable String instance) throws IOException {
    writer.value(instance);
  }

  /**
   * Default marshal for {@link Enum}.
   * @param writer the writer
   * @param instance the instance
   * @throws IOException Signals that an I/O exception has occurred. {@link Enum#name()}.
   */
  public static void marshal(
      JsonWriter writer,
      Enum<?> instance) throws IOException {
    writer.value(instance.name());
  }

  public static void marshal(
      JsonWriter writer,
      @Nullable Object instance) throws IOException {
    writer.value(instance.toString());
  }
}
