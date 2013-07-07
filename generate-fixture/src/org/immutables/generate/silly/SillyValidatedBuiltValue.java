package org.immutables.generate.silly;

import org.immutables.annotation.GenerateImmutable;
import org.immutables.annotation.GenerateCheck;
import static com.google.common.base.Preconditions.checkState;

@GenerateImmutable
public abstract class SillyValidatedBuiltValue {
  public abstract int value();

  public abstract boolean negativeOnly();

  @GenerateCheck
  protected void validate() {
    checkState(!negativeOnly() || value() < 0, "if negativeOnly = true, value should be negative");
  }
}
