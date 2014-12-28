/*
    Copyright 2013-2014 Immutables Authors and Contributors

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.immutables.value.processor.meta;

import com.google.common.base.Functions;
import com.google.common.base.Joiner;
import com.google.common.base.Optional;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.immutables.value.Json;
import org.immutables.value.Mongo;
import org.immutables.value.Value;
import org.immutables.value.processor.meta.Proto.Protoclass;
import org.immutables.value.processor.meta.Styles.UsingName.AttributeNames;
import javax.annotation.Nullable;
import javax.lang.model.element.*;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.util.*;

/**
 * It's pointless to refactor this mess until
 * 1) Some sort of type calculus toolkit used/created
 * 2) Facets/Implicits in Generator toolkit with auto-memoising implemented
 */
public class ValueAttribute extends TypeIntrospectionBase {
  private static final Joiner COMMA_JOINER = Joiner.on(", ");
  private static final String NULLABLE_PREFIX = "@javax.annotation.Nullable ";
  private static final String NULLABLE_SIMPLE_NAME = "Nullable";
  private static final String ID_ATTRIBUTE_NAME = "_id";

  public AttributeNames names;

  public String name() {
    return names.raw;
  }

  public boolean isGenerateFunction;
  public boolean isGeneratePredicate;
  public boolean isGenerateDefault;
  public boolean isGenerateDerived;
  public boolean isGenerateAbstract;
  public boolean isGenerateLazy;
  public ImmutableList<String> typeParameters = ImmutableList.of();
  public Reporter reporter;

  TypeMirror returnType;
  Element element;
  ValueType containingType;
  String returnTypeName;

  public boolean isBoolean() {
    return internalTypeMirror().getKind() == TypeKind.BOOLEAN;
  }

  public boolean isStringType() {
    return returnTypeName.equals(String.class.getName());
  }

  public boolean charType() {
    return internalTypeMirror().getKind() == TypeKind.CHAR;
  }

  public String atNullability() {
    return isNullable() ? NULLABLE_PREFIX : "";
  }

  public boolean isSimpleLiteralType() {
    return isPrimitive()
        || isStringType()
        || isEnumType();
  }

  public boolean isMandatory() {
    return isGenerateAbstract && !isContainerType() && !isNullable();
  }

  public boolean isNullable() {
    return nullable;
  }

  @Override
  public boolean isComparable() {
    return isNumberType() || super.isComparable();
  }

  @Nullable
  private String marshaledName;

  public String getMarshaledName() {
    if (marshaledName == null) {
      marshaledName = inferMarshaledName();
    }
    return marshaledName;
  }

  private String inferMarshaledName() {
    @Nullable Json.Named namedAnnotaion = element.getAnnotation(Json.Named.class);
    if (namedAnnotaion != null) {
      String name = namedAnnotaion.value();
      if (!name.isEmpty()) {
        return name;
      }
    }
    @Nullable Mongo.Id idAnnotation = element.getAnnotation(Mongo.Id.class);
    if (idAnnotation != null) {
      return ID_ATTRIBUTE_NAME;
    }
    return names.raw;
  }

  public boolean isForceEmpty() {
    return element.getAnnotation(Json.ForceEmpty.class) != null;
  }

  @Override
  protected TypeMirror internalTypeMirror() {
    return returnType;
  }

  public String getType() {
    return returnTypeName;
  }

  public List<CharSequence> getAnnotations() {
    return AnnotationPrinting.getAnnotationLines(element);
  }

  public boolean isJsonIgnore() {
    return element.getAnnotation(Json.Ignore.class) != null;
  }

  private String implementationType;

