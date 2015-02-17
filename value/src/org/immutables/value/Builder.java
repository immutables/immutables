package org.immutables.value;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;
import java.lang.annotation.Target;

/**
 * <em>
 * This umbrella annotaion does nothing, it is used as {@literal @}{@code Builder.Factory}
 * and is used for static factory methods to generate arbitrary builders.
 * Immutable values as {@link Immutable Value.Immutable} generate builder by default, unless
 * turned off using {@literal @}{@link Immutable#builder() Value.Immutable(builder=false)}</em>
 * @see Factory
 */
//@Target({})
public @interface Builder {
  /**
   * Annotate static factory methods that produce some value (non-void, non-private) to create
   * builder out of constructor parameters.
   * 
   * <pre>
   * class Sum {
   *   {@literal @}Builder.Factory
   *   static Integer from(int a, int b) {
   *      return a + b;
   *   }
   * }
   * ... // generates builder
   * Integer result = new SumBuilder()
   *    .a(111)
   *    .b(222)
   *    .build();
   * </pre>
   * <p>
   * Class level and package level style annotations fully supported (see {@link Style}).
   */
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target(ElementType.METHOD)
  public @interface Factory {}

//  @Documented
//  @Retention(RetentionPolicy.SOURCE)
//  @Target({ElementType.PARAMETER})
//  public @interface Factory {}
}
