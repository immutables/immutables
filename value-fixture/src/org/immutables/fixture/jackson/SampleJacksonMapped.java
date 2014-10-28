package org.immutables.fixture.jackson;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.List;
import org.immutables.value.Jackson;
import org.immutables.value.Json;
import org.immutables.value.Value;

@Value.Immutable
@Json.Marshaled
@Jackson.Mapped
@JsonDeserialize(as = ImmutableSampleJacksonMapped.class)
public interface SampleJacksonMapped {
  String a();

  List<Integer> b();
}