  public String getImplementationType() {
    if (implementationType == null) {
      String implementationRawType = "";
      if (isMapType()) {
        if (isGenerateSortedMap()) {
          implementationRawType = UnshadeGuava.typeString("collect.ImmutableSortedMap");
        } else {
          implementationRawType = UnshadeGuava.typeString("collect.ImmutableMap");
        }
      } else if (isListType()) {
        implementationRawType = UnshadeGuava.typeString("collect.ImmutableList");
      } else if (isGenerateSortedSet()) {
        implementationRawType = UnshadeGuava.typeString("collect.ImmutableSortedSet");
      } else if (isSetType()) {
        implementationRawType = isGenerateOrdinalValueSet()
            ? "org.immutables.common.".concat("collect.ImmutableOrdinalSet")
            : UnshadeGuava.typeString("collect.ImmutableSet");
      }

      if (implementationRawType.isEmpty()) {
        implementationType = getType();
      } else {
        implementationType = implementationRawType;

        List<String> parameters = typeParameters();
        if (!parameters.isEmpty()) {
          implementationType += "<" + COMMA_JOINER.join(parameters) + ">";
        }
      }
    }
    return implementationType;
  }

  public List<String> typeParameters() {
    ensureTypeIntrospected();
    return arrayComponent != null ? ImmutableList.of(arrayComponent.toString()) : typeParameters;
  }

  private Boolean isMapType;

  public boolean isMapType() {
    if (isRegularAttribute()) {
      return false;
    }
    if (isMapType == null) {
      isMapType = returnTypeName.startsWith(Map.class.getName())
          || isSortedMapType();
    }
    return isMapType;
  }

  private Boolean isListType;

  public boolean isListType() {
    if (isRegularAttribute()) {
      return false;
    }
    if (isListType == null) {
      isListType = returnTypeName.startsWith(List.class.getName());
    }
    return isListType;
  }

  @Nullable
  private Boolean isSetType;
  @Nullable
  private Boolean isSortedSetType;
  @Nullable
  private Boolean isSortedMapType;
  @Nullable
  private Boolean shouldGenerateSortedSet;
  @Nullable
  private Boolean shouldGenerateSortedMap;

  private OrderKind orderKind = OrderKind.NONE;

  private enum OrderKind {
    NONE, NATURAL, REVERSE
  }

  public boolean isSetType() {
    if (isRegularAttribute()) {
      return false;
    }
    if (isSetType == null) {
      isSetType = returnTypeName.startsWith(Set.class.getName());
    }
    return isSetType;
  }

  public boolean hasNaturalOrder() {
    return orderKind == OrderKind.NATURAL;
  }

  public boolean hasReverseOrder() {
    return orderKind == OrderKind.REVERSE;
  }

  public boolean isSortedSetType() {
    if (isRegularAttribute()) {
      return false;
    }
    if (isSortedSetType == null) {
      isSortedSetType = returnTypeName.startsWith(SortedSet.class.getName())
          || returnTypeName.startsWith(NavigableSet.class.getName());
    }
    return isSortedSetType;
  }

  public boolean isSortedMapType() {
    if (isRegularAttribute()) {
      return false;
    }
    if (isSortedMapType == null) {
      isSortedMapType = returnTypeName.startsWith(SortedMap.class.getName())
          || returnTypeName.startsWith(NavigableMap.class.getName());
    }
    return isSortedMapType;
  }

  public boolean isGenerateSortedSet() {
    if (isRegularAttribute()) {
      return false;
    }
    if (shouldGenerateSortedSet == null) {
      boolean isEligibleSetType = isSortedSetType();
      checkOrderAnnotations();
      shouldGenerateSortedSet = isEligibleSetType && orderKind != OrderKind.NONE;
    }
    return shouldGenerateSortedSet;
  }

  public boolean isGenerateSortedMap() {
    if (isRegularAttribute()) {
      return false;
    }
    if (shouldGenerateSortedMap == null) {
      boolean isEligibleType = isSortedMapType();
      checkOrderAnnotations();
      shouldGenerateSortedMap = isEligibleType && orderKind != OrderKind.NONE;
    }
    return shouldGenerateSortedMap;
  }

