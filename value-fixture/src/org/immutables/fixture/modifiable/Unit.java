package org.immutables.fixture.modifiable;

import java.util.List;
import org.immutables.value.Value;

@Value.Modifiable
abstract class Unit {
  abstract List<Float> getPrices();
}
