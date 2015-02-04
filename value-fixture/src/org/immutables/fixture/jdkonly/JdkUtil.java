package org.immutables.fixture.jdkonly;

import org.immutables.value.Value;

/**
 * Compilation test for the utils
 */
@Value.Immutable
@Value.Style(jdkOnly = true)
public interface JdkUtil {
  boolean a();

  double d();

  float f();

  long l();

  char c();

  Object o();
}
