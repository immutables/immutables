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

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Strings;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import java.lang.annotation.Annotation;
import java.util.Arrays;
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
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleAnnotationValueVisitor7;
import org.immutables.value.Jackson;
import org.immutables.value.Json;
import org.immutables.value.Mongo;
import org.immutables.value.Parboil;
import org.immutables.value.Value;
import org.immutables.value.processor.meta.Styles.UsingName.TypeNames;

/**
 * It's pointless to refactor this mess until
 * 1) Some sort of type calculus toolkit used/created
 * 2) Facets/Implicits in Generator toolkit with auto-memoising implemented
 */
public class ValueType extends TypeIntrospectionBase {

  public String typeMoreObjects;

  public TypeElement element;
  public List<ValueAttribute> attributes = Lists.newArrayList();
  public boolean isHashCodeDefined;
  public boolean isEqualToDefined;
  public boolean isToStringDefined;
  public boolean emptyNesting;

  public boolean isUseBuilder() {
    return immutableFeatures.builder();
  }

  public TypeNames namings;

  public SegmentedName segmentedName;

  public String getSimpleName() {
    return segmentedName.simpleName;
  }

  public boolean isGenerateJacksonMapped() {
    return element.getAnnotation(Jackson.Mapped.class) != null;
  }

  public String valueTypeName() {
    return element.getQualifiedName().toString();
  }

  public boolean isTopLevel() {
    return segmentedName.enclosingClassName.isEmpty();
  }

  public boolean isAnnotationType() {
    return element.getKind() == ElementKind.ANNOTATION_TYPE;
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
    return emptyNesting && element.getAnnotation(Parboil.Ast.class) != null;
  }

  public boolean isGenerateTransformer() {
    return emptyNesting && element.getAnnotation(Value.Transformer.class) != null;
  }

  private CaseStructure caseStructure;

  public CaseStructure getCases() {
    if (caseStructure == null) {
      caseStructure = new CaseStructure(this);
    }
    return caseStructure;
  }

  private ValueType nestingParent;

  public void setNestingParent(ValueType nestingParent) {
    this.nestingParent = nestingParent;
  }

  private List<ValueType> nestedChildren;

  public void setNestedChildren(List<ValueType> nestedChildren) {
    this.nestedChildren = nestedChildren;
    for (ValueType child : nestedChildren) {
      child.setNestingParent(this);
    }
  }

  public List<ValueType> getNestedChildren() {
    return nestedChildren;
  }

  public boolean isHasNestingParent() {
    return nestingParent != null;
  }

  public boolean isHasNestedChildren() {
    return nestedChildren != null;
  }

  @Nullable
  public String validationMethodName;

  public boolean isIface() {
    return element.getKind() == ElementKind.INTERFACE
        || element.getKind() == ElementKind.ANNOTATION_TYPE;
  }

  public String getInheritsKeyword() {
    return isIface() ? "implements" : "extends";
  }

  public <T extends Annotation> T getAnnotationFromThisOrEnclosingElement(Class<T> annotationType) {
    T annotation = element.getAnnotation(annotationType);
    if (annotation == null && nestingParent != null) {
      annotation = nestingParent.element.getAnnotation(annotationType);
    }
    return annotation;
  }

  public boolean isGenerateGetters() {
    return element.getAnnotation(Value.Getters.class) != null;
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
    Value.Immutable immutable = immutableFeatures;
    if (immutable != null && immutable.nonpublic()) {
      return "";
    }
    if (element.getModifiers().contains(Modifier.PUBLIC)) {
      return "public ";
    }
    return "";
  }

  public boolean isGenerateOrdinalValue() {
    return isOrdinalValue();
  }

  public boolean isUseConstructorOnly() {
    return isUseConstructor() && !isUseBuilder();
  }

  public Value.Immutable immutableFeatures;

  public boolean isUseCopyMethods() {
    return immutableFeatures.copy();
  }

  public boolean isUseSingleton() {
    return immutableFeatures.singleton() || getImplementedAttributes().isEmpty();
  }

  public boolean isUseInterned() {
    return immutableFeatures.intern();
  }

  public boolean isUsePrehashed() {
    return isUseInterned()
        || isGenerateOrdinalValue()
        || immutableFeatures.prehash();
  }

  public boolean isGenerateMarshaled() {
    return (getAnnotationFromThisOrEnclosingElement(Json.Marshaled.class) != null) ||
        isGenerateRepository();
  }

  public boolean isGenerateRepository() {
    return element.getAnnotation(Mongo.Repository.class) != null;
  }

  private Boolean hasAbstractBuilder;

