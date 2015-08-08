package org.immutables.fixture.jackson;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.immutables.value.Value;

@JsonSerialize
@Retention(RetentionPolicy.CLASS)
public @interface MetaJacksonAnnotation {}

@MetaJacksonAnnotation
@Value.Immutable
interface Val {
  // compile check for presense of
  // the Jackson specific creator method
  // triggered by meta annotation
  @SuppressWarnings("deprecation")
  default void use() {
    ImmutableVal.fromJson(null);
  }
}
