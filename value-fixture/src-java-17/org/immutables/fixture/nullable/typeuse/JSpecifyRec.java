package org.immutables.fixture.nullable.typeuse;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.immutables.value.Value;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
@Value.Builder
@Value.Style(optionalAcceptNullable = true, jdk9Collections = true)
public record JSpecifyRec(
    @Nullable Integer aa,
    @Nullable List<@Nullable String> lst,
    @Nullable Set<Integer> sts,
    Optional<String> opt,
    Optional<Object> obj) {}
