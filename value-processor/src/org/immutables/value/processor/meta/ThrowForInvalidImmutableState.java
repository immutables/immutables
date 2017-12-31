package org.immutables.value.processor.meta;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.*;
import javax.lang.model.util.ElementFilter;

public final class ThrowForInvalidImmutableState {
  public final String qualifiedName;
  public final boolean isCustom;
  public final boolean hasAttributeNamesConstructor;

  private ThrowForInvalidImmutableState(
      String qualifiedName,
      boolean isCustom,
      boolean hasAttributeNamesConstructor) {
    this.qualifiedName = qualifiedName;
    this.isCustom = isCustom;
    this.hasAttributeNamesConstructor = hasAttributeNamesConstructor;
  }

  private static final ThrowForInvalidImmutableState UNCUSTOMIZED =
      new ThrowForInvalidImmutableState(IllegalStateException.class.getName(), false, false);

  static ThrowForInvalidImmutableState from(ProcessingEnvironment processing, StyleInfo style) {
    String qualifiedName = style.throwForInvalidImmutableStateName();
    if (isStandardIllegalStateException(qualifiedName)) {
      return UNCUSTOMIZED;
    }
    TypeElement element = processing.getElementUtils().getTypeElement(qualifiedName);
    if (element == null) {
      // This shouldn't ever happen as annotation carry class literal, so it should always
      // be on the classpath, but I don't want to take chances or raise unuseful errors.
      // so just fallback to uncustomized instance.
      return UNCUSTOMIZED;
    }
    return new ThrowForInvalidImmutableState(
        qualifiedName,
        true,
        hasStringArrayConstructor(element));
  }

  private static boolean isStandardIllegalStateException(String qualifiedName) {
    return qualifiedName.equals(IllegalStateException.class.getName());
  }

  private static boolean hasStringArrayConstructor(TypeElement element) {
    for (ExecutableElement e : ElementFilter.constructorsIn(element.getEnclosedElements())) {
      if (e.getModifiers().contains(Modifier.PUBLIC) && e.getParameters().size() == 1) {
        if (isArrayOfStrings(e.getParameters().get(0).asType())) {
          return true;
        }
      }
    }
    return false;
  }

  private static boolean isArrayOfStrings(TypeMirror type) {
    return type.getKind() == TypeKind.ARRAY
        && ((ArrayType) type).getComponentType().toString().equals(String.class.getName());
  }

  @Override
  public String toString() {
    return qualifiedName;
  }
}
