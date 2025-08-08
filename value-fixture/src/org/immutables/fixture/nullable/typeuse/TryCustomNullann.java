package org.immutables.fixture.nullable.typeuse;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(
    optionalAcceptNullable = true,
    nullableAnnotation = "CusNull",
    fallbackNullableAnnotation = CusNull.class,
    jdkOnly = true
)
public interface TryCustomNullann extends WithTryCustomNullann {
  @CusNull Integer aa();
  @CusNull List<@CusNull String> lst();
  @CusNull Map<Integer, String> map();
  //TypeMirrors for Arrays do not provide any type annotations
  //@CusNull String @CusNull [] arr();
  Optional<String> opt();
}
