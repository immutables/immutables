package org.immutables.fixture.j17;

import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Builder
@Value.Style(stagedBuilder = true)
public record RecordGenericStaged<B, C>(int a, B b, @Nullable C c) {

  public static <B, C> RecordGenericStagedBuilderStages.BuildStart<B, C> alsoStaged() {
    return RecordGenericStagedBuilderStages.start();
  }
}
