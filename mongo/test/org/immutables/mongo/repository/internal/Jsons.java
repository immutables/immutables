package org.immutables.mongo.repository.internal;

import com.google.gson.JsonObject;
import com.google.gson.internal.bind.TypeAdapters;
import com.google.gson.stream.JsonReader;
import org.bson.BsonBinaryReader;
import org.bson.BsonBinaryWriter;
import org.bson.BsonDocument;
import org.bson.codecs.BsonDocumentCodec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.io.BasicOutputBuffer;

import java.io.IOException;
import java.nio.ByteBuffer;

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
}
