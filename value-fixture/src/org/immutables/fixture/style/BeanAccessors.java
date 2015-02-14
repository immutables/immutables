package org.immutables.fixture.style;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.immutables.value.Value;

/**
 * Annotations that applies speculative Java Bean-style accessor naming convention
 * to the generate immutable and other derived classes (builders).
 * It works by being annotated with {@literal @}{@link Value.Style} annotation which specifies
 * customized naming templates. This annotation could be placed on a class, surrounding
 * {@link Value.Nested} class or even a package (declared in {@code package-info.java}). This
 * annotation more of example of how to define your own styles as meta-annotation rather than a
 * useful annotation.
 */
@Value.Style(get = {"is*", "get*"}, init = "set*")
@Target({ElementType.TYPE, ElementType.PACKAGE, ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.SOURCE)
public @interface BeanAccessors {}
