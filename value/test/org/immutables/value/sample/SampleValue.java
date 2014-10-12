package org.immutables.value.sample;

import org.immutables.annotation.GenerateConstructorParameter;
import java.util.List;
import org.immutables.value.Value;

@Value.Immutable
public interface SampleValue {
  int a();

  List<Integer> c();
}
