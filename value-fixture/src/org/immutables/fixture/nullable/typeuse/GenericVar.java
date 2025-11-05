package org.immutables.fixture.nullable.typeuse;

import java.util.List;
import org.immutables.fixture.nullable.AllowNulls;
import org.immutables.fixture.nullable.SkipNulls;
import org.immutables.value.Value;
import org.jspecify.annotations.NullMarked;
import org.jspecify.annotations.Nullable;

@NullMarked
@Value.Immutable
@Value.Style(jdkOnly = true)
public abstract class GenericVar<V> {
  public abstract @Nullable V value();

  // This nullable is for elements, but we can only recognize the whole
  // array as nullable
  public abstract @Nullable V[] array();
  // This sorta works correctly with a 
  public abstract V @Nullable [] bracadabraz();
  // This is working only because of @AllowNulls
  public abstract @AllowNulls List<@Nullable V> list();

  public abstract List<@SkipNulls String> list2();

  @Value.Default
  public byte[] defaultArray() {
    return new byte[]{};
  }
}
