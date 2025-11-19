package org.immutables.fixture.j17;

import org.immutables.value.Value;

@Value.Builder
@Value.Style(jdk9Collections = true, jdkOnly = true, stagedBuilder = true)
public record PersonRecord<T extends Comparable<T>>(int id, String displayName, T payload)
    implements Person, WithPersonRecord<T> {
  static void use() {
    PersonRecordBuilderStages.<String>start()
        .id(1)
        .displayName("Joe")
        .payload("Load")
        .build()
        .withId(2)
        .withDisplayName("Doe");
  }
}
