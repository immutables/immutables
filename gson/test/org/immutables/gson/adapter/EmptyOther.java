package org.immutables.gson.adapter;

import com.google.gson.JsonObject;
import org.immutables.gson.Gson;
import org.immutables.value.Value;

@Value.Immutable
@Gson.TypeAdapters
public interface EmptyOther {
  @Gson.Other
  JsonObject other();
}
