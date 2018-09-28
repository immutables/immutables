package org.immutables.mongo.bson4gson;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonNull;
import com.google.gson.JsonObject;
import com.google.gson.internal.bind.TypeAdapters;
import org.bson.BsonArray;
import org.bson.BsonBoolean;
import org.bson.BsonDocument;
import org.bson.BsonDocumentReader;
import org.bson.BsonDocumentWriter;
import org.bson.BsonDouble;
import org.bson.BsonInt32;
import org.bson.BsonInt64;
import org.bson.BsonNull;
import org.bson.BsonString;
import org.bson.BsonType;
import org.bson.json.JsonReader;
import org.junit.Test;

import java.io.IOException;

import static org.immutables.check.Checkers.check;

public class BsonReaderTest {

  @Test
  public void array() throws Exception {
    compare("[]");
    compare("[[]]");
    compare("[[[]]]");
    compare("[[], []]");
    compare("[[], [[]]]");
    compare("[[], [[]], []]");
    compare("[1]");
    compare("[1, 2]");
    compare("[1, 2, 3]");
    compare("[true]");
    compare("[true, true]");
    compare("[true, true, false]");
    compare("[0.11, 11.22, 3]");
    compare("[\"foo\"]");
    compare("[\"\"]");
    compare("[\"\", \"\"]");
    compare("[\"\", \"foo\"]");
    compare("[\"foo\", \"bar\"]");
    compare("[1, true, 0, 1.111]");
    compare("[null]");
    compare("[null, 1, false]");
    compare("[0.0, -1.2, 3]");
    compare("[[0], [1]]");
    compare("[[0], [], 1]");
    compare("[true, [], []]");
    compare("[{}]");
    compare("[{}, {}]");
    compare("[{}, {}, {}]");
    compare("[{\"a\": 1}, {\"b\": null}, {\"c\": false}]");
    compare("[{\"0\": 1}, [], {\"1\": null}, {}]");
  }

  @Test
  public void scalar() throws Exception {
    compare("0");
    compare("0.0");
    compare("-1");
    compare("-200");
    compare(Long.toString(Long.MIN_VALUE));
    compare(Long.toString(Long.MAX_VALUE));
    compare(Integer.toString(Integer.MIN_VALUE));
    compare(Integer.toString(Integer.MAX_VALUE));
    compare(Byte.toString(Byte.MIN_VALUE));
    compare(Byte.toString(Byte.MAX_VALUE));
    compare(Short.toString(Short.MIN_VALUE));
    compare(Short.toString(Short.MAX_VALUE));
    compare("0.1");
    compare("-0.1111");
    compare("-2.222");
    compare("0.11111111111");
    compare("true");
    compare("false");
    compare("null");
    compare("\"foo\"");
    compare("\"\"");
    compare("\"null\"");
  }

  @Test
  public void object() throws Exception {
    compare("{}");
    compare("{\"foo\": \"bar\"}");
    compare("{\"foo\": 1}");
    compare("{\"foo\": true}");
    compare("{\"foo\": 0.1}");
    compare("{\"foo\": null}");
    compare("{\"foo\": {}}");
    compare("{\"foo\": []}");
    compare("{\"foo\": [{}]}");
    compare("{\"foo\": [{}, {}]}");
    compare("{\"foo\": [1, 2, 3]}");
    compare("{\"foo\": [null]}");
    compare("{\"foo\": \"\"}");
    compare("{\"foo\": \"2017-09-09\"}");
    compare("{\"foo\": {\"bar\": \"qux\"}}");
    compare("{\"foo\": 1, \"bar\": 2}");
    compare("{\"foo\": [], \"bar\": {}}");
    compare("{\"foo\": {\"bar\": {\"baz\": true}}}");
  }

