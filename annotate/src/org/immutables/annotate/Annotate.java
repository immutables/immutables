package org.immutables.annotate;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;

/**
 * Meta-annotation that if detected on the annotation, will turn target annotation into special
 * instruction to inject derived (described) annotation code into target places.
 */
@Documented
@java.lang.annotation.Target(ElementType.ANNOTATION_TYPE)
public @interface Annotate {
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
   * attributes from the annotated annotation (the one which is annotated by {@link Annotate} and
   * placed on abstract value type or abstract attribute.
   * @see #code()
   */
  Class<?> type() default Void.class;

  Where[] target();

  /** Logical elements of generated code to which annotations can be applied to. */
  enum Where {
    FIELD,
    ACCESSOR,
    CONSTRUCTOR_PARAMETER,
    INITIALIZER,
    ELEMENT_INITIALIZER,
    BUILDER_TYPE,
    IMMUTABLE_TYPE
  }
}
