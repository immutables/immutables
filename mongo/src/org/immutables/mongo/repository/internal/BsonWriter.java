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
package org.immutables.mongo.repository.internal;

import com.google.common.base.Preconditions;
import com.google.gson.stream.JsonWriter;
import de.undercouch.bson4jackson.BsonGenerator;
import de.undercouch.bson4jackson.types.ObjectId;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.regex.Pattern;
import javax.annotation.concurrent.NotThreadSafe;
import org.immutables.gson.stream.JsonGeneratorWriter;

/**
 * BSON subclass for {@link JsonWriter} that uses bson4jackson. Adds methods to write BSON
 * specific types.
 */
@NotThreadSafe
public class BsonWriter extends JsonGeneratorWriter {
  private final BsonGenerator generator;

  public BsonWriter(BsonGenerator generator) {
    super(generator);
    this.generator = generator;
  }

  public void valueTimeInstant(Long value) throws IOException {
    generator.writeDateTime(new Date(value));
  }

  public void valueBinary(byte[] data) throws IOException {
    generator.writeBinary(data);
  }

  public void value(Pattern pattern) throws IOException {
    generator.writeRegex(pattern);
  }

  public void valueObjectId(byte[] data) throws IOException {
    Preconditions.checkArgument(data.length == 12, "ObjectId byte[] should have exactly 12 bytes");
    ByteBuffer bytes = ByteBuffer.wrap(data);
    ObjectId objectId = new ObjectId(bytes.getInt(), bytes.getInt(), bytes.getInt());
    generator.writeObjectId(objectId);
  }
}
