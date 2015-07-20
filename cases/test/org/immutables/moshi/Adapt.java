package org.immutables.moshi;

import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multiset;
import com.google.common.collect.SetMultimap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.immutables.value.Value;

@Value.Immutable(builder = false)
@Value.Enclosing
@Json.Adapters
public interface Adapt {

  @Value.Parameter
  @Json.Named("setische")
  Set<Inr> set();

  @Value.Parameter
  Multiset<Nst> bag();

  @Value.Immutable
  public interface Inr {
    String[] arr();

    List<Integer> list();

    List<List<Integer>> listList();

    Map<String, Nst> map();

    ListMultimap<String, Nst> listMultimap();

    @Json.Ignore
    SetMultimap<Integer, Nst> setMultimap();
  }

  @Value.Immutable
  public interface Nst {
    @Json.Named("i")
    int value();

    @Json.Named("s")
    String string();
  }
}
