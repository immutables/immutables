package org.immutables.modeling;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Retention;
import java.lang.annotation.Documented;

/**
 * Type should implement {@code Facet} if it's mixable into
 * @param <E> wrapped element type
 */
public interface Facet<E> {

  @Documented
  @Target({ ElementType.METHOD, ElementType.TYPE })
  @Retention(RetentionPolicy.SOURCE)
  public @interface Memoised {}
}
