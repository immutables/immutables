package org.immutables.fixture.builder;

import org.immutables.value.Value.Immutable.ImplementationVisibility;
import com.google.common.collect.Iterables;
import java.util.List;
import java.util.SortedSet;
import javax.annotation.Nullable;
import org.immutables.value.Value;

/**
 * Builders for simple attributes, collection, generic and primitive variations.
 * Builders are public as of style annotation.
 */
@Value.Style(defaults = @Value.Immutable(visibility = ImplementationVisibility.PUBLIC))
class ImprovisedFactories {

  @Value.Builder
  static String superstring(int theory, String reality, @Nullable Void evidence) {
    return theory + " != " + reality + ", " + evidence;
  }

  @Value.Builder
  static Iterable<Object> concat(List<String> strings, @Value.NaturalOrder SortedSet<Integer> numbers) {
    return Iterables.<Object>concat(strings, numbers);
  }

  @Value.Builder
  static int sum(int a, int b) {
    return a + b;
  }

  void use() {
    String superstring = new SuperstringBuilder()
        .theory(0)
        .reality("")
        .evidence(null)
        .build();

    Iterable<Object> concat = new ConcatBuilder()
        .addStrings(superstring)
        .addNumbers(4, 2, 3)
        .build();

    concat.toString();

    int sumOf1and2 = new SumBuilder()
        .a(1)
        .b(2)
        .build();

    String.valueOf(sumOf1and2);
  }
}
