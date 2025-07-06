package org.immutables.datatype;

import org.immutables.value.Value.Enclosing;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Parameter;

@Data
@Enclosing
public interface Vvv {

  @Immutable
  interface Zzt extends Vvv {
    int a();

    String b();

    class Builder extends ImmutableVvv.Zzt.Builder {}
  }

  @Immutable(builder = false)
  interface Bbz extends Vvv {
    @Parameter
    int a();

    @Parameter
    String b();
  }
}
