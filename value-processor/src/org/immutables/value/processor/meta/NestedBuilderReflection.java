package org.immutables.value.processor.meta;

import java.util.Arrays;
import java.util.List;
import org.immutables.value.Value.Immutable;
import org.immutables.value.Value.Lazy;
import org.immutables.value.Value.Parameter;
import org.immutables.value.Value.Style;

/**
 * Reflects over the returnType and generates strings for the {@link NestedBuilderDescriptor}
 */
@Immutable(builder = false)
public abstract class NestedBuilderReflection {

  @Parameter
  abstract ValueAttribute valueAttribute();

  private List<Strategy> getStrategies() {
    return Arrays.asList(
        ImmutableFirstPartyStrategy.of(valueAttribute()),
        ImmutableThirdPartyStrategy.of(valueAttribute()));
  }

  boolean isNestedBuilder() {
    if (!valueAttribute().containingType.constitution.style().nestedBuilderDetection()) {
      return false;
    }

    for (Strategy strategy : getStrategies()) {
      if (strategy.isNestedBuilder()) {
        return true;
      }
    }
    return false;
  }

  NestedBuilderDescriptor getNestedBuilderDescriptor() {
    return getReflectionStrategy().getNestedBuilderDescriptor();
  }

  @Lazy
  Strategy getReflectionStrategy() {
    if (!isNestedBuilder()) {
      throw new RuntimeException(
          "Should not call getReflectionStrategy unless isNestedBuilder is true");
    }

    for (Strategy strategy : getStrategies()) {
      if (strategy.isNestedBuilder()) {
        return strategy;
      }
    }

    throw new AssertionError("isNestedBuilder flip-flopped from true to false.");
  }


  /**
   * Allows for different mechanisms of discovering nested builders.
   *
   * This is needed because Immutables value objects may not be available on the
   * class path during processing, which will be order dependent.
   */
  interface Strategy {

    boolean isNestedBuilder();

    NestedBuilderDescriptor getNestedBuilderDescriptor();
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
    public boolean isNestedBuilder() {
      return valueAttribute().attributeValueType != null &&
          valueAttribute().attributeValueType.isUseBuilder();

    }

