package org.immutables.fixture.nullable;

import org.immutables.fixture.nullable.typeuse.CusNull;
import org.immutables.fixture.nullable.typeuse.ImmutableLetsTryJSpecify;
import org.immutables.fixture.nullable.typeuse.ImmutableTryCustomNullann;
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
  }
}