  public boolean isHasAbstractBuilder() {
    if (hasAbstractBuilder == null) {
      boolean abstractBuilderDeclared = false;
      List<? extends Element> enclosedElements = element.getEnclosedElements();
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
    Mongo.Repository annotation = element.getAnnotation(Mongo.Repository.class);
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

  public Set<String> getGenerateMarshaledImportRoutines() {
    if (importedMarshalledRoutines == null) {
      Set<String> imports = Sets.newLinkedHashSet();

      for (ValueAttribute a : attributes()) {
        imports.addAll(a.getMarshaledImportRoutines());
      }

      collectImportRoutines(imports);
      importedMarshalledRoutines = ImmutableSet.copyOf(imports);
    }

    return importedMarshalledRoutines;
  }

  private void collectImportRoutines(Set<String> imports) {
    Element element = this.element;
    for (;;) {
      imports.addAll(
          extractClassNamesFromMirrors(Json.Import.class,
              "value",
              element.getAnnotationMirrors()));

      Element enclosingElement = element.getEnclosingElement();
      if (enclosingElement == null || element instanceof PackageElement) {
        break;
      }
      element = enclosingElement;
    }
  }

  @Nullable
  public ValueAttribute getIdAttribute() {
    for (ValueAttribute attribute : getImplementedAttributes()) {
      if (attribute.isIdAttribute()) {
        return attribute;
      }
    }
    return null;
  }

  private Set<String> generateMarshaledTypes;

  public Set<String> getGenerateMarshaledTypes() {
    if (generateMarshaledTypes == null) {
      Set<String> marshaledTypes = Sets.newLinkedHashSet();
      for (ValueAttribute a : attributes()) {
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
    return extractedClassNamesFromAnnotationMirrors(
        annotationType.getCanonicalName(),
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
            e.getValue().accept(new SimpleAnnotationValueVisitor7<Void, Void>() {
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

  public List<ValueAttribute> getSettableAttributes() {
    return attributes()
        .filter(Predicates.or(
            ValueAttributeFunctions.isGenerateAbstract(),
            ValueAttributeFunctions.isGenerateDefault()))
        .toList();
  }

  public boolean isUseConstructor() {
    return !getConstructorArguments().isEmpty()
        || (!isUseBuilder() && !isUseSingleton() && getImplementedAttributes().isEmpty());
  }

  public List<ValueAttribute> getConstructorArguments() {
    return attributes()
        .filter(Predicates.compose(Predicates.not(Predicates.equalTo(-1)), ToConstructorArgumentOrder.FUNCTION))
        .toSortedList(Ordering.natural().onResultOf(ToConstructorArgumentOrder.FUNCTION));
  }

  public List<ValueAttribute> getConstructorOmited() {
    return FluentIterable.from(getImplementedAttributes())
        .filter(Predicates.compose(Predicates.equalTo(-1), ToConstructorArgumentOrder.FUNCTION))
        .toList();
  }

  private enum NonAuxiliary implements Predicate<ValueAttribute> {
    PREDICATE;
    @Override
    public boolean apply(ValueAttribute input) {
      return !input.isAuxiliary();
    }
  }

  private enum ToConstructorArgumentOrder implements Function<ValueAttribute, Integer> {
    FUNCTION;

    @Override
    public Integer apply(ValueAttribute input) {
      return input.getConstructorArgumentOrder();
    }
  }

  public List<ValueAttribute> getExcludableAttributes() {
    List<ValueAttribute> excludables = Lists.newArrayList();
    for (ValueAttribute attribute : attributes()) {
      if (attribute.isGenerateAbstract() && (attribute.isContainerType() && !attribute.isArrayType())) {
        excludables.add(attribute);
      }
    }
    return excludables;
  }

  public List<ValueAttribute> mandatoryAttributes() {
    List<ValueAttribute> mandatory = Lists.newArrayList();
    for (ValueAttribute attribute : getSettableAttributes()) {
      if (attribute.isMandatory()) {
        mandatory.add(attribute);
      }
    }
    return mandatory;
  }

  public List<ValueAttribute> getLazyAttributes() {
    List<ValueAttribute> lazyAttributes = Lists.newArrayList();
    for (ValueAttribute attribute : attributes()) {
      if (attribute.isGenerateLazy()) {
        lazyAttributes.add(attribute);
      }
    }
    return lazyAttributes;
  }

  public List<ValueAttribute> getAllAccessibleAttributes() {
    return ImmutableList.<ValueAttribute>builder()
        .addAll(getImplementedAttributes())
        .addAll(getLazyAttributes())
        .build();
  }

  private List<ValueAttribute> implementedAttributes;

  private FluentIterable<ValueAttribute> attributes() {
    return FluentIterable.from(attributes);
  }

  public List<ValueAttribute> getMarshaledAttributes() {
    ImmutableList.Builder<ValueAttribute> builder = ImmutableList.builder();
    for (ValueAttribute attribute : getImplementedAttributes()) {
      if (!attribute.isJsonIgnore()) {
        builder.add(attribute);
      }
    }
    return builder.build();
  }

  public List<ValueAttribute> getUnmarshaledAttributes() {
    ImmutableList.Builder<ValueAttribute> builder = ImmutableList.builder();
    for (ValueAttribute attribute : getSettableAttributes()) {
      if (!attribute.isJsonIgnore()) {
        builder.add(attribute);
      }
    }
    return builder.build();
  }

  public List<ValueAttribute> getPrimitiveDefaultAttributes() {
    ImmutableList.Builder<ValueAttribute> builder = ImmutableList.builder();
    for (ValueAttribute attribute : getSettableAttributes()) {
      if (attribute.isPrimitive() && attribute.isGenerateDefault()) {
        builder.add(attribute);
      }
    }
    return builder.build();
  }

  public List<ValueAttribute> getImplementedAttributes() {
    if (implementedAttributes == null) {
      implementedAttributes = attributes()
          .filter(Predicates.or(Arrays.asList(
              ValueAttributeFunctions.isGenerateAbstract(),
              ValueAttributeFunctions.isGenerateDefault(),
              ValueAttributeFunctions.isGenerateDerived())))
          .toList();
    }
    return implementedAttributes;
  }

  public List<ValueAttribute> getEquivalenceAttributes() {
    return FluentIterable.from(getImplementedAttributes())
        .filter(NonAuxiliary.PREDICATE)
        .toList();
  }

  public boolean hasSingleParameterConstructor() {
    return isUseConstructor() && getConstructorArguments().size() == 1;
  }

  @Override
  protected TypeMirror internalTypeMirror() {
    return element.asType();
  }
}
