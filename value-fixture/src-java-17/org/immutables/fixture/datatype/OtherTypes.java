package org.immutables.fixture.datatype;

import java.util.List;
import javax.annotation.Nullable;
import org.immutables.datatype.Data;
import org.immutables.value.Value;

/** Generating data descriptor in combination of records (with builders) and abstract value types. */
@Data
@Value.Style(builder = "new")
@Value.Enclosing
public sealed interface OtherTypes {

  @Value.Builder
  record RcAbc(int a, String b, boolean c) implements OtherTypes {}

  @Value.Builder
  record RcTuw(String b, List<String> l) implements OtherTypes {
    static class Builder extends ImmutableOtherTypes.RcTuwBuilder {}
  }

  @Value.Immutable
  non-sealed interface AbVl extends OtherTypes {
    int a();
  }
}
