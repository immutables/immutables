package org.immutables.mongo.repository.internal;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.internal.bind.TypeAdapters;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.math.BigDecimal;
import java.math.BigInteger;

import org.junit.Test;
import static org.immutables.check.Checkers.check;

public class BsonWriterTest {

  @Test
  public void scalars() throws Exception {
    write("1");
    write("0");
    write("{}");
  }

  @Test
  public void array() throws Exception {
    write("[]");
    write("[[]]");
    write("[[[]]]");
  }

  @Test
  public void objects() throws Exception {
    write("{ \"foo\": 123, \"bar\": 444}");
  }

  @Test
  public void customTypes() throws Exception {
    JsonObject obj = new JsonObject();
    obj.addProperty("byte", (byte) 1);
    obj.addProperty("short", (short) 4);
    obj.addProperty("int", 2222);
    obj.addProperty("long", 1111L);
    obj.addProperty("float", 55F);
    obj.addProperty("double", 128D);
    obj.addProperty("boolean", true);
    obj.addProperty("null", (String) null);
    obj.addProperty("string", "Hello");
    write(obj);
  }

  @Test
  public void bigNumbers() throws Exception {
    JsonObject obj = new JsonObject();
    obj.addProperty("bigInteger", new BigInteger(Long.toString(Long.MAX_VALUE)).multiply(new BigInteger("128")));
    obj.addProperty("bigDecimal", new BigDecimal(Long.MAX_VALUE).multiply(new BigDecimal(1024)));
    write(obj);
  }

  private static void write(String string) throws IOException {
    write(TypeAdapters.JSON_ELEMENT.fromJson(string));
  }

  private static void write(JsonElement gson) throws IOException {

    // BSON likes encoding full document (not simple elements like BsonValue)
    if (!gson.isJsonObject()) {
      JsonObject temp = new JsonObject();
      temp.add("ignore", gson);
      gson = temp;
    }
    Writer output = new StringWriter();
    com.google.gson.stream.JsonWriter bsonWriter = new BsonWriter(new org.bson.json.JsonWriter(output));
    TypeAdapters.JSON_ELEMENT.write(bsonWriter, gson);

    JsonElement bson = TypeAdapters.JSON_ELEMENT.read(new BsonReader(new org.bson.json.JsonReader(output.toString())));

    check(gson).is(bson);
  }
}