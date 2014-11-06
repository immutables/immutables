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

import com.google.common.base.Functions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
import org.immutables.value.Json;
import org.immutables.value.Mongo;
import org.immutables.value.Value;
import org.immutables.value.processor.meta.NamingStyles.UsingName.AttributeNames;

/**
 * It's pointless to refactor this mess until
 * 1) Some sort of type calculus toolkit used/created
 * 2) Facets/Implicits in Generator toolkit with auto-memoising implemented
 */
public class ValueAttribute extends TypeIntrospectionBase {
  private static final String NULLABLE_PREFIX = "@javax.annotation.Nullable ";
  private static final String NULLABLE_SIMPLE_NAME = "Nullable";
  private static final String GOOGLE_COMMON_PREFIX = "com.go".concat("ogle.common.");

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

  TypeMirror returnType;
  ExecutableElement element;
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
    return isGenerateAbstract() && !isContainerType() && !isNullable();
  }

  public boolean isNullable() {
    ensureAnnotationsIntrospected();
    return nullable;
  }

  @Override
  public boolean isComparable() {
    return isNumberType() || super.isComparable();
  }

  public String getMarshaledName() {
    @Nullable
    Json.Named options = element.getAnnotation(Json.Named.class);
    if (options != null) {
      String name = options.value();
      if (!name.isEmpty()) {
        return name;
      }
    }
    return names.raw;
  }

  public boolean isForceEmpty() {
    return element.getAnnotation(Json.ForceEmpty.class) != null;
  }

  @Nullable
  public CharSequence getAnnotationDefaultValue() {
    @Nullable
    AnnotationValue defaultValue = element.getDefaultValue();
    if (defaultValue != null) {
      return AnnotationPrinting.toCharSequence(defaultValue);
    }
    return null;
  }

  @Override
  protected TypeMirror internalTypeMirror() {
    return returnType;
  }

  public boolean isGenerateLazy() {
    return isGenerateLazy;
  }

  public boolean isGenerateDefault() {
    return isGenerateDefault;
  }

  public boolean isGenerateDerived() {
    return isGenerateDerived;
  }

  public boolean isGenerateAbstract() {
    return isGenerateAbstract;
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
            googleCommonHiddenFromShadePlugin("collect.ImmutableMap"));
      } else if (isListType()) {
        implementationType = type.replace(List.class.getName(),
            googleCommonHiddenFromShadePlugin("collect.ImmutableList"));
      } else if (isSetType()) {
        implementationType = type.replace(Set.class.getName(),
            isGenerateOrdinalValueSet()
                ? "org.immutables.common.collect.".concat("ImmutableOrdinalSet")
                : googleCommonHiddenFromShadePlugin("collect.ImmutableSet"));
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

  private Boolean isSetType;

  public boolean isSetType() {
    if (isRegularAttribute()) {
      return false;
    }
    if (isSetType == null) {
      isSetType = returnTypeName.startsWith(Set.class.getName());
    }
    return isSetType;
  }

  private Boolean isOptionalType;

  public boolean isOptionalType() {
    if (isRegularAttribute()) {
      return false;
    }
    if (isOptionalType == null) {
      isOptionalType = returnTypeName.startsWith(googleCommonHiddenFromShadePlugin("base.Optional"));
    }
    return isOptionalType;
  }

  private String googleCommonHiddenFromShadePlugin(Object string) {
    return GOOGLE_COMMON_PREFIX + string;
  }

  public boolean isCollectionType() {
    return isSetType() || isListType();
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

  private boolean marshaledElement;
  private boolean marshaledSecondaryElement;
  private boolean specialMarshaledElement;
  private boolean specialMarshaledSecondaryElement;

  private boolean hasEnumFirstTypeParameter;
  private TypeElement containedTypeElement;
  private boolean generateOrdinalValueSet;
  private TypeMirror arrayComponent;
  private boolean regularAttribute;
  private boolean nullable;

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
                @Nullable
                Json.Marshaled generateMarshaler = typeElement.getAnnotation(Json.Marshaled.class);
                marshaledSecondaryElement = generateMarshaler != null;
                specialMarshaledSecondaryElement =
                    isSpecialMarshaledElement(marshaledSecondaryElement, typeElement.getQualifiedName());
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

      @Nullable
      Json.Marshaled generateMarshaler = typeElement.getAnnotation(Json.Marshaled.class);
      marshaledElement = generateMarshaler != null;
      specialMarshaledElement = isSpecialMarshaledElement(marshaledElement, typeElement.getQualifiedName());
    }

    super.introspectType();
  }

  private boolean annotationsIntrospected;

  private void ensureAnnotationsIntrospected() {
    if (!annotationsIntrospected) {
      for (AnnotationMirror annotation : element.getAnnotationMirrors()) {
        if (annotation.getAnnotationType().asElement().getSimpleName().contentEquals(NULLABLE_SIMPLE_NAME)) {
          if (!isPrimitive()) {
            nullable = true;
            regularAttribute = true;
          }
        }
      }
    }
    annotationsIntrospected = true;
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
        : isSetType() ? Set.class.getSimpleName() : "";
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
    return specialMarshaledElement;
  }

  public boolean isSpecialMarshaledSecondaryElement() {
    ensureTypeIntrospected();
    return specialMarshaledSecondaryElement;
  }

  public boolean isMarshaledElement() {
    if (isPrimitive()) {
      return false;
    }
    ensureTypeIntrospected();
    return marshaledElement;
  }

  public boolean isMarshaledSecondaryElement() {
    ensureTypeIntrospected();
    return marshaledSecondaryElement;
  }

  public boolean isPrimitiveElement() {
    return isPrimitiveType(getUnwrappedElementType());
  }

  private boolean isSpecialMarshaledElement(boolean isMarshaled, Object qualifiedName) {
    if (isMarshaled) {
      return true;
    }
    return !isRegularMarshalableType(qualifiedName.toString());
  }

  private static String marshalerNameFor(String typeName) {
    SegmentedName name = SegmentedName.from(typeName);
    return name.packageName + "." + name.simpleName + "Marshaler";
  }

  Collection<String> getMarshaledImportRoutines() {
    Collection<String> imports = Lists.newArrayListWithExpectedSize(2);
    if (isMarshaledElement()) {
      String typeName = isContainerType()
          ? getUnwrappedElementType()
          : getType();

      imports.add(marshalerNameFor(typeName));
    }

    if (isMapType() && isMarshaledSecondaryElement()) {
      imports.add(marshalerNameFor(getUnwrappedSecondaryElementType()));
    }
    return imports;
  }

  Collection<String> getSpecialMarshaledTypes() {
    Collection<String> marshaledTypeSet = Lists.newArrayListWithExpectedSize(2);
    if (isSpecialMarshaledElement()) {
      String typeName = isContainerType()
          ? getUnwrappedElementType()
          : getType();

      addIfSpecialMarshalable(marshaledTypeSet, typeName);
    }
    if (isMapType() && isSpecialMarshaledSecondaryElement()) {
      addIfSpecialMarshalable(marshaledTypeSet, getUnwrappedSecondaryElementType());
    }
    return marshaledTypeSet;
  }

  static void addIfSpecialMarshalable(Collection<String> marshaledTypes, String typeName) {
    if (!isRegularMarshalableType(typeName)) {
      marshaledTypes.add(typeName);
    }
  }

  public boolean isAuxiliary() {
    return element.getAnnotation(Value.Auxiliary.class) != null;
  }

  public boolean isMarshaledIgnore() {
    ensureTypeIntrospected();
    return isMarshaledElement();
  }
}
