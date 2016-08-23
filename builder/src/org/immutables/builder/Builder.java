/*
   Copyright 2015 Immutables Authors and Contributors

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
package org.immutables.builder;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Style;

/**
 * This umbrella annotaion does nothing. Use nested annotations, such as {@literal @}
 * {@code Builder.Factory} to generate builders for arbitrary static factory methods.
 * and is used for static factory methods to generate arbitrary builders.
 * Immutable values as {@link Immutable Value.Immutable} generate builder by default, unless
 * turned off using {@literal @}{@link Immutable#builder() Value.Immutable(builder=false)}
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
  @Target(ElementType.METHOD)
  public @interface Factory {}

  /**
   * The same as {@link Factory}, but for constructors rather than static methods.
   * 
   * <pre>
   * class Sum {
   *   {@literal @}Builder.Constructor
   *    Sum(int a, int b) {
   *      return a + b;
   *   }
   * }
   * ... // use generated builder
   * Sum sum = new SumBuilder()
   *    .a(111)
   *    .b(222)
   *    .build();
   * </pre>
   * <p>
   * Class level and package level style annotations fully supported (see {@link Style}).
   */
  @Documented
  @Target(ElementType.CONSTRUCTOR)
  public @interface Constructor {}

  /**
   * Factory method parameter might be turned into builder parameter using this annotation.
   * 
   * <pre>
   * class NodeFactory {
   *   {@literal @}Builder.Factory
   *   static Node node({@literal @}Builder.Parameter Object value, Optional&lt;Node&gt; left, Optional&lt;Node&gt; right) {
   *      return ...
   *   }
   * }
   * ... // notice the constructor parameter generated
   * Integer result = new NodeBuilder(new Object())
   *    .left(node1)
   *    .right(node2)
   *    .build();
   * </pre>
   * <p>
   * Also note that with some limitation this annotation works on value type attribute to generate
   * builder parameter.
   */
  @Documented
  @Target({ElementType.PARAMETER, ElementType.METHOD})
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
   * If proper {@link #defaultName()} value is specified, then one of the state will be considered
   * the default. If no default is specified then it is mandatory to call switcher method once. If
   * default is specified then it switcher method call could be omitted.
   * 
   * <pre>
   * class BulbFactory {
   *   enum Switcher {
   *     OFF, ON
   *   }
   *   {@literal @}Builder.Factory
   *   static Bulb bulb({@literal @}Builder.Switch(defaultName = "OFF") Switcher light) {
   *      return ...
   *   }
   * }
   * ... // notice no 'offLight' generated
   * Bulb b = new BulbBuilder() // default is Switcher.OFF
   *    .onLight() // but we can switch to Switcher.ON
   *    .build();
   * </pre>
   * <p>
   * Also note that with some limitation this annotation works on value type attribute to generate
   * switch option.
   */
  @Documented
  @Target({ElementType.PARAMETER, ElementType.METHOD})
  public @interface Switch {
    /**
     * Specify constant name of default enum value for this switch. The name should match constant
     * identifier name. If empty of none specified, then switch will be mandatory to set on builder.
     * @return name enum constant
     */
    String defaultName() default "";
  }
}
