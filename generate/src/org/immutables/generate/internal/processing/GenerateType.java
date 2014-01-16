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

import com.google.common.base.Function;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;
import javax.annotation.Nullable;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleAnnotationValueVisitor6;
import org.immutables.annotation.GenerateDefault;
import org.immutables.annotation.GenerateImmutable;
import org.immutables.annotation.GenerateMarshaler;
import org.immutables.annotation.GenerateRepository;
import static com.google.common.base.Preconditions.*;

public abstract class GenerateType extends TypeIntrospectionBase {

  @Nullable
  private String validationMethodName;

  @Nullable
  public String getValidationMethodName() {
    return validationMethodName;
  }

  public void setValidationMethodName(@Nullable String validationMethodName) {
    this.validationMethodName = validationMethodName;
  }

  public String getPackageName() {
    return packageFullyQualifiedName();
  }

  public String getName() {
    return internalName();
  }

  public String getAccessPrefix() {
    return internalTypeElement().getModifiers().contains(Modifier.PUBLIC)
        ? "public "
        : "";
  }

  public boolean isGenerateOrdinalValue() {
    return isOrdinalValue();
  }

  public boolean isUseConstructorOnly() {
    return isUseConstructor() && !isUseBuilder();
  }

  public boolean isUseSingleton() {
    return internalTypeElement().getAnnotation(GenerateImmutable.class).singleton();
  }

  public boolean isUseInterned() {
    return internalTypeElement().getAnnotation(GenerateImmutable.class).interned();
  }

  public boolean isUsePrehashed() {
    return isUseInterned() || internalTypeElement().getAnnotation(GenerateImmutable.class).prehashed();
  }

  public boolean isGenerateMarshaled() {
    return (internalTypeElement().getAnnotation(GenerateMarshaler.class) != null) || isGenerateDocument();
  }

  public boolean isGenerateDocument() {
    return internalTypeElement().getAnnotation(GenerateRepository.class) != null;
  }

  private Boolean hasAbstractBuilder;

  public boolean isHasAbstractBuilder() {
    if (hasAbstractBuilder == null) {
      boolean abstractBuilderDeclared = false;
      List<? extends Element> enclosedElements = internalTypeElement().getEnclosedElements();
      for (Element element : enclosedElements) {
        if (element.getKind() == ElementKind.CLASS) {
          if (element.getSimpleName().contentEquals("Builder")) {
            // We do not handle here if builder class is abstract static and not private
            // It's all to discretion compilation checking
            abstractBuilderDeclared = true;
            break;
          }
        }
      }

      hasAbstractBuilder = abstractBuilderDeclared;
    }
    return hasAbstractBuilder;
  }

  public String getDocumentName() {
    @Nullable
    GenerateRepository annotation = internalTypeElement().getAnnotation(GenerateRepository.class);
    if (annotation != null && !annotation.value().isEmpty()) {
      return annotation.value();
    }
    return inferDocumentCollectionName(getName());
  }

  private String inferDocumentCollectionName(String name) {
    checkPositionIndex(0, name.length());
    char[] a = name.toCharArray();
    a[0] = Character.toLowerCase(a[0]);
    return String.valueOf(a);
  }

  private Set<String> importedMarshalledRoutines;

  public Set<String> getGenerateMarshaledImportRoutines() throws Exception {
    if (importedMarshalledRoutines == null) {
      Set<String> imports = Sets.newLinkedHashSet();

      for (GenerateAttribute a : attributes()) {
        if (a.isMarshaledElement()) {
          String typeName = a.isContainerType()
              ? a.getUnwrappedElementType()
              : a.getType();

          imports.add(typeName + "Marshaler");
        }
        if (a.isMarshaledSecondaryElement()) {
          imports.add(a.getUnwrappedSecondaryElementType() + "Marshaler");
        }
      }

      collectImportRoutines(imports);
      importedMarshalledRoutines = ImmutableSet.copyOf(imports);
    }

    return importedMarshalledRoutines;
  }

  private void collectImportRoutines(Set<String> imports) {
    imports.addAll(extractClassNamesFromMirrors(GenerateMarshaler.class, "importRoutines",
        internalTypeElement().getAnnotationMirrors()));

    imports.addAll(extractClassNamesFromMirrors(GenerateMarshaler.class, "importRoutines",
        internalTypeElement().getEnclosingElement().getAnnotationMirrors()));
  }

  @Nullable
  public GenerateAttribute getIdAttribute() {
    for (GenerateAttribute attribute : getImplementedAttributes()) {
      if (attribute.getMarshaledName().equals("_id")) {
        return attribute;
      }
    }
    return null;
  }

  private Set<String> generateMarshaledTypes;

  public Set<String> getGenerateMarshaledTypes() throws Exception {
    if (generateMarshaledTypes == null) {
      Set<String> marshaledTypes = Sets.newLinkedHashSet();

      for (GenerateAttribute a : attributes()) {
        if (a.isSpecialMarshaledElement()) {
          String typeName = a.isContainerType()
              ? a.getUnwrappedElementType()
              : a.getType();

          addIfSpecialMarshalable(marshaledTypes, typeName);
        }
        if (a.isSpecialMarshaledSecondaryElement()) {
          addIfSpecialMarshalable(marshaledTypes, a.getUnwrappedSecondaryElementType());
        }
      }

      generateMarshaledTypes = marshaledTypes;
    }
    return generateMarshaledTypes;
  }

