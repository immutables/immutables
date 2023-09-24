package org.immutables.fixture.modifiable;

import org.immutables.value.Value;

public interface FromTypesModifiables {

  interface Iface {
    boolean a();
  }

  // renaming from->mergeFrom is needed to test non-hardcoded resolution of from/mergeFrom methods
  @Value.Immutable
  @Value.Style(from = "mergeFrom")
  @Value.Modifiable
  interface FromType {
    int a();

    class Builder extends ImmutableFromType.Builder {}
  }

  @Value.Immutable
  @Value.Style(from = "mergeFrom")
  @Value.Modifiable
  public abstract class FromManyTypes implements Iface {
    public abstract int b();
  }

  @Value.Immutable
  @Value.Style(from = "")
  @Value.Modifiable
  interface NoFrom {
    int a();
    class Builder extends ImmutableNoFrom.Builder {}
  }
}
