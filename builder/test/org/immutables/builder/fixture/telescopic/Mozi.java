package org.immutables.builder.fixture.telescopic;

import com.google.common.base.Optional;
import com.google.common.collect.Multimap;
import java.util.List;
import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Style(stagedBuilder = true)
@Value.Immutable
public interface Mozi<K, V> {
  K a();

  V b();

  List<Long> c();

  Optional<String> d();

  Multimap<K, V> e();

  @Nullable
  String f();
}
