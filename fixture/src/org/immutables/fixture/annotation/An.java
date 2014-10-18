package org.immutables.fixture.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.immutables.value.Value;

@Retention(RetentionPolicy.RUNTIME)
@Value.Immutable
@interface An {
  @Value.Parameter
  int value();

  Be[] bees() default { @Be, @Be };
}

@Retention(RetentionPolicy.SOURCE)
@Value.Immutable
@interface Be {
  Class<? extends Number>[] cl() default { Integer.class };
}
