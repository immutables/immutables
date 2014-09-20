package org.immutables.modeling.meta;

import java.util.List;
import org.immutables.annotation.GenerateImmutable;
import org.immutables.annotation.GenerateNested;

@GenerateNested
public final class Meta {
  private Meta() {}

  @GenerateImmutable
  public interface Attribute {
    String name();

    Type type();
  }

  @GenerateImmutable
  public interface Type {
    List<Facet> facet();
  }

  @GenerateImmutable
  public interface Facet {
    Type target();

    List<Attribute> attribute();
  }
}
