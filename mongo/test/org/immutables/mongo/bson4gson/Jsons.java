package org.immutables.mongo.bson4gson;

import com.google.gson.JsonObject;
import com.google.gson.internal.bind.TypeAdapters;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import org.bson.BsonBinaryReader;
import org.bson.BsonBinaryWriter;
import org.bson.BsonDocument;
import org.bson.BsonDocumentReader;
import org.bson.BsonValue;
import org.bson.codecs.BsonDocumentCodec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.io.BasicOutputBuffer;
import org.immutables.mongo.bson4gson.BsonReader;
import org.immutables.mongo.bson4gson.BsonWriter;

import java.io.IOException;
import java.nio.ByteBuffer;

import static org.immutables.check.Checkers.check;

/**
 * Util methods to convert to/from bson/gson types.
 */
final class Jsons {

  static org.bson.BsonReader asBsonReader(JsonObject gson) throws IOException {
    BasicOutputBuffer buffer = new BasicOutputBuffer();
    BsonWriter writer = new BsonWriter(new BsonBinaryWriter(buffer));
    TypeAdapters.JSON_ELEMENT.write(writer, gson);
    return new BsonBinaryReader(ByteBuffer.wrap(buffer.toByteArray()));
  }

  static JsonWriter asGsonWriter(org.bson.BsonWriter writer) {
    return new BsonWriter(writer);
  }

  static JsonReader asGsonReader(BsonDocument bson) {
    BasicOutputBuffer output = new BasicOutputBuffer();
    new BsonDocumentCodec().encode(new BsonBinaryWriter(output), bson, EncoderContext.builder().build());
    return new BsonReader(new BsonBinaryReader(ByteBuffer.wrap(output.toByteArray())));
  }

  static BsonDocument toBson(JsonObject gson) throws IOException {
    return new BsonDocumentCodec().decode(asBsonReader(gson), DecoderContext.builder().build());
  }

  static JsonObject toGson(BsonDocument bson) throws IOException {
    return TypeAdapters.JSON_ELEMENT.read(asGsonReader(bson)).getAsJsonObject();
  }

  /**
   * Creates reader for position at {@code value}
   */
  static JsonReader readerAt(BsonValue value) throws IOException {
    BsonDocument doc = new BsonDocument("value", value);
    BsonReader reader = new BsonReader(new BsonDocumentReader(doc));
    // advance AFTER value token
    reader.beginObject();
    check(reader.peek()).is(JsonToken.NAME);
    check(reader.nextName()).is("value");
    return reader;
  }
}
