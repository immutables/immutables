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

import com.google.common.base.CaseFormat;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.PackageElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.SimpleAnnotationValueVisitor7;
import org.immutables.value.ext.ExtValue;
import org.immutables.value.ext.Json;
import org.immutables.value.ext.Mongo;
import org.immutables.value.ext.Parboil;
import org.immutables.value.processor.meta.Constitution.NameForms;
import org.immutables.value.processor.meta.Proto.DeclaringType;
import org.immutables.value.processor.meta.Proto.Protoclass;
import org.immutables.value.processor.meta.Styles.UsingName.TypeNames;
import org.immutables.value.processor.meta.ValueAttribute.SimpleTypeDerivationBase;

/**
 * It's pointless to refactor this mess until
 * 1) Some sort of type calculus toolkit used/created
 * 2) Facets/Implicits in Generator toolkit with auto-memoising implemented
 */
public final class ValueType extends TypeIntrospectionBase {
  private static final String SUPER_BUILDER_TYPE_NAME = "Builder";
  private static final ImmutableSet<String> JACKSON_MAPPING_ANNOTATION_CLASSES =
      ImmutableSet.of(
          "com.fasterxml.jackson.databind.annotation.JsonSerialize",
          "com.fasterxml.jackson.databind.annotation.JsonDeserialize");

  // TBD Should we change this field to usage of [classpath.available] templating directive???
  @Nullable
  public String typeMoreObjects;

  public Element element;
  public List<ValueAttribute> attributes = Lists.newArrayList();
  public boolean isHashCodeDefined;
  public boolean isEqualToDefined;
  public boolean isToStringDefined;
  public Constitution constitution;
  Round round;

  public TypeNames names() {
    return constitution.names();
  }

  public NameForms factoryOf() {
    return constitution.factoryOf();
  }

  public NameForms factoryCopyOf() {
    return constitution.factoryCopyOf();
  }

  public NameForms factoryInstance() {
    return constitution.factoryInstance();
  }

  public NameForms factoryBuilder() {
    return constitution.factoryBuilder();
  }

  public Protoclass.Kind kind() {
    return constitution.protoclass().kind();
  }

  public NameForms typeBuilder() {
    return constitution.typeBuilder();
  }

  public NameForms typeAbstract() {
    return constitution.typeAbstract();
  }

  public NameForms typeValue() {
    return constitution.typeValue();
  }

  public NameForms typeImmutable() {
    return constitution.typeImmutable();
  }

  public NameForms typeEnclosing() {
    return constitution.typeEnclosing();
  }

  public boolean isUseBuilder() {
    return immutableFeatures.builder();
  }

  public boolean isImplementationHidden() {
    return constitution.isImplementationHidden();
  }

  public boolean isGenerateJdkOnly() {
    return typeMoreObjects == null || constitution.style().jdkOnly();
  }

  public boolean isGenerateJacksonMapped() {
    if (generateJacksonMapped == null) {
      generateJacksonMapped = inferJacksonMapped();
    }
    return generateJacksonMapped;
  }

  private boolean inferJacksonMapped() {
    List<? extends AnnotationMirror> annotationMirrors = element.getAnnotationMirrors();
    for (AnnotationMirror annotation : annotationMirrors) {
      TypeElement annotationElement = (TypeElement) annotation.getAnnotationType().asElement();
      if (JACKSON_MAPPING_ANNOTATION_CLASSES.contains(annotationElement.getQualifiedName().toString())) {
        return true;
      }
    }
    return false;
  }

  public boolean isTopLevel() {
    return !kind().isNested();
  }

  public boolean isAnnotationType() {
    return element.getKind() == ElementKind.ANNOTATION_TYPE;
  }

  public boolean isGenerateParboiled() {
    return kind().isEnclosing() && element.getAnnotation(Parboil.Ast.class) != null;
  }

  public boolean isGenerateTransformer() {
    return kind().isValue() && element.getAnnotation(ExtValue.Transformer.class) != null;
  }

  private CaseStructure caseStructure;

