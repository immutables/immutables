package org.immutables.fixture.nested;

import org.immutables.value.Value;

// compilation test to resolve ambiguity Foo/Bar
@Value.Enclosing
public interface Foo {
  String value();

  @Value.Immutable
  interface Bar extends Foo {}
}
