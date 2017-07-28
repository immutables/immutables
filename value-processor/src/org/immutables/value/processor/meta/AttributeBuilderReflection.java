package org.immutables.value.processor.meta;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import org.immutables.value.Value.Derived;
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
  private static Map<String, AttributeBuilderDescriptor> analyzedReturnTypes = new HashMap<>();

  // The discovery is based off of the class being investigated, AND the current attributeBuilder discovery pattern
  // The same class included in two parents, may or may not be nested builders based on that discovery pattern
  private static String cachingKey(ValueAttribute valueAttribute) {
    return String.format("%s-%s",
        valueAttribute.containedTypeElement.getQualifiedName(),
        Joiner.on(".").join(valueAttribute.containingType.constitution.style().attributeBuilder()));
  }

  public static AttributeBuilderReflection forValueType(ValueAttribute valueAttribute) {
    return ImmutableAttributeBuilderReflection.of(valueAttribute);
  }

  @Parameter
  abstract ValueAttribute valueAttribute();

  @Lazy
  protected List<Strategy> getStrategies() {
    // Order here matters. We want first party to be found first.
    return Arrays.asList(
        ImmutableFirstPartyStrategy.of(valueAttribute()),
        ThirdPartyAttributeBuilderStrategy.of(valueAttribute())
    );
  }

  @Lazy
  boolean isAttributeBuilder() {
    if (!valueAttribute().containingType.constitution.style().attributeBuilderDetection()) {
      return false;
    }

    if (valueAttribute().containedTypeElement == null) {
      return false;
    }

    String cacheKey = cachingKey(valueAttribute());

    if (analyzedReturnTypes.containsKey(cacheKey)) {
      return analyzedReturnTypes.get(cacheKey) != null;
    }

    for (Strategy strategy : getStrategies()) {
      if (strategy.isAttributeBuilder()) {
        return true;
      }
    }

    analyzedReturnTypes.put(cacheKey, null);

    return false;
  }

  @Lazy
  AttributeBuilderDescriptor getAttributeBuilderDescriptor() {
    if (!isAttributeBuilder()) {
      throw new IllegalStateException(
          "Should not call getReflectionStrategy unless isAttributeBuilder is true");
    }

    if (valueAttribute().containedTypeElement == null) {
      throw new AssertionError();
    }

    String cacheKey = cachingKey(valueAttribute());
    if (analyzedReturnTypes.containsKey(cacheKey)) {
      return Preconditions.checkNotNull(analyzedReturnTypes
          .get(cacheKey));
    }

    AttributeBuilderDescriptor descriptor = getReflectionStrategy().getAttributeBuilderDescriptor();
    analyzedReturnTypes.put(cacheKey, descriptor);

    return descriptor;
  }

  @Lazy
  protected Strategy getReflectionStrategy() {
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
      return ImmutableAttributeBuilderDescriptor.builder()
          .valueToBuilderTarget(ValueToBuilderTarget.BUILDER_INSTANCE)
          .valueToBuilderMethod(attributeValueType().names().from)
          .buildMethod(attributeValueType().names().build)
          .qualifiedValueTypeName(attributeValueType().typeImmutable().toString())
          .qualifiedBuilderTypeName(attributeValueType().typeBuilderImpl().toString())
          .qualifiedBuilderConstructorMethod(attributeValueType().factoryBuilder().toString())
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
   *
   * If this is not the case, we may need to use a processing method that allows deferring of
   * compilation. Example, moving from auto-value to immutables... though maybe you would just
   * implement a new strategy for that use case...
   */
  @Immutable(builder = false)
  abstract static class ThirdPartyAttributeBuilderStrategy implements Strategy {

    /**
     * Guaranteed not null if isAttributeBuilder is true.
     * @return model of how to generate attributeBuilder.
     */
    @Nullable @Parameter
    protected abstract AttributeBuilderThirdPartyModel builderModel();

    /**
     * Guaranteed not null if isAttributeBuilder is true.
     * @return containingType of the value attribute.
     */
    @Nullable @Parameter
    protected abstract TypeElement attributeValueType();

    @Override
    public boolean isAttributeBuilder() {
      return builderModel() != null;
    }

    @Nullable
    @Override
    @Derived
    public AttributeBuilderDescriptor getAttributeBuilderDescriptor() {
      if (!isAttributeBuilder()) {
        return null;
      }

      ValueToBuilderTarget target;
      ExecutableElement copyMethod = builderModel().copyMethod();
      ExecutableElement builderMethod = builderModel().builderMethod();
      ExecutableElement buildMethod = builderModel().buildMethod();
      TypeElement attributeBuilderType = builderModel().builderType();

      if (copyMethod.getKind() == ElementKind.CONSTRUCTOR) {
        target = ValueToBuilderTarget.BUILDER_CONSTRUCTOR;
      } else if (copyMethod.getModifiers().contains(Modifier.STATIC)) {
        if (copyMethod.getEnclosingElement().equals(attributeValueType())) {
          target = ValueToBuilderTarget.VALUE_TYPE;
        } else {
          target = ValueToBuilderTarget.BUILDER_TYPE;
        }
      } else {
        if (copyMethod.getEnclosingElement().equals(attributeValueType())) {
          target = ValueToBuilderTarget.VALUE_INSTANCE;
        } else {
          target = ValueToBuilderTarget.BUILDER_INSTANCE;
        }
      }

      String qualifiedBuilderConstructorMethod;
      if (builderMethod.getEnclosingElement().equals(attributeValueType())) {
        qualifiedBuilderConstructorMethod = String.format("%s.%s",
            attributeValueType().getQualifiedName(),
            builderMethod.getSimpleName());
      } else {
        if (builderMethod.getKind() == ElementKind.CONSTRUCTOR) {
          qualifiedBuilderConstructorMethod = String.format("new %s",
              attributeBuilderType.getQualifiedName());
        } else {
          qualifiedBuilderConstructorMethod = String.format("%s.%s",
              attributeBuilderType.getQualifiedName(),
              builderMethod.getSimpleName());
        }
      }

      return ImmutableAttributeBuilderDescriptor.builder()
          .valueToBuilderTarget(target)
          .valueToBuilderMethod(copyMethod.getSimpleName().toString())
          .buildMethod(buildMethod.getSimpleName().toString())
          .qualifiedValueTypeName(attributeValueType().getQualifiedName().toString())
          .qualifiedBuilderTypeName(attributeBuilderType.getQualifiedName().toString())
          .qualifiedBuilderConstructorMethod(qualifiedBuilderConstructorMethod)
          .build();
    }

    /**
     * @return strategy which has an AttributeBuilderDescriptor if attributeValue is an attributeBuilder.
     */
    static ThirdPartyAttributeBuilderStrategy of(ValueAttribute valueAttribute) {
      TypeElement attributeValueType = valueAttribute.containedTypeElement;
      if (attributeValueType == null) {
        return ImmutableThirdPartyAttributeBuilderStrategy.of(null, null);
      }

      // Map of possible builder class to needed methods.
      Map<TypeElement, AttributeBuilderThirdPartyModel.Creator> partiallyBuiltModels = new HashMap<>();
      for (Element possibleBuilderMethodCopyMethodOrClass
          : attributeValueType.getEnclosedElements()) {
        AttributeBuilderThirdPartyModel.Creator newBuilderModel = ModifiableCreator.create();

        if (isPossibleBuilderClass(possibleBuilderMethodCopyMethodOrClass, valueAttribute)) {
          newBuilderModel.builderType((TypeElement) possibleBuilderMethodCopyMethodOrClass);
        } else if (isPossibleBuilderMethod(possibleBuilderMethodCopyMethodOrClass, true, valueAttribute)) {
          newBuilderModel
              .builderMethod((ExecutableElement) possibleBuilderMethodCopyMethodOrClass);
        } else if (isPossibleCopyMethod(valueAttribute,
            possibleBuilderMethodCopyMethodOrClass, true)) {
          newBuilderModel
              .copyMethod((ExecutableElement) possibleBuilderMethodCopyMethodOrClass);
        }

        // We found something on the loop interesting
        if (newBuilderModel.findBuilderType() != null) {
          AttributeBuilderThirdPartyModel.Creator maybeCompleteModel;

          if (partiallyBuiltModels.containsKey(newBuilderModel.findBuilderType())) {
            AttributeBuilderThirdPartyModel.Creator partiallyBuiltModel = partiallyBuiltModels
                .get(newBuilderModel.findBuilderType());
            partiallyBuiltModel.mergeFrom(newBuilderModel);
            maybeCompleteModel = partiallyBuiltModel;
          } else {
            processPossibleBuilder(valueAttribute, newBuilderModel);
            partiallyBuiltModels.put(newBuilderModel.findBuilderType(), newBuilderModel);
            maybeCompleteModel = newBuilderModel;
          }

          if (maybeCompleteModel.complete()) {
            return ImmutableThirdPartyAttributeBuilderStrategy.of(maybeCompleteModel.toImmutable(), valueAttribute.containedTypeElement);
          }
        }
      }

      return ImmutableThirdPartyAttributeBuilderStrategy.of(null, null);
    }


    // NB: because of the null checks here, we will prefer using static initialization
    // from the value object, but eh, doesn't really work that well because we may
    // break out of the value loop if this call to processPossibleBuilder completes the model.
    private static void processPossibleBuilder(ValueAttribute valueAttribute,
        AttributeBuilderThirdPartyModel.Creator builderModel) {
      for (Element possibleBuildMethodOrConstructor
          : builderModel.findBuilderType().getEnclosedElements()) {

        if (builderModel.buildMethod() == null
            && isPossibleBuildMethod(valueAttribute,
            possibleBuildMethodOrConstructor)) {
          builderModel.buildMethod((ExecutableElement) possibleBuildMethodOrConstructor);
        }

        if (builderModel.builderMethod() == null
            && isPossibleBuilderMethod(possibleBuildMethodOrConstructor, false, valueAttribute)) {
          builderModel.builderMethod((ExecutableElement) possibleBuildMethodOrConstructor);
        }

        if (builderModel.copyMethod() == null
            && isPossibleCopyMethod(valueAttribute, possibleBuildMethodOrConstructor, false)) {
          builderModel.copyMethod((ExecutableElement) possibleBuildMethodOrConstructor);
        }
      }
    }

    /**
     * Returns true if there's a public way to build the value type with an instance no-arg method.
     *
     * @param valueAttribute value attribute to check.
     * @param possibleBuildMethod method which matches {@link StyleMirror#attributeBuilder()}
     * @return true if this is the possibleBuildMethod can build the value type.
     */
    private static boolean isPossibleBuildMethod(ValueAttribute valueAttribute, Element possibleBuildMethod) {
      if (possibleBuildMethod.getKind() != ElementKind.METHOD) {
        return false;
      }

      ExecutableElement candidateBuildMethod = (ExecutableElement) possibleBuildMethod;
      return !candidateBuildMethod.getModifiers().contains(Modifier.STATIC)
          && candidateBuildMethod.getModifiers().contains(Modifier.PUBLIC)
          && candidateBuildMethod.getTypeParameters().isEmpty()
          && candidateBuildMethod.getReturnType().getKind() == TypeKind.DECLARED
          && (candidateBuildMethod.getReturnType()).equals(valueAttribute.containedTypeElement.asType());
    }

    /**
     * Return true if the possibleBuilderMethod matches the
     * Style#attributeBuilder() and returns a class.
     *
     * TODO: may need to make this return true if the return type is an interface too...
     *
     * @param possibleBuilderMethod executableElement
     */
    private static boolean isPossibleBuilderMethod(Element possibleBuilderMethod, boolean onValueType, ValueAttribute valueAttribute) {
      if (possibleBuilderMethod.getKind() == ElementKind.METHOD) {
        String detectedAttributeBuilder = valueAttribute.containingType.names()
            .rawFromAttributeBuilder(possibleBuilderMethod.getSimpleName().toString());

        if (detectedAttributeBuilder.isEmpty()) {
          return false;
        }
        ExecutableElement candidateMethod = (ExecutableElement) possibleBuilderMethod;

        return possibleBuilderMethod.getModifiers().containsAll(
            Arrays.asList(Modifier.STATIC, Modifier.PUBLIC))
            && candidateMethod.getParameters().isEmpty()
            && candidateMethod
            .getReturnType().getKind()
            == TypeKind.DECLARED
            && ((DeclaredType) candidateMethod.getReturnType())
            .asElement().getKind() == ElementKind.CLASS;

      } else if (!onValueType && possibleBuilderMethod.getKind() == ElementKind.CONSTRUCTOR) {
        if (!valueAttribute.containingType.names().newTokenInAttributeBuilder()) {
          return false;
        }

        ExecutableElement candidateConstructor = (ExecutableElement) possibleBuilderMethod;

        return candidateConstructor.getModifiers().contains(Modifier.PUBLIC)
            && candidateConstructor.getTypeParameters().isEmpty();
      }
      return false;
    }

    /**
     * Determine if inner class could be a builder.
     *
     * @param possibleBuilderClass nested value element that could be builder class.
     * @return true if it's a static inner class.
     */
    private static boolean isPossibleBuilderClass(Element possibleBuilderClass, ValueAttribute valueAttribute) {
      if (possibleBuilderClass.getKind() == ElementKind.CLASS) {

        if (valueAttribute.containingType.names().newTokenInAttributeBuilder()) {
          return possibleBuilderClass.getModifiers().contains(Modifier.STATIC)
              && possibleBuilderClass.getKind() == ElementKind.CLASS;
        }
      }

      return false;
    }

    /**
     * Applies to both builder and value candidates.
     *
     * @param valueAttribute the valueAttribute.
     * @param possibleCopyMethod candidate to check.
     */
    protected static boolean isPossibleCopyMethod(ValueAttribute valueAttribute,
        Element possibleCopyMethod, boolean onValueType) {
      if (possibleCopyMethod.getKind() == ElementKind.METHOD) {
        ExecutableElement candidateCopyMethod = (ExecutableElement) possibleCopyMethod;

        if (candidateCopyMethod.getParameters().size() == 1
            && candidateCopyMethod.getParameters().get(0).asType().equals(valueAttribute.containedTypeElement.asType())) {

          return true;
          // handle proto style toBuilder() copy method... lots of BuilderModels created because of this
        } else if (onValueType
            && candidateCopyMethod.getParameters().size() == 0
            && !candidateCopyMethod.getModifiers().contains(Modifier.STATIC)) {
          return true;
        }
      } else if (!onValueType && possibleCopyMethod.getKind() == ElementKind.CONSTRUCTOR) {

        if (!valueAttribute.containingType.names().newTokenInAttributeBuilder()) {
          return false;
        }

        ExecutableElement candidateConstructor = (ExecutableElement) possibleCopyMethod;
        return candidateConstructor.getParameters().size() == 1
            && candidateConstructor.getParameters().get(0).asType().equals(valueAttribute.containedTypeElement.asType());
      }

      return false;
    }
  }
}
