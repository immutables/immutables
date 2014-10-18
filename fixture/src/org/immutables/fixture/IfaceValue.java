package org.immutables.fixture;

import java.util.List;
import org.immutables.value.Value;

@Value.Immutable
public interface IfaceValue {

  @Value.Parameter
  int getNumber();

  @Value.Auxiliary
  List<String> auxiliary();
}
