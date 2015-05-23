package org.immutables.fixture.jackson;

import org.immutables.value.Value;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Retention;

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
    ImmutableVal.fromAllAttributes();
  }
}
