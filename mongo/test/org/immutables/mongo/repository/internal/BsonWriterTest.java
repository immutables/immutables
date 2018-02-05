package org.immutables.mongo.repository.internal;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.internal.bind.TypeAdapters;
import org.junit.Test;

import java.io.IOException;

import static org.immutables.check.Checkers.check;

public class BsonWriterTest {

  @Test
  public void scalars() throws Exception {
    write("1");
    write("0");
    write("true");
    write("false");
    write("null");
  }

  @Test
  public void array() throws Exception {
    write("[]");
    write("[[]]");
    write("[[[]]]");
  }

  @Test
  public void objects() throws Exception {
    write("{}");
    write("{ \"foo\": 123, \"bar\": 444}");
  }

  @Test
  public void customTypes() throws Exception {
    JsonObject obj = new JsonObject();
    obj.addProperty("null", (String) null);
    obj.addProperty("string", "Hello");
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

    JsonElement bson = Jsons.toGson(Jsons.toBson(gson.getAsJsonObject()));
    check(gson).is(bson);
  }
}