  private void checkOrderAnnotations() {
    boolean isEligibleType = isSortedMapType() || isSortedSetType();

    @Nullable Value.NaturalOrder naturalOrderAnnotation = element.getAnnotation(Value.NaturalOrder.class);
    @Nullable Value.ReverseOrder reverseOrderAnnotation = element.getAnnotation(Value.ReverseOrder.class);

    if (naturalOrderAnnotation != null && reverseOrderAnnotation != null) {
      reporter.withElement(element)
          .error("@Value.Natural and @Value.Reverse annotations could not be used on the same attribute");
    } else if (naturalOrderAnnotation != null) {
      if (isEligibleType) {
        if (isComparable()) {
          orderKind = OrderKind.NATURAL;
        } else {
          reporter.withElement(element)
              .forAnnotation(Value.NaturalOrder.class)
              .error("@Value.Natural should used on a set of Comparable elements (map keys)");
        }
      } else {
        reporter.withElement(element)
            .forAnnotation(Value.NaturalOrder.class)
            .error("@Value.Natural should specify order for SortedSet, SortedMap, NavigableSet or NavigableMap attributes");
      }
    } else if (reverseOrderAnnotation != null) {
      if (isEligibleType) {
        if (isComparable()) {
          orderKind = OrderKind.REVERSE;
        } else {
          reporter.withElement(element)
              .forAnnotation(Value.ReverseOrder.class)
              .error("@Value.Reverse should used with a set of Comparable elements");
        }
      } else {
        reporter.withElement(element)
            .forAnnotation(Value.ReverseOrder.class)
            .error("@Value.Reverse should specify order for SortedSet, SortedMap, NavigableSet or NavigableMap attributes");
      }
    }
  }

  private Boolean isOptionalType;

  public boolean isOptionalType() {
    if (isRegularAttribute()) {
      return false;
    }
    if (isOptionalType == null) {
      isOptionalType = returnTypeName.startsWith(UnshadeGuava.typeString("base.Optional"));
    }
    return isOptionalType;
  }

  public boolean isCollectionType() {
    return isSortedSetType() || isSetType() || isListType();
  }

  public boolean isGenerateEnumSet() {
    ensureTypeIntrospected();
    return isSetType() && hasEnumFirstTypeParameter;
  }

  @Nullable
  private CharSequence defaultInterface;

  public CharSequence defaultInterface() {
    if (!isGenerateDefault) {
      return "";
    }
    if (defaultInterface == null) {
      defaultInterface = inferDefaultInterface();
    }
    return defaultInterface;
  }

  private CharSequence inferDefaultInterface() {
    Element enclosing = element.getEnclosingElement();
    if (enclosing.getKind() == ElementKind.INTERFACE) {
      if (containingType.element.getKind() == ElementKind.INTERFACE) {
        return containingType.typeAbstract().relative();
      }
    }
    return "";
  }

  public boolean isGenerateEnumMap() {
    ensureTypeIntrospected();
    return isMapType() && hasEnumFirstTypeParameter;
  }

  public String getUnwrappedElementType() {
    return unwrapType(containmentTypeName());
  }

  public String getWrappedElementType() {
    return wrapType(containmentTypeName());
  }

  private String containmentTypeName() {
    return (isArrayType() || isContainerType()) ? firstTypeParameter() : returnTypeName;
  }

  public String getRawType() {
    String type = getType();
    int endIndex = type.length();
    int firstIndexOfGenerics = type.indexOf('<');
    if (firstIndexOfGenerics > 0) {
      endIndex = firstIndexOfGenerics;
    }
    return type.substring(0, endIndex);
  }

  public String getConsumedElementType() {
    return (isUnwrappedElementPrimitiveType()
        || String.class.getName().equals(containmentTypeName())
        || hasEnumFirstTypeParameter)
        ? getWrappedElementType()
        : "? extends " + getWrappedElementType();
  }

  private String extractRawType(String className) {
    int indexOfGenerics = className.indexOf('<');
    if (indexOfGenerics > 0) {
      return className.substring(0, indexOfGenerics);
    }
    return className;
  }

  public boolean isUnwrappedElementPrimitiveType() {
    return isPrimitiveType(getUnwrappedElementType());
  }

