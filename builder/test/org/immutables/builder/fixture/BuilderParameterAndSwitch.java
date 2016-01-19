package org.immutables.builder.fixture;

import java.lang.annotation.RetentionPolicy;
import javax.annotation.Nullable;
import org.immutables.builder.Builder;
import org.immutables.value.Value;

@Value.Immutable
public interface BuilderParameterAndSwitch {
  @Builder.Parameter
  int theory();

  @Nullable
  String reality();

  @Builder.Parameter
  String value();

  @Builder.Switch
  RetentionPolicy policy();
}
