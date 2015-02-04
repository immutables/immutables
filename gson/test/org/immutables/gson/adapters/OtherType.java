package org.immutables.gson.adapters;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.SetMultimap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.immutables.gson.Gson;
import org.immutables.value.Value;

@Value.Immutable(builder = false)
@Value.Nested
@Gson.TypeAdapted
public interface OtherType {

  @Value.Parameter
  ListMultimap<String, Nst> listMultimap();

  @Value.Parameter
  SetMultimap<Integer, Nst> setMultimap();

  @Value.Parameter
  Map<String, Nst> map();

  @Value.Immutable
  public interface Inr {
    List<Integer> list();

    Map<String, Nst> map();

    Set<Inr> set();

    Multiset<Nst> bag();
  }

  @Value.Immutable
  public interface Nst {
    int value();

    String string();
  }
}
