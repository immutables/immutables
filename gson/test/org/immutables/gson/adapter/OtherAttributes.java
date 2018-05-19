package org.immutables.gson.adapter;

import com.google.gson.JsonObject;
import org.immutables.gson.Gson;
import org.immutables.value.Value;

@Gson.TypeAdapters
@Value.Immutable
public interface OtherAttributes {
  int a();

  String b();

  @Gson.Other
  JsonObject rest();
}
