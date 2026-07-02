package org.immutables.fixture.nullable;

import java.util.Map;
import org.immutables.fixture.nullable.typeuse.CusNull;
import org.immutables.fixture.nullable.typeuse.ImmutableChildOverrides;
import org.immutables.fixture.nullable.typeuse.ImmutableFirstChild;
import org.immutables.fixture.nullable.typeuse.ImmutableLetsTryJSpecify;
import org.immutables.fixture.nullable.typeuse.ImmutableMandatoryOnlyNullMarked;
import org.immutables.fixture.nullable.typeuse.ImmutableMyField;
import org.immutables.fixture.nullable.typeuse.ImmutableSecondChild;
import org.immutables.fixture.nullable.typeuse.ImmutableTryCustomNullann;
import org.immutables.fixture.nullable.typeuse.NullableArrays;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;
import static org.immutables.check.Checkers.check;

public class TypeuseAnnotationTest {
  @Test void jspecifyNullable() throws Exception {
    check(ImmutableLetsTryJSpecify.class.getDeclaredField("opt")
        .getAnnotatedType().getAnnotation(Nullable.class)).notNull();

    check(ImmutableLetsTryJSpecify.class.getDeclaredField("lst")
        .getAnnotatedType().getAnnotation(Nullable.class)).notNull();

    check(ImmutableLetsTryJSpecify.class.getDeclaredMethod("lst")
        .getAnnotatedReturnType().getAnnotation(Nullable.class)).notNull();
  }

  @Test void customTypeuseNullable() throws Exception {
    check(ImmutableTryCustomNullann.class.getDeclaredField("opt")
        .getAnnotatedType().getAnnotation(CusNull.class)).notNull();

    check(ImmutableTryCustomNullann.class.getDeclaredField("lst")
        .getAnnotatedType().getAnnotation(CusNull.class)).notNull();

    check(ImmutableTryCustomNullann.class.getDeclaredMethod("lst")
        .getAnnotatedReturnType().getAnnotation(CusNull.class)).notNull();

    check(ImmutableTryCustomNullann.class.getDeclaredMethod("withLst", Iterable.class)
        .getParameters()[0]
        .getAnnotatedType()
        .getAnnotation(CusNull.class)).notNull();

    check(ImmutableTryCustomNullann.Builder.class.getDeclaredMethod("lst", Iterable.class)
        .getParameters()[0]
        .getAnnotatedType()
        .getAnnotation(CusNull.class)).notNull();

    check(ImmutableTryCustomNullann.class.getDeclaredMethod("withMap", Map.class)
        .getParameters()[0]
        .getAnnotatedType()
        .getAnnotation(CusNull.class)).notNull();

    check(ImmutableTryCustomNullann.class.getDeclaredMethod("withMap", Map.class)
        .getParameters()[0]
        .getAnnotatedType()
        .getAnnotation(CusNull.class)).notNull();

    check(ImmutableTryCustomNullann.Builder.class.getDeclaredMethod("map", Map.class)
        .getParameters()[0]
        .getAnnotatedType()
        .getAnnotation(CusNull.class)).notNull();
  }

  @Test void nullableConcreteArraysFromSupertype() {
    byte[] bytes = {1, 2, 3};
    String[] strings = {"a", "b"};

    NullableArrays.ChildOverrides original = ImmutableChildOverrides.builder()
        .primitiveArray(bytes)
        .referenceArray(strings)
        .build();

    // .from() supertype overload should copy nullable array attributes
    NullableArrays.ChildOverrides copied = ImmutableChildOverrides.builder()
        .from((NullableArrays) original)
        .build();

    check(copied.primitiveArray()).notNull();
    check(copied.referenceArray()).notNull();

    // null arrays should be accepted
    NullableArrays.ChildOverrides withNulls = ImmutableChildOverrides.builder().build();
    check(withNulls.primitiveArray()).isNull();
    check(withNulls.referenceArray()).isNull();
  }

  @Test void mandatoryOnlyDoesNotLeakNullableOnMandatory() throws Exception {
    // Explicitly @Nullable attribute SHOULD be annotated.
    check(ImmutableMandatoryOnlyNullMarked.class.getDeclaredField("optional")
        .getAnnotatedType().getAnnotation(Nullable.class)).notNull();

    check(ImmutableMandatoryOnlyNullMarked.class.getDeclaredMethod("getOptional")
        .getAnnotatedReturnType().getAnnotation(Nullable.class)).notNull();

    // Mandatory attribute must NOT be annotated @Nullable.
    check(ImmutableMandatoryOnlyNullMarked.class.getDeclaredField("mandatory")
        .getAnnotatedType().getAnnotation(Nullable.class)).isNull();

    check(ImmutableMandatoryOnlyNullMarked.class.getDeclaredMethod("getMandatory")
        .getAnnotatedReturnType().getAnnotation(Nullable.class)).isNull();
  }

  @Test void typeuseInheritance() throws Exception {
    ImmutableFirstChild firstChild = ImmutableFirstChild.builder()
        .string("123")
        .myField(ImmutableMyField.builder().build())
        .build();
    ImmutableSecondChild secondChild = ImmutableSecondChild.builder()
        .from(firstChild)
        .build();

    check(secondChild.myField()).notNull();
  }
}
