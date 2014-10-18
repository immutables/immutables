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

import org.immutables.mongo.Mongo;
import org.immutables.json.Json;
import com.google.common.base.*;
import com.google.common.collect.*;
import org.immutables.value.Parboil;
import org.immutables.value.Value;
import javax.annotation.Nullable;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleAnnotationValueVisitor6;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Map.Entry;
import java.util.Set;

/**
 * NEED TO BE HEAVILY REFACTORED AFTER TEMPLATE MIGRATIONS (FACETS?)
 */
public abstract class DiscoveredValue extends TypeIntrospectionBase {

  private SegmentedName segmentedName;

  public SegmentedName getSegmentedName() {
    return segmentedName;
  }

  public void setSegmentedName(SegmentedName segmentedName) {
    this.segmentedName = segmentedName;
  }

  public String getSimpleName() {
    return segmentedName.simpleName;
  }

  private boolean emptyNesting;

  public boolean isEmptyNesting() {
    return emptyNesting;
  }

  public void setEmptyNesting(boolean emptyNesting) {
    this.emptyNesting = emptyNesting;
  }

  public String valueTypeName() {
    return internalTypeElement().getQualifiedName().toString();
  }

  public boolean isAnnotationType() {
    return internalTypeElement().getKind() == ElementKind.ANNOTATION_TYPE;
  }

  public String implementationTypeName() {
    return Joiner.on('.')
        .skipNulls()
        .join(
            Strings.emptyToNull(getPackageName()),
            getImmutableReferenceName());
  }

  public String getImmutableReferenceName() {
    return "Immutable" + (isHasNestingParent() ? getName() : getSimpleName());
  }

  public boolean isGenerateParboiled() {
    return isEmptyNesting() && internalTypeElement().getAnnotation(Parboil.Ast.class) != null;
  }

  public boolean isGenerateTransformer() {
    return isEmptyNesting() && internalTypeElement().getAnnotation(Value.Transformer.class) != null;
  }

  private CaseStructure caseStructure;

  public CaseStructure getCases() {
    if (caseStructure == null) {
      caseStructure = new CaseStructure(this);
    }
    return caseStructure;
  }

  /**
   * Something less than half of 255 parameter limit in java methods (not counting 2-slot double
   * and long parameters).
   */
  private static final int SOME_RANDOM_LIMIT = 100;

  private DiscoveredValue nestingParent;

  public void setNestingParent(DiscoveredValue nestingParent) {
    this.nestingParent = nestingParent;
  }

  private List<DiscoveredValue> nestedChildren;

  public void setNestedChildren(List<DiscoveredValue> nestedChildren) {
    this.nestedChildren = nestedChildren;
    for (DiscoveredValue child : nestedChildren) {
      child.setNestingParent(this);
    }
  }

  public List<DiscoveredValue> getNestedChildren() {
    return nestedChildren;
  }

  public boolean isHasNestingParent() {
    return nestingParent != null;
  }

  public boolean isHasNestedChildren() {
    return nestedChildren != null;
  }

  @Nullable
  private String validationMethodName;

  @Nullable
  public String getValidationMethodName() {
    return validationMethodName;
  }

  public boolean isIface() {
    return internalTypeElement().getKind() == ElementKind.INTERFACE
        || internalTypeElement().getKind() == ElementKind.ANNOTATION_TYPE;
  }

  public String getInheritsKeyword() {
    return isIface() ? "implements" : "extends";
  }

  public <T extends Annotation> T getAnnotationFromThisOrEnclosingElement(Class<T> annotationType) {
    T annotation = internalTypeElement().getAnnotation(annotationType);
    if (annotation == null && nestingParent != null) {
      annotation = nestingParent.internalTypeElement().getAnnotation(annotationType);
    }
    return annotation;
  }

  public boolean isGenerateGetters() {
    return internalTypeElement().getAnnotation(Value.Getters.class) != null;
  }

  public void setValidationMethodName(@Nullable String validationMethodName) {
    this.validationMethodName = validationMethodName;
  }

  public String getPackageName() {
    return segmentedName.packageName;
  }

  public String getName() {
    return segmentedName.referenceClassName;
  }

