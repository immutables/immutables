package org.immutables.fixture.nullable;

import java.util.List;
import java.util.Map;
import org.immutables.value.Value;

@Value.Style(jdkOnly = true)
@Value.Immutable
public interface NullableElements {
  List<@NullableUse Void> al();

  List<@SkipNulls String> sk();

  Map<String, @NullableUse Integer> bl();

  Map<String, @SkipNulls Integer> sm();
}
