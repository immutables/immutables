/*
    Copyright 2014 Immutables Authors and Contributors

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
package org.immutables.value.processor.meta;

import com.google.common.annotations.Beta;
import org.immutables.mirror.Mirror;

public final class ValueMirrors {
  private ValueMirrors() {}

  @Mirror.Annotation("org.immutables.value.Value.Immutable")
  public @interface Immutable {

    /**
     * If {@code singleton=true}, generates internal singleton object constructed without any
     * specified parameters. Default is {@literal false}.
     */
    boolean singleton() default false;

    /**
     * If {@code intern=true} then instances will be strong interned on construction.
     * Default is {@literal false}.
     * @see com.google.common.collect.Interners#newStrongInterner()
     */
    boolean intern() default false;

    /**
     * If {@code copy=false} then generation of copying methods will be disabled.
     * This appies to static "copyOf" methods as well as modify-by-copy "withAttributeName" methods.
     * Default is {@literal false}, i.e do not generate copy methods.
     */
    boolean copy() default false;

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

  @Mirror.Annotation("org.immutables.value.Value.Include")
  public @interface Include {
    Class<?>[] value();
  }

  @Mirror.Annotation("org.immutables.value.Value.Nested")
  public @interface Nested {}

  @Mirror.Annotation("org.immutables.value.Value.Builder")
  public @interface Builder {}

  @Mirror.Annotation("org.immutables.value.Value.Derived")
  public @interface Derived {}

  @Mirror.Annotation("org.immutables.value.Value.Default")
  public @interface Default {}

  @Mirror.Annotation("org.immutables.value.Value.Auxiliary")
  public @interface Auxiliary {}

  @Mirror.Annotation("org.immutables.value.Value.Lazy")
  public @interface Lazy {}

  @Mirror.Annotation("org.immutables.value.Value.Parameter")
  public @interface Parameter {
    int order() default 0;
  }

  @Mirror.Annotation("org.immutables.value.Value.Check")
  public @interface Check {}

  @Mirror.Annotation("org.immutables.value.Value.NaturalOrder")
  public @interface NaturalOrder {}

  @Mirror.Annotation("org.immutables.value.Value.ReverseOrder")
  public @interface ReverseOrder {}

  @Mirror.Annotation("org.immutables.value.Value.Modifiable")
  public @interface Modifiable {}

  @Mirror.Annotation("org.immutables.value.Value.Style")
  public @interface Style {
    /**
     * Patterns to recognize accessors.
     * @return naming template
     */
    String[] get() default {};

    /**
     * Builder initialization method. i.e. "setter" in builder.
     * @return naming template
     */
    String init() default "*";

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
     * Modifiable type name template.
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
     * Specify the mode in which accibility visibility is derived from abstract value type.
     * It is a good idea to not specify such attributea inline with immutable values, but rather
     * create style annotation (@see Style).
     * @return implementation visibility
     */
    @Beta
    ImplementationVisibility visibility() default ImplementationVisibility.SAME;

    /**
     * When {@code true} &mdash; forces to generate code which use only JDK 7+ standard library
     * classes. It is {@code false} by default, however usage of JDK-only classes will be turned on
     * automatically if <em>Google Guava</em> library is not found in classpath. The generated code
     * will have subtle differences, but nevertheless will be functionally equivalent.
     * <p>
     * <em>Note that some additional annotation processors may not work without
     * Guava being accessible to the generated classes</em>
     * @return if forced JDK-only class usage
     */
    boolean jdkOnly() default false;

    /**
     * If implementation visibility is more restrictive than visibility of abstract value type, then
     * implementation type will not be exposed as a return type of {@code build()} or {@code of()}
     * constructon methods. Builder visibility will follow.
     */
    @Beta
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