  public String getDefName() {
    return isHasNestingParent() ? segmentedName.simpleName : ("Immutable" + segmentedName.simpleName);
  }

  public String getAccessPrefix() {
    // !getGenerataeImmutableProperties().nonpublic() &&
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

  private Value.Immutable immutableProperties;

  private Value.Immutable getGenerataeImmutableProperties() {
    if (immutableProperties == null) {
      immutableProperties = internalTypeElement().getAnnotation(Value.Immutable.class);
    }
    return immutableProperties;
  }

  public boolean isUseCopyMethods() {
    return getGenerataeImmutableProperties().withers()
        && getImplementedAttributes().size() > 0 && getImplementedAttributes().size() < SOME_RANDOM_LIMIT;
  }

  public boolean isUseSingleton() {
    return getGenerataeImmutableProperties().singleton();
  }

  public boolean isUseInterned() {
    return getGenerataeImmutableProperties().intern();
  }

  public boolean isUsePrehashed() {
    return isUseInterned()
        || isGenerateOrdinalValue()
        || getGenerataeImmutableProperties().prehash();
  }

  public boolean isGenerateMarshaled() {
    return (getAnnotationFromThisOrEnclosingElement(Json.Marshaled.class) != null) ||
        isGenerateRepository();
  }

  public boolean isGenerateRepository() {
    return internalTypeElement().getAnnotation(Mongo.Repository.class) != null;
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
    Mongo.Repository annotation = internalTypeElement().getAnnotation(Mongo.Repository.class);
    if (annotation != null && !annotation.value().isEmpty()) {
      return annotation.value();
    }
    return inferDocumentCollectionName(getName());
  }

  private String inferDocumentCollectionName(String name) {
    char[] a = name.toCharArray();
    a[0] = Character.toLowerCase(a[0]);
    return String.valueOf(a);
  }

  private Set<String> importedMarshalledRoutines;

  public Set<String> getGenerateMarshaledImportRoutines() throws Exception {
    if (importedMarshalledRoutines == null) {
      Set<String> imports = Sets.newLinkedHashSet();

      for (DiscoveredAttribute a : filteredAttributes()) {
        imports.addAll(a.getMarshaledImportRoutines());
      }

      collectImportRoutines(imports);
      importedMarshalledRoutines = ImmutableSet.copyOf(imports);
    }

    return importedMarshalledRoutines;
  }

  private void collectImportRoutines(Set<String> imports) {
    Element element = internalTypeElement();
    for (;;) {
      imports.addAll(
          extractClassNamesFromMirrors(Json.Marshaled.class,
              "importRoutines",
              element.getAnnotationMirrors()));

      Element enclosingElement = element.getEnclosingElement();
      if (enclosingElement == null || element instanceof PackageElement) {
        break;
      }
      element = enclosingElement;
    }
  }

  @Nullable
  public DiscoveredAttribute getIdAttribute() {
    for (DiscoveredAttribute attribute : getImplementedAttributes()) {
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
      for (DiscoveredAttribute a : filteredAttributes()) {
        marshaledTypes.addAll(a.getSpecialMarshaledTypes());
      }
      generateMarshaledTypes = marshaledTypes;
    }
    return generateMarshaledTypes;
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

  public List<DiscoveredAttribute> getSettableAttributes() {
    return filteredAttributes()
        .filter(Predicates.or(
            DiscoveredAttributes.isGenerateAbstract(),
            DiscoveredAttributes.isGenerateDefault()))
        .toList();
  }

  public boolean isUseConstructor() {
    return !getConstructorArguments().isEmpty()
        || (!isUseBuilder() && !isUseSingleton() && getImplementedAttributes().isEmpty());
  }

  public List<DiscoveredAttribute> getConstructorArguments() {
    return filteredAttributes()
        .filter(Predicates.compose(Predicates.not(Predicates.equalTo(-1)), ToConstructorArgumentOrder.FUNCTION))
        .toSortedList(Ordering.natural().onResultOf(ToConstructorArgumentOrder.FUNCTION));
  }

  public List<DiscoveredAttribute> getConstructorOmited() {
    return filteredAttributes()
        .filter(Predicates.compose(Predicates.equalTo(-1), ToConstructorArgumentOrder.FUNCTION))
        .toList();
  }

  public List<DiscoveredAttribute> getAlignedAttributes() {
    return filteredAttributes()
        .filter(Predicates.compose(Predicates.not(Predicates.equalTo(-1)), ToAlignOrder.FUNCTION))
        .toSortedList(Ordering.natural().onResultOf(ToAlignOrder.FUNCTION));
  }

  private enum NonAuxiliary implements Predicate<DiscoveredAttribute> {
    PREDICATE;
    @Override
    public boolean apply(DiscoveredAttribute input) {
      return !input.isAuxiliary();
    }
  }

  private enum ToConstructorArgumentOrder implements Function<DiscoveredAttribute, Integer> {
    FUNCTION;

    @Override
    public Integer apply(DiscoveredAttribute input) {
      return input.getConstructorArgumentOrder();
    }
  }

  private enum ToAlignOrder implements Function<DiscoveredAttribute, Integer> {
    FUNCTION;

    @Override
    public Integer apply(DiscoveredAttribute input) {
      return input.getAlignOrder();
    }
  }

  public List<DiscoveredAttribute> getExcludableAttributes() {
    List<DiscoveredAttribute> excludables = Lists.newArrayList();
    for (DiscoveredAttribute attribute : filteredAttributes()) {
      if (attribute.isGenerateAbstract() && (attribute.isContainerType() && !attribute.isArrayType())) {
        excludables.add(attribute);
      }
    }
    return excludables;
  }

  public List<DiscoveredAttribute> mandatoryAttributes() {
    List<DiscoveredAttribute> mandatory = Lists.newArrayList();
    for (DiscoveredAttribute attribute : getSettableAttributes()) {
      if (attribute.isMandatory()) {
        mandatory.add(attribute);
      }
    }
    return mandatory;
  }

  public List<DiscoveredAttribute> getLazyAttributes() {
    List<DiscoveredAttribute> lazyAttributes = Lists.newArrayList();
    for (DiscoveredAttribute attribute : filteredAttributes()) {
      if (attribute.isGenerateLazy()) {
        lazyAttributes.add(attribute);
      }
    }
    return lazyAttributes;
  }

  public List<DiscoveredAttribute> getAllAccessibleAttributes() {
    return ImmutableList.<DiscoveredAttribute>builder()
        .addAll(getImplementedAttributes())
        .addAll(getLazyAttributes())
        .build();
  }

  private List<DiscoveredAttribute> implementedAttributes;

  private FluentIterable<DiscoveredAttribute> filteredAttributes() {
    return FluentIterable.from(attributes());
  }

  @SuppressWarnings("unchecked")
  public List<DiscoveredAttribute> getImplementedAttributes() {
    if (implementedAttributes == null) {
      implementedAttributes = filteredAttributes()
          .filter(Predicates.or(
              DiscoveredAttributes.isGenerateAbstract(),
              DiscoveredAttributes.isGenerateDefault(),
              DiscoveredAttributes.isGenerateDerived()))
          .toList();
    }
    return implementedAttributes;
  }

  public List<DiscoveredAttribute> getEquivalenceAttributes() {
    return FluentIterable.from(getImplementedAttributes())
        .filter(NonAuxiliary.PREDICATE)
        .toList();
  }

  public List<DiscoveredAttribute> getHelperAttributes() {
    return filteredAttributes()
        .filter(Predicates.or(
            DiscoveredAttributes.isGenerateFunction(),
            DiscoveredAttributes.isGeneratePredicate()))
        .toList();
  }

  public boolean hasSingleParameterConstructor() {
    return isUseConstructor() && getConstructorArguments().size() == 1;
  }

  @Override
  protected TypeMirror internalTypeMirror() {
    return internalTypeElement().asType();
  }

  public abstract TypeElement internalTypeElement();

  public abstract List<DiscoveredAttribute> attributes();

  public boolean isGenerateModifiable() {
    return true;
  }

  public boolean isHashCodeDefined() {
    return false;
  }

  public boolean isEqualToDefined() {
    return false;
  }

  public boolean isToStringDefined() {
    return false;
  }

  public boolean isUseBuilder() {
    return true;
  }
}
