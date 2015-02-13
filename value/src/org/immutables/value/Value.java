/*
    Copyright 2014-2015 Immutables Authors and Contributors

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
package org.immutables.value;

import java.util.Collections;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.NavigableMap;
import java.util.NavigableSet;
import java.util.SortedMap;
import java.util.SortedSet;

/**
 * This annotation provides namespace for annotations that models generated value objects.
 * Use one of the nested annotation.
 * @see Value.Immutable
 * @see Value.Include
 * @see Value.Nested
 */
// @Target({}) // may cause problems with auto completion
@Retention(RetentionPolicy.SOURCE)
public @interface Value {
  /**
   * Instruct processor to generate immutable implementation of abstract value type.
   * Classes, Interface and Annotation types are supported including top level and non-private
   * static inner declaration.
   * <p>
   * <em>Be warned that such immutable object may contain attributes that are not recursively immutable, thus
   * not every object will be completely immutable. While this may be useful for some workarounds,
   * one should generally avoid creating immutable object with attribute values that could be mutated</em>
   * <p>
   * Generated accessor methods have annotation copied from original accessor method. However
   * {@code org.immutables.*} and {@code java.lang.*} are not copied.
   */
  @Documented
  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Immutable {

    /**
     * If {@code singleton=true}, generates internal singleton object constructed without any
     * specified parameters. Default is {@literal false}.
     * <p>
     * Note that {@code singleton=true} does not imply that only one instance of given abstract
     * type. But it does mean that only one "default" instance of the immutable implementation type
     * exist.
     */
    boolean singleton() default false;

    /**
     * If {@code intern=true} then instances will be strong interned on construction.
     * Default is {@literal false}.
     */
    boolean intern() default false;

    /**
     * If {@code copy=false} then generation of copying methods will be disabled.
     * This appies to static "copyOf" methods as well as modify-by-copy "withAttributeName" methods
     * which returns modified copy using structural sharing where possible.
     * Default value is {@literal true}, i.e generate copy methods.
     */
    boolean copy() default true;

    /**
     * If {@code prehash=true} then {@code hashCode} will be precomputed during construction.
     * This could speed up collection lookups for objects with lots of attributes and nested
     * objects.
     * In general, use this when {@code hashCode} computation is expensive and will be used a lot.
     */
    boolean prehash() default false;

    /**
     * If {@code builder=false}, disables generation of {@code builder()}. Default is
     * {@literal true}.
     */
    boolean builder() default true;
  }

  /**
   * Includes specified abstract value types into generation of processing.
   * This is usually used to generate immutable implementation of classes from different
   * packages that source code cannot be changed to place {@literal @}{@code Value.Immutable}.
   * Only public types of suppored kinds is supported (see {@link Value.Immutable}).
   */
  @Documented
  @Target({ElementType.TYPE, ElementType.PACKAGE})
  @Retention(RetentionPolicy.SOURCE)
  public @interface Include {
    Class<?>[] value();
  }

  /**
   * This annotation could be applied to top level class which contains nested abstract value types.
   * Immutable implementation classes will be generated as classes nested into special "umbrella"
   * top
   * level class, essentialy named after annotated class with "Immutable" prefix. This could mix
   * with {@link Value.Immutable} annotation, so immutable implementation class will contains
   * nested immutable implementation classes.
   * <p>
   * Implementation classes nested under top level class with "Immutable" prefix
   * <ul>
   * <li>Have simple names without "Immutable" prefix
   * <li>Could even be star-imported for easy clutter-free usage.
   * </ul>
   * <p>
   * 
   * <pre>
   * {@literal @}Value.Nested
   * class GraphPrimitives {
   *   {@literal @}Value.Immutable
   *   interace Vertex {}
   *   {@literal @}Value.Immutable
   *   static class Edge {}
   * }
   * ...
   * import ...ImmutableGraphPrimitives.*;
   * ...
   * Edge.builder().build();
   * Vertex.builder().build();
   * </pre>
   */
  @Documented
  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Nested {}

  /**
   * Annotate static factory methods that produce some value (non-void, non-private) to create
   * builder out of constructor parameters.
   * 
   * <pre>
   * class Sum {
   *   {@literal @}Value.Builder
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
   * <p>
   * <em>
   * This annotation is for static factory methods to generate arbitrary builders. It's not for
   * immutable values as {@link Immutable Value.Immutable} generate builder by default, unless
   * turned off using {@literal @}{@link Immutable#builder() Value.Immutable(builder=false)}</em>
   */
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target(ElementType.METHOD)
  public @interface Builder {}

  /**
   * This kind of attribute cannot be set during building, but they are eagerly computed from other
   * attributes and stored in field. Should be applied to non-abstract method - attribute value
   * initializer.
   */
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target(ElementType.METHOD)
  public @interface Derived {}

