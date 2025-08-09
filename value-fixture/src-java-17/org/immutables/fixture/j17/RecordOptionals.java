package org.immutables.fixture.j17;

import java.lang.annotation.RetentionPolicy;
import java.util.Optional;
import java.util.OptionalInt;
import org.immutables.value.Value;
import org.jspecify.annotations.NullMarked;

@NullMarked
@Value.Builder
@Value.Style(optionalAcceptNullable = true)
public record RecordOptionals(
    Optional<Object> obj,
    Optional<String> str,
    Optional<Integer> intWrapper,
    Optional<RetentionPolicy> enumOpt,
    OptionalInt intSpecialized) implements WithRecordOptionals {
}
