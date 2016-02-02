package org.immutables.fixture;

import org.immutables.value.Value;

// Compilation test to override final method defined in interface, but otherwise unrelated methods.
interface HasFinalMethodDeclared {
  String ret();
}

abstract class HasFinalMethodDefined {
  public final String ret() {
    return "x";
  }
}

@Value.Immutable
public abstract class HasInheritedFinalMethod extends HasFinalMethodDefined implements HasFinalMethodDeclared {}
