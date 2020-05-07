package org.immutables.data;

import java.util.Map;
import javax.annotation.Nullable;
import org.immutables.value.Value.Enclosing;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Parameter;

@Data
@Immutable
@Enclosing
public interface Dtt {
  int a();

  String b();

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

    @Nullable
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
}