  /**
   * Reading from BSON to GSON
   */
  @Test
  public void bsonToGson() throws Exception {
    BsonDocument document = new BsonDocument();
    document.append("boolean", new BsonBoolean(true));
    document.append("int32", new BsonInt32(32));
    document.append("int64", new BsonInt64(64));
    document.append("double", new BsonDouble(42.42D));
    document.append("string", new BsonString("foo"));
    document.append("null", new BsonNull());
    document.append("array", new BsonArray());
    document.append("object", new BsonDocument());

    JsonElement element = TypeAdapters.JSON_ELEMENT.read(new BsonReader(new BsonDocumentReader(document)));
    check(element.isJsonObject());

    check(element.getAsJsonObject().get("boolean").getAsJsonPrimitive().isBoolean());
    check(element.getAsJsonObject().get("boolean").getAsJsonPrimitive().getAsBoolean());

    check(element.getAsJsonObject().get("int32").getAsJsonPrimitive().isNumber());
    check(element.getAsJsonObject().get("int32").getAsJsonPrimitive().getAsNumber().intValue()).is(32);

    check(element.getAsJsonObject().get("int64").getAsJsonPrimitive().isNumber());
    check(element.getAsJsonObject().get("int64").getAsJsonPrimitive().getAsNumber().longValue()).is(64L);

    check(element.getAsJsonObject().get("double").getAsJsonPrimitive().isNumber());
    check(element.getAsJsonObject().get("double").getAsJsonPrimitive().getAsNumber().doubleValue()).is(42.42D);

    check(element.getAsJsonObject().get("string").getAsJsonPrimitive().isString());
    check(element.getAsJsonObject().get("string").getAsJsonPrimitive().getAsString()).is("foo");

    check(element.getAsJsonObject().get("null").isJsonNull());
    check(element.getAsJsonObject().get("array").isJsonArray());
    check(element.getAsJsonObject().get("object").isJsonObject());
  }

  /**
   * Tests direct bson and gson mappings
   */
  @Test
  public void gsonToBson() throws Exception {
    JsonObject obj = new JsonObject();
    obj.addProperty("boolean", true);
    obj.addProperty("int32", 32);
    obj.addProperty("int64", 64L);
    obj.addProperty("double", 42.42D);
    obj.addProperty("string", "foo");
    obj.add("null", JsonNull.INSTANCE);
    obj.add("array", new JsonArray());
    obj.add("object", new JsonObject());


    BsonDocument doc = Jsons.toBson(obj);
    TypeAdapters.JSON_ELEMENT.write(new BsonWriter(new BsonDocumentWriter(doc)), obj);

    check(doc.keySet()).notEmpty();

    check(doc.get("boolean").getBsonType()).is(BsonType.BOOLEAN);
    check(doc.get("boolean").asBoolean());
    check(doc.get("int32").getBsonType()).is(BsonType.INT32);
    check(doc.get("int32").asInt32().getValue()).is(32);
    check(doc.get("int64").getBsonType()).is(BsonType.INT64);
    check(doc.get("int64").asInt64().getValue()).is(64L);
    check(doc.get("double").getBsonType()).is(BsonType.DOUBLE);
    check(doc.get("double").asDouble().getValue()).is(42.42D);
    check(doc.get("string").getBsonType()).is(BsonType.STRING);
    check(doc.get("string").asString().getValue()).is("foo");
    check(doc.get("null").getBsonType()).is(BsonType.NULL);
    check(doc.get("null").isNull());
    check(doc.get("array").getBsonType()).is(BsonType.ARRAY);
    check(doc.get("array").asArray()).isEmpty();
    check(doc.get("object").getBsonType()).is(BsonType.DOCUMENT);
    check(doc.get("object").asDocument().keySet()).isEmpty();
  }

  private static void compare(String string) throws IOException {
    JsonElement bson = TypeAdapters.JSON_ELEMENT.read(new BsonReader(new JsonReader(string))); // compare as BSON
    JsonElement gson = TypeAdapters.JSON_ELEMENT.fromJson(string); // compare as JSON
    check(bson).is(gson); // compare the two
  }

}