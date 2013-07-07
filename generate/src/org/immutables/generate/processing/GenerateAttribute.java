/*
    Copyright 2013 Immutables.org authors

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
package org.immutables.generate.processing;

import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableBiMap;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.primitives.Primitives;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import javax.annotation.Nullable;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.immutables.annotation.GenerateAlign;
import org.immutables.annotation.GenerateAsDefault;
import org.immutables.annotation.GenerateConstructorArgument;
import org.immutables.annotation.GenerateMarshaledAs;
import org.immutables.annotation.GenerateMarshaler;
import org.immutables.annotation.GenerateRepository;

public abstract class GenerateAttribute {

  private static final Predicate<CharSequence> UNDEFINABLE_PATTERN = Predicates.containsPattern("\\.Undefinable$");
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

  // XXX Departure from immutability, sorry.
  // Was bored to regenerate: self-processing not used
  public void setAttributeElement(ExecutableElement element) {
    this.element = element;
  }

  public String getMarshaledName() {
    @Nullable
    GenerateMarshaledAs options = element.getAnnotation(GenerateMarshaledAs.class);
    if (options != null) {
      String name = options.value();
      if (!name.isEmpty()) {
        return name;
      }
    }
    return internalName();
  }

  public boolean isComparable() {
    ensureTypeIntrospected();
    return implementedInterfacesNames.contains(Comparable.class.getName());
  }

  public boolean isForceEmpty() {
    @Nullable
    GenerateMarshaledAs options = element.getAnnotation(GenerateMarshaledAs.class);
    return options != null ? options.forceEmpty() : false;
  }

  protected abstract TypeMirror internalTypeMirror();

  private List<String> typeParameters = Collections.emptyList();

  @GenerateAsDefault
  public boolean isGenerateFunction() {
    return false;
  }

  @GenerateAsDefault
  public boolean isGeneratePredicate() {
    return false;
  }

  @GenerateAsDefault
  public boolean isGenerateDefault() {
    return false;
  }

  @GenerateAsDefault
  public boolean isGenerateDerived() {
    return false;
  }

  @GenerateAsDefault
  public boolean isGenerateAbstract() {
    return false;
  }

  protected abstract String internalName();

  public String getName() {
    return internalName();
  }

  protected abstract String internalTypeName();

  public String getType() {
    return internalTypeName();
  }

  public List<String> typeParameters() {
    ensureTypeIntrospected();
    return typeParameters;
  }

  public boolean isMapType() {
    return internalTypeName().startsWith(Map.class.getName());
    // || internalTypeName().startsWith(NavigableMap.class.getName());
  }

  public boolean isSortedMapType() {
    return internalTypeName().startsWith(NavigableMap.class.getName());
  }

  public boolean isListType() {
    return internalTypeName().startsWith(List.class.getName());
  }

  public boolean isSetType() {
    return internalTypeName().startsWith(Set.class.getName());
  }

  public boolean isOptionalType() {
    return internalTypeName().startsWith(Optional.class.getName());
  }

  public boolean isMarshaledElement() {
    if (isPrimitive()) {
      return false;
    }

    ensureTypeIntrospected();
    return marshaledElement;
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
    return Objects.firstNonNull(BOXED_TO_PRIMITIVE_TYPES.inverse().get(typeName), typeName);
  }

  private String unwrapType(String typeName) {
    return Objects.firstNonNull(BOXED_TO_PRIMITIVE_TYPES.get(typeName), typeName);
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

  public boolean isEnumType() {
    ensureTypeIntrospected();
    return extendedClassesNames.contains(Enum.class.getName());
  }

  public boolean isUndefinable() {
    ensureTypeIntrospected();
    return FluentIterable.from(implementedInterfacesNames).anyMatch(UNDEFINABLE_PATTERN);
  }

  public String getDirectSupertype() {
    ensureTypeIntrospected();
    return Iterables.getFirst(extendedClassesNames, null);
  }

  public List<String> getExpectedSubclasses() {
    List<String> classNames =
        GenerateType.extractedClassNamesFromAnnotationMirrors(
            GenerateMarshaledAs.class.getName(), "expectedSubclasses", element.getAnnotationMirrors());

    return classNames;
  }

  private volatile boolean introspected;
  private ImmutableList<String> extendedClassesNames;
  private ImmutableSet<String> implementedInterfacesNames;
  private boolean marshaledElement;
  private boolean marshaledSecondaryElement;
  private boolean hasEnumFirstTypeParameter;
  private TypeElement containedTypeElement;
  private boolean specialMarshaledElement;
  private boolean specialMarshaledSecondaryElement;

  private void ensureTypeIntrospected() {
    if (!introspected) {
      introspectType();
      introspected = true;
    }
  }

  public boolean isDocumentElement() {
    ensureTypeIntrospected();
    return containedTypeElement != null
        && containedTypeElement.getAnnotation(GenerateRepository.class) != null;
  }

  public boolean isMarshaledSecondaryElement() {
    if (!isMapType()) {
      return false;
    }
    ensureTypeIntrospected();
    return marshaledSecondaryElement;
  }

  private void introspectType() {
    List<TypeMirror> extendedClasses = Lists.newArrayList();
    Set<TypeMirror> implementedInterfaces = Sets.newLinkedHashSet();

    TypeMirror typeMirror = internalTypeMirror();

    if (isContainerType()) {

      // TODO THIS IS THE MESS
      if (typeMirror instanceof DeclaredType) {
        DeclaredType declaredType = (DeclaredType) typeMirror;

        List<String> typeParameters = Lists.newArrayList();

        List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();

        if (!typeArguments.isEmpty()) {
          // BOGUS 1? can it be reused for map for example
          if (typeArguments.size() == 1) {
            TypeMirror typeArgument = typeArguments.get(0);
            if (typeArgument instanceof DeclaredType) {
              typeMirror = typeArgument;
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

    if (typeMirror.getKind() == TypeKind.DECLARED) {
      collectHierarchyMirrors(typeMirror, extendedClasses, implementedInterfaces);
    }

    extendedClassesNames = FluentIterable.from(extendedClasses)
        .filter(DeclaredType.class)
        .transform(ToNameOfTypeElement.FUNCTION)
        .toList();

    implementedInterfacesNames = FluentIterable.from(implementedInterfaces)
        .filter(DeclaredType.class)
        .transform(ToNameOfTypeElement.FUNCTION)
        .toSet();
  }

  public boolean isSpecialMarshaledElement() {
    ensureTypeIntrospected();
    return specialMarshaledElement;
  }

  public boolean isSpecialMarshaledSecondaryElement() {
    ensureTypeIntrospected();
    return specialMarshaledSecondaryElement;
  }

  private boolean isSpecialMarshaledElement(boolean isMarshaled, Object qualifiedName) {
    if (isMarshaled) {
      return true;
    }
    return !isRegularMashalableType(qualifiedName.toString());
  }

  static boolean isRegularMashalableType(String name) {
    return String.class.getName().equals(name)
        || PRIMITIVE_TYPES.containsKey(name)
        || BOXED_TO_PRIMITIVE_TYPES.containsKey(name);
  }

  private void collectHierarchyMirrors(
      TypeMirror topClass,
      List<TypeMirror> extendedClasses,
      Set<TypeMirror> implementedInterfaces) {
    if (topClass.getKind() != TypeKind.DECLARED || topClass.toString().equals(Object.class.getName())) {
      return;
    }
    collectInterfacesMirrors(topClass, implementedInterfaces);

    TypeElement e = toTypeElement(topClass);
    TypeMirror superClass = e.getSuperclass();

    extendedClasses.add(superClass);
    collectHierarchyMirrors(superClass, extendedClasses, implementedInterfaces);

    for (TypeMirror typeMirror : e.getInterfaces()) {
      collectInterfacesMirrors(typeMirror, implementedInterfaces);
    }
  }

  private void collectInterfacesMirrors(
      TypeMirror topClass,
      Set<TypeMirror> implementedInterfaces) {
    TypeElement e = toTypeElement(topClass);

    if (e.getKind().isInterface()) {
      implementedInterfaces.add(topClass);
      for (TypeMirror typeMirror : e.getInterfaces()) {
        collectInterfacesMirrors(typeMirror, implementedInterfaces);
      }
    }
  }

  public int getMinValue() {
    return isAligned()
        ? element.getAnnotation(GenerateAlign.class).min()
        : 0;
  }

  public int getMaxValue() {
    return isAligned()
        ? element.getAnnotation(GenerateAlign.class).max()
        : 0;
  }

  private TypeElement toTypeElement(TypeMirror input) {
    return ToDeclaredTypeElement.FUNCTION.apply(input);
  }

  private enum ToDeclaredTypeElement implements Function<TypeMirror, TypeElement> {
    FUNCTION;
    @Override
    public TypeElement apply(TypeMirror input) {
      return (TypeElement) ((DeclaredType) input).asElement();
    }
  }

  private enum ToNameOfTypeElement implements Function<TypeMirror, String> {
    FUNCTION;
    @Override
    public String apply(TypeMirror input) {
      return ToDeclaredTypeElement.FUNCTION.apply(input).getQualifiedName().toString();
    }
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
    return element.getAnnotation(GenerateAlign.class) != null
        && (!double.class.getName().equals(t)
            && !float.class.getName().equals(t)
            && !long.class.getName().equals(t));
  }

  public boolean isPrimitive() {
    return PRIMITIVE_TYPES.containsKey(internalTypeName());
  }

  public int getConstructorArgumentOrder() {
    GenerateConstructorArgument annotation = element.getAnnotation(GenerateConstructorArgument.class);
    return annotation != null ? annotation.order() : -1;
  }

  public int getAlignOrder() {
    GenerateAlign annotation = element.getAnnotation(GenerateAlign.class);
    return annotation != null ? annotation.order() : -1;
  }
}
