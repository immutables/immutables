package org.immutables.fixture.modifiable;

import java.util.List;
import java.util.Map;
import org.immutables.value.Value;

@Value.Immutable
@Value.Modifiable
@Value.Style(
    beanFriendlyModifiables = true,
    create = "new",
    get = {"get*", "is*"})
public interface BeanFriendly {

  boolean isPrimary();

  int getId();

  String getDescription();

  List<String> getNames();

  Map<String, String> getOptions();
}
