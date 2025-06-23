package org.immutables.value.processor.meta;

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
import javax.lang.model.type.TypeKind;
import javax.lang.model.util.Types;
import com.google.common.base.Preconditions;
import org.immutables.value.Value.Derived;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Lazy;
import org.immutables.value.Value.Parameter;
import org.immutables.value.processor.meta.AttributeBuilderDescriptor.ValueToBuilderTarget;

/**
 * Reflects over the returnType and generates strings for the {@link AttributeBuilderDescriptor}
 */
@Immutable(builder = false)
public abstract class AttributeBuilderReflection {
  private static final Map<String, AttributeBuilderDescriptor> analyzedReturnTypes = new HashMap<>();

  // The discovery is based off of the class being investigated, AND the current attributeBuilder discovery pattern
  // The same class included in two parents, may or may not be nested builders based on that discovery pattern
  private static String cachingKey(ValueAttribute valueAttribute) {
    return valueAttribute.containedTypeElement.getQualifiedName()
        + Arrays.toString(valueAttribute.containingType.constitution.style().attributeBuilder());
  }

  public static AttributeBuilderReflection forValueType(ValueAttribute valueAttribute) {
    return ImmutableAttributeBuilderReflection.of(valueAttribute);
  }

  @Parameter
  abstract ValueAttribute valueAttribute();

  @Lazy
  List<Strategy> getStrategies() {
    // Order here matters. We want first party to be found first.
    return Arrays.asList(
        ImmutableFirstPartyStrategy.of(valueAttribute()),
        ThirdPartyAttributeBuilderStrategy.of(valueAttribute())
    );
  }