  public boolean isUnwrappedSecondaryElementPrimitiveType() {
    return isPrimitiveType(getUnwrappedSecondaryElementType());
  }

  public String firstTypeParameter() {
    return Iterables.getFirst(typeParameters(), "");
  }

  public String secondTypeParameter() {
    return Iterables.get(typeParameters(), 1);
  }

  public String getElementType() {
    return containmentTypeName();
  }

  @Nullable
  private List<SimpleTypeDerivationBase> expectedSubclasses;

  public List<SimpleTypeDerivationBase> getExpectedSubclasses() {
    if (expectedSubclasses == null) {
      if (isPrimitiveElement()) {
        expectedSubclasses = ImmutableList.<SimpleTypeDerivationBase>of();
      } else {
        @Nullable Iterable<TypeElement> expectedSubclassesType = findDeclaredSubclasses();
        expectedSubclasses =
            expectedSubclassesType != null
                ? toMarshaledTypeDerivations(expectedSubclassesType)
                : ImmutableList.<SimpleTypeDerivationBase>of();
      }
    }
    return expectedSubclasses;
  }

  @Nullable
  private Iterable<TypeElement> findDeclaredSubclasses() {
    if (element.getAnnotation(Json.Subclasses.class) != null) {
      return listExpectedSubclassesFromElement(element);
    }
    ensureTypeIntrospected();
    if (containedTypeElement != null) {
      return listExpectedSubclassesFromElement(containedTypeElement);
    }
    return null;
  }

  private List<SimpleTypeDerivationBase> toMarshaledTypeDerivations(Iterable<TypeElement> elements) {
    List<SimpleTypeDerivationBase> derivations = Lists.newArrayList();
    for (TypeElement element : elements) {
      Optional<Protoclass> protoclass = round.definedValueProtoclassFor(element);
      if (protoclass.isPresent()) {
        if (protoclass.get().declaringType().get().hasAnnotation(Json.Marshaled.class)) {
          derivations.add(new SimpleTypeDerivationBase(protoclass.get()));
        }
      }
    }
    return ImmutableList.copyOf(derivations);
  }

  /**
   * used to derive type name, using package prefix and simple name, it's expected that prefix of
   * suffix will be added to a type name
   */
  public static final class SimpleTypeDerivationBase {
    public final String packaged;
    public final String name;

    SimpleTypeDerivationBase(Protoclass protoclass) {
      this.packaged = protoclass.packageOf().asPrefix();
      this.name = protoclass.createTypeNames().raw;
    }
  }

  private static FluentIterable<TypeElement> listExpectedSubclassesFromElement(Element element) {
    return FluentIterable.from(
        ValueType.extractedTypesFromAnnotationMirrors(
            Json.Subclasses.class.getCanonicalName(),
            "value",
            element.getAnnotationMirrors()))
        .transform(Proto.DeclatedTypeToElement.FUNCTION);
  }

  @Nullable
  private TypeElement marshaledElement;
  @Nullable
  private TypeElement marshaledSecondaryElement;
  @Nullable
  private TypeElement specialMarshaledElement;
  @Nullable
  private TypeElement specialMarshaledSecondaryElement;

  private boolean hasEnumFirstTypeParameter;
  private TypeElement containedTypeElement;
  private boolean generateOrdinalValueSet;
  private TypeMirror arrayComponent;
  private boolean regularAttribute;
  private boolean nullable;
  Round round;

  public boolean isGenerateOrdinalValueSet() {
    if (!isSetType()) {
      return false;
    }
    ensureTypeIntrospected();
    return generateOrdinalValueSet;
  }

  public boolean isDocumentElement() {
    ensureTypeIntrospected();
    return containedTypeElement != null
        && containedTypeElement.getAnnotation(Mongo.Repository.class) != null;
  }

  public boolean isArrayType() {
    return !isRegularAttribute()
        && internalTypeMirror().getKind() == TypeKind.ARRAY;
  }

