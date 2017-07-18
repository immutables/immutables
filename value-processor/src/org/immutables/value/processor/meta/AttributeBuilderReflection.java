package org.immutables.value.processor.meta;

import com.google.common.base.Optional;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Lazy;
import org.immutables.value.Value.Parameter;
import org.immutables.value.Value.Style;
import org.immutables.value.Value.Style.ImplementationVisibility;
import org.immutables.value.processor.meta.AttributeBuilderDescriptor.ValueToBuilderTarget;

/**
 * Reflects over the returnType and generates strings for the {@link AttributeBuilderDescriptor}
 */
@Immutable(builder = false)
@Style(visibility = ImplementationVisibility.PRIVATE)
public abstract class AttributeBuilderReflection {

  static Map<TypeMirror, AttributeBuilderReflection> analyzedReturnTypes = new HashMap<>();

  //TODO: not 100% cached because returnType can be a list (or map etc.)
  // Maybe bad to cache? Some safe way to cache without holding onto processing environment?
  public static AttributeBuilderReflection forValueType(ValueAttribute valueAttribute) {

    if (!analyzedReturnTypes.containsKey(valueAttribute.returnType)) {
      analyzedReturnTypes
          .put(valueAttribute.returnType, ImmutableAttributeBuilderReflection.of(valueAttribute));
    }

    return analyzedReturnTypes.get(valueAttribute.returnType);
  }

  @Parameter
  abstract ValueAttribute valueAttribute();

  private List<Strategy> getStrategies() {
    // Order here matters. We want first party to be found first.
    return Arrays.asList(
        ImmutableFirstPartyStrategy.of(valueAttribute()),
        ImmutableThirdPartyStaticBuilderStrategy.of(valueAttribute())
    );
  }

  boolean isAttributeBuilder() {
    if (!valueAttribute().containingType.constitution.style().attributeBuilderDetection()) {
      return false;
    }

    for (Strategy strategy : getStrategies()) {
      if (strategy.isAttributeBuilder()) {
        return true;
      }
    }
    return false;
  }

  AttributeBuilderDescriptor getAttributeBuilderDescriptor() {
    return getReflectionStrategy().getAttributeBuilderDescriptor();
  }

  @Lazy
  Strategy getReflectionStrategy() {
    if (!isAttributeBuilder()) {
      throw new RuntimeException(
          "Should not call getReflectionStrategy unless isAttributeBuilder is true");
    }

    for (Strategy strategy : getStrategies()) {
      if (strategy.isAttributeBuilder()) {
        return strategy;
      }
    }

    throw new AssertionError("isAttributeBuilder flip-flopped from true to false.");
  }

  /**
   * Allows for different mechanisms of discovering nested builders.
   *
   * This is needed because Immutables value objects may not be available on the
   * class path during processing, which will be order dependent.
   */
  interface Strategy {

    boolean isAttributeBuilder();

    AttributeBuilderDescriptor getAttributeBuilderDescriptor();
  }

  /**
   * Strategy for processing first party immutables.  Honors both deepImmutableDiscovery, and
   * builder extension.
   */
  @Immutable(builder = false)
  abstract static class FirstPartyStrategy implements Strategy {

    @Parameter
    abstract ValueAttribute valueAttribute();

    @Override
    public boolean isAttributeBuilder() {
      return valueAttribute().attributeValueType != null &&
          valueAttribute().attributeValueType.isUseBuilder();

    }

    @Override
    public AttributeBuilderDescriptor getAttributeBuilderDescriptor() {
      String createNewBuilder = String.format("%s.%s",
          attributeValueType().getTopSimple(),
          attributeValueType().names().builder());

      return ImmutableAttributeBuilderDescriptor.builder()
          .valueToBuilderTarget(ValueToBuilderTarget.BUILDER_INSTANCE)
          .valueToBuilderMethod(attributeValueType().names().from)
          .buildMethod(attributeValueType().names().build)
          .qualifiedValueTypeName(attributeValueType().name())
          .qualifiedBuilderTypeName(attributeValueType().typeBuilderImpl().toString())
          .qualifiedBuilderConstructorMethod(createNewBuilder)
          .build();

    }

    ValueType attributeValueType() {
      return valueAttribute().attributeValueType;
    }
  }

  /**
   * Strategy for parsing third party immutables. for example: the protocol buffer API.
   *
   * Assumes that all third party classes are available on the class path at processing time.
   * If this is not the case, we may need to use a processing method that allows deferring of
   * compilation
   */
  @Immutable(builder = false)
  abstract static class ThirdPartyStaticBuilderStrategy implements Strategy {

    private Optional<ExecutableElement> maybeBuildMethod = null;

    @Parameter
    abstract ValueAttribute valueAttribute();

    @Override
    public boolean isAttributeBuilder() {
      return maybeAttributeValueType().isPresent()
          && maybeAttributeBuilderType().isPresent()
          && maybeBuilderConstructorMethod().isPresent()
          && maybeCopyMethod().isPresent()
          && maybeBuildMethod().isPresent();
    }

