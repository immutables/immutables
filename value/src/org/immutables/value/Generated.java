package org.immutables.value;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.immutables.value.Value.Style;

/**
 * Annotation to mark generated classes. This annotations was introduced as class and
 * runtime retained annotation is often needed to differentiate to include exclude generated
 * classes when processing class files or by using reflection. Can be disabled by
 * {@link Style#allowedClasspathAnnotations()}.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Generated {
  /**
   * Used to include reference (usually package relative) to class which was an annotated model
   * from which this class was generated from.
   * @return relative class name (can be package name or method reference).
   */
  String from() default "";

  /**
   * Symbolic name of generator/template used to emit file.
   * @return name of generator
   */
  String generator() default "";
}