  public CaseStructure getCases() {
    if (caseStructure == null) {
      caseStructure = new CaseStructure(this);
    }
    return caseStructure;
  }

  public List<ValueType> nested = Collections.emptyList();

  @Nullable
  private ValueType enclosingValue;

  public void addNested(ValueType nested) {
    if (this.nested.isEmpty()) {
      this.nested = Lists.newArrayList();
    }
    this.nested.add(nested);
    nested.enclosingValue = this;
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

  public <T extends Annotation> boolean hasAnnotation(Class<T> annotationType) {
    Optional<DeclaringType> declaringType = constitution.protoclass().declaringType();
    if (declaringType.isPresent()) {
      if (declaringType.get().hasAnnotation(annotationType)) {
        return true;
      }
    }
    return false;
  }

  public String $$package() {
    return constitution.protoclass().packageOf().name();
  }

  public String name() {
    return names().raw;
  }

  public boolean isGenerateOrdinalValue() {
    return !isGenerateJdkOnly() && isOrdinalValue();
  }

  public boolean isUseConstructorOnly() {
    return isUseConstructor() && !isUseBuilder();
  }

  public ImmutableMirror immutableFeatures;

  public boolean isUseCopyMethods() {
    return immutableFeatures.copy()
        && !constitution.returnsAbstractValueType()
        && !getImplementedAttributes().isEmpty();
  }

  public boolean isUseCopyConstructor() {
    return isUseCopyMethods() && (isUseConstructor() || isUseBuilder());
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

  private Boolean generateMarshaled;

  public boolean isGenerateMarshaled() {
    if (generateMarshaled == null) {
      generateMarshaled = hasAnnotation(Json.Marshaled.class)
          || isGenerateRepository();
    }
    return generateMarshaled;
  }

  public boolean isGenerateTypeAdapted() {
    // TODO propagate this from MetaAnnotated
    return TypeAdaptedMirror.isPresent(element);
  }

  public boolean isGenerateModifiable() {
    return ModifiableMirror.isPresent(element);
  }

  public boolean isGenerateRepository() {
    return element.getAnnotation(Mongo.Repository.class) != null;
  }

  private Boolean hasAbstractBuilder;

  public boolean isHasAbstractBuilder() {
    if (hasAbstractBuilder == null) {
      boolean abstractBuilderDeclared = false;
      for (Element t : element.getEnclosedElements()) {
        if (t.getKind() == ElementKind.CLASS) {
          if (t.getSimpleName().contentEquals(SUPER_BUILDER_TYPE_NAME)) {
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
    @Nullable Mongo.Repository annotation = element.getAnnotation(Mongo.Repository.class);
    if (annotation != null && !annotation.value().isEmpty()) {
      return annotation.value();
    }
    return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, name());
  }

  private Set<String> importedMarshalRoutines;

  public Set<String> getGenerateImportedMarshalRoutines() {
    if (importedMarshalRoutines == null) {
      Set<String> imports = Sets.newLinkedHashSet();
      collectImportRoutines(imports);
      importedMarshalRoutines = ImmutableSet.copyOf(imports);
    }
    return importedMarshalRoutines;
  }

  private List<SimpleTypeDerivationBase> importedMarshalers;

  public List<SimpleTypeDerivationBase> getGenerateImportedMarshalers() {
    if (importedMarshalers == null) {
      List<SimpleTypeDerivationBase> imports = Lists.newArrayList();
      for (ValueAttribute a : attributes()) {
        imports.addAll(a.getMarshaledImportRoutines());
      }
      importedMarshalers = ImmutableList.copyOf(imports);
    }
    return importedMarshalers;
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

  public boolean isUseReferenceEquality() {
    if (isAnnotationType()) {
      return false;
    }
    return isUseInterned() || isGenerateOrdinalValue() || isUseSingletonOnly();
  }

  public boolean isUseSingletonOnly() {
    return isUseSingleton() && !isUseConstructor() && !isUseBuilder();
  }

  private List<String> extractClassNamesFromMirrors(
      Class<?> annotationType,
      String annotationValueName,
      List<? extends AnnotationMirror> annotationMirrors) {
    return FluentIterable.from(
        extractedTypesFromAnnotationMirrors(
            annotationType.getCanonicalName(),
            annotationValueName,
            annotationMirrors))
        .transform(Functions.toStringFunction())
        .toList();
  }

  public static Iterable<DeclaredType> extractedTypesFromAnnotationMirrors(
      String annotationTypeName,
      String annotationValueName,
      List<? extends AnnotationMirror> annotationMirrors) {
    final List<DeclaredType> collectTypes = Lists.newArrayList();

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
                if (t instanceof DeclaredType) {
                  collectTypes.add((DeclaredType) t);
                }
                return null;
              }
            }, null);
          }
        }
      }
    }

