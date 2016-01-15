package org.immutables.builder.fixture;

public class BuilderParameterAndSwitchUse {
  void use() {
    ImmutableBuilderParameterAndSwitch.builder(2, "")
        .runtimePolicy()
        .sourcePolicy()
        .classPolicy()
        .build();

    new BuilderParameterAndSwitch2.Building(2, "guess")
        .runtimePolicy()
        .build();
  }
}
