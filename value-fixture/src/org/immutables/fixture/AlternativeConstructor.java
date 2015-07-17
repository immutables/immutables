package org.immutables.fixture;
import java.util.ArrayList;
import java.util.List;
import org.immutables.value.Value;

@Value.Immutable
public abstract class AlternativeConstructor {
  @Value.Parameter
  public abstract List<String> foo();

  public static AlternativeConstructor of(List<String> foo) {
    return ImmutableAlternativeConstructor.of(foo);
  }

  public static void main(String[] args) {
    System.out.format("%s\n", AlternativeConstructor.of(new ArrayList<>()));
  }
}