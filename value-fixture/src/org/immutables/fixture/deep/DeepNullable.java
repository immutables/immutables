package org.immutables.fixture.deep;

import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Immutable
interface DeepChild {}

@Value.Immutable
interface DeepStepChild {}

// should not use shortcut constructor (compile error) when deepImmutablesDetection
@Value.Immutable
@Value.Style(deepImmutablesDetection = true, allParameters = true)
interface DeepNullable {
  @Nullable
  DeepChild child();

  @Nullable
  DeepStepChild another();
}


