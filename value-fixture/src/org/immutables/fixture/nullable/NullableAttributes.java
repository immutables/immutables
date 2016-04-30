package org.immutables.fixture.nullable;

import java.util.Collections;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import java.util.Set;
import org.immutables.gson.Gson;
import java.util.Map;
import java.util.List;
import javax.annotation.Nullable;
import org.immutables.value.Value;


@Gson.TypeAdapters
@Value.Immutable
@JsonDeserialize(as = ImmutableNullableAttributes.class)
@Value.Style(defaultAsDefault = true)
public interface NullableAttributes {
  @Nullable
  Integer integer();

  @Nullable
  List<String> list();

  @Nullable
  default Set<Integer> set() {
    return Collections.emptySet();
  }

  @Nullable
  Integer[] array();

  @Nullable
  default Double[] defArray() {
    return new Double[] {Double.NaN};
  }

  @Nullable
  Map<String, Object> map();
}
