/*
   Copyright 2014-2018 Immutables Authors and Contributors

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
  public @interface Immutable {

    /**
     * If {@code singleton=true}, generates internal singleton object constructed without any
     * specified parameters. Default is {@literal false}. To access singleton instance use
     * {@code .of()} static accessor method.
     * <p>
     * This requires that all attributes have default value (including collections which can be left
     * empty). If some required attributes exist it will result in compilation error. Note that in
     * case object do not have attributes, singleton instance will be generated automatically.
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
     * This appies to static "copyOf" methods as well as modify-by-copy "withAttributeName" methods
     * which returns modified copy using structural sharing where possible.
     * Default value is {@literal true}, i.e generate copy methods.
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
     * @return if generate hash code precomputing
     */
    boolean prehash() default false;

    /**
     * If {@code builder=false}, disables generation of {@code builder()}. Default is
     * {@literal true}.
     * @return if generate builder
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
  public @interface Include {
    Class<?>[] value();
  }

  /**
   * This annotation could be applied to top level class which contains nested abstract
   * value types to provide namespacing for the generated implementation classes.
   * Immutable implementation classes will be generated as classes enclosed into special "umbrella"
   * top level class, essentialy named after annotated class with "Immutable" prefix (prefix could
   * be customized using {@link Style#typeImmutableEnclosing()}). This could mix
   * with {@link Value.Immutable} annotation, so immutable implementation class will contains
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
  public @interface Enclosing {}

  /**
   * This kind of attribute cannot be set during building, but they are eagerly computed from other
   * attributes and stored in field. Should be applied to non-abstract method - attribute value
   * initializer.
   */
  @Documented
  @Target(ElementType.METHOD)
  public @interface Derived {}

  /**
   * Annotates accessor that should be turned in set-able generated attribute. However, it is
   * non-mandatory to set it via builder. Default value will be assigned to attribute if none
   * supplied, this value will be obtained by calling method annotated this annotation.
   */
  @Documented
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
  @Target(ElementType.METHOD)
  public @interface Auxiliary {}

  /**
   * Lazy attributes cannot be set, defined as method that computes value, which is invoke lazily
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
  public @interface Lazy {}

  /**
   * Works with {@link Value.Immutable} classes to mark abstract accessor method be included as
   * "{@code of(..)}" constructor parameter.
   * <p>
   * Following rules applies:
   * <ul>
   * <li>No constructor generated if none of methods have {@link Value.Parameter} annotation</li>
   * <li>For object to be constructible with a constructor - all non-default and non-derived
   * attributes should be annotated with {@link Value.Parameter}.
   * </ul>
   */
  @Documented
  @Target({ElementType.METHOD, ElementType.PARAMETER})
  public @interface Parameter {
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
     * behind should not result in any practical incompatibilites.
     * </em>
     * @return order
     */
    int order() default -1;

    /**
     * Specify as {@code false} to cancel out parameter: an attribute would not be considered as a
     * parameter. This is useful to override the effect of {@link Style#allParameters()} flag.
     * By default it is {@code true} and should be omited.
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
  public @interface ReverseOrder {}

  /**
   * Generate modifiable implementation of abstract value class. Modifiable implementation class
   * might be useful when you either need overflexible builder or, alternatively, partially built
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
  public @interface Modifiable {}

  /**
   * Marks attribute for exclusion from auto-generated {@code toString} method. It will
   * be just excluded by default. However you can choose to put special masking characters next to
   * the attribute instead of value, like 3 stars or 4 pound signs, this replacement string
   * can be set using {@link Style#redactedMask()} style attribute.
   */
  @Documented
  @Target(ElementType.METHOD)
  public @interface Redacted {}

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
  public @interface Style {
    /**
     * Patterns to recognize accessors. For example <code>get = {"is*", "get*"}</code> will
     * mimick style of bean getters. If none specified or if none matches, then raw accessor name
     * will be taken literally.
     * <p>
     * By default only {@code get*} prefix is recognized, along with falling back to use accessor
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
     * @see #init()
     * @see #set()
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
     * <p>
     * Since version {@code 2.1.5} you can also use "new" template string to generate public
     * constructor instead of factory. The public constructor functionality is experimentatal. Note
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
     * @see #newBuilder()
     * @see #build()
     * @return naming template
     */
    String builder() default "builder";

    /**
     * Builder creator method, it differes from {@link #builder()} in that this naming is used for
     * builders that are external to immutable objects, such as top level builders for values or
     * factories. This naming allow special keyword "new" value, which is the default.
     * "new" will customize builder to be created using constructor rather than
     * factory method.
     * @see #build()
     * @return naming template
     */
    String newBuilder() default "new";

    /**
     * Method to initialize builder with values from instance.
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
     * Generation of build or throws method requires presense of a function type on the classpath,
     * provided either by Java 8 or Guava on Java 7. If used on java 7 without Guava, this style
     * will have no effect: no method will be generated.
     * <p>
     * <em>Note: This attribute-style is experimental and may be changed in near releases.
     * You should not rely on it if don't ready to change code on minor updates of the annotation
     * processor</em>
     * @return naming template
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
     * @return naming template
     */
    String canBuild() default "";

    /**
     * Method to determine if all required attributes are set.
     * Default method name choice for this is mostly random.
     * @return naming template
     */
    String isInitialized() default "isInitialized";

    /**
     * Method to determine if attribute is set
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
     * @see #typeImmutable()
     * @see #typeImmutableEnclosing()
     * @return naming template
     */
    String typeImmutableNested() default "*";

    /**
     * Modifiable companion class name template
     * @return naming template
     */
    String typeModifiable() default "Modifiable*";

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
     * <em>Note that some additional annotation processors (for example mongo repository generator)
     * may not work without Guava being accessible to the generated classes,
     * and thus will not honor this attribute</em>
     * @return if forced JDK-only class usage
     */
    boolean jdkOnly() default false;

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
     * When {@code true} @mdash; disables check that all required attributes have been provided to a
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
     * 
     * @return if all attributes will be considered parameters
     */
    boolean allParameters() default false;

    /**
     * This funny-named named attribute, when enabled makes default accessor methods defined in
     * interfaces/traits to behave as if they annotated as {@literal @}{@link Value.Default}.
     * This is not a default behaviour to preserve compatibility and also to have an choice to not
     * opt-in for this new functionality when not needed.
     * @return if consider default method accessors as {@literal @}{@code Value.Default}
     */
    boolean defaultAsDefault() default false;

    /**
     * Enable if you needed to copy header comments from an originating source file with abstract
     * types to generated (derived) implementation classes. Header comments are comments preceeding
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
     * for {@code passAnnotations} attribute. Howerver there are some special annotations which are
     * copied using special logic, such as {@code Nullable} annotations (and Jackson annotations)
     * <p>
     * This style parameter is experimental and may change in future.
     * @return types of annotations to pass to an immutable implementation class and its
     *         attributes.
     */
    Class<? extends Annotation>[] passAnnotations() default {};

    /**
     * List of additional annotations to pass through for any jackson json object
     * @Deprecated this is no longer necessary.
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
     * Specify whether init and copy methods for an unwrapped {@code X} of {@code Optional<X>}
     * should accept {@code null} values as empty value. By default nulls are rejected in favor of
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
     * Generate a default no argument constructor in generated code. Note that this property will
     * be ignored if {@link Immutable#singleton()} returns <code>true</code>.
     * @return {@code true} if will generate a default no argument constructor, disabled by default.
     */
    boolean privateNoargConstructor() default false;

    /**
     * Enabling {@code attributelessSingleton} switches to old behavior of 2.0.x version when
     * immutables wich had no attributes defined acted as .
     * As of 2.1 we are more strict and explicit with singletons and are not generating it by
     * default, only when {@link Immutable#singleton()} is explicitly enabled.
     * @return {@code true} no attribute immutables will be auto-singletons.
     */
    boolean attributelessSingleton() default false;

    /**
     * As of 2.1 we are switching to safe generation of derived and default values when there are
     * more than one such attribute. This {@code unsafeDefaultAndDerived} style could be enabled to
     * revert to old, unsafe behavior.
     * <p>
     * In order to initialize default and derived attributes method bodies (initializers) will be
     * invoked. Intializers could refer to other attributes, some of which might be also derived or
     * uninitialized default values. As it's extremely difficult to reliably inspect initializer
     * methods bodies and compute proper ordering, we use some special generated code which figures
     * it out in runtime. If there will be a cycle in initializers, then
     * {@link IllegalStateException} will be thrown.
     * <p>
     * If you set {@code unsafeDefaultAndDerived} to {@code true}, then simpler, unsafe code will be
     * generated. With usafe code you cannot refer to other default or derived attributes in
     * initializers as otherwise result will be undefined as order of initialization is not
     * guaranteed.
     * @return {@code true} if old unsafe (but potentially with less overhead) generation should be
     *         used.
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
     * @see #clear()
     * @return {@code true} if clean method would be generated.
     */
    boolean clearBuilder() default false;

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
     * Effectively this is a shortcut to initialize value object in a more consice way. This works
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
     *         abstract type, rather than work with immutable implementation class.
     */
    boolean overshadowImplementation() default false;

    /**
     * By default builder is generated as inner builder class nested in immutable value class.
     * Setting this to {@code true} will flip the picture — immutable implementation class will be
     * nested inside builder, which will be top level class. In case if {@link #visibility()} is set
     * to {@link ImplementationVisibility#PRIVATE} this feature is turned on automatically.
     * @return {@code true} if builder should be generated as top level class and implementation
     *         will became static inner class inside builder.
     */
    boolean implementationNestedInBuilder() default false;

    /**
     * As there are some ambiguities with how certain field prefixes work ("get", "is") we by
     * default force whatever is inferred by Immutables. Disable forcing Jackson property names if
     * you use naming strategies. Also make sure you recognize both "get*" and "is*" as attribute
     * name patterns as Jackson infers default names using JavaBean convention.
     * @return {@code true} if force jackson property names. default it {@code true}, set
     *         {@code false} to disable
     */
    boolean forceJacksonPropertyNames() default true;

    /**
     * @return if put {@code JsonIngore} on fields. default {@code false}
     */
    boolean forceJacksonIgnoreFields() default false;

    /**
     * Setting this to {@code false} would disable any special jackson integration capabilities.
     * While out-of-the-box Jackson readiness is a good things in the most cases, for some cases
     * it might get in the way of highly customized Jackson infrastructure. When disabled, there are
     * no any special stuff generated such as {@code JsonProperty} annotations or internal
     * {@code Json} delegate class together with {@code JsonCreator} method. This allows to place
     * {@code JsonSerialialize/JsonDeserialialize} annotations on the value types without redundand
     * support code being generated.
     * @return {@code true} if generate special Jackson code when encountered
     *         {@code JsonSerialialize/JsonDeserialialize}. Default is {@code true}.
     */
    boolean jacksonIntegration() default true;

    /**
     * When {@code weakInterning} is enabled, then for value types with
     * {@literal @}{@code Value.Immutable(intern=true)} weak (see {@link WeakReference})
     * interning will be used.
     * @return {@code true} if enable weak interning for {@code intern=true} values,
     *         defaults to {@code false}
     */
    boolean weakInterning() default false;

    /**
     * Specify the mode in which visibility of generated value type is derived from abstract value
     * type. It is a good idea to not specify such attributes inline with immutable values, but
     * rather create style annotation (@see Style).
     * @return implementation visibility
     */
    BuilderVisibility builderVisibility() default BuilderVisibility.PUBLIC;

    /**
     * Runtime exception to throw when an immutable object is in an invalid state. I.e. when some
     * mandatory attributes are missing and immutable object cannot be built. The runtime exception
     * class must have a constructor that takes a single string, otherwise there will be compile
     * error in the generated code.
     * <p>
     * The default exception type is {@link IllegalStateException}. In case if
     * specified exception type have public constructor taking array of strings (can be varargs),
     * then missing parameter names will be passed to that constructor. Otherwise, string
     * constructor is always expected to be present to take formatted error message. It is always
     * advisable have string constructor even in the presense of attribute names array constructor
     * as some additional generators might use string constuctor for reporting other invalid state
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
     * Runtime exception to throw when null reference is passed to non-nullable parameter or occured
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
     *
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
     * Instead
     * @see Depluralize
     * @return {@code true} if depluralization enabled.
     */
    boolean depluralize() default false;

    /**
     * Dictionary of exceptions — array of "singular:plural" pairs as alternative to mechanical "*s"
     * depluralization. Suppress trimming of trailing "s" for certain words by using exceptions of
     * form {@code "words:words"} or simply {@code "words"}. Important to note is that words will be
     * converted to lowercase and identifier in question consists of couple of words joined using
     * camel case — only a last segment will be considered for depluralization when matching
     * dictionary. Uninterpretable pairs will be ignored. By default no dictionary is supplied and
     * depluralization performed only by mechanical "*s" trimming.
     * <p>
     * This attribute is semi-deprecated in favor of using {@link Depluralize#dictionary()}
     * annotation which may be placed on a package, type or as meta-annotation. And dictionary will
     * be merged accross all applicable definitions.
     * @see #depluralize()
     * @see Depluralize#dictionary()
     * @return array of "singular:plural" pairs.
     */
    String[] depluralizeDictionary() default {};

    /**
     * You can provide classes which must contain copyOf method with relevant overloads which should
     * not have unambigous cases as it will be fully as subject to JLS rules of static imports and
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
     *         will be generated along with corresponding invokations of {@code immutableCopyOf}
     *         method when accepting parameters.
     */
    Class<?>[] immutableCopyOfRoutines() default {};

    /**
     * Staged (aka telescopic) builders are a special flavor of builders which provides compile-time
     * safety to a staged building. Providing proposals to insert mandatory attributes one by one,
     * and then allowing adding any optional or collection attributes in any order before calling
     * the build method. This option also implies {@link #strictBuilder()} is automatically enabled.
     * <p>
     * <em>Note: this functionality is experimental and may not play well with other functionality.
     * </em>
     * @return if telescopic builders are used, {@code false} by default
     */
    boolean stagedBuilder() default false;

    /**
     * Setting {@code builtinContainerAttributes} to {@code false} would disable generation of
     * built-in convenience features of automatically recongized container types such as
     * {@link Optional}, {@link List}, {@link Map}. This will turn all attribute types into nothing
     * special setters(initializers) and getters. However any registered encodings (type
     * customizers) will be still processed. One of the purposes of this style control is to provide
     * clean-slate when only registered encodings will impact type generation, but none of the
     * built-in types would be applied. Note: that this style controls recognition of the
     * attribute types, but not kind of attributes such as those specified by {@code Value.Default}
     * or {@code Nullable} annotations.
     * @return {@code true} if builtin container attributes should be supported. {@code true} is the
     *         default
     */
    boolean builtinContainerAttributes() default true;

    /**
     * If enabled modifable type will have void setters and will look more like JavaBean. This is
     * modifiable companion types only, not for builders and other types of generated artifacts.
     * <p>
     * <em>Note, we are not supporting JavaBean specification in any way except that Immutables can
     * be used/configured to be partially compatible with some of the conventions.</em>
     * </p>
     * @return {@code true} for void setters and minor tweaks to make modifiables more
     *         bean-friendly. {@code false} is the default
     */
    boolean beanFriendlyModifiables() default false;

    /**
     * If enabled mandatory attributes would be auto-propagated to be parameters of value object
     * constuctor.
     * <p>
     * <em>This parameter conflicts with {@link #allParameters()} and is ignored when
     * {@code allParameters} is enabled</em>
     * @return {@code true} to turn mandatory attributes into parameters. {@code false} is the
     *         default
     */
    boolean allMandatoryParameters() default false;

    /**
     * String to substitute value of the attribute in a generated {@code toString} implementation
     * when {@link Redacted} annotation is applied to the attribute.
     * <p>
     * By default it is an empty string, which also mean that the attribute will not appear in the
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
     * This style parameter is experimental and may change in future.
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
    String[] attributeBuilder() default {"*Builder", "builder", "new"};

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
     * 
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
     *         default, which allows any auto-discovered annotation support for backward
     *         compatibility.
     */
    Class<? extends Annotation>[] allowedClasspathAnnotations() default {};

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

    public enum BuilderVisibility {
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

    public enum ValidationMethod {
      /**
       * Disables null and mandatory attribute checks. Any missing primitives will be initialized to
       * their zero-based values: {@code false}, {@code 0}, {@code '\0'}. Object references will be
       * nulls. Any optional, default and collection attributes will be initialized with their
       * appropriate default values regardless of this validation setting.
       */
      NONE,
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
       * or interface with default methods to provide `@Value.Check` which would explictly call
       * validation of your choice. Please see discussion and examples provided in the following
       * github issue:
       * <a href="https://github.com/immutables/immutables/issues/26">immutables/immutables#26</a>
       */
      VALIDATION_API
    }

    /**
     * Enables depluratization and may provide depluralization dictionary.
     * The annotation which may be placed on a package, type or as meta-annotation. And dictionary
     * will be merged accross all applicable definitions.
     * @see Style#depluralize()
     */
    @Target({ElementType.TYPE, ElementType.PACKAGE, ElementType.ANNOTATION_TYPE})
    public @interface Depluralize {
      /**
       * Depluralization dictionary.
       * @see Style#depluralizeDictionary()
       * @return array of "singular:plural" pairs.
       */
      String[] dictionary() default {};
    }
  }
}
