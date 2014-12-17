/*
    Copyright 2013-2014 Immutables.org authors

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

import javax.lang.model.element.PackageElement;
import javax.annotation.processing.ProcessingEnvironment;
import com.google.common.base.Functions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.Set;
import java.util.SortedSet;
import javax.annotation.Nullable;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.immutables.generator.AnnotationMirrors;
import org.immutables.value.Json;
import org.immutables.value.Mongo;
import org.immutables.value.Value;
import org.immutables.value.processor.meta.Styles.UsingName.AttributeNames;

/**
 * It's pointless to refactor this mess until
 * 1) Some sort of type calculus toolkit used/created
 * 2) Facets/Implicits in Generator toolkit with auto-memoising implemented
 */
public class ValueAttribute extends TypeIntrospectionBase {
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
  public ImmutableList<String> typeParameters;
  public Reporter reporter;

  TypeMirror returnType;
  Element element;
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

  @Deprecated
  @Nullable
  public CharSequence getAnnotationDefaultValue() {
    // Currently it is not used?
    if (element instanceof ExecutableElement) {
      @Nullable AnnotationValue defaultValue = ((ExecutableElement) element).getDefaultValue();
      if (defaultValue != null) {
        return AnnotationMirrors.toCharSequence(defaultValue);
      }
    }
    return null;
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
      String type = getType();
      if (isMapType()) {
        implementationType = type.replace(Map.class.getName(),
            UnshadeGuava.typeString("collect.ImmutableMap"));
      } else if (isListType()) {
        implementationType = type.replace(List.class.getName(),
            UnshadeGuava.typeString("collect.ImmutableList"));
      } else if (isGenerateSortedSet()) {
        implementationType = type.replace(List.class.getName(),
            UnshadeGuava.typeString("collect.ImmutableSortedSet"));
      } else if (isSetType()) {
        implementationType = type.replace(Set.class.getName(),
            isGenerateOrdinalValueSet()
                ? "org.immutables.common.collect.".concat("ImmutableOrdinalSet")
                : UnshadeGuava.typeString("collect.ImmutableSet"));
      } else {
        implementationType = type;
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
      isMapType = returnTypeName.startsWith(Map.class.getName());
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
  private Boolean shouldGenerateSortedSet;
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
    return isSortedSetType() && orderKind == OrderKind.NATURAL;
  }

  public boolean hasReverseOrder() {
    return isSortedSetType() && orderKind == OrderKind.REVERSE;
  }

  public boolean isSortedSetType() {
    if (isRegularAttribute()) {
      return false;
    }
    if (isSortedSetType == null) {
      isSortedSetType = returnTypeName.startsWith(Set.class.getName())
          || returnTypeName.startsWith(SortedSet.class.getName())
          || returnTypeName.startsWith(NavigableSet.class.getName());
    }
    return isSortedSetType;
  }

  public boolean isGenerateSortedSet() {
    if (isRegularAttribute()) {
      return false;
    }
    if (shouldGenerateSortedSet == null) {
      boolean isEligibleSetType = isSortedSetType();

      @Nullable Value.Order.Natural naturalOrderAnnotation = element.getAnnotation(Value.Order.Natural.class);
      @Nullable Value.Order.Reverse reverseOrderAnnotation = element.getAnnotation(Value.Order.Reverse.class);

      if (naturalOrderAnnotation != null && reverseOrderAnnotation != null) {
        reporter.withElement(element)
            .error("@Value.Order.Natural and @Value.Order.Reverse annotations could not be used on the same attribute");
      } else if (naturalOrderAnnotation != null) {
        if (isEligibleSetType) {
          if (isComparable()) {
            orderKind = OrderKind.NATURAL;
          } else {
            reporter.withElement(element)
                .forAnnotation(Value.Order.Natural.class)
                .error("@Value.Order.Natural should used on a set of Comparable elements");
          }
        } else {
          reporter.withElement(element)
              .forAnnotation(Value.Order.Natural.class)
              .error("@Value.Order.Natural should specify order for Set, SortedSet or NavigableSet attribute");
        }
      } else if (reverseOrderAnnotation != null) {
        if (isEligibleSetType) {
          if (isComparable()) {
            orderKind = OrderKind.REVERSE;
          } else {
            reporter.withElement(element)
                .forAnnotation(Value.Order.Reverse.class)
                .error("@Value.Order.Reverse should used with a set of Comparable elements");
          }
        } else {
          reporter.withElement(element)
              .forAnnotation(Value.Order.Reverse.class)
              .error("@Value.Order.Reverse should specify order for Set, SortedSet or NavigableSet attribute");
        }
      }
      shouldGenerateSortedSet = isEligibleSetType && orderKind != OrderKind.NONE;
    }
    return shouldGenerateSortedSet;
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
  private List<String> expectedSubclasses;

  public List<String> getExpectedSubclasses() {
    if (expectedSubclasses == null) {
      if (element.getAnnotation(Json.Subclasses.class) != null) {
        expectedSubclasses = listExpectedSubclassesFromElement(element);
      } else {
        ensureTypeIntrospected();
        expectedSubclasses =
            containedTypeElement != null
                ? listExpectedSubclassesFromElement(containedTypeElement)
                : ImmutableList.<String>of();
      }
    }
    return expectedSubclasses;
  }

  private static List<String> listExpectedSubclassesFromElement(Element element) {
    return ValueType.extractedClassNamesFromAnnotationMirrors(
        Json.Subclasses.class.getCanonicalName(), "value", element.getAnnotationMirrors());
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
  public ProcessingEnvironment processing;

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

      // TODO THIS IS THE MESS
      if (typeMirror instanceof DeclaredType) {
        DeclaredType declaredType = (DeclaredType) typeMirror;

        List<String> typeParameters = Lists.newArrayList();

        List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();

        if (!typeArguments.isEmpty()) {
          // XXX 1? can it be reused for map for example
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

  private String marshalerNameFor(TypeElement element) {
    String marshalerClassName = element.getSimpleName().toString() + "Marshaler";
    PackageElement packageElement = processing.getElementUtils().getPackageOf(element);
    if (!packageElement.isUnnamed()) {
      marshalerClassName = packageElement.getQualifiedName() + "." + marshalerClassName;
    }
    return marshalerClassName;
  }

  Collection<String> getMarshaledImportRoutines() {
    Collection<String> imports = Lists.newArrayListWithExpectedSize(2);
    if (isMarshaledElement()) {
      imports.add(marshalerNameFor(marshaledElement));
    }

    if (isMapType() && isMarshaledSecondaryElement()) {
      imports.add(marshalerNameFor(marshaledSecondaryElement));
    }
    return imports;
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
          if (isCollectionType()) {
            regularAttribute = true;
          }
        }
      }
    }

    if (isGenerateDefault && isContainerType() && !isArrayType()) {
      // ad-hoc. stick it here, but should work
      regularAttribute = true;

      reporter.withElement(element)
          .forAnnotation(Value.Default.class)
          .warning("@Value.Default on a container attribute make it lose it's special treatment");
    }
  }
}
