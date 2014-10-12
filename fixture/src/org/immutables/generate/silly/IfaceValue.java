package org.immutables.generate.silly;

import java.util.List;
import org.immutables.value.Value;

@Value.Immutable
public interface IfaceValue {

  @Value.Parameter
  int getNumber();

  @Value.Auxiliary
  List<String> auxiliary();
}
