package org.immutables.encode;

import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Retention;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Encoding defines
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface Encoding {

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Init {
    boolean builder() default true;

    boolean wither() default true;
  }

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Naming {
    String value();
  }

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Derive {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Expose {}

  @Target(ElementType.FIELD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Impl {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Copy {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Stringify {}
}
