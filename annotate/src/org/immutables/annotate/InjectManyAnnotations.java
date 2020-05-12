package org.immutables.annotate;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Allows many injections annotations to be specified at the same time.
 */
@Documented
@Target(ElementType.ANNOTATION_TYPE)
public @interface InjectManyAnnotations {
	InjectAnnotation[] value();
}
