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
package org.immutables.common.repository.internal;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import de.undercouch.bson4jackson.BsonGenerator;
import de.undercouch.bson4jackson.types.ObjectId;
import java.io.IOException;
import java.util.Date;
import javax.annotation.Nullable;
import org.immutables.common.repository.Id;
import org.immutables.common.time.TimeInstant;
import org.immutables.common.time.TimeMeasure;

/**
 * Marshalers of some special types for repositories goes here.
 */
public final class ExtendedBuiltinMarshalingRoutines {
  private ExtendedBuiltinMarshalingRoutines() {}

  /**
   * Unmarshal.
   * @param parser the parser
   * @param idNull the id null
   * @param expectedClass the expected class
   * @return the id
   * @throws IOException Signals that an I/O exception has occurred.
   */
  @SuppressWarnings("deprecation")
  public static Id unmarshal(
      JsonParser parser,
      @Nullable Id idNull,
      Class<Id> expectedClass) throws IOException {
    JsonToken token = parser.getCurrentToken();
    if (token == JsonToken.VALUE_EMBEDDED_OBJECT) {
      ObjectId objectId = (ObjectId) parser.getEmbeddedObject();
      return Id.of(
          objectId.getTime(),
          objectId.getMachine(),
          objectId.getInc());
    }

    return Id.fromString(parser.getText());
  }

  @SuppressWarnings("deprecation")
  public static void marshal(
      JsonGenerator generator,
      Id value) throws IOException {
    if (generator instanceof BsonGenerator) {
      ((BsonGenerator) generator).writeObjectId(new ObjectId(
          value.time(),
          value.machine(),
          value.inc()));
    } else {
      generator.writeString(value.toString());
    }
  }

  // TimeInstant
  /**
   * Unmarshal.
   * @param parser the parser
   * @param instantNull the instant null
   * @param expectedClass the expected class
   * @return the time instant
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static TimeInstant unmarshal(
      JsonParser parser,
      @Nullable TimeInstant instantNull,
      Class<TimeInstant> expectedClass) throws IOException {

    JsonToken token = parser.getCurrentToken();
    if (token == JsonToken.VALUE_EMBEDDED_OBJECT) {
      Date date = (Date) parser.getEmbeddedObject();
      return TimeInstant.of(date.getTime());
    }

    return TimeInstant.of(parser.getLongValue());
  }

  public static void marshal(
      JsonGenerator generator,
      TimeInstant value) throws IOException {

    if (generator instanceof BsonGenerator) {
      ((BsonGenerator) generator).writeDateTime(new Date(value.value()));
    } else {
      generator.writeNumber(value.value());
    }
  }

  // TimeMeasure
  /**
   * Unmarshal.
   * @param parser the parser
   * @param instantNull the instant null
   * @param expectedClass the expected class
   * @return the time measure
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static TimeMeasure unmarshal(
      JsonParser parser,
      @Nullable TimeMeasure instantNull,
      Class<TimeMeasure> expectedClass) throws IOException {
    return TimeMeasure.fromString(parser.getText());
  }

  public static void marshal(
      JsonGenerator generator,
      TimeMeasure value) throws IOException {
    generator.writeString(value.toString());
  }
}
