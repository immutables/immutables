package simple;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

// Placed here to not fall into org.immutables package
// (our annotation are skipped during copying)
@Retention(RetentionPolicy.RUNTIME)
public @interface GetterAnnotation {
  RetentionPolicy policy();

  Class<?> type();

  String string();

  InnerAnnotation[] value() default {};

  @Retention(RetentionPolicy.RUNTIME)
  public @interface InnerAnnotation {}
}
