/*
   Copyright 2018 Immutables Authors and Contributors

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
package org.immutables.annotate;

import java.lang.annotation.*;

/**
 * Meta-annotation that if detected on the annotation, will turn target annotation into special
 * instruction to inject derived annotation code into target places. This annotated annotations -
 * directives are called <em>injection annotations</em>, the derived annotation's code to be
 * inserted is called <em>target annotation</em>. The injection annotations
 * themselves are to be placed on attributes, abstract value types or packages, impacting all
 * covered types and attributes for injection of target annotations (see {@link #type()},
 * {@link #code()}) into specified placed (see {@link Where}) of the generated classes.
 * <h5>Examples</h5>
 * 1) Inject Deprecated into fields "a" and "b" of the generated class
 * 
 * <pre>
 * &#64;InjectAnnotation(type = Deprecated.class, target = Where.FIELD)
 * &#64;interface InjectDeprecated {}
 * 
 * &#64;Value.Immutable
 * &#64;InjectDeprecated
 * interface Val {
 *   int a();
 * 
 *   int b();
 * }
 * </pre>
 * 
 * 2) Inject @TargetAnn(message="I'm BUILDER", of="Val2") onto generated ImmutableVal2.Builder
 * 
 * <pre>
 * &#64;interface TargetAnn {
 *   String message();
 * 
 *   String of();
 * }
 * 
 * &#64;InjectAnnotation(type = TargetAnn.class, code = "(message=[[echo]], of=\"[[!name]]\")", target = Where.BUILDER_TYPE)
 * &#64;interface InjectBuilderTarget {
 *   String echo();
 * }
 * 
 * &#64;Value.Immutable
 * &#64;InjectBuilderTarget(echo = "I'm BUILDER")
 * interface Val2 {
 *   int a();
 * }
 * </pre>
 * 
 * 3) Inject Point(x = 2, y = 3) on field "a" and Point(x = 4, y = 5) on field "b"
 * 
 * <pre>
 * &#64;interface Point {
 *   int x();
 * 
 *   int y();
 * }
 * 
 * &#64;InjectAnnotation(code = "@Point([[*]])", target = Where.FIELD)
 * // watch-out for the relative or qualified name when putting annotation name into code attribute
 * // that would be inserted and resolved from a generated code. Putting FQCN is recommended.
 * &#64;interface PointInject {
 *   int x();
 * 
 *   int y();
 * }
 * 
 * &#64;Value.Immutable
 * interface Val2 {
 *   &#64;PointInject(x = 2, y = 3)
 *   String a();
 * 
 *   &#64;PointInject(x = 4, y = 5)
 *   String b();
 * }
 * </pre>
 */
@Documented
@Target(ElementType.ANNOTATION_TYPE)
@Repeatable(InjectManyAnnotations.class)
public @interface InjectAnnotation {
  /**
   * Used to specify whole source code for the annotation. Can specify whole target annotation(s)
   * code or just attributes in parentheses and the {@code type} attribute will be used for the
   * annotation fully qualified name.
   * Special symbols can be used to further refine code of the annotation:
   * <ul>
   * <li>{@code [[*]]} inserts all attributes of defining annotation (the one which is annotated
   * with {@code InjectAnnotation}) into target annotation code. Obviously those should match: be
   * source-compatible. These will fully format attribute and literals, including commas between,
   * but excluding any surrounding parentheses, so they can be mixed to non-overlapping hardcoded
   * attributes.
   * <li>{@code [[!name]]} inserts the simple name of the target attribute (or type) into annotation
   * code, insertions are literal, without any quotes etc.
   * <li>{@code [[*names]]} inserts the simple names of all attributes defined by abstract value
   * type as as comma separated array initializer of quoted string literals. {@code {"a", "b", "c"}}
   * <li>{@code [[}<em>attr_name</em>{@code ]]} inserts source formatted value of injection
   * annotation into code, where <em>attr_name</em> is one of the name of injection annotation
   * attributes.
   * </ul>
   * <p>
   * <em>If code includes {@literal @} symbol at the beginning, {@link #type()} would be ignored, if
   * code does not includes annotation start symbol and {@link #type()} specified, then annotation
   * symbol and type name would be prepended to the {@link #code()}, so code essentially can be used
   * to override set of annotation attributes.</em>
   */
  String code() default "";

  /**
   * Specify annotation type, this is an alternative to specifying {@link #code()}. All the
   * attributes from the annotated annotation (the one which is annotated by
   * {@link InjectAnnotation}) are placed on abstract value type or abstract attribute.
   * Default value is {@code InjectAnnotation.class} which is just a placeholder for unspecified
   * value.
   * @see #code()
   */
  Class<? extends Annotation> type() default InjectAnnotation.class;

  /**
   * Enables special behavior when annotation is injected to {@link #target()} only if
   * {@link #type()} is set and corresponding model element have the same annotation. {@link #code}
   * expansion will still work and can override annotation type (if starts with full annotation
   * definition).
   * @return {@code true} if annotation insertion is triggered by the presence of the same
   *         annotation on a model element.
   */
  boolean ifPresent() default false;

  /**
   * The places where to put generated annotation. If annotation type have been specified by
   * {@link #type()} (and is not overridden by #code()), then there will be element target check,
   * otherwise (if fully specified by {@link #code()}) annotation will be always placed and it's
   * better match to target element type.
   */
  Where[] target();

  /**
   * Unique key is used when there's a need to prevent putting multiple conflicting annotations on
   * the element if it's covered by injection annotations. Putting it straight: when traversing all
   * injection annotations (or as meta-annotations) which covers the element in question, starting
   * from most specific to least specific, if there already was annotation injected by some key,
   * the following annotations by the same key will be discarded.
   * if not specified explicitly (empty string in the annotation attribute) the key will be
   * auto-inferred as {@code type()} if specified or from code template string
   */
  String deduplicationKey() default "";

  /** Logical elements of generated code to which annotations can be applied to. */
  enum Where {
    FIELD,
    ACCESSOR,
    SYNTHETIC_FIELDS,
    CONSTRUCTOR_PARAMETER,
    INITIALIZER,
    ELEMENT_INITIALIZER,
    BUILDER_TYPE,
    IMMUTABLE_TYPE,
    MODIFIABLE_TYPE,
    /** Constructor if public or a factory method constructor otherwise.  */
    CONSTRUCTOR,
  }
}
