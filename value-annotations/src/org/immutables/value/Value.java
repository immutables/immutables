/*
   Copyright 2014-2025 Immutables Authors and Contributors

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

import java.io.Serializable;
import java.lang.annotation.*;
import java.lang.ref.WeakReference;
import java.util.*;

/**
 * This annotation provides namespace for annotations for immutable value object generation.
 * Use one of the nested annotation.
 * @see Value.Immutable
 * @see Value.Include
 * @see Value.Enclosing
 */
public @interface Value {
  /**
   * Instruct processor to generate immutable implementation of abstract value type.
   * Classes, Interface and Annotation types are supported including top level and non-private
   * static inner types.
   * <p>
   * Annotation has attributes to configure generation of immutable implementation classes, which
   * are usually configured per-type: should the builder ({@link #builder()}) be generated or
   * instances interned ({@link #intern()}). You can use {@link Style} or custom style annotation to
   * tune naming conventions and other settings of code-generation, along with default value for
   * per-type attributes ({@link Style#defaults()})
   * <p>
   * Generated accessor methods have annotation copied from original accessor method. However
   * {@code org.immutables.*} and {@code java.lang.*} are not copied.
   * <em>Be warned that such immutable object may contain attributes with types that are not
   * guaranteed to be immutable, thus not every object will be recursively immutable.
   * While this may be useful in some cases,
   * one should generally avoid creating immutable object with attribute values that could be
   * mutated.</em>
   * <p>
   * @see Style
   * @see Include
   */
  @Documented
  @Target(ElementType.TYPE)
  @interface Immutable {

    /**
     * If {@code singleton=true}, generates internal singleton object constructed without any
     * specified parameters. Default is {@literal false}. To access singleton instance use
     * {@code .of()} static accessor method.
     * <p>
     * This requires that all attributes have default value (including collections which can be left
     * empty). If some required attributes exist it will result in compilation error. Note that in
     * case object does not have attributes, singleton instance will be generated automatically.
     * <p>
     * Note that {@code singleton=true} does not imply that only one instance of given abstract
     * type. But it does mean that only one "default" instance of the immutable implementation type
     * exist.
     * @return if generate singleton default instance
     */
    boolean singleton() default false;

    /**
     * If {@code intern=true} then instances will be strong interned on construction.
     * Default is {@literal false}.
     * @return if generate strongly interned instances
     */
    boolean intern() default false;

    /**
     * If {@code copy=false} then generation of copying methods will be disabled.
     * This applies to static "copyOf" methods as well as modify-by-copy "withAttributeName" methods
     * which return modified copy using structural sharing where possible.
     * Default value is {@literal true}, i.e. generate copy methods.
     * @return if generate copy methods
     */
    boolean copy() default true;

    /**
     * If {@code prehash=true} then {@code hashCode} will be precomputed during construction.
     * This could speed up map/set lookups for objects with lots of attributes and nested
     * objects.
     * In general, use this when {@code hashCode} computation is expensive and will be used a lot.
     * Note that if {@link Style#privateNoargConstructor()} == <code>true</code> this option will be
     * ignored.
     * For lazy (deferred) {@code hashCode} computation use {@link #lazyhash()}
     * @return if generate hash code precomputing
     */
    boolean prehash() default false;

    /**
     * If {@code lazyhash=true} then internal {@code hashCode} will be computed (and cached) on
     * first {@code hashCode()}
     * method call.
     * For eager {@code hashCode} computation (in constructor) use {@link #prehash()}.
     * @return to lazily compute the {@code hashCode}
     */
    boolean lazyhash() default false;

    /**
     * If {@code builder=false}, disables generation of {@code builder()}. The default is
     * {@literal true} and builder is generated unless turned off.
     * @return if generate builder
     */
    boolean builder() default true;
  }

  /**
   * This annotation works on records to create builders for records.
   * Place this {@code Builder} annotation on record to generate builder from its
   * canonical constructor parameters (record components).
   * <pre>
   * &#064;Builder
   * record A(int b, String c) {}
   * </pre>
   * Immutable values as {@link Immutable Value.Immutable} generate builder by default, unless
   * turned off using {@literal @}{@link Immutable#builder() Value.Immutable(builder=false)},
   * so this annotation does nothing for {@code Value.Immutable} types.
   */
  @Target(ElementType.TYPE)
  @interface Builder {}

  /**
   * Includes specified abstract value types into generation of processing.
   * This is usually used to generate immutable implementation of classes from different
   * packages that source code cannot be changed to place {@literal @}{@code Value.Immutable}.
   * Only public types of suppored kinds is supported (see {@link Value.Immutable}).
   */
  @Documented
  @Target({ElementType.TYPE, ElementType.PACKAGE})
  @interface Include {
    Class<?>[] value();
  }

  /**
   * This annotation could be applied to top level class which contains nested abstract
   * value types to provide namespacing for the generated implementation classes.
   * Immutable implementation classes will be generated as classes enclosed into special "umbrella"
   * top level class, essentialy named after annotated class with "Immutable" prefix (prefix could
   * be customized using {@link Style#typeImmutableEnclosing()}). This could mix
   * with {@link Value.Immutable} annotation, so immutable implementation class will contain
   * nested immutable implementation classes.
   * <p>
   * Implementation classes nested under top level class with "Immutable" prefix
   * <ul>
   * <li>Have simple names without "Immutable" prefix
   * <li>Could be star-imported for easy clutter-free usage.
   * </ul>
   * <p>
   *
   * <pre>
   * {@literal @}Value.Enclosing
   * class GraphPrimitives {
   *   {@literal @}Value.Immutable
   *   interface Vertex {}
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
  @interface Enclosing {}

  /**
   * This kind of attribute cannot be set during building, but they are eagerly computed from other
   * attributes and stored in field. Should be applied to non-abstract method - attribute value
   * initializer.
   */
  @Documented
  @Target(ElementType.METHOD)
  @interface Derived {}

  /**
   * Annotates accessor that should be turned in set-able generated attribute. However, it is
   * non-mandatory to set it via builder. Default value will be assigned to attribute if none
   * supplied, this value will be obtained by calling method annotated this annotation.
   */
  @Documented
  @Target(ElementType.METHOD)
  @interface Default {
    /**
     * Annotate abstract accessor, record parameter or factory method parameter,
     * providing compile-time constant {@link java.lang.String} default value.
     * Must match parameter type / accessor method return type.
     */
    @Documented
    @Target({ElementType.PARAMETER, ElementType.METHOD})
    @interface String {
      java.lang.String value();
    }

    /**
     * Annotate abstract accessor, record parameter or factory method parameter,
     * providing compile-time constant {@code int} default value.
     * Must match parameter type / accessor method return type.
     */
    @Documented
    @Target({ElementType.PARAMETER, ElementType.METHOD})
    @interface Int {
      int value();
    }

    /**
     * Annotate abstract accessor, record parameter or factory method parameter,
     * providing compile-time constant {@code long} default value.
     * Must match parameter type / accessor method return type.
     */
    @Documented
    @Target({ElementType.PARAMETER, ElementType.METHOD})
    @interface Long {
      long value();
    }

    /**
     * Annotate abstract accessor, record parameter or factory method parameter,
     * providing compile-time constant {@code char} default value.
     * Must match parameter type / accessor method return type.
     */
    @Documented
    @Target({ElementType.PARAMETER, ElementType.METHOD})
    @interface Char {
      char value();
    }

    /**
     * Annotate abstract accessor, record parameter or factory method parameter,
     * providing compile-time constant {@code double} default value.
     * Must match parameter type / accessor method return type.
     */
    @Documented
    @Target({ElementType.PARAMETER, ElementType.METHOD})
    @interface Double {
      double value();
    }

    /**
     * Annotate abstract accessor, record parameter or factory method parameter,
     * providing compile-time constant {@code float} default value.
     * Must match parameter type / accessor method return type.
     */
    @Documented
    @Target({ElementType.PARAMETER, ElementType.METHOD})
    @interface Float {
      float value();
    }

    /**
     * Annotate abstract accessor, record parameter or factory method parameter,
     * providing compile-time constant {@code boolean} default value.
     * Must match parameter type / accessor method return type.
     */
    @Documented
    @Target({ElementType.PARAMETER, ElementType.METHOD})
    @interface Boolean {
      boolean value();
    }
  }

