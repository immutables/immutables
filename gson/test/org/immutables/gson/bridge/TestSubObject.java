package org.immutables.gson.bridge;

import org.immutables.gson.Gson;
import org.immutables.value.Value;

@Gson.TypeAdapters
@Value.Immutable
public interface TestSubObject {
  String a();

  Number b();
}
