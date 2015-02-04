package org.immutables.fixture.jackson;

import org.immutables.value.Json;
import org.immutables.value.Value;

@Value.Immutable
@Json.Marshaled
@Value.Style(
        init = "set*",
        typeAbstract = "Abstract*",
        typeImmutable = "*",
        get = {"is*", "get*"})
public interface AbstractCalabra {
  String getA();

  boolean isB();
}
