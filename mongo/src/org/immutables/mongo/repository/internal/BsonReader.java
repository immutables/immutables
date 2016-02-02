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

import com.google.gson.stream.JsonReader;
import de.undercouch.bson4jackson.BsonParser;
import de.undercouch.bson4jackson.types.ObjectId;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.regex.Pattern;
import javax.annotation.concurrent.NotThreadSafe;
import org.immutables.gson.stream.JsonParserReader;

/**
 * BSON subclass for {@link JsonReader} that uses bson4jackson. Adds methods to read BSON
 * specific types.
 */
@NotThreadSafe
public class BsonReader extends JsonParserReader {
  private final BsonParser parser;

  public BsonReader(BsonParser parser) {
    super(parser);
    this.parser = parser;
  }

  public boolean peekedTimeInstant() throws IOException {
    peek();
    return parser.getEmbeddedObject() instanceof Date;
  }

  public long nextTimeInstant() throws IOException {
    if (peekedTimeInstant()) {
      consumePeek();
      return ((Date) parser.getEmbeddedObject()).getTime();
    }
    throw unexpectedFor("UTCDate");
  }

  private IllegalStateException unexpectedFor(String bsonType) throws IOException {
    throw new IllegalStateException(String.format(
        "Expected %s, but was but was %s : %s", bsonType, parser.getCurrentToken(), parser.getEmbeddedObject()));
  }

  public boolean peekedBinary() throws IOException {
    peek();
    return parser.getEmbeddedObject() instanceof byte[];
  }

  public byte[] nextBinary() throws IOException {
    if (peekedBinary()) {
      consumePeek();
      return (byte[]) parser.getEmbeddedObject();
    }
    throw unexpectedFor("Binary");
  }

  public boolean peekedObjectId() throws IOException {
    peek();
    return parser.getEmbeddedObject() instanceof ObjectId;
  }

  public byte[] nextObjectId() throws IOException {
    if (peekedObjectId()) {
      consumePeek();
      ObjectId id = (ObjectId) parser.getEmbeddedObject();
      byte[] bytes = new byte[12];
      ByteBuffer buffer = ByteBuffer.wrap(bytes);
      buffer.putInt(id.getTime());
      buffer.putInt(id.getMachine());
      buffer.putInt(id.getInc());
      return bytes;
    }
    throw unexpectedFor("ObjectID");
  }

  public boolean peekedPattern() throws IOException {
    peek();
    return parser.getEmbeddedObject() instanceof Pattern;
  }

  public Pattern nextPattern() throws IOException {
    if (peekedPattern()) {
      consumePeek();
      return (Pattern) parser.getEmbeddedObject();
    }
    throw unexpectedFor("Regexp");
  }
}
