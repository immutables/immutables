package org.immutables.builder.fixture.telescopic;

import java.util.Arrays;
import java.util.List;
import org.immutables.builder.Builder;
import org.immutables.value.Value;

@Value.Style(
    newBuilder = "newBuilder",
    stagedBuilder = true
)
public class SamplePojo<T> {
  protected final List<String> fields;
  final List<String> identifiers;
  final T payload;

  @Builder.Constructor
  protected SamplePojo(String service, String data, T payload, List<String> identifiers) {
    this.identifiers = identifiers;
    this.fields = Arrays.asList(service, data);
    this.payload = payload;
  }
}
