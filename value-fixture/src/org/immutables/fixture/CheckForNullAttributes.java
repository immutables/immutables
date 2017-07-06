package org.immutables.fixture;

import java.util.Set;
import javax.annotation.CheckForNull;
import org.immutables.value.Value;

@Value.Immutable
public interface CheckForNullAttributes {
  @CheckForNull
  String a();

  @CheckForNull
  Set<String> b();
}
