package org.immutables.fixture.j17;

import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Builder
@Value.Style(stagedBuilder = true)
public record RecordStaged(int a, String b, @Nullable String c) {

  public static RecordStagedBuilderStages.BuildStart builder() {
    return new RecordStagedBuilder();
  }
}
