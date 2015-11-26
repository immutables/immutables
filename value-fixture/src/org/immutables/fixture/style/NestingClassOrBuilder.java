package org.immutables.fixture.style;

import org.immutables.value.Value;
import org.immutables.value.Value.Style.BuilderVisibility;
import org.immutables.value.Value.Style.ImplementationVisibility;

interface NestingClassOrBuilder {
  @Value.Immutable
  @Value.Style(
      visibility = ImplementationVisibility.PACKAGE,
      overshadowImplementation = true,
      implementationNestedInBuilder = true,
      builder = "new")
  interface ImplNestedInBuild {}

  @Value.Immutable
  @Value.Style(
      visibility = ImplementationVisibility.PUBLIC,
      builderVisibility = BuilderVisibility.PACKAGE)
  interface NonPublicBuild {}

  default void use() {
    ImplNestedInBuildBuilder builder = new ImplNestedInBuildBuilder();
    ImplNestedInBuild abstractValue = builder.build();
    abstractValue.getClass();

    ImmutableNonPublicBuild.Builder packagePrivateBuilder = ImmutableNonPublicBuild.builder();
    ImmutableNonPublicBuild immutable = packagePrivateBuilder.build();
    immutable.getClass();
  }
}
