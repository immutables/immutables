package org.immutables.fixture.j17;

import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Enclosing
@Value.Style(stagedBuilder = true)
public interface EnclosedStaged {

  @Value.Builder
  record Rec1(int a, String b, @Nullable String c) {}

  @Value.Builder
  record Rec2(int aa) {}
}
