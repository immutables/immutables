package org.immutables.fixture.deep.bug1540;

import org.immutables.value.Value;

@Value.Immutable
@Value.Style(
  deepImmutablesDetection = true,
  builtinContainerAttributes = false)
public interface ChildType extends SuperType<ChildType> {}