  /**
   * Annotates accessor that should be turned in set-able generated attribute. However, it is
   * non-mandatory to set it via builder. Default value will be assigned to attribute if none
   * supplied, this value will be obtained by calling method annotated this annotation.
   */
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target(ElementType.METHOD)
  public @interface Default {}

  /**
   * Annotate attribute as <em>auxiliary</em> and it will be stored and will be accessible, but will
   * be excluded from generated {@code equals}, {@code hashCode} and {@code toString} methods.
   * {@link Lazy Lazy} attributes are always <em>auxiliary</em>.
   * @see Value.Immutable
   * @see Value.Derived
   * @see Value.Default
   */
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target(ElementType.METHOD)
  public @interface Auxiliary {}

  /**
   * Lazy attributes cannot be set, defined as method that computes value, which is invoke lazily
   * once and only once in a thread safe manner.
   * 
   * <pre>
   * &#064;Value.Immutable
   * public abstract class Order {
   * 
   *   public abstract List&lt;Item&gt; items();
   * 
   *   &#064;Value.Lazy
   *   public int totalCost() {
   *     int cost = 0;
   * 
   *     for (Item i : items())
   *       cost += i.count() * i.price();
   * 
   *     return cost;
   *   }
   * }
   * </pre>
   * <p>
   * This kind of attribute cannot be set during building, but they are lazily computed from other
   * attributes and stored in non-final field, but initialization is guarded by synchronization with
   * volatile field check. Should be applied to non-abstract method - attribute value initializer.
   * <p>
   * In general, lazy attribute initializer is more safe than using {@link Derived} attributes, lazy
   * attribute's initializer method body can refer to abstract mandatory and container attributes as
   * well as to other lazy attributes. Though lazy attributes act as {@link Auxiliary}.
   */
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target(ElementType.METHOD)
  public @interface Lazy {}

  /**
   * Works with {@link Value.Immutable} classes to mark abstract accessor method be included as
   * "{@code of(..)}" constructor parameter.
   * <p>
   * Following rules applies:
   * <ul>
   * <li>No constructor generated, if none of methods have {@link Value.Parameter} annotation</li>
   * <li>For object to be constructible with a constructor - all non-default and non-derived
   * attributes should be annotated with {@link Value.Parameter}.
   * </ul>
   */
  @Documented
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Parameter {
    /**
     * Used to specify order of constructor argument. It's defaults to zero and allows for
     * non-contiguous order values (arguments are sorted ascending by this order value).
     * <p>
     * <em>This attribute was introduced as JDT annotation processor internally tracks alphabetical order
     * of members (non-standard as of Java 6), this differs from Javac, which uses order of declaration appearance
     * in a source file. Thus, in order to support portable constructor argument definitions,
     * developer should supply argument order explicitly. As of version 1.0, we implemented workaround for
     * the Eclipse compiler, but it's still might be needed if you wish to reorder arguments</em>
     * @return order
     */
    int order() default 0;
  }

  /**
   * Annotates method that should be invoked internally to validate invariants
   * after instance had been created, but before returned to a client.
   * Annotated method must be parameter-less (non-private) method and have a {@code void} return
   * type,
   * which also should not throw a checked exceptions.
   * <p>
   * Precondition checking should not be used to validate against context dependent business rules,
   * but to preserve consistency and guarantee that instances will be usable. Precondition check
   * methods runs when immutable object <em>instantiated and all attributes are initialized</em>,
   * but <em>before returned to caller</em>. Any instance that failed precondition check is
   * unreachable to caller due to runtime exception.
   */
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target(ElementType.METHOD)
  public @interface Check {}

  /**
   * Specified natural ordering for the implemented {@link SortedSet}, {@link NavigableSet} or
   * {@link SortedMap}, {@link NavigableMap}. It an error to annotate
   * sorted collection of elements which are not implementing {@link Comparable}.
   * Non-annotated special collection will be
   * generated/implemented as "nothing-special" attributes.
   * @see ReverseOrder
   */
  @Documented
  @Target({ElementType.METHOD, ElementType.PARAMETER})
  @Retention(RetentionPolicy.SOURCE)
  public @interface NaturalOrder {}

  /**
   * Specified reversed natural ordering for the implemented {@link SortedSet}, {@link NavigableSet}
   * or {@link SortedMap}, {@link NavigableMap}. It an error to annotate
   * sorted collection of elements which are not implementing {@link Comparable}.
   * Non-annotated special collection will be
   * generated/implemented as "nothing-special" attributes.
   * @see Collections#reverseOrder()
   * @see NaturalOrder
   */
  @Documented
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface ReverseOrder {}

