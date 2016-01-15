package org.immutables.builder.fixture;

import java.lang.annotation.RetentionPolicy;
import org.immutables.builder.Builder;
import org.immutables.value.Value;

@Value.Style(typeInnerBuilder = "Building")
@Value.Immutable
public interface BuilderParameterAndSwitch2 {
  @Builder.Parameter
  int theory();

  @Builder.Parameter
  String value();

  @Builder.Switch
  RetentionPolicy policy();

  class Building extends ImmutableBuilderParameterAndSwitch2.Builder {
    public Building() {
      super(0, "");
    }

    public Building(int theory, String label) {
      super(theory, label);
    }
  }
}