  @Lazy
  boolean isAttributeBuilder() {
    if (!valueAttribute().style().attributeBuilderDetection()) {
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

  @Lazy Strategy getReflectionStrategy() {
    for (Strategy strategy : getStrategies()) {
      if (strategy.isAttributeBuilder()) {
        return strategy;
      }
    }

    throw new AssertionError("isAttributeBuilder flip-flopped from true to false.");
  }

  /**
   * Allows for different mechanisms of discovering nested builders.
   * <p>
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
      return valueAttribute().attributeValueType != null
          && valueAttribute().attributeValueType.isUseBuilder();
    }

    @Override
    public AttributeBuilderDescriptor getAttributeBuilderDescriptor() {
      ValueAttribute attribute = valueAttribute();
      ValueType type = attribute.attributeValueType;

      return ImmutableAttributeBuilderDescriptor.builder()
          .attributeName(attribute.name())
          .valueToBuilderTarget(ValueToBuilderTarget.BUILDER_INSTANCE)
          .valueToBuilderMethod(type.names().from)
          .buildMethod(type.names().build)
          .qualifiedValueTypeName(type.typeValue().toString())
          .qualifiedBuilderTypeName(type.typeBuilder().toString())
          .qualifiedBuilderConstructorMethod(type.factoryBuilder().toString())
          .build();
    }
  }

  /**
   * Strategy for parsing third party immutables. for example: the protocol buffer API.
   * <p>
   * Assumes that all third party classes are available on the class path at processing time.
   * <p>
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
    abstract AttributeBuilderThirdPartyModel builderModel();

    /**
     * Guaranteed not null if isAttributeBuilder is true.
     * @return containingType of the value attribute.
     */
    @Nullable @Parameter
    abstract TypeElement attributeValueType();

    @Parameter
    abstract String attributeName();

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
      AttributeBuilderThirdPartyModel model = builderModel();
      ExecutableElement copyMethod = model.copyMethod();
      ExecutableElement builderMethod = model.builderMethod();
      ExecutableElement buildMethod = model.buildMethod();
      TypeElement attributeBuilderType = model.builderType();

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
        qualifiedBuilderConstructorMethod =
            attributeValueType().getQualifiedName() + "." + builderMethod.getSimpleName();
      } else {
        if (builderMethod.getKind() == ElementKind.CONSTRUCTOR) {
          qualifiedBuilderConstructorMethod = "new " + attributeBuilderType.getQualifiedName();
        } else {
          qualifiedBuilderConstructorMethod =
              attributeBuilderType.getQualifiedName() + "." + builderMethod.getSimpleName();
        }
      }

      return ImmutableAttributeBuilderDescriptor.builder()
          .attributeName(attributeName())
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
        return ImmutableThirdPartyAttributeBuilderStrategy.of(null, null, valueAttribute.name());
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
            return ImmutableThirdPartyAttributeBuilderStrategy.of(
                maybeCompleteModel.toImmutable(),
                valueAttribute.containedTypeElement,
                valueAttribute.name());
          }
        }
      }

      return ImmutableThirdPartyAttributeBuilderStrategy.of(null, null, valueAttribute.name());
    }

    // NB: because of the null checks here, we will prefer using static initialization
    // from the value object, but eh, doesn't really work that well because we may
    // break out of the value loop if this call to processPossibleBuilder completes the model.
    private static void processPossibleBuilder(ValueAttribute attribute, AttributeBuilderThirdPartyModel.Creator builderModel) {
      for (Element possibleBuildMethodOrConstructor : builderModel.findBuilderType().getEnclosedElements()) {

        if (builderModel.buildMethod() == null
            && isPossibleBuildMethod(attribute, possibleBuildMethodOrConstructor)) {
          builderModel.buildMethod((ExecutableElement) possibleBuildMethodOrConstructor);
        }

        if (builderModel.builderMethod() == null
            && isPossibleBuilderMethod(possibleBuildMethodOrConstructor, false, attribute)) {
          builderModel.builderMethod((ExecutableElement) possibleBuildMethodOrConstructor);
        }

        if (builderModel.copyMethod() == null
            && isPossibleCopyMethod(attribute, possibleBuildMethodOrConstructor, false)) {
          builderModel.copyMethod((ExecutableElement) possibleBuildMethodOrConstructor);
        }
      }
    }

    /**
     * Returns true if there's a public way to build the value type with an instance no-arg method.
     * @param attribute value attribute to check.
     * @param possibleBuildMethod method which matches {@link StyleMirror#attributeBuilder()}
     * @return true if this is the possibleBuildMethod can build the value type.
     */
    private static boolean isPossibleBuildMethod(ValueAttribute attribute, Element possibleBuildMethod) {
      if (possibleBuildMethod.getKind() != ElementKind.METHOD) {
        return false;
      }

      if (!attribute.containingType.names().possibleAttributeBuilder(possibleBuildMethod.getSimpleName())) {
        return false;
      }

      Types typeUtils = attribute.containingType.constitution.protoclass()
          .environment()
          .processing()
          .getTypeUtils();

      ExecutableElement candidateBuildMethod = (ExecutableElement) possibleBuildMethod;
      return !candidateBuildMethod.getModifiers().contains(Modifier.STATIC)
          && candidateBuildMethod.getModifiers().contains(Modifier.PUBLIC)
          && candidateBuildMethod.getTypeParameters().isEmpty()
          && candidateBuildMethod.getReturnType().getKind() == TypeKind.DECLARED
          && typeUtils.isSameType(candidateBuildMethod.getReturnType(), attribute.containedTypeElement.asType());
    }

    /**
     * Return true if the possibleBuilderMethod matches the
     * Style#attributeBuilder() and returns a class.
     * TODO: may need to make this return true if the return type is an interface too...
     * @param possibleBuilderMethod executableElement
     */
    private static boolean isPossibleBuilderMethod(Element possibleBuilderMethod, boolean onValueType,
        ValueAttribute valueAttribute) {
      if (possibleBuilderMethod.getKind() == ElementKind.METHOD) {
        if (!valueAttribute.containingType.names().possibleAttributeBuilder(possibleBuilderMethod.getSimpleName())) {
          return false;
        }

        ExecutableElement candidateMethod = (ExecutableElement) possibleBuilderMethod;

        TypeKind kind = candidateMethod.getReturnType().getKind();

        return possibleBuilderMethod.getModifiers().containsAll(
            Arrays.asList(Modifier.STATIC, Modifier.PUBLIC))
            && candidateMethod.getParameters().isEmpty()
            && candidateMethod.getReturnType().getKind() == TypeKind.DECLARED
            && !kind.isPrimitive() && kind != TypeKind.ARRAY;
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
     * @param possibleBuilderClass nested value element that could be builder class.
     * @return true if it's a static inner class.
     */
    private static boolean isPossibleBuilderClass(Element possibleBuilderClass, ValueAttribute valueAttribute) {
      if (possibleBuilderClass.getKind() == ElementKind.CLASS) {
        return possibleBuilderClass.getModifiers().contains(Modifier.STATIC)
            && possibleBuilderClass.getKind() == ElementKind.CLASS
            && valueAttribute.containingType.names().possibleAttributeBuilder(possibleBuilderClass.getSimpleName());
      }

      return false;
    }

    /**
     * Applies to both builder and value candidates.
     * @param valueAttribute the valueAttribute.
     * @param possibleCopyMethod candidate to check.
     */
    protected static boolean isPossibleCopyMethod(
        ValueAttribute valueAttribute,
        Element possibleCopyMethod,
        boolean onValueType) {
      if (possibleCopyMethod.getKind() == ElementKind.METHOD) {
        if (!valueAttribute.containingType.names().possibleAttributeBuilder(possibleCopyMethod.getSimpleName())) {
          return false;
        }

        ExecutableElement candidateCopyMethod = (ExecutableElement) possibleCopyMethod;

        Types typeUtils = valueAttribute.containingType.constitution.protoclass()
            .environment()
            .processing()
            .getTypeUtils();

        if (candidateCopyMethod.getParameters().size() == 1
            && typeUtils.isSameType(
            candidateCopyMethod.getParameters().get(0).asType(),
            valueAttribute.containedTypeElement.asType())) {

          TypeKind kind = candidateCopyMethod.getReturnType().getKind();
          return !kind.isPrimitive() && kind != TypeKind.ARRAY;
          // handle proto style toBuilder() copy method... lots of BuilderModels created because of this
        } else if (onValueType
            && candidateCopyMethod.getParameters().isEmpty()
            && !candidateCopyMethod.getModifiers().contains(Modifier.STATIC)) {

          TypeKind kind = candidateCopyMethod.getReturnType().getKind();
          return !kind.isPrimitive() && kind != TypeKind.ARRAY;
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
