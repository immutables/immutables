package org.immutable.fixture.annotate;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.immutables.annotate.InjectAnnotation;

@InjectAnnotation(
    type = OriginalMapping.class,
    target = InjectAnnotation.Where.FIELD,
    code = "([[*]])")
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface InjectMappingHere {
  Class<?> target();
}
