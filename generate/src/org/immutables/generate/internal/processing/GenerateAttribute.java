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
package org.immutables.generate.internal.processing;

import com.google.common.base.Functions;
import com.google.common.base.MoreObjects;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.primitives.Primitives;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.annotation.Nullable;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import org.immutables.annotation.GenerateAuxiliary;
import org.immutables.annotation.GenerateConstructorParameter;
import org.immutables.annotation.GenerateDefault;
import org.immutables.annotation.GenerateMarshaled;
import org.immutables.annotation.GenerateMarshaledSubclasses;
import org.immutables.annotation.GenerateMarshaler;
import org.immutables.annotation.GeneratePackedBits;
import org.immutables.annotation.GenerateRepository;

/**
 * DISCLAIMER: ALL THIS LOGIC IS A PIECE OF CRAP THAT ACCUMULATED OVER TIME.
 * QUALITY OF RESULTED GENERATED CLASSES ALWAYS WAS HIGHEST PRIORITY
 * BUT MODIFIABILITY SUFFERS, SO NEW VERSION WILL REIMPLEMENT IT FROM SCRATCH.
 */
public abstract class GenerateAttribute extends TypeIntrospectionBase {

  private static final String STRING_CLASS_NAME = String.class.getName();
  private static final String GOOGLE_COMMON_PREFIX = "com.go".concat("ogle.common.");
  private static final ImmutableMap<String, Class<?>> PRIMITIVE_TYPES;

  static {
    ImmutableMap.Builder<String, Class<?>> builder = ImmutableMap.builder();
    for (Class<?> primitive : Primitives.allPrimitiveTypes()) {
      builder.put(primitive.getName(), primitive);
    }
    PRIMITIVE_TYPES = builder.build();
  }

  private static final ImmutableBiMap<String, String> BOXED_TO_PRIMITIVE_TYPES;
  private ExecutableElement element;

  static {
    ImmutableBiMap.Builder<String, String> builder = ImmutableBiMap.builder();
    for (Class<?> primitive : Primitives.allPrimitiveTypes()) {
      builder.put(Primitives.wrap(primitive).getName(), primitive.getName());
    }
    BOXED_TO_PRIMITIVE_TYPES = builder.build();
  }

  public void setAttributeElement(ExecutableElement element) {
    this.element = element;
  }

  @Override
  public boolean isComparable() {
    return isNumberType() || super.isComparable();
  }

  public String getMarshaledName() {
    @Nullable
    GenerateMarshaled options = element.getAnnotation(GenerateMarshaled.class);
    if (options != null) {
      String name = options.value();
      if (!name.isEmpty()) {
        return name;
      }
    }
    return internalName();
  }

  public boolean isForceEmpty() {
    @Nullable
    GenerateMarshaled options = element.getAnnotation(GenerateMarshaled.class);
    return options != null ? options.forceEmpty() : false;
  }

  private List<String> typeParameters = Collections.emptyList();

  @Override
  protected abstract TypeMirror internalTypeMirror();

  @GenerateDefault
  public boolean isGenerateFunction() {
    return false;
  }

  @GenerateDefault
  public boolean isGenerateLazy() {
    return false;
  }

  @GenerateDefault
  public boolean isGeneratePredicate() {
    return false;
  }

  @GenerateDefault
  public boolean isGenerateDefault() {
    return false;
  }

  @GenerateDefault
  public boolean isGenerateDerived() {
    return false;
  }

  @GenerateDefault
  public boolean isGenerateAbstract() {
    return false;
  }

  protected abstract String internalTypeName();

  protected abstract String internalName();

  public String getName() {
    return internalName();
  }

  public String getType() {
    return internalTypeName();
  }

  public List<String> getAnnotations() {
    return AnnotationPrinter.extractAnnotationLines(element);
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
    return typeParameters;
  }

  private Boolean isMapType;

  public boolean isMapType() {
    if (isMapType == null) {
      isMapType = internalTypeName().startsWith(Map.class.getName());
    }
    return isMapType;
  }

  private Boolean isListType;

  public boolean isListType() {
    if (isListType == null) {
      isListType = internalTypeName().startsWith(List.class.getName());
    }
    return isListType;
  }

  private Boolean isSetType;

  public boolean isSetType() {
    if (isSetType == null) {
      isSetType = internalTypeName().startsWith(Set.class.getName());
    }
    return isSetType;
  }

  private Boolean isOptionalType;

