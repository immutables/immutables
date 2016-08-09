package org.immutables.encode;

import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Retention;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Encoding defines set of template methods and fields which describes how attributes of specific
 * types are implemented in generated value class. {@code Encoding} annotation is processed by the
 * annotation processor (the same as used for value objects) and generates annotation named
 * {@code *Enabled} in the same package, inserting encoding simple class name in placeholder.
 * Encoding class consists of special fields methods, builder inner static class with it's own
 * fields and methods.
 * <p>
 * When programming the encoding class, remember that code analyser is not akin full-fledged java
 * compiler, but a set of simplified routines which cannot possibly
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
   * Use single asterisk symbol in a name to denote a placeholder where attribute name would be
   * insterted.
   * We call namings of form {@code "*"} as identity naming, and the ones which have no placeholder,
   * as in {@code "set"} - constant namings.
   * Like "with*Added" for copy with addition method.
   * Elements that can have customized naming:
   * <ul>
   * <li>Helper fields and methods</li>
   * </ul>
   * <p>
   * <em>
   * Please note, that with customized naming it is possible (but in general, not recommended) to put
   * constant naming (without {@code "*"} placeholder) on elements. But when you do this it can result
   * in name clashes in generated code.
   * </em>
   * @see #depluralize()
   */
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Naming {
    /**
     * Naming pattern for element.
     * @return naming pattern
     */
    String value();

    /**
     * Set to {@code true} if depluralizer needs to kick in and
     * try to convert attribute name to a singular form before applying pattern ({@link #value()}).
     * @return if depluralized attribute name
     */
    boolean depluralize() default false;
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
