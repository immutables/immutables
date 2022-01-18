package org.immutable.fixture.annotate;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.immutables.annotate.InjectAnnotation;

@InjectAnnotation(
    type = OriginalMapping.class,
    target = InjectAnnotation.Where.FIELD,
    ifPresent = true)
@Retention(RetentionPolicy.SOURCE)
@Target({ElementType.TYPE, ElementType.METHOD})
public @interface InjectPresentMapping {
}
