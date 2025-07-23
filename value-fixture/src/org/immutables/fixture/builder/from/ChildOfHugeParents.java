package org.immutables.fixture.builder.from;

import org.immutables.value.Value;

@Value.Immutable
public interface ChildOfHugeParents extends HugeParentOne, HugeParentTwo {
}
