package org.immutables.fixture.nullable.typeuse;

import org.immutables.value.Value;
import org.immutables.value.Value.Style.ValidationMethod;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

/**
 * Reproduces immutables/immutables#1665: under {@code @NullMarked} (JSpecify) combined with
 * {@code validationMethod = MANDATORY_ONLY}, mandatory reference attributes get an internal
 * "synthetic" nullability which must NOT leak onto the generated field/accessor as a type-use
 * {@code @Nullable}. Only attributes explicitly marked {@code @Nullable} should be nullable.
 */
@NullMarked
@Value.Immutable
@Value.Style(
    jdkOnly = true,
    defaultAsDefault = true,
    get = {"get*", "is*"},
    nullableAnnotation = "org.jspecify.annotations.Nullable",
    validationMethod = ValidationMethod.MANDATORY_ONLY)
public interface MandatoryOnlyNullMarked {
  /** Mandatory attribute: must never be annotated {@code @Nullable} in generated code. */
  String getMandatory();

  /** Explicitly nullable attribute: must be annotated {@code @Nullable} in generated code. */
  @Nullable
  String getOptional();
}
