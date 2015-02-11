package org.immutables.gson.adapter;

import com.google.common.collect.ImmutableMap;
import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.Map;
import java.util.Set;
import org.junit.Test;
import static org.immutables.check.Checkers.*;

public class GsonFeaturesTest {
  final Gson gsonWithOptions = new GsonBuilder()
      .setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
      .serializeNulls()
      .registerTypeAdapterFactory(new SimpleTypeAdapters())
      .registerTypeAdapterFactory(new UnsimpleTypeAdapters())
      .create();

  final Gson gsonDefault = new GsonBuilder()
      .registerTypeAdapterFactory(new SimpleTypeAdapters())
      .registerTypeAdapterFactory(new UnsimpleTypeAdapters())
      .create();

  @Test
  public void namingApplied() {
    Simple s1 = ImmutableSimple.builder()
        .addCharacterList('a')
        .optionalString("s")
        .nlb(1)
        .build();

    JsonObject json = gsonWithOptions.toJsonTree(s1).getAsJsonObject();
    check(keysIn(json)).hasAll("character_list", "optional_string", "_nullable_");
  }

  @Test
  public void emptyAsNullsAndSerializeNulls() {
    Simple simple = ImmutableSimple.builder().build();
    JsonObject json = gsonWithOptions.toJsonTree(simple).getAsJsonObject();
    check(keysIn(json)).hasContentInAnyOrder("character_list", "optional_string", "_nullable_");
  }

  @Test
  public void emptyAsNullsAndNotSerializeNulls() {
    Simple s1 = ImmutableSimple.builder().build();
    JsonObject json = gsonDefault.toJsonTree(s1).getAsJsonObject();
    check(keysIn(json)).isEmpty();
  }

  @Test
  public void emptyAsNotNulls() {
    Unsimple unsimple = ImmutableUnsimple.builder().build();
    JsonObject json = gsonDefault.toJsonTree(unsimple).getAsJsonObject();
    check(keysIn(json)).isOf("characterList");
  }

  private Set<String> keysIn(JsonObject json) {
    ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
    for (Map.Entry<String, JsonElement> entry : json.entrySet()) {
      builder.put(entry);
    }
    return builder.build().keySet();
  }
}
