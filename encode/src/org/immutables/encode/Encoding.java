package org.immutables.encode;

import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Retention;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Encoding defines set of template methods and fields which describes how type is embedded.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface Encoding {

  /**
   * Implementation field must be annotated, it also must be private;
   */
  @Target(ElementType.FIELD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Impl {}

  /**
   * Many elements (but not all) supports customized naming patterns.
   * Like "with*Added" for copy with addition method.
   */
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Naming {
    String value();
  }

  /**
   * Expose describes how type matches to the encoding via return type.
   * It also provides implementation to the accessor. There can zero or more
   * expose templates. If none provided, it will be automatically derived as returning
   * implementation field. Couple of expose methods might be usefull when you.
   * Remember, only one such method can be activated by encoding matching.
   * It also should should match the init and builder init-copy types.
   */
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Expose {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Init {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Copy {}

  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Builder {}

  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Build {}
}
