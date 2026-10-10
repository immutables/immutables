package org.immutables.fixture.jackson3.packall.child;

import org.immutables.value.Value;

/**
 * Lives in a subpackage of the package that carries {@code @StrictStyle}
 * (Jackson 3 {@code @JsonSerialize}/{@code @JsonDeserialize} meta-annotations).
 * Regression for #1649: Jackson 3 flags must walk parent packages like Jackson 2.
 */
@Value.Immutable
public interface _ChildBound {
  int getA();
  String getB();
}
