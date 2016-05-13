package org.immutables.fixture.jdkonly;

import java.util.Arrays;
import org.junit.Test;
import com.google.common.collect.ImmutableSortedSet;
import static org.immutables.check.Checkers.check;

public class DefaultTest {
  @Test
  public void defArray() {
    ImmutableDefaultArray a1 = ImmutableDefaultArray.builder().build();
    check(a1.prop() != a1.prop());
    
    check(a1.ints() instanceof ImmutableSortedSet);
  }
}
