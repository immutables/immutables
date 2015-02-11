package org.immutables.gson.adapter;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonParseException;
import com.google.gson.JsonPrimitive;
import com.google.gson.reflect.TypeToken;
import org.junit.Test;
import static org.immutables.check.Checkers.*;

public class ExpectedSubtypesAdapterTest {
  final TypeToken<Long> longType = TypeToken.get(Long.class);
  final TypeToken<Boolean> booleanType = TypeToken.get(Boolean.class);
  final TypeToken<Object> objectType = TypeToken.get(Object.class);
  final Gson gson = new Gson();

  @Test
  public void readSubtype() {
    Gson gson = new Gson();

    ExpectedSubtypesAdapter<Object> subtypesAdaper =
        ExpectedSubtypesAdapter.create(gson, objectType, longType, booleanType);

    check(subtypesAdaper.fromJsonTree(new JsonPrimitive(true))).is(true);
    check(subtypesAdaper.fromJsonTree(new JsonPrimitive(111))).is(111L);
  }

  @Test
  public void writeSubtype() {
    Gson gson = new Gson();

    ExpectedSubtypesAdapter<Object> subtypesAdaper =
        ExpectedSubtypesAdapter.create(gson, objectType, longType, booleanType);

    check(subtypesAdaper.toJsonTree(true)).is(new JsonPrimitive(true));
    check(subtypesAdaper.toJsonTree(111L)).is(new JsonPrimitive(111L));
  }

  @Test(expected = JsonParseException.class)
  public void failSubtype() {

    ExpectedSubtypesAdapter<Object> subtypesAdaper =
        ExpectedSubtypesAdapter.create(gson, objectType, longType, booleanType);

    subtypesAdaper.fromJsonTree(new JsonArray());
  }
}
