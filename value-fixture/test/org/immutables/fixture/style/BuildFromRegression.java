package org.immutables.fixture.style;

import java.util.List;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(visibility = Value.Style.ImplementationVisibility.PUBLIC)
abstract class BuildFromRegression {
  public abstract List<String> values();

  public ImmutableBuildFromRegression replace(ImmutableBuildFromRegression that) {
    return ImmutableBuildFromRegression.copyOf(this)
        .withValues(that.values());
  }

  public ImmutableBuildFromRegression append(ImmutableBuildFromRegression that) {
    return ImmutableBuildFromRegression.builder()
        .from(this)
        .addAllValues(that.values())
        .build();
  }
}

interface Supertype2 {
  int a();
}

@Value.Immutable
@Value.Style(visibility = Value.Style.ImplementationVisibility.PUBLIC)
abstract class BuildFromRegression2 implements Supertype2 {
  public abstract List<String> values();

  public ImmutableBuildFromRegression2 append(ImmutableBuildFromRegression2 that) {
    return ImmutableBuildFromRegression2.builder()
        .from(this)
        .addAllValues(that.values())
        .build();
  }

  public ImmutableBuildFromRegression2 append(Supertype2 that) {
    return ImmutableBuildFromRegression2.builder()
        .from(this)
        .from(that)
        .build();
  }
}
