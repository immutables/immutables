package org.immutables.gson.adapter;

import org.immutables.value.Value;
import org.immutables.gson.Gson;

@Gson.TypeAdapters(nullAsDefault = true)
public interface NullAsDefault {
  @Value.Immutable
  class Val {
    @Value.Default
    public int a() {
      return -1;
    }

    @Value.Default
    public String b() {
      return "n/a";
    }
  }
}
