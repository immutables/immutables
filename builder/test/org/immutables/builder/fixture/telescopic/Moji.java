package org.immutables.builder.fixture.telescopic;

import com.google.common.base.Optional;
import com.google.common.collect.Multimap;
import java.util.List;
import javax.annotation.Nullable;
import org.immutables.value.Value;

@Value.Style(stagedBuilder = true)
@Value.Immutable
public interface Moji {
  int a();

  String b();

  List<Long> c();

  Optional<String> d();

  Optional<Number> n();

  Multimap<Integer, String> e();

  @Nullable
  String f();
}
