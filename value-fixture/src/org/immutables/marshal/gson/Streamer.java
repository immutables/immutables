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
package org.immutables.marshal.gson;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import java.io.IOException;
import javax.annotation.WillNotClose;

/**
 * Marshaler that can marshal and unmarshal instances of expected type {@code T}.
 * @param <T> marshaled type
 */
public abstract class Streamer<T> {
  /**
   * Unmarshal instance of marshaled object.
   * <p>
   * {@link JsonParser}'s current token should be first the token of a to be unmarshaled instance,
   * such as {@link JsonToken#START_OBJECT} etc or any other (scalar or array start in some cases).
   * On method exit, current token will be the last token of an unmarshaled instance, such as
   * {@link JsonToken#END_OBJECT}.
   * @param parser jackson JSON parser
   * @return instance of {@code T}
   * @throws IOException either IO failure or parsing problem
   */
  public abstract T unmarshalInstance(@WillNotClose JsonReader parser) throws IOException;

  /**
   * Marshal instance of generator. It is responsibility of the marshaler to output
   * any start and end tokens such as {@link JsonToken#START_OBJECT} or {@link JsonToken#END_OBJECT}
   * or any other kind of tokens.
   * @param generator jackson JSON generator
   * @param instance instance of {@code T}
   * @throws IOException either IO failure or parsing problem
   */
  public abstract void marshalInstance(@WillNotClose JsonWriter generator, T instance) throws IOException;

  /**
   * Expected type of this marshaler.
   * <p>
   * <em>Note that actual marshaled and unmarshaled instances may have types that are subtypes of expected type</em>
   * @return expected type
   */
  public abstract Class<T> getExpectedType();
}