    @Override
    public NestedBuilderDescriptor getNestedBuilderDescriptor() {
      System.out.println("MOTHER FUCKER");
      System.out.println(attributeValueType().names().build);
      String createNewBuilder = String.format("%s.%s",
          attributeValueType().getTopSimple(),
          attributeValueType().names().builder());
      return ImmutableNestedBuilderDescriptor.builder()
          .isCopyMethodStatic(true)
          .copyMethod(String.format("%s().%s",
              createNewBuilder, attributeValueType().names().from))
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
  abstract static class ThirdPartyStrategy implements Strategy {

    @Parameter
    abstract ValueAttribute valueAttribute();

    @Override
    public boolean isNestedBuilder() {
      return false;
    }

    @Override
    public NestedBuilderDescriptor getNestedBuilderDescriptor() {
      throw new UnsupportedOperationException();
    }
  }


  /**
   * Examines the {@link ValueAttribute#returnType}, determines if it follows a builder
   * pattern, and if it is, returns the DeclaredType of the ReturnedType.
   *
   * If {@link Style#nestedBuilderDetection()} is {@literal false}, return null.
   * If valueAttribute is an immutable, we return the proper shit
   * If valueAttribute is a list, then this returns the value declared type of the template.
   */
  /*
  @Lazy
  @Nullable
  public DeclaredType getValueDeclaredType() {
    if (!valueAttribute().styleInfo().nestedBuilderDetection()) {
      return null;
    }
    if (!possibleNestedBuilder()) {
      return null;
    }

    Object nestedBuilderStyle = null;

    DeclaredType nestedBuilderType = unboxedReturnType();
    if (nestedBuilderType == null) {
      return null;
    }

    if (isImmutable()) {
      nestedBuilderStyle = immutablesNestedBuilderStyle();
    } else {
      // TODO: need to do something better here to supprt metaInf, etc.
      nestedBuilderStyle = null;
    }

    return nestedBuilderStyle;
  }

  public boolean possibleNestedBuilder() {
    if (valueAttribute().typeKind().isRegular()
        && valueAttribute().returnType.getKind() == TypeKind.DECLARED) {
      return true;
    }

    if (valueAttribute().typeKind().isList()) {
      System.out.println(valueAttribute().firstTypeParameter());
      TypeElement unboxedReturnType = valueAttribute()
          .environment()
          .processing()
          .getElementUtils()
          .getTypeElement(valueAttribute().firstTypeParameter());
      return unboxedReturnType.getKind() == ElementKind.CLASS
          || unboxedReturnType.getKind() == ElementKind.INTERFACE;
    }

    return false;
  }

  @Lazy
  public boolean isImmutable() {
    return unboxedReturnType().asElement().getAnnotation(Immutable.class) != null;
  }
  */

  /**
   * @return type mirror for the return type, or the template argument of a single collection
   * element.
   */
  /*
  @Lazy
  protected DeclaredType unboxedReturnType() {
    if (valueAttribute().typeKind().isRegular()
        && valueAttribute().returnType.getKind() == TypeKind.DECLARED) {
      return (DeclaredType) valueAttribute().returnType;
    } else if (valueAttribute().typeKind().isList()) {
      TypeElement unboxedReturnType = valueAttribute()
          .environment()
          .processing()
          .getElementUtils()
          .getTypeElement(valueAttribute().firstTypeParameter());

      if (unboxedReturnType.getKind() == ElementKind.CLASS
          || unboxedReturnType.getKind() == ElementKind.INTERFACE) {
        return (DeclaredType) unboxedReturnType.asType();
      }
    }

    throw new RuntimeException(
        String.format("Could not handle valueAttribuet %s", valueAttribute()));
  }

  @Lazy
  protected Object immutablesNestedBuilderStyle() {
    return null;
  }

  @Lazy
  public ValueType getNestedImmutableValueType() {
    if (!isImmutable()) {
      throw new RuntimeException(
          "You should not be calling getNestedImmutableValueType for non immutable.");
    }

    ValueType nestedImmutable = new ValueType();
    DeclaredType returnType = unboxedReturnType();

    // TODO: get some help with this, we want to honor style of the generated immutable builder
    DeclaringType declaringType = valueAttribute().environment().round()
        .declaringTypeFrom((TypeElement) returnType.asElement());
    ImmutableProto.Protoclass protoClass = ImmutableProto.Protoclass.builder()
        .environment(valueAttribute().environment())
        .sourceElement(returnType.asElement())
        .packageOf(declaringType.packageOf())
        // Eh, tried  a bunch of these, not sure what it's supposed to be
        .kind(Kind.INCLUDED_ON_TYPE)
        .build();

    new ValueTypeComposer().compose(nestedImmutable, protoClass);
    return nestedImmutable;
  }

  @Lazy
  public TypeElement getThirdPartyValueType() {
    if (isImmutable()) {
      throw new RuntimeException(
          "You should not be calling getNestedThirdPartyBuilderType for an immutable");
    }

    return (TypeElement) unboxedReturnType().asElement();
  }

  @Lazy
  public TypeElement getNestedThirdPartyBuilderType() {
    if (isImmutable()) {
      throw new RuntimeException(
          "You should not be calling getNestedThirdPartyBuilderType for an immutable");
    }
    // This can't be null, because we shouldn't be trying to configure the attribute as a nested builder
    // A little convoluted... need to work on it.
    StyleInfo style = valueAttribute().styleInfo();

    DeclaredType returnType = unboxedReturnType();
    TypeElement typeElement = (TypeElement) returnType.asElement();

    ExecutableElement foundMethod = null;

    for (Element possibleMethod : typeElement.getEnclosedElements()) {
      if (possibleMethod.getSimpleName().toString()
          .equals(style.getBuilder())) {
        if (possibleMethod.getKind() == ElementKind.METHOD) {
          foundMethod = (ExecutableElement) possibleMethod;
          break;
        }
      }
    }

    if (foundMethod == null) {
      throw new RuntimeException(String.format(
          "NestedBuilder configuration error, Could not find builder for type %s with method %s",
          returnType, style.getBuilder()));
    }

    if (!(foundMethod.getReturnType().getKind() == TypeKind.DECLARED)) {
      throw new RuntimeException(
          String.format("NestedBuilder configuration error, found builder method, but "
                  + "it's not actually the type, is there a subclass or something that matches %s,"
                  + "return type %s of kind %s",
              style.getBuilder(), foundMethod.getReturnType(),
              foundMethod.getReturnType().getKind()));
    }

    return (TypeElement) ((DeclaredType) foundMethod.getReturnType()).asElement();

  }
  */
}
