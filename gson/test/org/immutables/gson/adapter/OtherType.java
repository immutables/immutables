package org.immutables.gson.adapter;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.SetMultimap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.immutables.gson.Gson;
import org.immutables.value.Value;

@Value.Immutable(builder = false)
@Value.Enclosing
@Gson.TypeAdapters
public interface OtherType {

  @Value.Parameter
  ListMultimap<String, Hjk> listMultimap();

  @Value.Parameter
  SetMultimap<Integer, Hjk> setMultimap();

  Map<String, Lui> map();

  @Value.Immutable
  public interface Lui {
    List<Integer> list();

    Map<String, Hjk> map();

    Set<Hjk> set();

    Multiset<Hjk> bag();

    @Value.Parameter
    Object[] arr();
  }

  @Value.Immutable(builder = false)
  public interface Hjk {
    @Value.Parameter
    String[] arr();
  }
}
