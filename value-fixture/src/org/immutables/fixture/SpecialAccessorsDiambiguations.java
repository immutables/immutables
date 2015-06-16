package org.immutables.fixture;

import org.immutables.value.Value;

/**
 * Compilation test for identifier disambiguation like local variable 'h' in hashCode computation
 * or prehashed hashCode field in present of getHashCode attribute. "getHashCode" and "isToString"
 * are not turned into "hashCode" and "toString" to not class with attribute-like methods from
 * Object.
 */
@Value.Immutable(prehash = true)
@Value.Style(get = {"is*", "get*"})
public abstract class SpecialAccessorsDiambiguations {
  public abstract int getHashCode();

  public abstract int computeHashCode();

  public abstract String isToString();

  public abstract int h();

  public abstract int get__();
}
