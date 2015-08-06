package org.immutables.fixture.jackson;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@JsonSerialize
@Retention(RetentionPolicy.CLASS)
public @interface MetaJacksonAnnotation {}

@MetaJacksonAnnotation
@Value.Immutable
interface Val {
  // compile check for presense of
  // the Jackson specific creator method
  // triggered by meta annotation
  default void use() {
    ImmutableVal.fromJson(null);
  }
}
