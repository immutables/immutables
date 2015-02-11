package org.immutables.gson.adapter;

import com.google.common.base.Optional;
import java.util.List;
import java.util.Map;
import org.immutables.gson.Gson;
import org.immutables.value.Value;

@Gson.TypeAdapters
@Value.Nested
public interface Polymorphics {

  @Gson.ExpectedSubtypes(D.class)
  interface A {}

  @Value.Immutable(builder = false)
  interface B extends A {
    @Value.Parameter
    int v1();
  }

  @Value.Immutable(builder = false)
  interface C extends A {
    @Value.Parameter
    String v2();
  }

  @Value.Immutable(builder = false)
  interface D extends A {
    @Value.Parameter
    boolean v1();
  }

  @Value.Immutable
  interface Host {

    Optional<A> from();

    @Gson.ExpectedSubtypes({B.class, C.class, D.class})
    List<A> list();

    @Gson.ExpectedSubtypes({})
    Map<String, A> autodetect();
  }
}
