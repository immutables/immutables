package org.immutables.datatype;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import org.immutables.value.Value;
import org.immutables.value.Value.Enclosing;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Parameter;

@Data
@Immutable
@Enclosing
public interface Dtt {
  int a();

  @Nully
  String b();

  @Value.Default
  default int n() {
    return 1;
  }

  @Value.Default
  default boolean z() {
    return false;
  }

  class Builder extends ImmutableDtt.Builder {}

  @Immutable
  interface Vgg<D> {
    boolean c();

    D d();

    class Builder<D> extends ImmutableDtt.Vgg.Builder<D> {}
  }

  @Immutable
  interface Hjj<H> {
    Map<String, H> ef();

    @Nully
    H h();

    class Builder<H> extends ImmutableDtt.Hjj.Builder<H> {}
  }

  @Immutable(builder = false)
  interface Uio {
    @Parameter
    int u();

    @Parameter
    String v();
  }

  @Immutable(builder = false, singleton = true)
  interface Sin {}

  @Immutable
  interface Opt<O> {
    Optional<O> o();
    OptionalInt i();
    OptionalLong l();
    OptionalDouble d();
    class Builder<O> extends ImmutableDtt.Opt.Builder<O> {}
  }

  @Data.Inline
  @Immutable
  interface Inl {
    @Parameter
    int value();
  }

  @Immutable
  interface Ign {
    @Data.Ignore OptionalInt g();
    class Builder extends ImmutableDtt.Ign.Builder {}
  }

  @Immutable
  abstract class Really<T, Y> extends Complicated<T, String> implements Comparable<T> {
    abstract int i();
    abstract Y y();

    @Override public int compareTo(T o) {return 0;}
  }

  interface Ditto {
    List<String> d();
  }

  interface Generics<V, Z, U> {
    @Nully V v();
    Set<Z> z();
    List<U> u();
  }

  abstract class Complicated<A, B> implements Generics<B, B, Void>, Ditto {
    abstract Optional<A> a();
    abstract B b();
  }
}
