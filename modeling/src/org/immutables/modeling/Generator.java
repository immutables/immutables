package org.immutables.modeling;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Namespasing annotation used to group nested Generator - related annotations.
 */
@Retention(RetentionPolicy.SOURCE)
public @interface Generator {

  @Inherited
  @Target({ ElementType.TYPE, ElementType.PACKAGE })
  public @interface Import {
    Class<?>[] value();
  }

  @Target(ElementType.FIELD)
  public @interface Typedef {}

  @Documented
  @Target(ElementType.TYPE)
  public @interface Template {}

  @Documented
  @Target({ ElementType.METHOD, ElementType.TYPE })
  @Retention(RetentionPolicy.SOURCE)
  public @interface Memoised {}
}
