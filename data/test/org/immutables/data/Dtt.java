package org.immutables.data;

import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import javax.annotation.Nullable;
import org.immutables.value.Value;
import org.immutables.value.Value.Enclosing;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Parameter;

@Data
@Immutable
@Enclosing
public interface Dtt {
  int a();

  @CustomNullableAnnotation
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
  public interface Vgg<D> {
    boolean c();

    D d();

    class Builder<D> extends ImmutableDtt.Vgg.Builder<D> {}
  }

  @Immutable
  public interface Hjj<H> {
    Map<String, H> ef();

    @CustomNullableAnnotation
    H h();

    class Builder<H> extends ImmutableDtt.Hjj.Builder<H> {}
  }

  @Immutable(builder = false)
  public interface Uio {
    @Parameter
    int u();

    @Parameter
    String v();
  }

  @Immutable(builder = false, singleton = true)
  public interface Sin {}
  
  @Immutable
  public interface Opt<O> {
    Optional<O> o();
    OptionalInt i();
    OptionalLong l();
    OptionalDouble d();
    class Builder<O> extends ImmutableDtt.Opt.Builder<O> {}
  }
  
  @Data.Inline
  @Immutable
  public interface Inl {
    @Parameter
    int value();
  }
  
  @Immutable
  public interface Ign {
    @Data.Ignore OptionalInt g();
    class Builder extends ImmutableDtt.Ign.Builder {}
  }
}
