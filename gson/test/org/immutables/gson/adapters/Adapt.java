package org.immutables.gson.adapters;

import org.immutables.gson.Gson;
import org.immutables.value.Value;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Value.Immutable(builder = false)
@Value.Nested
@Gson.TypeAdapted
public interface Adapt {

  @Value.Parameter
  Set<Inr> set();

  @Value.Immutable
  public interface Inr {
    List<Integer> list();

    Map<String, Nst> map();
  }

  @Value.Immutable
  public interface Nst {
    int value();

    String string();
  }

}
