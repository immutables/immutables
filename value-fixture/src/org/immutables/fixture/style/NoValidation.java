package org.immutables.fixture.style;

import org.immutables.value.Value.Style.ValidationMethod;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(validationMethod = ValidationMethod.NONE)
public interface NoValidation {
  @Value.Parameter
  String a();

  @Value.Parameter
  Integer b();
}
