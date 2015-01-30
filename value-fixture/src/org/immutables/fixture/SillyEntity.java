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
package org.immutables.fixture;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.google.common.primitives.UnsignedInteger;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.immutables.value.ext.Json;
import org.immutables.value.ext.Mongo;
import org.immutables.value.Value;

@Value.Immutable
@Json.Import({SillyEntity.class})
@Mongo.Repository
public abstract class SillyEntity {

  @Mongo.Id
  public abstract int id();

  @Json.Named("v")
  public abstract String val();

  @Json.Named("p")
  public abstract Map<String, Integer> payload();

  @Json.Named("i")
  public abstract List<Integer> ints();

  @Value.Derived
  public UnsignedInteger der() {
    return UnsignedInteger.valueOf(1);
  }

  public static void marshal(JsonGenerator generator, UnsignedInteger integer) throws IOException {
    generator.writeNumber(integer.doubleValue());
  }

  /**
   * Unmarshal.
   * @param parser the parser
   * @param integerNull the integer null
   * @param expectedClass the expected class
   * @return the unsigned integer
   * @throws IOException Signals that an I/O exception has occurred.
   */
  public static UnsignedInteger unmarshal(
      JsonParser parser,
      @Nullable UnsignedInteger integerNull,
      Class<UnsignedInteger> expectedClass)
      throws IOException {
    return UnsignedInteger.valueOf((long) parser.getDoubleValue());
  }
}