  private void addIfSpecialMarshalable(Set<String> marshaledTypes, String typeName) {
    if (!GenerateAttribute.isRegularMashalableType(typeName)) {
      marshaledTypes.add(typeName);
    }
  }

  private List<String> extractClassNamesFromMirrors(
      Class<?> annotationType,
      String annotationValueName,
      List<? extends AnnotationMirror> annotationMirrors) {
    return extractedClassNamesFromAnnotationMirrors(annotationType.getName(),
        annotationValueName,
        annotationMirrors);
  }

  public static List<String> extractedClassNamesFromAnnotationMirrors(
      String annotationTypeName,
      String annotationValueName,
      List<? extends AnnotationMirror> annotationMirrors) {
    final List<String> collectClassNames = Lists.<String>newArrayList();

    for (AnnotationMirror annotationMirror : annotationMirrors) {
      if (annotationMirror.getAnnotationType().toString().equals(annotationTypeName)) {
        for (Entry<? extends ExecutableElement, ? extends AnnotationValue> e : annotationMirror.getElementValues()
            .entrySet()) {
          if (e.getKey().getSimpleName().contentEquals(annotationValueName)) {
            e.getValue().accept(new SimpleAnnotationValueVisitor6<Void, Void>() {
              @Override
              public Void visitArray(List<? extends AnnotationValue> vals, Void p) {
                for (AnnotationValue annotationValue : vals) {
                  annotationValue.accept(this, p);
                }
                return null;
              }

              @Override
              public Void visitType(TypeMirror t, Void p) {
                collectClassNames.add(t.toString());
                return null;
              }
            }, null);
          }
        }
      }
    }

    return ImmutableList.copyOf(collectClassNames);
  }

  public List<GenerateAttribute> getSettableAttributes() {
    return FluentIterable.from(attributes())
        .filter(Predicates.or(
            GenerateAttributes.isGenerateAbstract(),
            GenerateAttributes.isGenerateDefault()))
        .toList();
  }

  public boolean isUseConstructor() {
    return !getConstructorArguments().isEmpty()
        || (!isUseBuilder() && !isUseSingleton() && getImplementedAttributes().isEmpty());
  }

  public List<GenerateAttribute> getConstructorArguments() {
    return FluentIterable.from(attributes())
        .filter(Predicates.compose(Predicates.not(Predicates.equalTo(-1)), ToConstructorArgumentOrder.FUNCTION))
        .toSortedList(Ordering.natural().onResultOf(ToConstructorArgumentOrder.FUNCTION));
  }

  public List<GenerateAttribute> getConstructorOmited() {
    return FluentIterable.from(attributes())
        .filter(Predicates.compose(Predicates.equalTo(-1), ToConstructorArgumentOrder.FUNCTION))
        .toList();
  }

  public List<GenerateAttribute> getAlignedAttributes() {
    return FluentIterable.from(attributes())
        .filter(Predicates.compose(Predicates.not(Predicates.equalTo(-1)), ToAlignOrder.FUNCTION))
        .toSortedList(Ordering.natural().onResultOf(ToAlignOrder.FUNCTION));
  }

  private enum ToConstructorArgumentOrder implements Function<GenerateAttribute, Integer> {
    FUNCTION;

    @Override
    public Integer apply(GenerateAttribute input) {
      return input.getConstructorArgumentOrder();
    }
  }

  private enum ToAlignOrder implements Function<GenerateAttribute, Integer> {
    FUNCTION;

    @Override
    public Integer apply(GenerateAttribute input) {
      return input.getAlignOrder();
    }
  }

  public List<GenerateAttribute> getExcludableAttributes() {
    List<GenerateAttribute> excludables = Lists.newArrayList();
    for (GenerateAttribute attribute : attributes()) {
      if (attribute.isGenerateAbstract() && attribute.isContainerType()) {
        excludables.add(attribute);
      }
    }
    return excludables;
  }

  public List<GenerateAttribute> getLazyAttributes() {
    List<GenerateAttribute> lazyAttributes = Lists.newArrayList();
    for (GenerateAttribute attribute : attributes()) {
      if (attribute.isGenerateLazy()) {
        lazyAttributes.add(attribute);
      }
    }
    return lazyAttributes;
  }

  @SuppressWarnings("unchecked")
  public List<GenerateAttribute> getImplementedAttributes() {
    return FluentIterable.from(attributes())
        .filter(Predicates.or(
            GenerateAttributes.isGenerateAbstract(),
            GenerateAttributes.isGenerateDefault(),
            GenerateAttributes.isGenerateDerived()))
        .toList();
  }

  public List<GenerateAttribute> getHelperAttributes() {
    return FluentIterable.from(attributes())
        .filter(Predicates.or(
            GenerateAttributes.isGenerateFunction(),
            GenerateAttributes.isGeneratePredicate()))
        .toList();
  }

  @Override
  protected TypeMirror internalTypeMirror() {
    return internalTypeElement().asType();
  }

  public abstract String packageFullyQualifiedName();

  public abstract String internalName();

  public abstract TypeElement internalTypeElement();

  public abstract List<GenerateAttribute> attributes();

  @GenerateDefault
  public boolean isGenerateModifiable() {
    return true;
  }

  @GenerateDefault
  public boolean isHashCodeDefined() {
    return false;
  }

  @GenerateDefault
  public boolean isEqualToDefined() {
    return false;
  }

  @GenerateDefault
  public boolean isToStringDefined() {
    return false;
  }

  @GenerateDefault
  public boolean isUseBuilder() {
    return true;
  }
}
