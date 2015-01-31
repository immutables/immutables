package org.immutables.value.ext;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.immutables.value.Value.Immutable;

public @interface MoreValue {
  /**
   * {@code Modifiable} is a companion annotation to {@link Immutable}. It instructs annotation
   * processor to generate modifiable companion class, which may be thought of as less
   * constrained builders. Modifiable objects conforms to the abstract value type, but not
   * necessarily could be used interchangably with immutable instances.
   * <p>
   * Please, note, that generated modifiable companion types will not be completely JavaBean-POJO
   * compatible (if one can define what it is). Here's the list of specific things about
   * {@code Modifiable} objects:
   * <ul>
   * <li>Modifiable objects are not thread safe, unlike canonical immutable instances</li>
   * <li>It has identity, but doesn't have value in terms of how equals and hashCode is implemented.
   * Convert to canonical immutable instance ({@code toImmutable*()}) to have a value. Overall,
   * using value equality for mutable objects is almost always a bad practive.</li>
   * <li>Runtime exception will be throws when trying to access mandatory attribute that has not
   * been set. Special accessors ({@code hasSet*}) could be used to find out whether attribute have
   * been set or not.</li>
   * <li>Special collection attributes are implemented as mutable collections which may be accessed
   * using getters, but cannot be replaced. Values could be changed, cleared, but there's no setters
   * for special collection attributes.</li>
   * <li>{@code Value.Derived}, {@code Value.Default} (if not set) and {@code Value.Lazy} attributes
   * will be recomputed on each access. Use immutable instances to properly handle those.</li>
   * </ul>
   * <p>
   * Among other limitations is that {@code Modifiable} types are generated as top level classes in
   * package and ignores special nesting provided by {@code Value.Nested} annotation.
   */
  @Documented
  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Modifiable {}

  public @interface Style {

    /**
     * "Deep analisys" enables more detailed analysis of attribute types which allows to generate
     * more convenient initializers. Impact on performance of this option turned on is not yet
     * investigated.
     * @return {@code true} if deep analysis is enabled
     */
    boolean deepAnalysis() default true;
  }

  // / Future styles
  /*
   * Modifiable companion class name template
   * @return naming template
   */
  // String typeModifiable() default "Modifiable*";

  /*
   * Modifiable object "setter" method. Used for mutable implementations.
   * @return naming template
   */
  // String set() default "set*";

  /*
   * @return naming template
   */
  // String isSet() default "*IsSet";

  /*
   * Factory method for modifiable (mutable) implementation
   * @return naming template
   */
  // String create() default "create";

  /*
   * Method to convert to instance of companion modifiable type to "canonical" immutable instance.
   * @return naming template
   */
  // String toImmutable() default "toImmutable*";

  /*
   * Modifiable companion class name template
   * @return naming template
   */
  // String typeTransformer() default "*Transformer";

  /*
   * Modifiable companion class name template
   * @return naming template
   */
  // String typeVisitor() default "*Visitor";

  /*
   * Unset attribute method. Used for mutable implementations.
   * @return naming template
   */
  // String unset() default "unset*";

  /*
   * Clear collection (or other container). Used for mutable implementations.
   * @return naming template
   */
  // String clear() default "clear*";
}
