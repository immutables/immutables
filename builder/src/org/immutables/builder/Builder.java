package org.immutables.builder;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;

/**
 * <em>
 * This umbrella annotaion does nothing. Use nested annotations, such as {@literal @}{@code Builder.Factory}
 * to generate builders for arbitrary static factory methods.
 * and is used for static factory methods to generate arbitrary builders.
 * Immutable values as {@link Immutable Value.Immutable} generate builder by default, unless
 * turned off using {@literal @}{@link Immutable#builder() Value.Immutable(builder=false)}</em>
 * @see Factory
 */
@Target({})
public @interface Builder {
  /**
   * Annotate static factory methods that produce some value (non-void, non-private) to create
   * builder out of constructor parameters.
   * 
   * <pre>
   * class Sum {
   *   {@literal @}Builder.Factory
   *   static Integer sum(int a, int b) {
   *      return a + b;
   *   }
   * }
   * ... // use generated builder
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

  /**
   * Factory method parameter might be turned into builder parameter using this annotation.
   * 
   * <pre>
   * class NodeFactory {
   *   {@literal @}Builder.Factory
   *   static Node node({@literal @}Builder.Parameter Object value, Optional<Node> left, Optional<Node> right) {
   *      return ...
   *   }
   * }
   * ... // notice the constructor parameter generated
   * Integer result = new NodeBuilder(new Object())
   *    .left(node1)
   *    .right(node2)
   *    .build();
   * </pre>
   */
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target(ElementType.PARAMETER)
  public @interface Parameter {}

  /**
   * Applicable only to enum parameters, this annotation turns parameters into switcher methods on
   * builder. Each switcher method applies corresponding constant value. Switch methods are named
   * after parameter name prefixed with properly cased (case transformed) enum constant name.
   * 
   * <pre>
   * class BulbFactory {
   *   enum Switcher {
   *     OFF, ON
   *   }
   *   {@literal @}Builder.Factory
   *   static Bulb bulb({@literal @}Builder.Switch Switcher light) {
   *      return ...
   *   }
   * }
   * ... // notice the switcher methods instead of enum initializer
   * Bulb b = new BulbBuilder()
   *    .onLight() // set to Switcher.ON
   *    .offLight() // set to Switcher.OFF
   *    .build();
   * </pre>
   * <p>
   * If proper {@link #defaultOrdinal()} value is specified, then one of the state will be
   * considered the default. If no default is specified then it is mandatory to call switcher method
   * once, otherwise switcher method may be omitted.
   * 
   * <pre>
   * class BulbFactory {
   *   enum Switcher {
   *     OFF, ON
   *   }
   *   {@literal @}Builder.Factory
   *   static Bulb bulb({@literal @}Builder.Switch(defaultOrdinal = 0) Switcher light) {
   *      return ...
   *   }
   * }
   * ... // notice no 'onLight' generated
   * Bulb b = new BulbBuilder() // default is Switcher.OFF
   *    .onLight() // set to Switcher.ON
   *    .build();
   * </pre>
   */
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target(ElementType.PARAMETER)
  public @interface Switch {
    int defaultOrdinal() default -1;
  }
}
