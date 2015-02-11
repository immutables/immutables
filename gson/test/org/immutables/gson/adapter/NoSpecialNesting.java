package org.immutables.gson.adapter;

import com.google.common.collect.Multiset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.immutables.gson.Gson;
import org.immutables.value.Value;

@Gson.TypeAdapters
@Value.Immutable(builder = false)
public interface NoSpecialNesting {

  @Gson.Named("SETH")
  @Value.Parameter
  Set<Inr> set();

  @Value.Parameter
  Multiset<Nst> bag();

  @Value.Immutable
  public interface Inr {
    String[] arr();

    List<Integer> list();

    Map<String, Nst> map();
  }

  @Value.Immutable
  public interface Nst {
    int value();

    String string();
  }
}
