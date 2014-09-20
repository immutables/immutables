package org.immutables.modeling.meta;

import org.immutables.annotation.GenerateConstructorParameter;
import org.immutables.annotation.GenerateImmutable;

@GenerateImmutable(nonpublic = true, builder = false)
public abstract class TypeReference {
  TypeReference() {}

  @GenerateConstructorParameter
  abstract String value();

  @Override
  public String toString() {
    return value();
  }

  public static TypeReference from(String qualifiedName) {
    return ImmutableTypeReference.of(qualifiedName);
  }

  public static TypeReference from(Class<?> type) {
    return from(type.getCanonicalName());
  }
}
