package org.immutables.fixture.generatorext;

import org.junit.Test;

public class RewriteTest {
  @Test(expected = IllegalArgumentException.class)
  public void rewrite() {
    // Our changed preconditions will be able to throw IllegalArgumentException
    // instead of NullPointerException
    ImmutableSampleRewritedImports.of(null);
  }
}
