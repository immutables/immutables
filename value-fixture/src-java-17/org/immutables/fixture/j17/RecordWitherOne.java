package org.immutables.fixture.j17;

import java.util.List;
import org.immutables.builder.Builder;
import org.immutables.value.Value;

@Value.Style(withUnaryOperator = "with*")
@Builder
public record RecordWitherOne(int aa, String bb, List<Integer> ints) implements WithRecordWitherOne {}
