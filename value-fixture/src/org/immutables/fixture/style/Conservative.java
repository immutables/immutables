package org.immutables.fixture.style;

import java.lang.annotation.Retention;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.immutables.value.Value;

@Value.Style(get = {"is*", "get*"},
    init = "set*",
    of = "new*",
    instance = "getInstance",
    builder = "new",
    visibility = Value.Style.ImplementationVisibility.PRIVATE)
@Target(value = {
    ElementType.TYPE,
    ElementType.PACKAGE,
    ElementType.ANNOTATION_TYPE})
@Retention(RetentionPolicy.SOURCE)
public @interface Conservative {

}