  @Override
  protected void introspectType() {
    TypeMirror typeMirror = internalTypeMirror();

    if (isContainerType()) {
      if (typeMirror instanceof DeclaredType) {
        DeclaredType declaredType = (DeclaredType) typeMirror;

        List<String> typeParameters = Lists.newArrayList();

        List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();

        if (!typeArguments.isEmpty()) {
          if (typeArguments.size() == 1) {
            final TypeMirror typeArgument = typeArguments.get(0);
            if (typeArgument instanceof DeclaredType) {
              typeMirror = typeArgument;
            }

            if (isSetType()) {
              generateOrdinalValueSet = new TypeIntrospectionBase() {
                @Override
                protected TypeMirror internalTypeMirror() {
                  return typeArgument;
                }
              }.isOrdinalValue();
            }
          }

          if (typeArguments.size() >= 1) {
            TypeMirror typeArgument = typeArguments.get(0);
            if (typeArgument instanceof DeclaredType) {
              TypeElement typeElement = (TypeElement) ((DeclaredType) typeArgument).asElement();
              hasEnumFirstTypeParameter = typeElement.getSuperclass().toString().startsWith(Enum.class.getName());
            }

            if (typeArguments.size() >= 2) {
              TypeMirror typeSecondArgument = typeArguments.get(1);

              if (typeSecondArgument instanceof DeclaredType) {
                TypeElement typeElement = (TypeElement) ((DeclaredType) typeSecondArgument).asElement();
                @Nullable Json.Marshaled generateMarshaler = typeElement.getAnnotation(Json.Marshaled.class);
                marshaledSecondaryElement = generateMarshaler != null ? typeElement : null;
                specialMarshaledSecondaryElement =
                    asSpecialMarshaledElement(marshaledSecondaryElement, typeElement);
              }
            }

            typeMirror = typeArgument;
          }

          typeParameters.addAll(Lists.transform(typeArguments, Functions.toStringFunction()));
        }

        this.typeParameters = ImmutableList.copyOf(typeParameters);
      }
    } else if (isArrayType()) {
      arrayComponent = ((ArrayType) typeMirror).getComponentType();
      typeMirror = arrayComponent;
    }

    if (typeMirror instanceof DeclaredType) {
      TypeElement typeElement = (TypeElement) ((DeclaredType) typeMirror).asElement();

      this.containedTypeElement = typeElement;

      @Nullable Json.Marshaled generateMarshaler = typeElement.getAnnotation(Json.Marshaled.class);
      marshaledElement = generateMarshaler != null ? typeElement : null;
      specialMarshaledElement = asSpecialMarshaledElement(marshaledElement, typeElement);
    }

    intospectTypeMirror(typeMirror);
  }

  private boolean isRegularAttribute() {
    return regularAttribute;
  }

  static boolean isRegularMarshalableType(String name) {
    return String.class.getName().equals(name)
        || isPrimitiveOrWrapped(name);
  }

  public String getRawCollectionType() {
    return isListType() ? List.class.getSimpleName()
        : isSetType() ? Set.class.getSimpleName()
            : isSortedSetType() ? SortedSet.class.getSimpleName() : "";
  }

  public String getSecondaryElementType() {
    return secondTypeParameter();
  }

  public String getUnwrappedSecondaryElementType() {
    return unwrapType(secondTypeParameter());
  }

  public String getWrappedSecondaryElementType() {
    return wrapType(secondTypeParameter());
  }

  public String getUnwrapperOrRawSecondaryElementType() {
    return extractRawType(getWrappedSecondaryElementType());
  }

  public String getUnwrapperOrRawElementType() {
    return extractRawType(getWrappedElementType());
  }

  public boolean isNumberType() {
    TypeKind kind = internalTypeMirror().getKind();
    return kind.isPrimitive()
        && kind != TypeKind.CHAR
        && kind != TypeKind.BOOLEAN;
  }

  public boolean isFloatType() {
    return isFloat() || isDouble();
  }

  public boolean isFloat() {
    return internalTypeMirror().getKind() == TypeKind.FLOAT;
  }

