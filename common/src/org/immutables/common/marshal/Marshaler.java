/*
    Copyright 2013 Immutables.org authors

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

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import java.io.IOException;
import javax.annotation.WillNotClose;

/**
 * Marshaler that can marshal and unmarshal instances of expected type.
 * @param <T> marshaled type
 */
public abstract class Marshaler<T> {
  public abstract T unmarshalInstance(@WillNotClose JsonParser parser) throws IOException;

  public abstract Iterable<T> unmarshalIterable(@WillNotClose JsonParser parser) throws IOException;

  public abstract void marshalInstance(@WillNotClose JsonGenerator generator, T instance) throws IOException;

  public abstract void marshalIterable(@WillNotClose JsonGenerator generator, Iterable<T> instance)
      throws IOException;

  /**
   * <p>
   * <em>Note that actual marshaled and unmarshaled instances may have types that are subtypes of expected type</em>
   * @return expected type
   */
  public abstract Class<T> getExpectedType();
}
