package org.immutables.generate.silly;

import org.immutables.annotation.GenerateConstructorArgument;
import org.immutables.annotation.GenerateImmutable;
import org.immutables.annotation.GenerateCheck;
import static com.google.common.base.Preconditions.*;

@GenerateImmutable(useBuilder = false)
public abstract class SillyValidatedConstructedValue {
  @GenerateConstructorArgument(order = 0)
  public abstract int value();

  @GenerateConstructorArgument(order = 1)
  public abstract boolean negativeOnly();

  @GenerateCheck
  protected void validate() {
    checkState(!negativeOnly() || value() < 0, "if negativeOnly = true, value should be negative");
  }
}
