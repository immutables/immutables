package org.immutables.fixture.j17;

import org.immutables.value.Value;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
@Value.Builder
@Value.Style(stagedBuilder = true)
public record RecordStagedNullMarked(String name, @Nullable String nickname, int age) {
  public static RecordStagedNullMarkedBuilderStages.BuildStart builder() {
    return RecordStagedNullMarkedBuilderStages.start();
  }
}
