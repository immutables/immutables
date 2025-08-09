package org.immutables.fixture.jdkonly;

import java.lang.annotation.RetentionPolicy;
import java.util.Optional;
import java.util.OptionalInt;
import org.immutables.value.Value;
import org.jspecify.annotations.NullMarked;

@NullMarked
@Value.Immutable
@Value.Style(optionalAcceptNullable = true)
public abstract class AllOptionalWithDefaults implements WithAllOptionalWithDefaults {
  @Value.Default
  public Optional<Object> computedValue() { // to check the class with local var in generated code
    return Optional.empty();
  }

  @Value.Default
  public Optional<String> str() {
    return Optional.of("foo");
  }

  @Value.Default
  public Optional<Integer> intWrapper() {
    return Optional.of(13);
  }

  @Value.Default
  public OptionalInt intSpecialized() {
    return OptionalInt.of(11);
  }

  @Value.Default
  public Optional<RetentionPolicy> enumOpt() {
    return Optional.of(RetentionPolicy.RUNTIME);
  }
}