  /**
   * Annotate attribute as <em>auxiliary</em> and it will be stored and will be accessible, but will
   * be excluded from generated {@code equals}, {@code hashCode} and {@code toString} methods.
   * {@link Lazy Lazy} attributes are always <em>auxiliary</em>.
   * @see Value.Immutable
   * @see Value.Derived
   * @see Value.Default
   */
  @Documented
  @Target(ElementType.METHOD)
  @interface Auxiliary {}

  /**
   * Lazy attributes cannot be set, defined as method that computes value, which is invoked lazily
   * once and only once in a thread safe manner.
   *
   * <pre>
   * {@literal @}Value.Immutable
   * public abstract class Order {
   *
   *   public abstract List&lt;Item&gt; items();
   *
   *   {@literal @}Value.Lazy
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
  @Target(ElementType.METHOD)
  @interface Lazy {}

  /**
   * Works with {@link Value.Immutable} classes to mark abstract accessor method be included as
   * "{@code of(..)}" constructor parameter.
   * <p>
   * Following rules applies:
   * <ul>
   * <li>No constructor generated if none of methods have {@link Value.Parameter} annotation</li>
   * <li>For object to be constructable with a constructor - all non-default and non-derived
   * attributes should be annotated with {@link Value.Parameter}.
   * </ul>
   */
  @Documented
  @Target({ElementType.METHOD, ElementType.PARAMETER})
  @interface Parameter {
    /**
     * Used to specify order of constructor argument. It defaults to zero and allows for
     * non-contiguous order values (arguments are sorted ascending by this order value).
     * <p>
     * <em>This attribute was introduced as potentially not all annotation processors could use
     * source order of elements, i.e. order of declaration appearance in a source file.
     * To support portable constructor argument definitions,
     * developer should supply argument order explicitly.
     * As of version 1.0, we implemented workaround for
     * the Eclipse compiler, so it is not strictly needed to specify order,
     * but it still might be needed if you wish to reorder arguments</em>
     * <em>
     * Since 2.5.6 the default value was changed to -1 to signify unspecified order, but the logic
     * behind should not result in any practical incompatibilities.
     * </em>
     * @return order
     */
    int order() default -1;

    /**
     * Specify as {@code false} to cancel out parameter: an attribute would not be considered as a
     * parameter. This is useful to override the effect of {@link Style#allParameters()} flag.
     * By default, it is {@code true} and should be omitted.
     * @return {@code false} if not a parameter
     */
    boolean value() default true;
  }

  /**
   * <p>
   * Annotates method that should be invoked internally to validate invariants after instance had
   * been created, but before returned to a client. Annotated method must be parameter-less
   * (non-private) method and have a {@code void} return type, which also should not throw a checked
   * exceptions.
   * </p>
   *
   * <pre>
   * {@literal @}Value.Immutable
   * public abstract class NumberContainer {
   *   public abstract List<Number> nonEmptyNumbers();
   *
   *   {@literal @}Value.Check
   *   protected void check() {
   *     Preconditions.checkState(!nonEmptyNumbers().isEmpty(),
   *         "'nonEmptyNumbers' should have at least one number");
   *   }
   * }
   *
   * // will throw IllegalStateException("'nonEmptyNumbers' should have at least one number")
   * ImmutableNumberContainer.builder().build();
   * </pre>
   * <p>
   * Precondition checking should not be used to validate against context dependent business rules,
   * but to preserve consistency and guarantee that instances will be usable. Precondition check
   * methods runs when immutable object <em>instantiated and all attributes are initialized</em>,
   * but <em>before returned to caller</em>. Any instance that failed precondition check is
   * unreachable to caller due to runtime exception.
   * </p>
   * <p>
   * There's additional variant of using this annotation to compute normalized value. This should be
   * a last-resort solution as implementation might be brittle and error-prone. If you declare
   * return type of validation method with return type specified as abstract value type, this
   * validation method will also be able to return substitute instance. Normalized instance should
   * always be of the immutable implementations type, otherwise {@link ClassCastException} will
   * occur during construction.
   * <em>Be warned that it's easy introduce unresolvable recursion if normalization is implemented
   * without
   * proper or with conflicting checks. Always return {@code this} if value do not require
   * normalization.</em>
   * </p>
   *
   * <pre>
   * {@literal @}Value.Immutable
   * public interface Normalize {
   *   int value();
   *
   *   {@literal @}Value.Check
   *   default Normalize normalize() {
   *     if (value() == Integer.MIN_VALUE) {
   *       return ImmutableNormalize.builder()
   *           .value(0)
   *           .build();
   *     }
   *     if (value() &lt; 0) {
   *       return ImmutableNormalize.builder()
   *           .value(-value())
   *           .build();
   *     }
   *     return this;
   *   }
   * }
   *
   * int shouldBePositive2 = ImmutableNormalize.builder()
   *     .value(-2)
   *     .build()
   *     .value();
   * </pre>
   */
  @Documented
  @Target(ElementType.METHOD)
  @interface Check {}

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
  @interface NaturalOrder {}

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
  @interface ReverseOrder {}

  /**
   * Generate modifiable implementation of abstract value class. Modifiable implementation class
   * might be useful when you either need over-flexible builder or, alternatively, partially built
   * representation of value type.
   * This annotation could be used as companion to {@link Immutable} to
   * provide modifiable variant that is convertible back and forth to immutable form. When it is
   * used in a standalone manner, i.e. without using
   * <p>
   * Generated class will have name with "Modifiable" prefix by default which can be customizable
   * using {@link Style#typeModifiable() "typeModifiable" style}. Use {@code create()} factory
   * method to create instances or using "new" operator which is also depends on
   * {@link Style#create() "create" style} . Generated modifiable class will have setter methods
   * that return {@code this} for chained invocation. Getters will be of the same shape as defined
   * by abstract value types.
   * <p>
   * <em>Note: unlike {@literal @}{@link Immutable}, this annotation has very little of additional
   * "magic"
   * and customisations implemented. Annotation like {@link Include}, {@link Enclosing},
   * {@link Lazy} do not work with
   * modifiable implementation</em>
   * <p>
   * <em>This is beta functionality that is likely to change</em>
   */
  @Documented
  @Target(ElementType.TYPE)
  @interface Modifiable {}

  /**
   * Marks attribute for exclusion from auto-generated {@code toString} method. It will
   * be just excluded by default. However, you can choose to put special masking characters next to
   * the attribute instead of value, like 3 stars or 4 pound signs, this replacement string
   * can be set using {@link Style#redactedMask()} style attribute.
   */
  @Documented
  @Target(ElementType.METHOD)
  @interface Redacted {}

  /**
   * Can be used to mark some abstract no-argument methods in supertypes (about to be implemented/extended by
   * abstract value types) as regular, non-attribute methods, i.e. annotation processor will not generate field,
   * accessor, and builder initialized for it, but instead leave it for developer to implement in abstract
   * value type (and there will be compilation error for generated class about not implementing all abstract
   * methods as one can expect). This annotation is needed only for methods matching an accessor with zero
   * parameters, which are to be turned into generated immutable attribute if no annotation specified. In
   * addition, if you place it on many arguments method it will not be validated and will just hang there unused.
   */
  @Documented
  @Target(ElementType.METHOD)
  @interface NonAttribute {}

  /**
   * Naming and structural style could be used to customize convention of the generated
   * immutable implementations and companion classes. It could be placed on a class or package
   * directly or serve as meta annotation. When used as meta-annotation, then annotation could
   * be placed on a class, surrounding top level class or even a package (declared in
   * {@code package-info.java}). This
   * annotation more of example of how to define your own styles as meta-annotation rather than a
   * useful annotation. When using meta-annotation
   * <p>
   * <em>
   * Be careful to not use keywords or inappropriate characters as parts of naming templates.
   * Some sneaky collisions may only manifest as compilation errors in generated code.</em>
   * <p>
   * <em>Specific styles will be ignored for a immutable type enclosed with class which is annotated
   * as {@literal @}{@link Value.Enclosing}. So define styles on the enclosing class.
   * In this way there will be no issues with the naming and structural conventions
   * mismatch on enclosing and nested types.</em>
   */
  @Target({ElementType.TYPE, ElementType.PACKAGE, ElementType.ANNOTATION_TYPE})
  @interface Style {
    /**
     * Patterns to recognize accessors. For example <code>get = {"is*", "get*"}</code> will
     * mimic style of bean getters. If none specified or if none matches, then raw accessor name
     * will be taken literally.
     * <p>
     * By default, only {@code get*} prefix is recognized, along with falling back to use accessor
     * name literally. It is up to you if you want to use "get" prefixes or not. Original author is
     * leaning towards not using noisy prefixes for attributes in immutable objects, drawing
     * similarity with annotation attributes, however usage of "get" is neither recommended, nor
     * discouraged.
     * <p>
     * <em>This is detection pattern, not formatting pattern. It defines how to recognize name, not
     * how to derive name</em>
     * @return naming template
     */
    String[] get() default "get*";

