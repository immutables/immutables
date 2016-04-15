package org.immutables.fixture.nullable;

import javax.annotation.Nullable;
import java.util.List;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.gson.Gson;
import org.immutables.value.Value;

@Gson.TypeAdapters
@Value.Immutable
@JsonDeserialize(as = ImmutableNonnullConstruction.class)
@Value.Style(allParameters = true, defaultAsDefault = true)
public interface NonnullConstruction {
  String[] arr();

  @Nullable
  String[] brr();

  default List<String> ax() {
    return null;// will blowup unless set explicitly to nonnull
  }

  @Value.Derived
  default int a() {
    return 1;
  }
}
