package org.immutables.annotate;

import java.lang.annotation.*;

/**
 * Meta-annotation that if detected on the annotation, will turn target annotation into special
 * instruction to inject derived annotation code into target places.
 */
@Documented
@Target(ElementType.ANNOTATION_TYPE)
public @interface InjectAnnotation {
  /**
   * Used to specify whole source code for the annotation. Can specify whole annotation(s) code or
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
   * {@link InjectAnnotation} and placed on abstract value type or abstract attribute.
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
   * @return {@code true} if annotation insertion is triggered by the presense of the same
   *         annotation on a model element.
   */
  boolean ifPresent() default false;

  /**
   * The places where to put generated annotation. If annotation type have been spefied by
   * {@link #type()} (and is not overriden by #code()), then there will be element target check,
   * otherwise (if fully specified by {@link #code()} annotation will be always placed and it's
   * better match to target element type.
   */
  Where[] target();

  /**
   * Unique key is used when there's a need to prevent putting multiple conflicting annotations on
   * the element if it's covered by injection annotations. Putting it straight: when traversing all
   * injection annotations (or as meta-annotations) which covers the element in question, starting
   * from most specific to least specific, if there already was annotation injected by the some key,
   * the following annotations by the same key will be discarded.
   * if not specified explicitly (empty string in the annotation attribute) the key will be
   * auto-inferred as {@code type()} if specifed or
   * string
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
    IMMUTABLE_TYPE
  }
}
