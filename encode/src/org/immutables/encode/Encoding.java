/*
   Copyright 2016 Immutables Authors and Contributors

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.immutables.encode;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
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

  /**
   * Builder init method template. Can be as simple as {@code .setValue(1)} to {@code .addVal},
   * {@code .addAll}, {@code .putAddAllWhatever} variations. Many such initializers may be
   * specified, so they will appear for each attribute handled by encoding.
   * <p>
   * Can be annotated with {@link Copy} annotation to specify that it is capable of accepting full
   * value returned by getter of the same attribute.
   */
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Init {}

  /**
   * With copy method template. Return type should be the type of implementation field. It can have
   * arbitrary parameters so you can describe update-copy to
   * persistent(structural-sharing-copy) collections. The return value of this method will be then
   * compared (in generated code) to the current field value using {@code ==} comparison and if
   * found equals (in terms of {@code ==}), then {@code this} will be returned from generated copy
   * routine to short circuit copyings of the whole object if modification would yield equivalent
   * object. This method would preferably be inlined if one-liner and is not reused in
   * other methods. Naming could be supplied.
   * <p>
   * The second use of this annotation is to have it along {@link Init} annotation on a builder
   * initializer method to stress that this initialized can copy the value as we can get from
   * exposed assessor. In other words, this builder setter annotated {@code @Init @Copy} can be used
   * to accept the value returned from getter.
   */
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Copy {}

  /**
   * Attribute's builder template. It should be nested static class with the same type parameters as
   * enclosing encoding
   */
  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Builder {}

  /**
   * Template method which describes how attribute should be built so it would can be assigned to.
   * No argumentet method with return type the same as implementation field's one ({@link Impl})
   * implementation field. This method would preferably be inlined if one-liner and is not reused in
   * other methods. No naming is appropriate as this method is always inlined or private helper
   * method
   */
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Build {}

  /**
   * Many elements (but not all) supports customized naming patterns.
   * Use single asterisk symbol in a name to denote a placeholder where attribute name would be
   * insterted.
   * We call namings of form {@code "*"} as identity naming, and the ones which have no placeholder,
   * as in {@code "set"} - constant namings.
   * Like "with*Added" for copy with addition method.
   * Elements that can have customized naming:
   * <ul>
   * <li>Copy/With methods</li>
   * <li>Helper fields and methods</li>
   * <li>Builder attribute initializer</li>
   * <li>Helper fields and methods on builder</li>
   * </ul>
   * <p>
   * As an alternative to specifying a pattern, you may want to reuse {@link #standard()} naming.
   * <p>
   * <em>
   * Please note, that with customized naming it is possible (but in general, not recommended) to put
   * constant naming (without {@code "*"} placeholder) on elements. But when you do this it can result
   * in name clashes in generated code.
   * </em>
   * @see #depluralize()
   * @see #standard()
   */
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Naming {
    /**
     * Naming pattern for element.
     * @return naming pattern
     */
    String value() default "*";

    /**
     * Set to {@code true} if depluralizer needs to kick in and
     * try to convert attribute name to a singular form before applying pattern ({@link #value()}).
     * Some standard namings imply depluralization ({@link StandardNaming#ADD},
     * {@link StandardNaming#PUT}).
     * @return if depluralized attribute name
     */
    boolean depluralize() default false;

    /**
     * Standard namings can be used to reuse standard styles, like the ones defined in
     * {@code Value.Immutable.Style} for the custom-defined elements in encoding. If
     * {@link StandardNaming} is used with the generator other than Immutables which do not have
     * notion of namings/styles, then, typical default values will be used.
     */
    StandardNaming standard() default StandardNaming.NONE;
  }

  /**
   * Standard namings can be used to reuse standard styles, like the ones defined in
   * {@code Value.Immutable.Style} for the custom-defined elements in encoding. If
   * {@link StandardNaming} is used with the generator other than Immutables which do not have
   * notion of namings/styles, then, typical default values will be used.
   */
  public enum StandardNaming {
    /** no standard naming is used. */
    NONE(""),
    /**
     * naming of the accessor method. as accessors are detected, this naming would signalize the
     * need to copy current detected accessors naming to some other element.
     */
    GET("*"),

    /** builder init method. */
    INIT("*"),

    /** with copy method. */
    WITH("with*"),

    /** builder add method, depluralized. */
    ADD("add*", true),

    /** builder add all method. */
    ADD_ALL("addAll*"),

    /** builder put method, depluralized. */
    PUT("put*", true),

    /** builder put all method. */
    PUT_ALL("putAll*"),

    /** builder is set method. */
    IS_SET("*IsSet"),

    /** Auxiliary set method. */
    SET("set*"),

    /** Auxiliary unset method. */
    UNSET("unset*");

    public final String pattern;
    public final boolean depluralize;

    StandardNaming(String pattern) {
      this(pattern, false);
    }

    StandardNaming(String pattern, boolean depluralize) {
      this.pattern = pattern;
      this.depluralize = depluralize;
    }
  }
}
