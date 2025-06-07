package org.immutables.fixture.nullable.typeuse;

import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(
    optionalAcceptNullable = true,
    nullableAnnotation = "CusNull",
    fallbackNullableAnnotation = CusNull.class)
public interface TryCustomNullann {
  @CusNull Integer aa();
  @CusNull List<@CusNull String> lst();
  Optional<String> opt();
}
