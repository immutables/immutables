package org.immutables.fixture.nullable;

import org.immutables.value.Value;

@Value.Immutable
@Value.Style(nullableAnnotation = "CheckForNull")
public interface CustomNullableAnnotation {

    @CheckForNull
    String string1();

    String string2();
}
