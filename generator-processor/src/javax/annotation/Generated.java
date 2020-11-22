package javax.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Stub for cross compilation (java8/java9+). */
@Retention(RetentionPolicy.SOURCE)
public @interface Generated {
  String[] value();
}