    @Override
    public AttributeBuilderDescriptor getAttributeBuilderDescriptor() {
      if (!isAttributeBuilder()) {
        throw new IllegalStateException("isAttributeBuilder should have been called first");
      }

      ExecutableElement copyMethod = maybeCopyMethod().get();
      ExecutableElement builderConstructor = maybeBuilderConstructorMethod().get();
      TypeElement attributeValueType = maybeAttributeValueType().get();
      TypeElement attributeBuilderType = maybeAttributeBuilderType().get();
      ExecutableElement buildMethod = maybeBuildMethod().get();

      ValueToBuilderTarget target;

      if (copyMethod.getModifiers().contains(Modifier.STATIC)) {
        if (copyMethod.getEnclosingElement().equals(attributeValueType)) {
          target = ValueToBuilderTarget.VALUE_TYPE;
        } else {
          target = ValueToBuilderTarget.BUILDER_TYPE;
        }
      } else {
        if (copyMethod.getEnclosingElement().equals(attributeValueType)) {
          target = ValueToBuilderTarget.VALUE_INSTANCE;
        } else {
          target = ValueToBuilderTarget.BUILDER_INSTANCE;
        }
      }

      String qualifiedBuilderConstructorMethod;
      if (builderConstructor.getEnclosingElement().equals(attributeValueType)) {
        qualifiedBuilderConstructorMethod = String.format("%s.%s",
            attributeValueType.getQualifiedName(),
            builderConstructor.getSimpleName());
      } else {
        // TODO: test
        if (builderConstructor.getKind() == ElementKind.CONSTRUCTOR) {
          qualifiedBuilderConstructorMethod = String.format("new %s.%s",
              attributeBuilderType.getQualifiedName(),
              builderConstructor.getSimpleName());
        } else {
          qualifiedBuilderConstructorMethod = String.format("%s.%s",
              attributeBuilderType.getQualifiedName(),
              builderConstructor.getSimpleName());
        }
      }

      return ImmutableAttributeBuilderDescriptor.builder()
          .valueToBuilderTarget(target)
          .valueToBuilderMethod(copyMethod.getSimpleName().toString())
          .buildMethod(buildMethod.getSimpleName().toString())
          .qualifiedValueTypeName(attributeValueType.getQualifiedName().toString())
          .qualifiedBuilderTypeName(attributeBuilderType.getQualifiedName().toString())
          .qualifiedBuilderConstructorMethod(qualifiedBuilderConstructorMethod)
          .build();
    }

    /**
     * @return type mirror for the return type, or the template argument of a single collection
     * element.
     */
    @Lazy
    protected Optional<TypeElement> maybeAttributeValueType() {
      if (valueAttribute().typeKind().isRegular()
          && valueAttribute().returnType.getKind() == TypeKind.DECLARED) {

        DeclaredType returnType = (DeclaredType) valueAttribute().returnType;
        TypeElement typeElement = (TypeElement) returnType.asElement();
        if (typeElement.getKind() == ElementKind.CLASS) {
          return Optional.of(typeElement);
        }
      } else if (valueAttribute().typeKind().isList()) {
        TypeElement attributeBuilderType = valueAttribute()
            .containingType.constitution
            .protoclass()
            .processing()
            .getElementUtils()
            .getTypeElement(valueAttribute().firstTypeParameter());

        if (attributeBuilderType != null && attributeBuilderType.getKind() == ElementKind.CLASS) {
          return Optional.of(attributeBuilderType);
        }
      }

      return Optional.absent();
    }

    @Lazy
    public Optional<TypeElement> maybeAttributeBuilderType() {
      Optional<ExecutableElement> maybeConstructorMethod = maybeBuilderConstructorMethod();
      if (!maybeConstructorMethod.isPresent()) {
        return Optional.absent();
      }

      // Casting is guaranteed by maybeBuilderConstructorMethod
      return Optional.of((TypeElement)
          ((DeclaredType) maybeConstructorMethod.get().getReturnType()).asElement());
    }

    @Lazy
    public Optional<ExecutableElement> maybeBuilderConstructorMethod() {

      Optional<TypeElement> maybeAttributeValueType = maybeAttributeValueType();
      if (!maybeAttributeValueType.isPresent()) {
        return Optional.absent();
      }

      TypeElement valueTypeElement = maybeAttributeValueType.get();

      // TODO: if new is in the attributeBuilder, look for inner classes and constructors
      // or use another strategy... either way.
      for (Element possibleBuilderConstructor : valueTypeElement.getEnclosedElements()) {

        if (couldBeBuilderConstructor(possibleBuilderConstructor)) {
          TypeElement candidateBuilderClass = (TypeElement)
              ((DeclaredType) ((ExecutableElement) possibleBuilderConstructor).getReturnType())
                  .asElement();

          for (Element possibleBuildMethod : candidateBuilderClass.getEnclosedElements()) {
            if (possibleBuildMethod.getKind() == ElementKind.METHOD
                && !possibleBuildMethod.getModifiers().contains(Modifier.STATIC)
                && ((ExecutableElement) possibleBuildMethod).getReturnType().getKind() ==
                TypeKind.DECLARED
                && ((DeclaredType) ((ExecutableElement) possibleBuildMethod).getReturnType())
                .asElement().equals(valueTypeElement)) {

              if (maybeBuildMethod != null) {
                throw new AssertionError("MaybeBuild Method initialized twice");
              }
              maybeBuildMethod = Optional.of((ExecutableElement) possibleBuildMethod);

              return Optional.of((ExecutableElement) possibleBuilderConstructor);
            }
          }
        }
      }

      return Optional.absent();
    }

