package org.immutables.modeling.processing;

import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;

public final class GeneratedTypes {
  private static final String GENERATOR_PREFIX = "Generator_";

  public static final String getSimpleName(TypeElement type) {
    return GENERATOR_PREFIX + type.getSimpleName();
  }

  public static final String getQualifiedName(Elements elements, TypeElement type) {
    return elements.getPackageOf(type).getQualifiedName() + "." + getSimpleName(type);
  }
}
