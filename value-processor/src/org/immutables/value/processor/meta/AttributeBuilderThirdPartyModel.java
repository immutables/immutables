package org.immutables.value.processor.meta;

import javax.annotation.Nullable;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Modifiable;
import org.immutables.value.Value.Style;

@Immutable
abstract class AttributeBuilderThirdPartyModel {
  // Must be instance
  protected abstract ExecutableElement buildMethod();

  // Constructor, Static, or Instance
  protected abstract ExecutableElement copyMethod();

  // Constructor or Static
  protected abstract ExecutableElement builderMethod();

  protected abstract TypeElement builderType();

  @Modifiable
  @Style(set = "*")
  abstract static class Creator extends AttributeBuilderThirdPartyModel {

    @Override
    @Nullable
    protected abstract ExecutableElement buildMethod();

    @Override
    @Nullable
    protected abstract ExecutableElement copyMethod();

    @Override
    @Nullable
    protected abstract ExecutableElement builderMethod();

    @Override
    @Nullable
    protected abstract TypeElement builderType();

    protected abstract AttributeBuilderThirdPartyModel buildMethod(ExecutableElement buildMethod);

    protected abstract AttributeBuilderThirdPartyModel copyMethod(ExecutableElement copyMethod);

    protected abstract AttributeBuilderThirdPartyModel builderMethod(ExecutableElement buildMethod);

    protected abstract AttributeBuilderThirdPartyModel builderType(TypeElement builderType);

    @Nullable
    public TypeElement findBuilderType() {
      if (buildMethod() != null) {
        return getBuilderTypeFromBuildMethod();
      }

      if (copyMethod() != null) {
        return getBuilderTypeFromCopyMethod();
      }

      if (builderMethod() != null) {
        return getBuilderTypeFromBuilderMethod();
      }

      if (builderType() != null) {
        return builderType();
      }

      return null;
    }

    public void mergeFrom(AttributeBuilderThirdPartyModel toCopyFrom) {
      if (buildMethod() == null) {
        buildMethod(toCopyFrom.buildMethod());
      }

      if (copyMethod() == null) {
        copyMethod(toCopyFrom.copyMethod());
      }

      if (builderMethod() == null) {
        builderMethod(toCopyFrom.builderMethod());
      }
    }

    private TypeElement getBuilderTypeFromBuilderMethod() {
      return builderMethod().getKind() == ElementKind.CONSTRUCTOR
          ? (TypeElement) builderMethod().getEnclosingElement()
          : (TypeElement) ((DeclaredType) builderMethod().getReturnType()).asElement();
    }

    private TypeElement getBuilderTypeFromBuildMethod() {
      return (TypeElement) buildMethod().getEnclosingElement();
    }

    private TypeElement getBuilderTypeFromCopyMethod() {
      return copyMethod().getKind() == ElementKind.CONSTRUCTOR
          ? (TypeElement) copyMethod().getEnclosingElement()
          : (TypeElement) ((DeclaredType) copyMethod().getReturnType()).asElement();
    }

    public boolean complete() {
      if (builderMethod() != null && buildMethod() != null && copyMethod() != null) {
        boolean transitiveEquality = getBuilderTypeFromBuilderMethod()
            .equals(getBuilderTypeFromBuildMethod());
        transitiveEquality = transitiveEquality
            && getBuilderTypeFromBuildMethod()
                .equals(getBuilderTypeFromCopyMethod());
        if (builderType() != null) {
          transitiveEquality =
              transitiveEquality && getBuilderTypeFromCopyMethod().equals(builderType());
        }

        if (!transitiveEquality) {
          throw new AssertionError();
        }

        return true;
      }

      return false;
    }

    // Should this be auto-genned? Though we still have to set the builder type.
    public AttributeBuilderThirdPartyModel toImmutable() {
      builderType(findBuilderType());
      return ImmutableAttributeBuilderThirdPartyModel.copyOf(this);
    }
  }
}
