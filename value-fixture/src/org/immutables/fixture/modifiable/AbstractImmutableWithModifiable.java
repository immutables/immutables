package org.immutables.fixture.modifiable;

import java.util.List;
import org.immutables.value.Value;
import org.immutables.value.Value.Style.ImplementationVisibility;
import org.immutables.value.Value.Style.ValidationMethod;

// Added to make sure validations
// based on the issue #544
@Value.Immutable
@Value.Modifiable
@Value.Style(
    get = {"is*", "get*"},
    init = "set*",
    typeAbstract = {"Abstract*"},
    typeImmutable = "*",
    optionalAcceptNullable = true,
    forceJacksonPropertyNames = false,
    validationMethod = ValidationMethod.VALIDATION_API,
    visibility = ImplementationVisibility.SAME)
public interface AbstractImmutableWithModifiable {
  int getId();

  String getDescription();

  List<String> getNames();
}