  /**
   * Naming and structural style could be used to customize convention of the generated
   * immutable implementations and companion classes. It could be placed on a class or package
   * directly or serve as meta annotation.
   * <p>
   * <em>
   * Be careful to not use keywords or inappropriate characters as parts of naming templates.
   * Some sneaky collisions may only manifest as compilation errors in generated code.</em>
   * <p>
   * <em>Specific styles will be ignored for a immutable type enclosed with class which is annotated
   * as {@literal @}{@link Value.Nested}. So define styles on the enclosing class.
   * In this way there will be no issues with the naming and structural conventions
   * mismatch on enclosing and nested types.</em>
   */
  @Target({ElementType.TYPE, ElementType.PACKAGE, ElementType.ANNOTATION_TYPE})
  @Retention(RetentionPolicy.RUNTIME)
  public @interface Style {
    /**
     * Patterns to recognize accessors. For example <code>get = {"is*", "get*"}</code> will
     * mimick style of bean getters.
     * @return naming template
     */
    String[] get() default {};

    /**
     * Builder initialization method. i.e. "setter" in builder.
     * @return naming template
     */
    String init() default "";

    /**
     * Modify-by-copy "with" method.
     * @return naming template
     */
    String with() default "with*";

    /**
     * Add value to collection attribute from iterable
     * @return naming template
     */
    String add() default "add*";

    /**
     * Add all values to collection attribute from iterable
     * @return naming template
     */
    String addAll() default "addAll*";

    /**
     * Puts entry to a map attribute
     * @return naming template
     */
    String put() default "put*";

    /**
     * Puts all entries to a map attribute
     * @return naming template
     */
    String putAll() default "putAll*";

    /**
     * Copy constructor method name.
     * @return naming template
     */
    String copyOf() default "copyOf";

    /**
     * Constructor method name.
     * @return naming template
     */
    String of() default "of";

    /**
     * Singleton accessor method name
     * @return naming template
     */
    String instance() default "of";

    /**
     * Builder creator method. This naming allow special keyword "new" value.
     * This will customize builder to be created using parameterless constructor rather than
     * factory methods.
     * @return naming template
     */
    String builder() default "builder";

    /**
     * Instance creation method on builder.
     * @return naming template
     */
    String build() default "build";

    /**
     * Builder class name.
     * @return naming template
     */
    String typeBuilder() default "Builder";

    /**
     * Naming templates to detect base/raw type name from provided abstract value type name.
     * @return naming templates
     */
    String[] typeAbstract() default {};

    /**
     * Name template to generate immutable implementation type by using base/raw type name.
     * Use {@link #typeAbstract()} to customize base/raw name inference.
     * @return naming template
     */
    String typeImmutable() default "Immutable*";

    /**
     * Umbrella nesting class name generated using {@link Nested}.
     * @return naming template
     */
    String typeImmutableEnclosing() default "Immutable*";

    /**
     * Immutable class name when generated under umbrella class using {@link Nested} annotation.
     * @see #typeImmutable()
     * @see #typeImmutableEnclosing()
     * @return naming template
     */
    String typeImmutableNested() default "*";

    /**
     * Specify default options for the generated immutable objects.
     * If at least one attribute is specifid in inline {@literal @}{@link Immutable} annotation,
     * then this default will not be taken into account, objects will be generated using attributes
     * from inline annotation.
     * @return default configuration
     */
    Immutable defaults() default @Immutable;

    /**
     * When {@code true} &mdash; forces to generate code which use only JDK 7+ standard library
     * classes. It is {@code false} by default, however usage of JDK-only classes will be turned on
     * automatically if <em>Google Guava</em> library is not found in classpath. The generated code
     * will have subtle differences, but nevertheless will be functionally equivalent.
     * <p>
     * <em>Note that some additional annotation processors may not work without
     * Guava being accessible to the generated classes, and thus will not honor this attribute</em>
     * @return if forced JDK-only class usage
     */
    boolean jdkOnly() default false;

    /**
     * Specify the mode in which accibility visibility is derived from abstract value type.
     * It is a good idea to not specify such attributea inline with immutable values, but rather
     * create style annotation (@see Style).
     * @return implementation visibility
     */
    ImplementationVisibility visibility() default ImplementationVisibility.SAME;

    /**
     * If implementation visibility is more restrictive than visibility of abstract value type, then
     * implementation type will not be exposed as a return type of {@code build()} or {@code of()}
     * constructon methods. Builder visibility will follow.
     */
    public enum ImplementationVisibility {
      /**
       * Generated implementation class forced to be public.
       */
      PUBLIC,
      /**
       * Visibility is the same
       */
      SAME,

      /**
       * Visibility is the same, but it is not returned from build and factory method, instead
       * abstract value type returned.
       */
      SAME_NON_RETURNED,
      /**
       * 
       */
      PACKAGE,
      /**
       * Allowed only when builder is enabled or nested inside enclosing type.
       * Builder visibility will follow the umbrella class visibility.
       */
      PRIVATE
    }
  }
}
