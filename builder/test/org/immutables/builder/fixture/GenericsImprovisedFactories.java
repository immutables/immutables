package org.immutables.builder.fixture;

import java.util.Set;
import com.google.common.collect.Iterables;
import java.util.List;
import javax.annotation.Nullable;
import org.immutables.builder.Builder;
import org.immutables.value.Value;

class GenericsImprovisedFactories2 {
  @Builder.Factory
  public static <K extends Object & Runnable> Iterable<K> genericConcat(
      List<K> strings,
      Set<K> numbers) {
    return Iterables.<K>concat(strings, numbers);
  }

  void use() {

    class X implements Runnable {
      @Override
      public void run() {}
    }
    Iterable<X> concat = new GenericConcatBuilder<X>()
        .addStrings(new X())
        .addNumbers(new X(), new X(), new X())
        .build();

    concat.toString();
  }
}

@Value.Style(newBuilder = "newBuilder")
class GenericsImprovisedFactories {

  @Builder.Factory
  @SuppressWarnings("all")
  public static <T, V extends RuntimeException> String genericSuperstring(int theory, T reality, @Nullable V evidence)
      throws V {
    if (evidence != null) {
      throw evidence;
    }
    return theory + " != " + reality;
  }

  void use() {

    String superstring = GenericSuperstringBuilder.<String, IllegalArgumentException>newBuilder()
        .theory(0)
        .reality("")
        .evidence(null)
        .build();

    superstring.toString();
  }
}
