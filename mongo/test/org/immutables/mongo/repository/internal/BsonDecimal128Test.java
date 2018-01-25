package org.immutables.mongo.repository.internal;

import com.google.gson.JsonObject;
import com.google.gson.internal.bind.TypeAdapters;
import com.google.gson.stream.JsonToken;
import org.bson.BsonBinaryReader;
import org.bson.BsonBinaryWriter;
import org.bson.BsonDecimal128;
import org.bson.BsonDocument;
import org.bson.BsonType;
import org.bson.codecs.BsonDocumentCodec;
import org.bson.codecs.DecoderContext;
import org.bson.codecs.EncoderContext;
import org.bson.io.BasicOutputBuffer;
import org.bson.types.Decimal128;
import org.junit.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.ByteBuffer;

import static org.immutables.check.Checkers.check;

/**
 * Tests for read/write of {@link org.bson.types.Decimal128}
 */
public class BsonDecimal128Test {

  @Test
  public void read() throws Exception {
    BsonDocument doc = new BsonDocument();
    doc.put("int", new BsonDecimal128(Decimal128.parse(Integer.toString(Integer.MAX_VALUE))));
    doc.put("long", new BsonDecimal128(new Decimal128(Long.MAX_VALUE)));
    doc.put("double", new BsonDecimal128(Decimal128.parse("12.111")));

    BasicOutputBuffer output = new BasicOutputBuffer();
    new BsonDocumentCodec().encode(new BsonBinaryWriter(output), doc, EncoderContext.builder().build());
    BsonReader reader =  new BsonReader(new BsonBinaryReader(ByteBuffer.wrap(output.toByteArray())));

    reader.beginObject();
    check(reader.nextName()).is("int");
    check(reader.peek()).is(JsonToken.NUMBER);
    check(reader.nextInt()).is(Integer.MAX_VALUE);

    check(reader.nextName()).is("long");
    check(reader.peek()).is(JsonToken.NUMBER);
    check(reader.nextLong()).is(Long.MAX_VALUE);

    check(reader.nextName()).is("double");
    check(reader.peek()).is(JsonToken.NUMBER);
    check(reader.nextDouble()).is(12.111D);

    reader.endObject();

    reader.close();
  }

  @Test
  public void write() throws Exception {
    JsonObject obj = new JsonObject();
    BigInteger bigInteger = new BigInteger(Long.toString(Long.MAX_VALUE)).multiply(new BigInteger("128"));
    obj.addProperty("bigInteger", bigInteger);

    BigDecimal bigDecimal = new BigDecimal(Long.MAX_VALUE).multiply(new BigDecimal(1024));
    obj.addProperty("bigDecimal", bigDecimal);

    BasicOutputBuffer buffer = new BasicOutputBuffer();
    BsonWriter writer = new BsonWriter(new BsonBinaryWriter(buffer));

    TypeAdapters.JSON_ELEMENT.write(writer, obj);

    BsonDocument doc = new BsonDocumentCodec().decode(new BsonBinaryReader(ByteBuffer.wrap(buffer.toByteArray())), DecoderContext.builder().build());

    check(doc.get("bigInteger").getBsonType()).is(BsonType.DECIMAL128);
    check(doc.get("bigInteger").asDecimal128().decimal128Value().bigDecimalValue().toBigInteger()).is(bigInteger);

    check(doc.get("bigDecimal").getBsonType()).is(BsonType.DECIMAL128);
    check(doc.get("bigDecimal").asDecimal128().decimal128Value().bigDecimalValue()).is(bigDecimal);
  }

}
