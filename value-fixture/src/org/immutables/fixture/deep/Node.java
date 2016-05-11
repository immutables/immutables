package org.immutables.fixture.deep;

import org.immutables.value.Value;

// Compilation test to prevent recussion
@Value.Style(deepImmutablesDetection = true)
public interface Node<T> {
  T value();

  Node<T> next();
}
