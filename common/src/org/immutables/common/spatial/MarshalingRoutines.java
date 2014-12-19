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
package org.immutables.common.spatial;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.google.common.collect.ImmutableList;
import java.io.IOException;
import javax.annotation.Nullable;
import org.immutables.common.marshal.internal.MarshalingSupport;

@SuppressWarnings("unused")
public final class MarshalingRoutines {
  private MarshalingRoutines() {}

  public static Point unmarshal(
      JsonParser parser,
      @Nullable Point instantNull,
      Class<Point> expectedClass) throws IOException {
    MarshalingSupport.ensureToken(JsonToken.START_ARRAY, parser.getCurrentToken(), Point.class);
    MarshalingSupport.ensureCondition(parser.nextToken().isNumeric(),
        "Point",
        "latitude",
        "double",
        parser.getCurrentToken());
    double latitude = parser.getDoubleValue();
    MarshalingSupport.ensureCondition(parser.nextToken().isNumeric(),
        "Point",
        "longitude",
        "double",
        parser.getCurrentToken());
    double longitude = parser.getDoubleValue();
    MarshalingSupport.ensureToken(JsonToken.END_ARRAY, parser.nextToken(), Point.class);
    return Point.of(latitude, longitude);
  }

  public static void marshal(
      JsonGenerator generator,
      Point value) throws IOException {
    generator.writeStartArray();
    generator.writeNumber(value.latitude());
    generator.writeNumber(value.longitude());
    generator.writeEndArray();
  }

  public static Polygon unmarshal(
      JsonParser parser,
      @Nullable Polygon instantNull,
      Class<Polygon> expectedClass) throws IOException {
    MarshalingSupport.ensureToken(JsonToken.START_ARRAY, parser.getCurrentToken(), Point.class);
    ImmutableList.Builder<Point> builder = ImmutableList.builder();
    while (parser.nextToken() != JsonToken.END_ARRAY) {
      builder.add(unmarshal(parser, null, Point.class));
    }
    return Polygon.of(builder.build());
  }

  public static void marshal(
      JsonGenerator generator,
      Polygon value) throws IOException {
    generator.writeStartArray();
    for (Point point : value.vertices()) {
      marshal(generator, point);
    }
    generator.writeEndArray();
  }

}