    /**
     * Builder initialization method. i.e. "setter" in builder.
     * Do not confuse with {@link #set()}
     * @return naming template
     */
    String init() default "*";

    /**
     * Modify-by-copy "with" method.
     * @return naming template
     * @see #init()
     * @see #set()
     */
    String with() default "with*";

    /**
     * Modify-by-copy method which receives {@link java.util.function.UnaryOperator}
     * to transform an attribute before constructing a copy of immutable object.
     * This feature is disabled by default, unless you specify a naming template for this method.
     * A template can be something like {@code "with*Mapped"}, {@code "update*"},
     * or {@code "transform*"} – the choice is yours.
     * Can even be {@code "with*"} or {@code "*"} in hope there will be no overload  collisions.
     * <p>
     * Unary operator transforms values, optional values and collection elements. (currently JDK Optional only, use
     * Encodings for custom optional and containers).
     * @return naming template. By default, it is empty and feature is disabled.
     */
    String withUnaryOperator() default "";

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
     * <p>
     * Since version {@code 2.1.5} you can also use "new" template string to generate public
     * constructor instead of factory. The public constructor functionality is experimental. Note
     * that having public constructor configured will not work if {@link Check} or
     * {@link Immutable#singleton()} is used and certain other functionality. In such cases compile
     * error would be raised.
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
     * This will customize builder to be created using constructor rather than
     * factory method.
     * @return naming template
     * @see #newBuilder()
     * @see #build()
     */
    String builder() default "builder";

    /**
     * Builder creator method, it differs from {@link #builder()} in that this naming is used for
     * builders that are external to immutable objects, such as top level builders for values or
     * factories. This naming allow special keyword "new" value, which is the default.
     * "new" will customize builder to be created using constructor rather than
     * factory method.
     * @return naming template
     * @see #build()
     */
    String newBuilder() default "new";

    /**
     * Method to initialize builder with values from instance. Can be set to empty
     * string to disable "from" method generation.
     * @return naming template
     */
    String from() default "from";

    /**
     * Instance creation method on builder.
     * @return naming template
     */
    String build() default "build";

    /**
     * Naming template for the {@code buildOrThrow} method which accept exception factory function
     * for throwing in case not all mandatory properties are set. Non-default (i.e. not empty)
     * template will essentially enable generation of this method, acting both as a naming template
     * and as a feature flag.
     * <p>
     * Generation of build or throws method requires presence of a function type on the classpath,
     * provided either by Java 8 or Guava on Java 7. If used on java 7 without Guava, this style
     * will have no effect: no method will be generated.
     * <p>
     * <em>Note: This attribute-style is experimental and may be changed in near releases.
     * You should not rely on it if don't ready to change code on minor updates of the annotation
     * processor</em>
     * @return naming template, if default/empty it will not be generated
     */
    String buildOrThrow() default "";

    /**
     * Naming template for the {@code canBuild} method on a builder that return boolean if it can
     * "safely" build an object, i.e. all attributes are set and instance would be returned by
     * calling {@code .build()} method without throwing {@link IllegalStateException}.
     * <em>Note: By default this method is not generated, unless a naming is provided. For example,
     * set it to {@code canBuild="canBuild"} and {@code boolean canBuild()} method will be
     * generated on builder.</em>
     * <em>Note: when using encoding, building of attributes is delegated to encoding specific
     * routines, so that general builder infrastructure may not know if attribute can be safely
     * constructed, in this case, canBuild method might give wrong answer.</em>
     * @return naming template, if default/empty it will not be generated
     */
    String canBuild() default "";

    /**
     * Naming template for the {@code toBuilder} method on a immutable instance that returns new builder on which
     * merge method is already called on this instance {@code builder.from(this)}. By default, this naming template
     * is empty, and this method is not generated. To enable generation of this method, you must specify naming
     * template,
     * {@code (toBuilder="toBuilder")}. Naming template can insert type name for an asterisk, but in most cases you
     * would just use method name verbatim.
     * <p>
     * <em>Note: This attribute-style is experimental and may be changed in near releases.
     * This is not compatible with strict builders and will not be generated if {@link #strictBuilder()} is
     * {@code true}.
     * Also, when enabled, this will not be considered an attribute if defined as {@code abstract Builder toBuilder()}
     * in abstract value type.
     * </em>
     * @return naming template, if default/empty it will not be generated
     */
    String toBuilder() default "";

    /**
     * Detects and use method named as specified to customize generated {@code hashCode}.
     * Abstract value types defined as interfaces cannot implement {@code default} methods of {@code java.lang.Object},
     * such as {@link Object#hashCode()} implemented as {@code default} interface methods and so cannot override
     * (actually "underride" if that is the right term) the default {@code hashCode} which is generated on immutable
     * implementation. So in order to customize and replace default generated code, you can now declare {@code default}
     * or {@code static} method named, say {@code hash}({@code underwriteInterfaceHashCode="hash"}),
     * will be used in generated {@code hashCode} method.
     * The signature of the {@code default}/{@code static} should be compatible to the ones
     * of {@link Object}, static methods suppose to take instances of the abstract value type as parameter
     * instead of a plain {@link Object}.
     * <p>
     * <em></em>Wiring of the custom method will only occur if found by the name and
     * matched by signature.
     * </em>
     * @return method name, if empty (the {@code default}) it will not be detected and wired in generation.
     */
    String underrideHashCode() default "";

    String underrideEquals() default "";

    String underrideToString() default "";

    /**
     * Delegates {@code toString} implementation completely to a fully qualified path to a method name, example
     * {@code delegateToString="com.whatever.packg.ToStringer.stringify"}. The path will be used literally in generated
     * code, and a single parameter will be passed to it, {@code this} immutable object instance.
     * <p><em>Note: If specified, it will take precedence over any other {@code toString} customization mechanism</em>
     * @return fully qualified static method name, if empty (by default) will not be used
     */
    String delegateToString() default "";

    /**
     * If {@code includeHashCode} is not empty it will be used as part of generated `hashCode`. This will be a verbatim
     * line of code used with the tag-placeholder {@code [[type]]} will be replaced with the simple
     * (or relative to top level) name of the abstract value type.
     * It's the responsibility of the user to put well-formed code to be put in context, including parenthesis, etc.
     * use try-see-fix approach here. Other fields will be included as usual, coming after this custom value.
     * <p>Examples might give you better ideas how to use it:
     * <pre>
     *   includeHashCode = "this.baseHashCode()"
     *   includeHashCode = "super.hashCode()"
     *   includeHashCode = "getClass().hashCode()"
     *   includeHashCode = "[[type]].class.hashCode()"
     *   includeHashCode = "(\"[[type]]\".length() + 1)"
     * </pre>
     * <p><em>Note: this will be ignored if `hashCode` will be manually written in the abstract value class or
     * {@link #underrideHashCode()} will be used for the same purpose</em>
     * @return interpolated code snippet, by default empty and have no effect
     */
    String includeHashCode() default "";

    /**
     * Method to determine if all required attributes are set.
     * Default method name choice for this is mostly random.
     * @return naming template
     */
    String isInitialized() default "isInitialized";

    /**
     * Naming template for a method to determine if attribute is set. These are used on Modifiables and, internally, on builders.
     * In order to expose it on builders (make it {@code public}, or package-private if attribute itself
     * is package-private), use {@link #isSetOnBuilder()} style flag.
     * @return naming template
     */
    String isSet() default "*IsSet";

    /**
     * Modifiable object "setter" method. Used for mutable implementations.
     * Do not confuse with {@link #init()}
     * @return naming template
     */
    String set() default "set*";

    /**
     * Unset attribute method. Used for mutable implementations.
     * @return naming template
     */
    String unset() default "unset*";

    /**
     * Clear all collection attributes and unset(or other container). Used for mutable
     * implementations.
     * @return naming template
     */
    String clear() default "clear";

    /**
     * Factory method for modifiable implementation, could be "new" to create objects using
     * constructor.
     * @return naming template
     */
    String create() default "create";

    /**
     * Method to convert to instance of modifiable type to "canonical" immutable instance.
     * @return naming template
     */
    String toImmutable() default "toImmutable";

    /**
     * Generated builder class name.
     * @return naming template
     */
    String typeBuilder() default "Builder";

    /**
     * Inner builder class name which will be matched to be extend/super for generated builder.
     * @return naming template
     */
    String typeInnerBuilder() default "Builder";

    /**
     * Naming templates to detect base/raw type name from provided abstract value type name.
     * If none specified or if none matches, then raw type name will be taken literally the same as
     * abstract value type name.
     * <p>
     * <em>This is detection pattern, not formatting pattern. It defines how to recognize name, not
     * how to derive name</em>
     * @return naming templates
     */
    String[] typeAbstract() default "Abstract*";

    /**
     * Name template to generate immutable implementation type by using base/raw type name.
     * Use {@link #typeAbstract()} to customize base/raw name inference.
     * @return naming template
     */
    String typeImmutable() default "Immutable*";

    /**
     * Umbrella nesting class name generated using {@link Enclosing}.
     * @return naming template
     */
    String typeImmutableEnclosing() default "Immutable*";

    /**
     * Immutable class name when generated under umbrella class using {@link Enclosing} annotation.
     * @return naming template
     * @see #typeImmutable()
     * @see #typeImmutableEnclosing()
     */
    String typeImmutableNested() default "*";

    /**
     * Modifiable companion class name template
     * @return naming template
     */
    String typeModifiable() default "Modifiable*";

    /**
     * Inner builder class name which will be matched to be extend/super for generated Modifiable
     * class.
     * @return naming template
     */
    String typeInnerModifiable() default "Modifiable";

    /**
     * Generated "with" interface name. Used to detect a demand and generate "with" interface.
     * @return naming template
     */
    String typeWith() default "With*";

    /**
     * <p>
     * Naming template {@code packageGenerated} used to derive name of the package where the
     * annotation processor should put generated classes for corresponding immutable values. By
     * default it equals to {@code "*"} which means to use the same package as abstract value type.
     * It can be configured to a specific package name, like {@code "com.acme.specific"}, or used to
     * specify pattern, like "*.gen" or "*.immutable.impl".
     * </p>
     * <p>
     * <em>Note: It is expected that most generators will honor this style attribute, but it's not
     * guaranteed.
     * When you generate derived classes in the same package (by default), then implementation could
     * access
     * and/or override package-private methods. If using a different package make sure to use public
     * or protected access where needed, otherwise illegal access compilation errors will be flagged
     * in the generated code.</em>
     * </p>
     * @return generated package naming template
     */
    String packageGenerated() default "*";

    /**
     * Specify default options for the generated immutable objects.
     * If at least one attribute is specified in inline {@literal @}{@link Immutable} annotation,
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
     * <em>Note that some additional annotation processors (for example mongo repository generator)
     * may not work without Guava being accessible to the generated classes,
     * and thus will not honor this attribute</em>
     * @return if forced JDK-only class usage
     */
    boolean jdkOnly() default false;

    /**
     * When {@code true} &mdash; will use JDK 9+ immutable collections to implement
     * {@link List}/{@link Set}/{@link Map} attributes.
     * In JDK 9+, immutable collections are instantiated via {@code of}/{@code copyOf}
     * static methods on {@code List}, {@code Set}, {@code Map} interfaces:
     * {@link List#of()}, {@link Set#copyOf(Collection)}, etc.
     * Please note that these collections do not support {@code null} elements,
     * also Sets and Maps do not maintain insertion order, so the order is arbitrary
     * and cannot be relied upon.
     * @return if {@code true} use JDK 9+ immutable {@code List}, {@code Set}, or {@code Map}
     */
    boolean jdk9Collections() default false;

    /**
     * When {@code true} &mdash; forces to generate strict builder code. Strict builders are forward
     * only. For collections and maps, there's no set/reset methods are generated in
     * favor of using additive only initializers. For regular attributes, initializers could be
     * called only once, subsequent reinitialization with throw exception.
     * Also, "from" method (named by {@link #from()}) will not be generated on builder: it
     * becomes error-inviting to reinitialize builder values. {@code Nullable} and {@code Default}
     * container attributes are not supported when {@code strictBuilder = true}.
     * <p>
     * Usage of strict builders helps to prevent initialization mistakes early on.
     * @return if strict builder enabled
     */
    boolean strictBuilder() default false;

    /**
     * Strict modifiable will refuse any accessor value (by throwing {@link IllegalStateException})
     * which is mandatory. Enabled by default. Set it to {@code false} and it will allow to get
     * current field value even if not initialized ({@code null} for references, {@code 0}, {@code false} &mdash; for
     * primitives).
     * @return default is {@code true}, enabling strict modifiable
     */
    boolean strictModifiable() default true;

    /**
     * When {@code true} &mdash; disables check that all required attributes have been provided to a
     * builder.
     */
    ValidationMethod validationMethod() default ValidationMethod.SIMPLE;

    /**
     * <p>
     * When {@code true} &mdash; all settable attributes are considered as they are annotated with
     * {@link Value.Parameter}. Use {@code Value.Parameter(false)} annotation on an attribute to
     * cancel the effect of {@code allParameters = true}.
     * </p>
     * <p>
     * This style could be used to create special tuple-style annotations:
     * </p>
     *
     * <pre>
     * {@literal @}Value.Style(
     *     typeImmutable = "*Tuple",
     *     allParameters = true,
     *     defaults = {@literal @}Value.Immutable(builder = false))
     * public @interface Tuple {}
     *
     * {@literal @}Tuple
     * {@literal @}Value.Immutable
     * interface Color {
     *   int red();
     *   int green();
     *   int blue();
     *   {@literal @}Value.Parameter(false)
     *   List<Info> auxiliaryInfo();
     * }
     *
     * ColorTuple.of(0xFF, 0x00, 0xFE);
     * </pre>
     * @return if all attributes will be considered parameters
     */
    boolean allParameters() default false;

    /**
     * This funny-named named attribute, when enabled makes default accessor methods defined in
     * interfaces/traits behave as if they annotated as {@literal @}{@link Value.Default}.
     * This is not a default behaviour to preserve compatibility and also to have a choice to not
     * opt-in for this new functionality when not needed.
     * @return if consider default method accessors as {@literal @}{@code Value.Default}
     */
    boolean defaultAsDefault() default false;

    /**
     * Enable if you needed to copy header comments from an originating source file with abstract
     * types to generated (derived) implementation classes. Header comments are comments preceding
     * package declaration statement. It could be used to copy license headers or even special
     * pragma comments (such as {@code //-no-import-rewrite}).
     * It is off by default because not often needed (as generated files are transient and not
     * stored in version control), but adds up to the processing time.
     * @return if copy header comments to generated classes.
     */
    boolean headerComments() default false;

    /**
     * List type of annotations to copy over from abstract value type to immutable implementation
     * class. Very often this functionality is not needed when annotations are declared as
     * {@link Inherited}, but there are cases where you need to pass specific non-inherited
     * annotations to the implementation class. In general, copying all type-level annotations is
     * not very safe for annotation processing and some other annotation consumers. By default, no
     * annotations are copied unless you specify non-empty annotation type list as value
     * for {@code passAnnotations} attribute. However, there are some special annotations which are
     * copied using special logic, such as {@code Nullable} annotations (and Jackson annotations)
     * <p>
     * This has a number of limitations, including that it's not suited for {@link ElementType#TYPE_USE}
     * annotations (annotations attached to types), only for method annotation. In many cases type-use
     * annotation would be replicated automatically in derived signatures, but in case of type-use
     * nullable annotations it's the best to specify the nullable annotation of choice using
     * {@link #fallbackNullableAnnotation()} so it will be used in generated code in all places where
     * original annotations would not propagate automatically.
     * @return types of annotations to pass to an immutable implementation class and its
     *     attributes.
     */
    Class<? extends Annotation>[] passAnnotations() default {};

    /**
     * List of additional annotations to pass through for any jackson json object.
     * Soft-deprecated this is no longer necessary.
     * @return types of annotations to pass to the json methods on an immutable implementation class
     */
    Class<? extends Annotation>[] additionalJsonAnnotations() default {};

    /**
     * Specify the mode in which visibility of generated value type is derived from abstract value
     * type. It is a good idea to not specify such attributes inline with immutable values, but
     * rather create style annotation (@see Style).
     * @return implementation visibility
     */
    ImplementationVisibility visibility() default ImplementationVisibility.SAME;

    /**
     * Specify the mode in which visibility of generated value type is derived from abstract value
     * type. Specifying this will override {@link #visibility()} and the reason why this option as a string exists,
     * is to avoid javac warnings mentioned in <a href="https://github.com/immutables/immutables/issues/291">#291</a>.
     * @return implementation visibility as string
     */
    String visibilityString() default "";

    /**
     * Specify whether init, copy and factory methods and constructors for an unwrapped {@code X} of
     * {@code Optional<X>}
     * should accept {@code null} values as empty value. By default, nulls are rejected in favor of
     * explicit conversion using {@code Optional.ofNullable}. Please note that initializers that
     * take explicit {@code Optional} value always reject nulls regardless of this setting.
     * @return optional elements accept nullables
     */
    boolean optionalAcceptNullable() default false;

    /**
     * Generate {@code SuppressWarnings("all")} in generated code. Set this to {@code false} to
     * expose all warnings in a generated code. To suppress other warnings issued by Immutables use
     * explicit annotations {@literal @}{@code SuppressWarning("immutables")} or {@literal @}
     * {@code SuppressWarning("all")}
     * @return {@code true} if will generate suppress all warnings, enabled by default.
     */
    boolean generateSuppressAllWarnings() default true;

    /**
     * Generate a private no-argument constructor in generated code. Note that this property will
     * be ignored if {@link Immutable#singleton()} returns <code>true</code>.
     * @return {@code true} if will generate a default no argument constructor, disabled by default.
     */
    boolean privateNoargConstructor() default false;

    /**
     * Generate a protected no-argument constructor, mainly for reflective usage by advanced
     * toolkits.
     * Note that this one overrides {@link #privateNoargConstructor()} if both are set to
     * {@code true}. This property will be ignored if {@link Immutable#singleton()} returns
     * <code>true</code>.
     * @return {@code true} if will generate a no argument constructor with protected visibility,
     *     disabled by default.
     */
    boolean protectedNoargConstructor() default false;

    /**
     * Enabling {@code attributelessSingleton} switches to old behavior of 2.0.x version when
     * immutable instances which had no attributes defined we automatically generated as singleton having
     * {@link #instance()} accessor.
     * As of 2.1 we are more strict and explicit with singletons and are not generating it by
     * default, only when {@link Immutable#singleton()} is explicitly enabled.
     * @return {@code true} if auto singleton on new attributes (default is {@code false})
     */
    boolean attributelessSingleton() default false;

    /**
     * As of 2.1 we are switching to safe generation of derived and default values when there are
     * more than one such attribute. This {@code unsafeDefaultAndDerived} style could be enabled to
     * revert to old, unsafe behavior.
     * <p>
     * In order to initialize default and derived attributes method bodies (initializers) will be
     * invoked. Initializers could refer to other attributes, some of which might be also derived or
     * uninitialized default values. As it's extremely difficult to reliably inspect initializer
     * methods bodies and compute proper ordering, we use some special generated code which figures
     * it out in runtime. If there will be a cycle in initializers, then
     * {@link IllegalStateException} will be thrown.
     * <p>
     * If you set {@code unsafeDefaultAndDerived} to {@code true}, then simpler, unsafe code will be
     * generated. With the unsafe code you cannot refer to other default or derived attributes in
     * initializers as otherwise result will be undefined as order of initialization is not
     * guaranteed.
     * @return {@code true} if old unsafe (but potentially with less overhead) generation should be
     *     used.
     */
    boolean unsafeDefaultAndDerived() default false;

    /**
     * When enabled: {@code clear} method will be generated on builder to reset state of a builder.
     * Primarily designed for resource constrained environments to minimize allocations. This
     * functionality is disabled by default as usually it's better to create fresh builders with a
     * clean state: in server side java it may be more efficient to allocate new builder than clean
     * previously allocated one.
     * <p>
     * <em>Note: this functionality is experimental and may be changed in further versions</em>
     * @return {@code true} if clean method would be generated.
     * @see #clear()
     */
    boolean clearBuilder() default false;

    /**
     * When enabled, builder will have isSet methods generated for each attribute, like {@code aIsSet()},
     * {@code bIsSet()}. These are telling if attribute was initialized on a builder.
     * The actual naming template is configured by {@link #isSet()} style attribute.
     * @return {@code true} if isSet methods for each attribute will be exposed on builders.
     * @see #isSet()
     * @see #clearBuilder()
     */
    boolean isSetOnBuilder() default false;

    /**
     * When this optimisation in enabled then the processor tries to defer allocation of
     * collection in builder and modifiable objects. The resulting code might somewhat be slower at
     * a microbenchmark scale due to additional "if" checks, but might save some allocations.
     * Does not work when {@link #strictBuilder()} is enabled. Disabled by default.
     * @return {@code true} if defer collection allocation.
     */
    boolean deferCollectionAllocation() default false;

    /**
     * Deep analysis of immutable types enables additional convenience features.
     * When enabled, each attribute is analyzed and if it is discovered to be an {@literal @}
     * {@code Value.Immutable} object (either abstract value type or generated implementation type),
     * then some special handling will be applied to it. As of now following functionality is
     * applied:
     * <ul>
     * <li>Accessors in a generated immutable type will be implemented with a covariant return type
     * of the immutable implementation of the abstract value type of the declared attribute. This
     * has no effect on the collection/container attributes to not interfere with invariant generic
     * types. Derived and Default attributes are also not supported as of now to avoid excessive
     * complexity</li>
     * <li>Builder initializers will have overloaded variants with parameters of the attribute value
     * object's constructor (if it has constructor as opposed to the ones which only have builder).
     * Effectively this is a shortcut to initialize value object in a more concise way. This works
     * for regular and collection attributes (but not for maps or arrays to avoid complex and
     * confusing overload).</li>
     * </ul>
     * See the example below which illustrates these behaviors.
     *
     * <pre>
     * {@literal @}Value.Style(deepImmutablesDetection = true)
     * public interface Canvas {
     *   {@literal @}Value.Immutable
     *   public interface Color {
     *     {@literal @}Value.Parameter double red();
     *     {@literal @}Value.Parameter double green();
     *     {@literal @}Value.Parameter double blue();
     *   }
     *
     *   {@literal @}Value.Immutable
     *   public interface Point {
     *     {@literal @}Value.Parameter int x();
     *     {@literal @}Value.Parameter int y();
     *   }
     *
     *   {@literal @}Value.Immutable
     *   public interface Line {
     *     Color color();
     *     Point start();
     *     Point end();
     *   }
     *
     *   public static void main(String... args) {
     *     ImmutableLine line = ImmutableLine.builder()
     *         .start(1, 2) // overload, equivalent of .start(ImmutablePoint.of(1, 2))
     *         .end(2, 3)
     *         // overload, equivalent of .end(ImmutablePoint.of(2, 3))
     *         .color(0.9, 0.7, 0.4)
     *         // overload, equivalent of .color(ImmutableColor.of(0.9, 0.7. 0.4))
     *         .build();
     *
     *     ImmutablePoint start = line.start(); // return type is ImmutablePoint rather than declared Point
     *     ImmutablePoint end = line.end(); // return type is ImmutablePoint rather than declared Point
     *     ImmutableColor color = line.color(); // return type is ImmutableColor rather than declared Color
     *
     *     ImmutableLine.builder()
     *         .start(start)
     *         .end(end)
     *         .color(color)
     *         .build();
     *   }
     * }
     * </pre>
     * <p>
     * Disabled by default as, speculatively, this might increase processing time. It will not work
     * for yet-to-be-generated types as attribute types, which allows only shallow analysis.
     * <p>
     * <em>Note: this functionality is experimental and may be changed in further versions. As of
     * version 2.2
     * we no longer add {@code *Of} suffix to the shortcut initializer attribute.</em>
     * @return {@code true} if deep detection is enabled.
     */
    boolean deepImmutablesDetection() default false;

    /**
     * Makes abstract value type predominantly used in generated signatures rather than immutable
     * implementation class. In case of {@link #visibility()} is more restrictive than
     * {@link #builderVisibility()} (for example is {@code PRIVATE}), then this
     * feature is turned on automatically.
     * <p>
     * <em>Note: not all generators or generation modes might honor this attribute</em>
     * @return {@code true} if methods of generated builders and other classes should return
     *     abstract type, rather than work with immutable implementation class.
     */
    boolean overshadowImplementation() default false;

    /**
     * By default, builder is generated as inner builder class nested in immutable value class.
     * Setting this to {@code true} will flip the picture — immutable implementation class will be
     * nested inside builder, which will be top level class. In case if {@link #visibility()} is set
     * to {@link ImplementationVisibility#PRIVATE} this feature is turned on automatically.
     * @return {@code true} if builder should be generated as top level class and implementation
     *     will become static inner class inside builder.
     */
    boolean implementationNestedInBuilder() default false;

    /**
     * As there are some ambiguities with how certain field prefixes work ("get", "is") we by
     * default force whatever is inferred by Immutables. Disable forcing Jackson property names if
     * you use naming strategies. Also make sure you recognize both "get*" and "is*" as attribute
     * name patterns as Jackson infers default names using JavaBean convention.
     * @return {@code true} if force jackson property names. default it {@code true}, set
     *     {@code false} to disable
     */
    boolean forceJacksonPropertyNames() default true;

    boolean setJacksonPropertyRequired() default true;

    /**
     * @return if put {@code JsonIgnore} on fields, defaults to {@code false}
     */
    boolean forceJacksonIgnoreFields() default false;

    /**
     * If {@code forceEqualsInWithers} enabled, generated with-copy methods will have
     * equality check forced. {@code ==} for integer primitives, {@code floatToIntBits ==} for floating point,
     * and {@code .equals()} for the reference values.
     * Some usage patterns might require strict equality check during copy to function properly,
     * while for other usages it's just an optimisation.
     * @return if equality check should be forced in withers, defaults to {@code false}
     */
    boolean forceEqualsInWithers() default false;

    /**
     * Setting this to {@code false} would disable any special jackson integration capabilities.
     * While out-of-the-box Jackson readiness is a good things in the most cases, for some cases
     * it might get in the way of highly customized Jackson infrastructure. When disabled, there are
     * no any special stuff generated such as {@code JsonProperty} annotations or internal
     * {@code Json} delegate class together with {@code JsonCreator} method. This allows to place
     * {@code JsonSerialize/JsonDeserialize} annotations on the value types without redundant
     * support code being generated.
     * @return {@code true} if generate special Jackson code when encountered
     *     {@code JsonSerialize/JsonDeserialize}. Default is {@code true}.
     */
    boolean jacksonIntegration() default true;

    /**
     * When {@code weakInterning} is enabled, then for value types with
     * {@literal @}{@code Value.Immutable(intern=true)} weak (see {@link WeakReference})
     * interning will be used.
     * @return {@code true} if enable weak interning for {@code intern=true} values,
     *     defaults to {@code false}
     */
    boolean weakInterning() default false;

    /**
     * All initializers (the methods to set attribute values on a builder) usually have public
     * visibility regardless of the visibility of the attribute accessors. Usually this doesn't
     * matter, especially for value types defined as interfaces (where all accessors public). But
     * sometimes there's a need to have abstract class with finer-grained access control to
     * attributes, which also require initializers to follow the access level of accessors defining
     * attributes. Set this flag to {@code false} if initializers (builder setters) should follow
     * access level of attributes.
     * <em>Note this flag is disregarded when {@link #stagedBuilder()} is enabled which generates
     * stage interfaces which requires public access anyway.</em>
     * @return {@code true} if force all initializers to public. Default is true
     */
    boolean alwaysPublicInitializers() default true;

    /**
     * Specify the mode in which visibility of generated value type is derived from abstract value
     * type. It is a good idea to not specify such attributes inline with immutable values, but
     * rather create style annotation (@see Style).
     * @return implementation visibility
     */
    BuilderVisibility builderVisibility() default BuilderVisibility.PUBLIC;

    /**
     * Specify the mode in which visibility of generated value type is derived from abstract value
     * type. Specifying this will override {@link #builderVisibility()} and the reason why this option as
     * a string exists, is to avoid javac warnings mentioned in
     * <a href="https://github.com/immutables/immutables/issues/291">#291</a>.
     * @return implementation visibility as string
     */
    String builderVisibilityString() default "";

    /**
     * Runtime exception to throw when an immutable object is in an invalid state. I.e. when some
     * mandatory attributes are missing and immutable object cannot be built. The runtime exception
     * class must have a constructor that takes a single string, otherwise there will be compile-time
     * error in the generated code.
     * <p>
     * The default exception type is {@link IllegalStateException}. In case if
     * specified exception type have public constructor taking array of strings (can be varargs),
     * then missing parameter names will be passed to that constructor. Otherwise, string
     * constructor is always expected to be present to take formatted error message. It is always
     * advisable have string constructor even in the presence of attribute names array constructor
     * as some additional generators might use string constructor for reporting other invalid state
     * issues.
     * <p>
     * <em>Technically we allow exception class to be checked (non-runtime), but not all processor
     * features might be generated correctly (they may not expect). So use checked exception only if
     * this work with your set of use-cases: there is no guarantee that it will be ever supported in
     * all processor components/templates</em>
     * @return exception type
     */
    Class<? extends RuntimeException> throwForInvalidImmutableState() default IllegalStateException.class;

    /**
     * Runtime exception to throw when null reference is passed to non-nullable parameter or occurred
     * in array/container that must not contain nulls. It is expected that the exception will have
     * public constructor receiving string as message/parameter name. The
     * default is {@link NullPointerException} and the calls are usually delegated to
     * {@link Objects#requireNonNull(Object)} or similar utility throwing
     * {@code NullPointerException}.
     * @return exception type
     */
    Class<? extends RuntimeException> throwForNullPointer() default NullPointerException.class;

    /**
     * Depluralize names for collection and map attributes used for generating derived method names,
     * such as {@link #add()} and {@link #put()}. In order to enable depluralization use
     * {@code depluralize = true}: this will trim trailing "s" if present to create singular form
     * ("*ies" to "*y" is also supported).
     * Exceptions are provided using {@link #depluralizeDictionary()} array of "singular:plural"
     * pairs as alternative to mechanical "*s" depluralization.
     *
     * <pre>
     * {@literal @}Value.Style(
     *    depluralize = true, // enable feature
     *    depluralizeDictionary = {"person:people", "foot:feet"}) // specifying dictionary of exceptions
     * </pre>
     * <p>
     * When given the dictionary defined as {@code "person:people", "foot:feet"} then
     * depluralization examples for collection {@code add*} method in builder would be:
     * <ul>
     * <li>boats -> addBoat</li>
     * <li>people -> addPerson</li>
     * <li>feet -> addFoot</li>
     * <li>feetPeople -> addFeetPerson</li>
     * <li>peopleRepublics -> addPeopleRepublic</li>
     * </ul>
     * The default value is a {@code false}: feature is disabled, compatible with previous
     * versions.
     * <p>
     * @return {@code true} if depluralization enabled.
     * @see Depluralize
     */
    boolean depluralize() default false;

    /**
     * Dictionary of exceptions — array of "singular:plural" pairs as alternative to mechanical "*s"
     * depluralization. Suppress trimming of trailing "s" for certain words by using exceptions in
     * form {@code "words:words"} or simply {@code "words"}. Important to note is that words will be
     * converted to lowercase and identifier in question consisting of one or more words joined using
     * camel case —  only a last segment will be considered for depluralization when matching
     * dictionary. Uninterpretable pairs will be ignored. By default, no dictionary is supplied and
     * depluralization performed only by mechanical "*s" trimming.
     * <p>
     * This attribute is semi-deprecated in favor of using {@link Depluralize#dictionary()}
     * annotation which may be placed on a package, type or as meta-annotation. Full dictionary will
     * be merged across all applicable definitions.
     * @return array of "singular:plural" pairs.
     * @see #depluralize()
     * @see Depluralize#dictionary()
     */
    String[] depluralizeDictionary() default {};

    /**
     * You can provide classes which must contain copyOf method with relevant overloads which should
     * not have ambiguous cases as it will be fully a subject to JLS rules of static imports and
     * compile time overload resolution.
     * <p>
     * Tha major use case is custom validation and normalization of the attribute values by types.
     * Validations specific to a value object or it's attributes could be performed using
     * <p>
     * <em>Note: This attribute-style is experimental and may be changed in near releases.
     * The manner in which routines are applied
     * </em>
     * </p>
     * @return classes, for which static imports like {@code import static ..Type.immutableCopyOf;}
     *     will be generated along with corresponding invocations of {@code immutableCopyOf}
     *     method when accepting parameters.
     */
    Class<?>[] immutableCopyOfRoutines() default {};

    /**
     * Staged (aka telescopic) builders are a special flavor of builders which provides compile-time
     * safety to a staged building. Providing proposals to insert mandatory attributes one by one,
     * and then allowing adding any optional or collection attributes in any order before calling
     * the build method. This option also implies {@link #strictBuilder()} is automatically enabled.
     * <p>
     * <em>Note: this functionality may not play well with other functionality, may be auto-disabled
     * in certain configurations like implementations nested in builder.
     * </em>
     * @return if telescopic builders are used, {@code false} by default
     */
    boolean stagedBuilder() default false;

    /**
     * Setting {@code builtinContainerAttributes} to {@code false} would disable generation of
     * built-in convenience features of automatically recognized container types such as
     * {@code Optional}, {@link List}, {@link Map}. This will turn all attribute types into nothing
     * special setters(initializers) and getters. However, any registered encodings (type
     * customizers) will be still processed. One of the purposes of this style control is to provide
     * clean-slate when only registered encodings will impact type generation, but none of the
     * built-in types would be applied. Note: that this style controls recognition of the
     * attribute types, but not kind of attributes such as those specified by {@code Value.Default}
     * or {@code Nullable} annotations.
     * @return {@code true} if builtin container attributes should be supported. {@code true} is the
     *     default
     */
    boolean builtinContainerAttributes() default true;

    /**
     * If enabled, modifiable type will have void setters and will look more like JavaBean. This is
     * modifiable companion types only, not for builders and other types of generated artifacts.
     * <p>
     * <em>Note, we are not supporting JavaBean specification in any way except that Immutables can
     * be used/configured to be partially compatible with some of the conventions.</em>
     * </p>
     * @return {@code true} for void setters and minor tweaks to make modifiables more
     *     bean-friendly. {@code false} is the default
     */
    boolean beanFriendlyModifiables() default false;

    /**
     * If enabled mandatory attributes would be auto-propagated to be parameters of value object
     * constructor.
     * <p>
     * <em>This parameter conflicts with {@link #allParameters()} and is ignored when
     * {@code allParameters} is enabled</em>
     * @return {@code true} to turn mandatory attributes into parameters. {@code false} is the
     *     default
     */
    boolean allMandatoryParameters() default false;

    /**
     * When {@code transientDerivedFields} is enabled (as it by default), the backing fields for
     * derived attributes will be marked as {@code transient} (unless type is {@link Serializable}
     * by using regular Java serialization (not for when structural serialization is on via
     * {@literal @}{@code Serial.Structural}). When set {@code false}, the processor we will not add
     * any {@code transient} annotations to derived fields. This is only for derived fields. Any
     * fields for lazy attributes are
     * always {@code transient} and ready to be reinitialized lazily after object is deserialized
     * regardless of serialization/persistence technology.
     * @return default is {@code true} for backward compatibility. Set to {@code false} to disable
     */
    boolean transientDerivedFields() default true;

    /**
     * Disable final fields only if there are no other way, considered unsafe. This is only about instance fields of
     * Immutable implementation class, will not apply to a lot of their places/generators.
     * @return default is {@code true}, do not switch off.
     */
    boolean finalInstanceFields() default true;

    /**
     * String to substitute value of the attribute in a generated {@code toString} implementation
     * when {@link Redacted} annotation is applied to the attribute.
     * <p>
     * By default, it is an empty string, which also mean that the attribute will not appear in the
     * {@code toString} output. If you set it to some value, then it will be printed.
     * @return redacted value substitution string
     */
    String redactedMask() default "";

    /**
     * Immutables recognizes nullable annotation by simple name. For most cases this is sufficient.
     * But for some cases it's needed to customize this annotation simple name and
     * {@code nullableAnnotation} can be used to set custom simple name for nullable annotation.
     * While we recommend against this change, this may be occasionally be needed.
     * <em>Except for simple name detection, {@code javax.annotation.Nullable} and
     * {@code javax.annotation.CheckForNull} are always recognized as nullable annotations.
     * </em>
     * @return nullable annotation simple name
     */
    String nullableAnnotation() default "Nullable";

    /**
     * When enabled: immutable attributes with discoverable builders receive the additional
     * builder API:
     * <p>
     * For single children:
     * <ul>
     * <li>{@code BuilderT *Builder()}
     * <li>{@code ParentT *Builder(BuilderT builder)}
     * </ul>
     * <p>
     * For a collection of children:
     * <li>{@code BuilderT add*Builder()}
     * <li>{@code ParentT addAll*Builder(Iterable<BuilderT> builderCollection)}
     * <li>{@code ParentT addAll*Builder(BuilderT... builderArgs)}
     * <li>{@code List<BuilderT> *Builders()}
     * </ul>
     * <p>
     * In strict mode, you may only set the builder via {@code *Builder(BuilderT builder)} once,
     * but you may call {@code *Builder()} multiple times, in which the same builder is returned.
     * If the nested immutable is also strict, then you will only be able to set properties on
     * the child builder once.
     * <p>
     * To discover builders on value attributes the value methods are scanned for method names
     * matching a patterns specified in {@link #attributeBuilder()}.
     * This style parameter is experimental and may change in the future.
     * @return true to enable the feature.
     */
    boolean attributeBuilderDetection() default false;

    /**
     * Pattern for detecting builders.
     * {@link #attributeBuilder()} applies to both
     * static and instance methods. In the case a builder is only discoverable through a
     * value instance method, the builder class must have a public no-arg static construction
     * method. To use a no-arg public constructor, a special token "new" should be specified.
     * example: new token required to find this builder.
     *
     * <pre>
     * class MyObject {
     *   class Builder {
     *     public Builder() {...}
     *     public Builder(MyObject copy) {...}
     *
     *     MyObject build() {...}
     *   }
     * }
     * </pre>
     *
     * <em>This is detection pattern, not formatting pattern. It defines how to recognize a nested
     * builder.</em>
     * Only applies if {@link #attributeBuilderDetection()} is {@code true}.
     * @return naming template
     */
    String[] attributeBuilder() default {"Builder", "*Builder", "builder", "from", "build", "*Build", "new"};

    /**
     * Naming template for retrieving a nested builder.
     * Only applies if {@link #attributeBuilderDetection()} is {@code true}.
     * @return naming template.
     */
    String getBuilder() default "*Builder";

    /**
     * Naming template for setting a nested builder.
     * This may be called only once in strict mode.
     * Only applies if {@link #attributeBuilderDetection()} is {@code true}.
     * @return naming template.
     */
    String setBuilder() default "*Builder";

    /**
     * Naming template for adding a new builder instance to a collection.
     * Only applies if {@link #attributeBuilderDetection()} is {@code true}.
     * @return naming template.
     */
    String addBuilder() default "add*Builder";

    /**
     * Naming template for adding a collection of builders.
     * Only applies if {@link #attributeBuilderDetection()} is {@code true}.
     * @return naming template.
     */
    String addAllBuilder() default "addAll*Builders";

    /**
     * Naming template for retrieving an immutable list of builders.
     * Only applies if {@link #attributeBuilderDetection()} is {@code true}.
     * @return naming template.
     */
    String getBuilders() default "*Builders";

    /**
     * Immutables applies some annotations if found on classpath. These include:
     * <ul>
     * <li>{@literal @}{@code org.immutables.value.Generated}
     * <li>{@literal @}{@code javax.annotation.Generated}
     * <li>{@literal @}{@code java.annotation.processing.Generated}
     * <li>{@literal @}{@code javax.annotation.concurrent.Immutable}
     * <li>{@literal @}{@code javax.annotation.ParametersAreNonnullByDefault}
     * <li>{@literal @}{@code javax.annotation.CheckReturnValue}
     * <li>{@literal @}{@code edu.umd.cs.findbugs.annotations.SuppressFBWarnings}
     * <li>{@literal @}{@code com.google.errorprone.annotations.Var}
     * <li>{@literal @}{@code com.google.errorprone.annotations.Immutable}
     * <li>... and others, etc
     * </ul>
     * This annotation attribute provides a whitelist (if not empty by default) to those
     * annotation discovered. This is motivated that a lot of build configurations gets very complex
     * with different build tools and IDEs, making it hard to solely use classpath management as a
     * way to configure auto-applied annotations.
     * <p>
     * In order to simply disable all such annotation auto-discovery, you can put some dummy
     * annotation like {@code java.lang.Override}, as in:
     *
     * <pre>
     * Style(allowedClasspathAnnotations = {java.lang.Override.class})
     * </pre>
     * <p>
     * The array will no longer be empty as by default so only specified entries will be applied.
     * Please note that standard {@code java.lang} annotations like {@link java.lang.Override},
     * {@link java.lang.Deprecated}, {@link java.lang.SuppressWarnings} will always be applied where
     * supported, so this configuration
     * have no effect on those. At the same time, {@code javax.annotation.*} or
     * {@code java.annotation.processing.*} are configurable by this style attribute. Another
     * exception is when some annotation is put on abstract value
     * type or attribute and then propagated to corresponding/related elements of generated
     * classes, these other mechanisms do not count as complementary annotation generation regulated
     * by this property.
     * <p>
     * Another way to inhibit some classpath entries from discovery is using
     * class-path fence using META-INF extension mechanism. That mechanism is a general classpath
     * discovery blacklist and overrides any whitelist allowed here. Add unwanted fully qualified
     * class name prefixes (for example, like full class names, or package names ending with a dot)
     * as the lines to the {@code META-INF/extensions/org.immutables.inhibit-classpath} resource
     * file available in classpath.
     * @return a non-empty array of only allowed auto-discovered annotations. Empty by
     *     default, which allows any auto-discovered annotation support for backward
     *     compatibility.
     */
    Class<? extends Annotation>[] allowedClasspathAnnotations() default {};

    /**
     * For many cases of nullable annotation is just copied to generated code when recognized (by simple name, see
     * {@link #nullableAnnotation()}). But for some cases we need to internally insert some nullable annotation when we
     * don't know any "original" annotation. By default, we assume that would be {@code javax.annotation.Nullable}
     * (if it is present on the classpath during compilation). When you set {@link #fallbackNullableAnnotation()}
     * to non-default value (default value is {@code java.lang.annotation.Inherited} which serves as a placeholder
     * for an unspecified value)
     * value, we would use that annotation in such cases.
     * <p><em>Note</em> that this annotation would always be on the compilation classpath (as it is specified as
     * class literal in a style annotation, but will not be otherwise validated as applicable and will be used verbatim
     * in all places where we ought to insert nullable annotation without the link to any "original" nullable
     * annotation in the handwritten code.
     * @return fallback nullable annotation to use. Default values is unspecified encoded as {@code Inherited.class}
     *     so that {@code javax.annotation.Nullable} annotation will be used if found on classpath.
     */
    Class<? extends Annotation> fallbackNullableAnnotation() default Inherited.class;

    /**
     * Setting to trim strings longer than a defined length when calling the toString method.
     * @return string limit, by default {@code 0} i.e. no limit
     */
    int limitStringLengthInToString() default 0;

    /**
     * If enabled, {@code jakarta.*} packages will take over any relevant {@code javax.*}.
     * This includes primarily {@code jakarta.annotation.*} and {@code jakarta.validation.*}.
     * <p>Note that classpath inhibitor or {@link #allowedClasspathAnnotations()} will still
     * take effect, it's just so that</p>
     * @return {@code true} if enabled. The default is {@code false}.
     */
    boolean jakarta() default false;

    /**
     * When set to {@code true}, processor will switch to legacy accessor (attribute)
     * source ordering traversal method. Prior to <em>Immutables</em> {@code v2.7.5},
     * the source ordering was defined be a "shallow-first" approach, where accessors
     * from the child class were added first, then the superclass, then the interfaces.
     * This changed in {@code v2.7.5}, where the order became interfaces, then superclass,
     * then child class, this is current default traversal order. In addition to a style
     * attribute, this behaviour can be also set via JVM boolean property (a {@code -D} one)
     * {@code org.immutables.useLegacyAccessorOrdering=true}
     * @return {@code true} if enabled. The default is {@code false}
     */
    boolean legacyAccessorOrdering() default false;

    /**
     * When set to {@code true}, processor will generate {@code toString()} method on builders,
     * which would be safe to call at any time. It will output values of the attributes set so far,
     * and will also list required attributes which are not set yet. Historically, we've tried that
     * our default generated code be compact, with only handful of conveniences,
     * so {@code toString()} on builders are only generated if this flag is turned on.
     * @return {@code true} if enabled. The default is {@code false}
     */
    boolean builderToString() default false;

    /**
     * Builder has {@link #from()} method generated which initializes builder values from an instance.
     * When abstract value type has some supertypes, an abstract class and or interfaces, from method can populate
     * values, even partially from an instances of such supertypes. Some release ago, we've started
     * to use runtime {@code instanceof} check when choosing which supertypes are implemented by a given instance
     * and some, arguably, complicated machinery with bit masks to initialize  attributes not more than once.
     * This change was motivated by the style of working when once would want to initialize values fully
     * from instances, regardless of "segregated" interfaces. In the cases where generics are used in supertype
     * attributes, and when instances happen to implement same interfaces but with different type arguments,
     * this dynamic approach can result in {@link ClassCastException} or heap pollution.
     *
     * <p>The other way is to just have copy logic in
     * statically resolved, i.e. at compile time overload, and copy/initialize only those properties which
     * are strictly defined by a supertype. When this {@code mergeFromSupertypesDynamically} style flag is
     * set to {@code false}, the generated code will switch to using simpler copy logic in compile-time resolved
     * overloads. The default is {@code true} to use {@code instanceof} checks and bit masks under the hood
     * to extract all attributes using all implemented supertypes.
     * @return {@code false} to disable. The default is {@code true}
     */
    boolean mergeFromSupertypesDynamically() default true;

    /**
     * If implementation visibility is more restrictive than visibility of abstract value type, then
     * implementation type will not be exposed as a return type of {@code build()} or {@code of()}
     * construction methods. Builder visibility will follow.
     */
    enum ImplementationVisibility {
      /**
       * Generated implementation class forced to be public.
       */
      PUBLIC,

      /**
       * Visibility is the same as abstract value type
       */
      SAME,

      /**
       * Visibility is the same, but it is not returned from build and factory method, instead
       * abstract value type returned.
       * @deprecated use combination with {@link Style#overshadowImplementation()}
       */
      @Deprecated
      SAME_NON_RETURNED,

      /**
       * Implementation will have package visibility
       */
      PACKAGE,

      /**
       * Allowed only when builder is enabled or nested inside enclosing type.
       * Builder visibility will follow the umbrella class visibility.
       */
      PRIVATE
    }

    enum BuilderVisibility {
      /**
       * Generated builder visibility is forced to be public.
       */
      PUBLIC,
      /**
       * Generated builder visibility is the same as abstract value type
       */
      SAME,
      /**
       * Generated builder visibility is forced to be package-private.
       */
      PACKAGE
    }

    enum ValidationMethod {
      /**
       * Disables null and mandatory attribute checks. Any missing primitives will be initialized to
       * their zero-based values: {@code false}, {@code 0}, {@code '\0'}. Object references will be
       * nulls. Any optional, default and collection attributes will be initialized with their
       * appropriate default values regardless of this validation setting.
       */
      NONE,
      /**
       * This validation method is similar to {@link #NONE} in that there are no null checks.
       * But all attributes which are not `@Default` or marked with `@Nullable` are still checked
       * to be provided (even with {@code null} values in case of object references.
       */
      MANDATORY_ONLY,
      /**
       * Simple validation, verifying that non-null attributes have been provided. This is classic
       * fail-fast, null-hostile behavior and works best in most cases.
       */
      SIMPLE,
      /**
       * Validation using Java Bean Validation API (JSR 303). It disables null checks, in favor or
       * {@literal @}{@code javax.validation.constraints.NotNull} and creates static validator per
       * objects. To better control the usage of JSR 303 Validator objects or enable fail-fast null
       * checks, please use custom validation mixin approach, where you create base abstract class
       * or interface with default methods to provide `@Value.Check` which would explicitly call
       * validation of your choice. Please see discussion and examples provided in the following
       * Github issue:
       * <a href="https://github.com/immutables/immutables/issues/26">immutables/immutables#26</a>
       */
      VALIDATION_API
    }

    /**
     * Enables depluralization and may provide depluralization dictionary via {@link #dictionary()} attribute.
     * The annotation which may be placed on a package, type or as meta-annotation. Full dictionary
     * will be merged across all applicable definitions.
     * @see Style#depluralize()
     */
    @Target({ElementType.TYPE, ElementType.PACKAGE, ElementType.ANNOTATION_TYPE})
    @interface Depluralize {
      /**
       * Depluralization dictionary.
       * @return array of "singular:plural" pairs.
       * @see Style#depluralizeDictionary()
       */
      String[] dictionary() default {};
    }
  }
}