  public boolean isOptionalType() {
    if (isOptionalType == null) {
      isOptionalType = internalTypeName().startsWith(googleCommonHiddenFromShadePlugin("base.Optional"));
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

  private String wrapType(String typeName) {
    return MoreObjects.firstNonNull(BOXED_TO_PRIMITIVE_TYPES.inverse().get(typeName), typeName);
  }

  private String unwrapType(String typeName) {
    return MoreObjects.firstNonNull(BOXED_TO_PRIMITIVE_TYPES.get(typeName), typeName);
  }

  private String containmentTypeName() {
    return isContainerType() ? firstTypeParameter() : internalTypeName();
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
        || STRING_CLASS_NAME.equals(containmentTypeName())
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
    return PRIMITIVE_TYPES.containsKey(getUnwrappedElementType());
  }

  public boolean isUnwrappedSecondaryElementPrimitiveType() {
    return PRIMITIVE_TYPES.containsKey(getUnwrappedSecondaryElementType());
  }

  public String firstTypeParameter() {
    return Iterables.getFirst(typeParameters(), "");
  }

  public String secondTypeParameter() {
    return Iterables.get(typeParameters(), 1);
  }

  public String getElementType() {
    return firstTypeParameter();
  }

  @Nullable
  private List<String> expectedSubclasses;

  public List<String> getExpectedSubclasses() {
    if (expectedSubclasses == null) {
      if (element.getAnnotation(GenerateMarshaledSubclasses.class) != null) {
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
    return GenerateType.extractedClassNamesFromAnnotationMirrors(
        GenerateMarshaledSubclasses.class.getName(), "value", element.getAnnotationMirrors());
  }

  private boolean marshaledElement;
  private boolean marshaledSecondaryElement;
  private boolean specialMarshaledElement;
  private boolean specialMarshaledSecondaryElement;

  private boolean hasEnumFirstTypeParameter;
  private TypeElement containedTypeElement;
  private boolean generateOrdinalValueSet;

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
        && containedTypeElement.getAnnotation(GenerateRepository.class) != null;
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
                GenerateMarshaler generateMarshaler = typeElement.getAnnotation(GenerateMarshaler.class);
                marshaledSecondaryElement = generateMarshaler != null;
                specialMarshaledSecondaryElement =
                    isSpecialMarshaledElement(marshaledSecondaryElement, typeElement.getQualifiedName());
              }
            }
          }

          typeParameters.addAll(Lists.transform(typeArguments, Functions.toStringFunction()));
        }

        this.typeParameters = typeParameters;
      }
    }

    if (typeMirror instanceof DeclaredType) {
      TypeElement typeElement = (TypeElement) ((DeclaredType) typeMirror).asElement();

      this.containedTypeElement = typeElement;

      @Nullable
      GenerateMarshaler generateDocument = typeElement.getAnnotation(GenerateMarshaler.class);
      marshaledElement = generateDocument != null;
      specialMarshaledElement = isSpecialMarshaledElement(marshaledElement, typeElement.getQualifiedName());
    }

    super.introspectType();
  }

  static boolean isRegularMashalableType(String name) {
    return STRING_CLASS_NAME.equals(name)
        || PRIMITIVE_TYPES.containsKey(name)
        || BOXED_TO_PRIMITIVE_TYPES.containsKey(name);
  }

  public int getMinValue() {
    return isAligned()
        ? element.getAnnotation(GeneratePackedBits.class).min()
        : 0;
  }

  public int getMaxValue() {
    return isAligned()
        ? element.getAnnotation(GeneratePackedBits.class).max()
        : 0;
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
    String t = internalTypeName();
    return isPrimitive()
        && !char.class.getName().equals(t)
        && !boolean.class.getName().equals(t);
  }

  public boolean isFloatType() {
    String t = internalTypeName();
    return float.class.getName().equals(t)
        || double.class.getName().equals(t);
  }

  public boolean isContainerType() {
    return isCollectionType()
        || isOptionalType()
        || isMapType();
  }

  public String getWrapperType() {
    return isPrimitive()
        ? Primitives.wrap(PRIMITIVE_TYPES.get(internalTypeName())).getName()
        : internalTypeName();
  }

  public boolean isAligned() {
    String t = internalTypeName();
    return element.getAnnotation(GeneratePackedBits.class) != null
        && isPrimitive()
        && (!double.class.getName().equals(t)
            && !float.class.getName().equals(t)
            && !long.class.getName().equals(t));
  }

  public boolean isPrimitive() {
    return PRIMITIVE_TYPES.containsKey(internalTypeName());
  }

  public int getConstructorArgumentOrder() {
    GenerateConstructorParameter annotation = element.getAnnotation(GenerateConstructorParameter.class);
    return annotation != null ? annotation.order() : -1;
  }

  public int getAlignOrder() {
    GeneratePackedBits annotation = element.getAnnotation(GeneratePackedBits.class);
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

  private boolean isSpecialMarshaledElement(boolean isMarshaled, Object qualifiedName) {
    if (isMarshaled) {
      return true;
    }
    return !isRegularMashalableType(qualifiedName.toString());
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
    if (!isRegularMashalableType(typeName)) {
      marshaledTypes.add(typeName);
    }
  }

  public boolean isAuxiliary() {
    return element.getAnnotation(GenerateAuxiliary.class) != null;
  }
}
