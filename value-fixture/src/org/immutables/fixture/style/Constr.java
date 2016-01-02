package org.immutables.fixture.style;

import org.immutables.value.Value;

@Value.Style(of = "new", typeImmutable = "Im*")
public interface Constr {

  @Value.Immutable
  public interface Rev {
    @Value.Parameter
    int major();

    @Value.Parameter
    int minor();
  }

  @Value.Immutable
  public interface Build {
    @Value.Parameter
    String date();

    @Value.Parameter
    String qualifier();
  }

  static void use() {
    new ImRev(1, 2);
    new ImBuild("2016.01.01", "Happy new build!");
  }
}
