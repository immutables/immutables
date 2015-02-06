package org.immutables.gson.adapters;

import com.google.common.collect.Multiset;
import com.google.common.collect.SetMultimap;
import com.google.common.collect.ListMultimap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.immutables.gson.Gson;
import org.immutables.value.Value;

@Value.Immutable(builder = false)
@Value.Nested
@Gson.TypeAdapted
public interface Adapt {

  @Value.Parameter
  Set<Inr> set();

  @Value.Parameter
  Multiset<Nst> bag();

  @Value.Immutable
  public interface Inr {
    String[] arr();

    List<Integer> list();

    Map<String, Nst> map();

    ListMultimap<String, Nst> listMultimap();

    SetMultimap<Integer, Nst> setMultimap();
  }

  @Value.Immutable
  public interface Nst {
    int value();

    String string();
  }
}
