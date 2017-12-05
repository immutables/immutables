package org.immutables.fixture.annotation;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.google.errorprone.annotations.Immutable;
import org.immutables.value.Value;

/**
 * An abstract type which triggers the generation of an immutable type with hidden mutable state.
 *
 * <ul>
 * <li>{@code @JsonDeserialize} causes a private secondary mutable subclass to be generated.
 * <li>{@code @Value.Default} causes the temporary storage of an {@code InitShim} in a field.
 * <li>{@code @Value.Lazy} causes the annotated property to be computed lazily, requiring it to be
 *     stored in a non-final field. Its initialization state is tracked using a mutable bitmap.
 * </ul>
 *
 * <p>This type is annotated with Error Prone's {@code @Immutable} annotation. The Error Prone
 * checker would normally complain about the mutable constructs enumerated above. The generated
 * code must thus take care to suppress such complaints.
 */
@Immutable
@JsonDeserialize
@Value.Immutable
@Value.Style(passAnnotations = Immutable.class)
abstract class AbstractDeeplyImmutable {
    public abstract String getA();

    @Value.Default
    public String getB() {
        return "";
    }

    @Value.Lazy
    public String getC() {
        return "";
    }
}
