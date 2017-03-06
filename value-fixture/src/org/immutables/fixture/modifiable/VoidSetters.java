package org.immutables.fixture.modifiable;

import java.util.Arrays;
import org.immutables.value.Value;
import java.util.List;

@Value.Immutable
@Value.Modifiable
@Value.Style(beanFriendlyModifiables = true, create = "new")
public interface VoidSetters {
  int getAa();

  String getBb();

  List<Double> getCc();

  default void use() {
    ModifiableVoidSetters m = new ModifiableVoidSetters();
    m.setAa(2);
    m.setBb("bb");
    m.setCc(Arrays.asList(1.1, 2.2, 3.3));
  }
}
