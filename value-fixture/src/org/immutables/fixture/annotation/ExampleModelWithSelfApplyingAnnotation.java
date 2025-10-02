package org.immutables.fixture.annotation;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import nonimmutables.SelfApplyingAnnotation;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableExampleModelWithSelfApplyingAnnotation.class)
@JsonDeserialize(as = ImmutableExampleModelWithSelfApplyingAnnotation.class)
public interface ExampleModelWithSelfApplyingAnnotation {
  @SelfApplyingAnnotation
  String attributeWithSelfApplyingAnnotations();
}
