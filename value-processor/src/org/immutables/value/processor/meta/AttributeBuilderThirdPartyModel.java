package org.immutables.value.processor.meta;

import javax.annotation.Nullable;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;

/**
 * Helper class to detect a builder by adding methods as discovered in stages.
 * TODO: maybe bring in mutable?
 */
class AttributeBuilderThirdPartyModel {

  // Must be instance
  @Nullable private ExecutableElement buildMethod;
  // Constructor, Static, or Instance
  @Nullable private ExecutableElement copyMethod;
  // Constructor or Static
  @Nullable private ExecutableElement builderMethod;

  @Nullable private TypeElement builderType;

  public ExecutableElement getBuildMethod() {
    return buildMethod;
  }

  public void setBuildMethod(ExecutableElement buildMethod) {
    this.buildMethod = buildMethod;
  }

  public ExecutableElement getBuilderMethod() {
    return builderMethod;
  }

  public void setBuilderMethod(ExecutableElement builderMethod) {
    this.builderMethod = builderMethod;
  }

  public ExecutableElement getCopyMethod() {
    return copyMethod;
  }

  public void setCopyMethod(ExecutableElement copyMethod) {
    this.copyMethod = copyMethod;
  }

  @Nullable
  public TypeElement getBuilderType() {
    if (buildMethod != null) {
      return getBuilderTypeFromBuildMethod();
    }

    if (copyMethod != null) {
      return getBuilderTypeFromCopyMethod();
    }

    if (builderMethod != null) {
      return getBuilderTypeFromBuilderMethod();
    }

    if (builderType != null) {
      return builderType;
    }

    return null;
  }

  public void setBuilderType(@Nullable TypeElement builderType) {
    this.builderType = builderType;
  }

  public void mergeFrom(AttributeBuilderThirdPartyModel toCopyFrom) {
    if (buildMethod == null) {
      buildMethod = toCopyFrom.buildMethod;
    }

    if (copyMethod == null) {
      copyMethod = toCopyFrom.copyMethod;
    }

    if (builderMethod == null) {
      builderMethod = toCopyFrom.builderMethod;
    }
  }

  private TypeElement getBuilderTypeFromBuilderMethod() {
    if (builderMethod.getKind() == ElementKind.CONSTRUCTOR) {
      return (TypeElement) builderMethod.getEnclosingElement();
    } else {
      return (TypeElement) ((DeclaredType) builderMethod.getReturnType()).asElement();
    }
  }

  private TypeElement getBuilderTypeFromBuildMethod() {
    return (TypeElement) buildMethod.getEnclosingElement();
  }

  private TypeElement getBuilderTypeFromCopyMethod() {
    if (copyMethod.getKind() == ElementKind.CONSTRUCTOR) {
      return (TypeElement) copyMethod.getEnclosingElement();
    } else {
      return (TypeElement) ((DeclaredType) copyMethod.getReturnType()).asElement();
    }
  }

  public boolean complete() {
    if (builderMethod != null && buildMethod != null && copyMethod != null) {
      boolean transitiveEquality = getBuilderTypeFromBuilderMethod()
          .equals(getBuilderTypeFromBuildMethod());
      transitiveEquality = transitiveEquality && getBuilderTypeFromBuildMethod()
          .equals(getBuilderTypeFromCopyMethod());
      if (builderType != null) {
        transitiveEquality =
            transitiveEquality && getBuilderTypeFromCopyMethod().equals(builderType);
      }

      if (!transitiveEquality) {
        throw new AssertionError();
      }

      return true;
    }

    return false;
  }
}
