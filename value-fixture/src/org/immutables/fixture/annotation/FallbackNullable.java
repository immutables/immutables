package org.immutables.fixture.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Optional;
import org.immutables.value.Value;

@Retention(RetentionPolicy.RUNTIME)
@interface Nil {}

@Value.Immutable
@Value.Style(optionalAcceptNullable = true, fallbackNullableAnnotation = Nil.class, nullableAnnotation = "Nil")
public interface FallbackNullable {
  Optional<String> jdkOptional();
  @Nil String nullable();
}
