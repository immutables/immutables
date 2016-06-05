package org.immutables.fixture.with;

import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.immutables.value.Value;
import org.immutables.value.Value.Style.ImplementationVisibility;

@Value.Immutable
@Value.Style(visibility = ImplementationVisibility.PACKAGE)
public abstract class Copied implements WithCopied {

  public abstract int attr();

  public abstract @Nullable Void voids();

  public abstract String[] arr();

  public abstract Map<String, Integer> map();

  public abstract List<Boolean> list();

  public static class Builder extends ImmutableCopied.Builder {}

  static void use() {
    new Copied.Builder()
        .attr(1)
        .build()
        .withList(true, false);
  }
}
