package org.immutables.fixture.style;

import java.util.List;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(finalInstanceFields = false, protectedNoargConstructor = true)
public interface NonFinalInstanceFields {
  int a();

  String b();

  List<Boolean> c();
}
