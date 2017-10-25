package org.immutables.annotate;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;

/**
 * Meta-annotation used to inject/print annotation make the annotation instructiona
 */
@Documented
@java.lang.annotation.Target(ElementType.ANNOTATION_TYPE)
public @interface Annotate {
  /**
   * Used to specify whole source code for the annotation.
   * <p>
   * <em>Only one of {@link #code()} or {@link #type()} should be
   * specified, not both</em>
   */
  String code() default "";

  /**
   * Can specify whole annotation.
   * <p>
   * <em>Only one of {@link #code()} or {@link #type()} should be
   * specified, not both</em>
   */
  Class<?> type() default Void.class;

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