    return collectTypes;
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
      return input.getConstructorParameterOrder();
    }
  }

  public List<ValueAttribute> getExcludableAttributes() {
    List<ValueAttribute> excludables = Lists.newArrayList();
    for (ValueAttribute attribute : attributes()) {
      if (attribute.isGenerateAbstract && (attribute.isContainerType() && !attribute.isArrayType())) {
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
      if (attribute.isGenerateLazy) {
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
  @Nullable
  private Boolean generateJacksonMapped;

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
      if (attribute.isPrimitive() && attribute.isGenerateDefault) {
        builder.add(attribute);
      }
    }
    return builder.build();
  }

  public List<ValueAttribute> getDefaultAttributes() {
    ImmutableList.Builder<ValueAttribute> builder = ImmutableList.builder();
    for (ValueAttribute attribute : getImplementedAttributes()) {
      if (attribute.isGenerateDefault) {
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

  private static class KindPredicate implements Predicate<ValueAttribute> {
    private final AttributeTypeKind kind;

    KindPredicate(AttributeTypeKind kind) {
      this.kind = kind;
    }

    @Override
    public boolean apply(ValueAttribute attribute) {
      return attribute.typeKind() == kind;
    }
  }

  public boolean isUseListUtility() {
    return useCollectionUtility(new KindPredicate(AttributeTypeKind.LIST));
  }

  public boolean isUseSetUtility() {
    return useCollectionUtility(new KindPredicate(AttributeTypeKind.SET));
  }

  public boolean isUseEnumSetUtility() {
    return useCollectionUtility(new KindPredicate(AttributeTypeKind.ENUM_SET));
  }

  public boolean isUseSortedSetUtility() {
    return useCollectionUtility(new KindPredicate(AttributeTypeKind.SORTED_SET));
  }

  public boolean isUseMapUtility() {
    return useCollectionUtility(new KindPredicate(AttributeTypeKind.MAP));
  }

  public boolean isUseEnumMapUtility() {
    return useCollectionUtility(new KindPredicate(AttributeTypeKind.ENUM_MAP));
  }

  public boolean isUseSortedMapUtility() {
    return useCollectionUtility(new KindPredicate(AttributeTypeKind.SORTED_MAP));
  }

  private boolean useCollectionUtility(Predicate<ValueAttribute> predicate) {
    for (ValueType n : nested) {
      if (Iterables.any(n.getSettableAttributes(), predicate)) {
        return true;
      }
    }
    return Iterables.any(getSettableAttributes(), predicate);
  }

  public boolean hasCollectionAttribute() {
    for (ValueAttribute attribute : getSettableAttributes()) {
      if (attribute.isCollectionType() || attribute.isMapLike()) {
        return true;
      }
    }
    return false;
  }

  public boolean isUseCollectionUtility() {
    for (ValueType n : nested) {
      if (n.hasCollectionAttribute()) {
        return true;
      }
    }
    return hasCollectionAttribute();
  }

  /**
   * Used for type content snapshoting
   */
  @Override
  public int hashCode() {
    return Objects.hash(constitution.protoclass().name());
  }

  @Override
  public String toString() {
    return "Value[" + name() + "]";
  }
}
