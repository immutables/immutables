package org.immutables.fixture.j17;

import org.immutables.value.Value;

@Value.Builder
public record RecordGeneric<A, B>(A aa, B bb) implements WithRecordGeneric<A, B> {}
