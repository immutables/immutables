package org.immutables.fixture.deep;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.immutables.value.Value;

@Target({ElementType.PACKAGE, ElementType.TYPE})
@Retention(RetentionPolicy.CLASS)
@Value.Style(
    deepImmutablesDetection = true,
    jdkOnly = true)
@interface DeepStyle {}

public interface DeepDetectionBroken {

  @DeepStyle
  @Value.Immutable
  interface Deep {
    @Value.Parameter
    int a();

    @Value.Parameter
    int b();
  }

  @DeepStyle
  @Value.Immutable
  interface Container {
    Deep getDeep();
  }

  static void use() {
    ImmutableContainer c = ImmutableContainer.builder().build();
    ImmutableDeep deep = c.getDeep();
    deep.withA(1);
  }
}