  public boolean isDouble() {
    return internalTypeMirror().getKind() == TypeKind.DOUBLE;
  }

  public boolean isNonRawElemementType() {
    return getElementType().indexOf('<') > 0;
  }

  public boolean isContainerType() {
    return isCollectionType()
        || isOptionalType()
        || isMapType();
  }

  public String getWrapperType() {
    return isPrimitive()
        ? wrapType(returnTypeName)
        : returnTypeName;
  }

  public boolean isPrimitive() {
    return internalTypeMirror().getKind().isPrimitive();
  }

  public int getConstructorArgumentOrder() {
    Value.Parameter annotation = element.getAnnotation(Value.Parameter.class);
    return annotation != null ? annotation.order() : -1;
  }

  public boolean isSpecialMarshaledElement() {
    ensureTypeIntrospected();
    return specialMarshaledElement != null;
  }

  public boolean isSpecialMarshaledSecondaryElement() {
    ensureTypeIntrospected();
    return specialMarshaledSecondaryElement != null;
  }

  public boolean isMarshaledElement() {
    if (isPrimitive()) {
      return false;
    }
    ensureTypeIntrospected();
    return marshaledElement != null;
  }

  public boolean isMarshaledSecondaryElement() {
    ensureTypeIntrospected();
    return marshaledSecondaryElement != null;
  }

  public boolean isPrimitiveElement() {
    return isPrimitiveType(getUnwrappedElementType());
  }

  private TypeElement asSpecialMarshaledElement(@Nullable TypeElement marshaled, TypeElement element) {
    if (marshaled != null) {
      return marshaled;
    }
    if (!isRegularMarshalableType(element.getQualifiedName().toString())) {
      return element;
    }
    return null;
  }

  Collection<SimpleTypeDerivationBase> getMarshaledImportRoutines() {
    Collection<TypeElement> imports = Lists.newArrayListWithExpectedSize(2);
    if (isMarshaledElement()) {
      imports.add(marshaledElement);
    }
    if (isMapType() && isMarshaledSecondaryElement()) {
      imports.add(marshaledSecondaryElement);
    }
    return toMarshaledTypeDerivations(imports);
  }

  Collection<String> getSpecialMarshaledTypes() {
    Collection<String> marshaledTypeSet = Lists.newArrayListWithExpectedSize(2);
    if (isSpecialMarshaledElement()) {
      String typeName = isContainerType()
          ? getUnwrappedElementType()
          : getType();

      marshaledTypeSet.add(typeName);
    }
    if (isMapType() && isSpecialMarshaledSecondaryElement()) {
      marshaledTypeSet.add(specialMarshaledSecondaryElement.getQualifiedName().toString());
    }
    return marshaledTypeSet;
  }

  public boolean isAuxiliary() {
    return element.getAnnotation(Value.Auxiliary.class) != null;
  }

  public boolean isMarshaledIgnore() {
    ensureTypeIntrospected();
    return isMarshaledElement();
  }

  boolean isIdAttribute() {
    return getMarshaledName().equals(ID_ATTRIBUTE_NAME);
  }

  /** Validates things that were not validated otherwise */
  void initAndValidate() {
    for (AnnotationMirror annotation : element.getAnnotationMirrors()) {
      if (annotation.getAnnotationType().asElement().getSimpleName().contentEquals(NULLABLE_SIMPLE_NAME)) {
        if (isPrimitive()) {
          reporter.withElement(element)
              .annotationNamed(NULLABLE_SIMPLE_NAME)
              .error("@Nullable could not be used with primitive type attibutes");
        } else {
          nullable = true;
          if (isContainerType()) {
            regularAttribute = true;
          }
        }
      }
    }

    if (isGenerateDefault && isContainerType()) {
      // ad-hoc. stick it here, but should work
      regularAttribute = true;

      reporter.withElement(element)
          .forAnnotation(Value.Default.class)
          .warning("@Value.Default on a container attribute make it lose it's special treatment");
    }
  }

  @Override
  public String toString() {
    return "Attribute[" + name() + "]";
  }
}