    /**
     * Return true if the possibleBuilderConstructor matches the
     * Style#attributeBuilder() and returns a class.
     *
     * TODO: may need to make this return true if the return type is an interface too...
     *
     * @param possibleBuilderConstructor executableElement
     */
    private boolean couldBeBuilderConstructor(Element possibleBuilderConstructor) {
      if (possibleBuilderConstructor.getKind() == ElementKind.CONSTRUCTOR
          || possibleBuilderConstructor.getKind() == ElementKind.METHOD) {

        // TODO: special handling for new keyword
        String detectedAttributeBuilder = valueAttribute().containingType.names()
            .rawFromAttributeBuilder(possibleBuilderConstructor.getSimpleName().toString());

        if (detectedAttributeBuilder.isEmpty()) {
          return false;
        }

        return possibleBuilderConstructor.getKind() == ElementKind.METHOD
            && ((ExecutableElement) possibleBuilderConstructor).getReturnType().getKind()
            == TypeKind.DECLARED
            && ((DeclaredType) ((ExecutableElement) possibleBuilderConstructor).getReturnType())
            .asElement().getKind() == ElementKind.CLASS;
      }

      return false;
    }

    /**
     * instance is set as part of discovering the constructor. We must be able to
     * round trip between builder type and constructor type.
     */
    @Lazy
    public Optional<ExecutableElement> maybeBuildMethod() {
      if (maybeBuildMethod == null) {
        throw new IllegalStateException("This should not be called until the build method is set.");
      }
      return maybeBuildMethod;
    }

    @Lazy
    public Optional<ExecutableElement> maybeCopyMethod() {
      Optional<TypeElement> maybeAttributeValueType = maybeAttributeValueType();
      Optional<TypeElement> maybeAttributeBuilderType = maybeAttributeBuilderType();
      if (!maybeAttributeValueType.isPresent() || !maybeAttributeBuilderType.isPresent()) {
        return Optional.absent();
      }

      TypeElement valueTypeElement = maybeAttributeValueType.get();
      TypeElement builderTypeElement = maybeAttributeBuilderType.get();

      Optional<ExecutableElement> foundCopyMethod = findMethod(valueTypeElement.asType(),
          builderTypeElement.asType(), builderTypeElement.getEnclosedElements());
      if (!foundCopyMethod.isPresent()) {
        foundCopyMethod = findMethod(valueTypeElement.asType(), builderTypeElement.asType(),
            valueTypeElement.getEnclosedElements());
      }
      return foundCopyMethod;
    }

    private Optional<ExecutableElement> findMethod(
        TypeMirror argumentType,
        TypeMirror returnType,
        List<? extends Element> enclosedElements) {
      for (Element possibleCopyMethod : enclosedElements) {
        if (possibleCopyMethod.getKind() == ElementKind.METHOD
            || possibleCopyMethod.getKind() == ElementKind.CONSTRUCTOR) {
          ExecutableElement candidateCopyMethod = (ExecutableElement) possibleCopyMethod;

          if (candidateCopyMethod.getParameters().size() == 1
              && candidateCopyMethod.getParameters().get(0).asType().equals(argumentType)
              && candidateCopyMethod.getReturnType().equals(returnType)) {

            return Optional.of(candidateCopyMethod);
          }
        }
      }

      return Optional.absent();
    }
  }

  /**
   * Strategy for parsing third party immutables. for example:
   *
   * <pre>
   * class MyObject {
   *   class Builder {
   *     public Builder() {...}
   *     public Builder(MyObject copy) {...}
   *
   *     MyObject build() {...}
   *   }
   * }
   * </pre>
   *
   * To find a builder, a nested class needs to have:
   * 1) a public or package private no-arg constructor (or static method).
   * 2) a public or package private single arge constructor (or static method) which takes the
   * outer class as a parameter.
   *
   * Discovery only operates on one nested level, but is recursive, so you can have
   * namespaced value objects.
   *
   * Assumes that all third party classes are available on the class path at processing time.
   * If this is not the case, we may need to use a processing method that allows deferring of
   * compilation.
   */
  abstract static class ThirdPartyNestedBuilderTypeStrategy implements Strategy {

    @Override
    public boolean isAttributeBuilder() {
      return false;
    }

    @Override
    public AttributeBuilderDescriptor getAttributeBuilderDescriptor() {
      throw new UnsupportedOperationException();
    }
  }
}
