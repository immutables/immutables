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
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import java.lang.annotation.ElementType;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;
import javax.annotation.Nullable;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import org.immutables.generator.SourceExtraction;
import org.immutables.generator.TypeHierarchyCollector;
import org.immutables.value.processor.meta.Constitution.AppliedNameForms;
import org.immutables.value.processor.meta.Constitution.InnerBuilderDefinition;
import org.immutables.value.processor.meta.Constitution.NameForms;
import org.immutables.value.processor.meta.Proto.DeclaringType;
import org.immutables.value.processor.meta.Proto.Environment;
import org.immutables.value.processor.meta.Proto.Protoclass;
import org.immutables.value.processor.meta.Styles.UsingName.TypeNames;

/**
 * It's pointless to refactor this mess until
 * 1) Some sort of type calculus toolkit used/created
 * 2) Facets/Implicits in Generator toolkit with auto-memoising implemented
 */
public final class ValueType extends TypeIntrospectionBase {
  private static final String SERIAL_VERSION_FIELD_NAME = "serialVersionUID";

  // TBD Should we change this field to usage of [classpath.available] templating directive???
  @Nullable
  public String typeMoreObjects;

  public Element element;
  public List<ValueAttribute> attributes = Lists.newArrayList();
  public boolean isHashCodeDefined;
  public boolean isEqualToDefined;
  public boolean isToStringDefined;
  public Constitution constitution;
  public int defaultAttributesCount;
  public int derivedAttributesCount;

  Round round;

  /**
   * Should be called when it is known that there type adapters generation provided.
   * @return the type adapters annotation
   */
  public GsonMirrors.TypeAdapters gsonTypeAdapters() {
    return constitution.protoclass().gsonTypeAdapters().get();
  }

  public CharSequence sourceHeader() {
    if (constitution.style().headerComments()) {
      Optional<DeclaringType> declaringType = constitution.protoclass().declaringType();
      if (declaringType.isPresent()) {
        return declaringType.get().associatedTopLevel().headerComments();
      }
    }
    return "";
  }

  public boolean hasDefaultAttributes() {
    return defaultAttributesCount > 0;
  }

  public boolean hasDerivedAttributes() {
    return derivedAttributesCount > 0;
  }

  public TypeNames names() {
    return constitution.names();
  }

  public AppliedNameForms factoryOf() {
    return constitution.factoryOf();
  }

  public AppliedNameForms factoryCopyOf() {
    return constitution.factoryCopyOf();
  }

  public AppliedNameForms factoryInstance() {
    return constitution.factoryInstance();
  }

  public AppliedNameForms factoryBuilder() {
    return constitution.factoryBuilder();
  }

  public Protoclass.Kind kind() {
    return constitution.protoclass().kind();
  }

  public NameForms typeBuilder() {
    return constitution.typeBuilder();
  }

  public NameForms typeBuilderImpl() {
    return constitution.typeImplementationBuilder();
  }

  public NameForms typeAbstract() {
    return constitution.typeAbstract();
  }

  public NameForms typeValue() {
    return constitution.typeValue();
  }

  public NameForms typeDocument() {
    return constitution.typeDocument();
  }

  public NameForms typeImmutable() {
    return constitution.typeImmutable();
  }

  public NameForms typeEnclosing() {
    return constitution.typeEnclosing();
  }

  public NameForms typeWith() {
    return constitution.typeWith();
  }

  public NameForms typePreferablyAbstract() {
    return constitution.typePreferablyAbstract();
  }

  public boolean isUseBuilder() {
    return immutableFeatures.builder();
  }

  public boolean isImplementationHidden() {
    return constitution.isImplementationHidden();
  }

  public boolean isGenerateTransformer() {
    return constitution.protoclass().isTransformer();
  }

  public boolean isGenerateAst() {
    return constitution.protoclass().isAst();
  }

  public boolean isGenerateJdkOnly() {
    return constitution.style().jdkOnly() || noGuavaInClasspath();
  }

  public boolean isGenerateBuildOrThrow() {
    return !constitution.style().buildOrThrow().isEmpty();
  }

  private boolean noGuavaInClasspath() {
    return typeMoreObjects == null;
  }

  public boolean isUseSimpleReadResolve() {
    return serial.isSimple() && (isUseValidation() || isUseSingletonOnly());
  }

  public boolean isOptionalAcceptNullable() {
    return constitution.style().optionalAcceptNullable();
  }

  @Nullable
  public Long serialVersionUID() {
    Protoclass p = constitution.protoclass();
    if (p.serialVersion().isPresent()) {
      return p.serialVersion().get();
    }
    return serial.isEnabled()
        ? findSerialVersionUID()
        : null;
  }

  private Long findSerialVersionUID() {
    for (VariableElement field : ElementFilter.fieldsIn(element.getEnclosedElements())) {
      if (field.getSimpleName().contentEquals(SERIAL_VERSION_FIELD_NAME)
          && field.asType().getKind() == TypeKind.LONG) {
        return (Long) field.getConstantValue();
      }
    }
    return null;
  }

  public boolean isUseValidation() {
    if (isGenerateOrdinalValue() || validationMethodName != null) {
      return true;
    }
    if (isUseSingletonOnly()) {
      // We don't use validation method just to intern singleton-only.
      // but only if we are not validated by method or generating ordinal value.
      return false;
    }
    if (isUseSingleton()) {
      return serial.isEnabled()
          || !useAttributelessSingleton();
    }
    return isUseInterned();
  }

  public boolean isGenerateJacksonMapped() {
    return constitution.protoclass().isJacksonSerialized();
  }

  public boolean isJacksonDeserialized() {
    Optional<DeclaringType> declaringType = constitution.protoclass().declaringType();
    return declaringType.isPresent()
        && declaringType.get().isJacksonDeserializedAnnotated();
  }

  public boolean isJacksonJsonTypeInfo() {
    if (constitution.protoclass().isJacksonJsonTypeInfo()) {
      return true;
    }
    for (DeclaredType type : implementedInterfaces()) {
      if (Proto.isJacksonJsonTypeInfoAnnotated(type.asElement())) {
        return true;
      }
    }
    for (DeclaredType type : extendedClasses()) {
      if (Proto.isJacksonJsonTypeInfoAnnotated(type.asElement())) {
        return true;
      }
    }
    return false;
  }

  public String getTopSimple() {
    if (enclosingValue != null) {
      return enclosingValue.typeEnclosing().simple();
    }
    if (constitution.isOutsideBuilder() || kind().isFactory()) {
      return typeBuilder().simple();
    }
    return typeImmutable().simple();
  }

  public boolean isTopLevel() {
    return !kind().isNested();
  }

  public boolean isGenerateRepository() {
    return constitution.protoclass().repository().isPresent();
  }

  public boolean isAnnotationType() {
    return element.getKind() == ElementKind.ANNOTATION_TYPE;
  }

  private CaseStructure caseStructure;

  public CaseStructure getCases() {
    if (caseStructure == null) {
      caseStructure = new CaseStructure(allKnownValuesInContext());
    }
    return caseStructure;
  }

  private Iterable<ValueType> allKnownValuesInContext() {
    List<ValueType> values = Lists.newArrayList(nested);

    Environment environment = constitution.protoclass().environment();
    Optional<TransformMirror> transform = constitution.protoclass().getTransform();
    if (transform.isPresent()) {
      for (Protoclass p : environment.protoclassesFrom(includedElements(transform.get()))) {
        values.add(environment.composeValue(p));
      }
    }

    return values;
  }

  private List<Element> includedElements(TransformMirror transform) {
    List<Element> includedElements = Lists.newArrayList();
    TypeMirror[] includeMirror = transform.includeMirror();
    for (TypeMirror mirror : includeMirror) {
      if (mirror.getKind() == TypeKind.DECLARED) {
        includedElements.add(((DeclaredType) mirror).asElement());
      }
    }
    return includedElements;
  }

  public List<CharSequence> passedAnnotations() {
    return Annotations.getAnnotationLines(
        element,
        Sets.union(constitution.protoclass().styles().style().passAnnotationsNames(), constitution.protoclass()
            .styles()
            .style()
            .additionalJsonAnnotationsNames()),
        false,
        ElementType.TYPE);
  }

  public Iterable<ValueType> allValues() {
    List<ValueType> values = Lists.newArrayList();
    if (kind().isValue()) {
      values.add(this);
    }
    values.addAll(nested);
    return values;
  }

  public List<ValueType> nested = Collections.emptyList();

  @Nullable
  ValueType enclosingValue;

  public void addNested(ValueType nested) {
    if (this.nested.isEmpty()) {
      this.nested = Lists.newArrayList();
    }
    this.nested.add(nested);
    nested.enclosingValue = this;
  }

  @Nullable
  public String validationMethodName;

  public boolean isImplementing() {
    return element.getKind() == ElementKind.INTERFACE
        || element.getKind() == ElementKind.ANNOTATION_TYPE;
  }

  public String $$package() {
    return constitution.implementationPackage();
  }

  public String name() {
    return names().raw;
  }

  public boolean isGenerateOrdinalValue() {
    return isOrdinalValue();
  }

  public boolean isGenerateSafeDerived() {
    boolean moreThanOne = defaultAttributesCount + derivedAttributesCount > 1;
    return moreThanOne
        && !isAnnotationType()
        && !constitution.style().unsafeDefaultAndDerived();
  }

  public boolean isUseConstructorOnly() {
    return isUseConstructor() && !isUseBuilder();
  }

  public ValueImmutableInfo immutableFeatures;

  public boolean isUseCopyMethods() {
    return immutableFeatures.copy()
        && !constitution.isImplementationHidden()
        && !getSettableAttributes().isEmpty();
  }

  public boolean isUseCopyConstructor() {
    return isUseCopyMethods() && (isUseConstructor() || isUseBuilder());
  }

  public boolean isUseSingleton() {
    return immutableFeatures.singleton()
        || useAttributelessSingleton()
        || useSingletonNoOtherWay();
  }

  public boolean isUseInterned() {
    return immutableFeatures.intern()
        && !isUseSingletonOnly();
  }

  public boolean isUsePrehashed() {
    return immutableFeatures.prehash()
        && !isGeneratePrivateNoargConstructor();
    // || isUseInterned()
    // || isGenerateOrdinalValue()
  }

  public InnerBuilderDefinition getInnerBuilder() {
    return constitution.innerBuilder();
  }

  public String getDocumentName() {
    Optional<RepositoryMirror> repositoryAnnotation = RepositoryMirror.find(element);
    if (repositoryAnnotation.isPresent()) {
      String value = repositoryAnnotation.get().value();
      if (!value.isEmpty()) {
        return value;
      }
    }
    return CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_CAMEL, name());
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

  public boolean isUseReferenceEquality() {
    if (isAnnotationType()) {
      return false;
    }
    return isUseInterned()
        || isUseSingletonOnly()
        || useAttributelessSingleton()
        || isGenerateOrdinalValue();
  }

  public boolean isUseEqualTo() {
    if (isGenerateOrdinalValue()) {
      return true;
    }
    if (isUseSingletonOnly()) {
      return false;
    }
    if (isUseInterned() || isUseSingleton()) {
      return true;
    }
    return !isEqualToDefined;
  }

  public boolean isUseSingletonOnly() {
    return isUseSingleton()
        && !isUseBuilder()
        && !isUseConstructor()
        && getWithSettableAfterConstruction().isEmpty();
  }

  private boolean useAttributelessSingleton() {
    return constitution.style().attributelessSingleton()
        && getSettableAttributes().isEmpty();
  }

  private boolean useSingletonNoOtherWay() {
    return !isUseBuilder()
        && !isUseConstructor()
        && getMandatoryAttributes().isEmpty();
  }

  public boolean isUseConstructor() {
    return !getConstructorArguments().isEmpty();
  }

  public boolean requiresAlternativeStrictConstructor() {
    for (ValueAttribute constructor : getConstructorArguments()) {
      if (constructor.requiresAlternativeStrictConstructor()) {
        return true;
      }
    }
    return false;
  }

  @Nullable
  private Set<ValueAttribute> constructorArguments;

  public Set<ValueAttribute> getConstructorArguments() {
    if (constructorArguments == null) {
      constructorArguments = computeConstructorArguments();
      if (constructorArguments.isEmpty() && constitution.style().allParameters()) {
        constructorArguments = ImmutableSet.copyOf(getSettableAttributes());
      }
      validateConstructorParameters(constructorArguments);
    }
    return constructorArguments;
  }

  public List<ValueAttribute> getWithSettableAfterConstruction() {
    if (isUseCopyMethods()) {
      return getConstructorExcluded();
    }
    return ImmutableList.of();
  }

  @Nullable
  private List<ValueAttribute> constructorExcluded;

  public List<ValueAttribute> getConstructorExcluded() {
    if (constructorExcluded == null) {
      constructorExcluded = FluentIterable.from(getSettableAttributes())
          .filter(Predicates.not(Predicates.in(getConstructorArguments())))
          .toList();
    }
    return constructorExcluded;
  }

  public List<ValueAttribute> getConstructableAttributes() {
    List<ValueAttribute> attributes = Lists.newArrayList(getConstructorArguments());
    attributes.addAll(getWithSettableAfterConstruction());
    return attributes;
  }

  private void validateConstructorParameters(Set<ValueAttribute> parameters) {
    if (kind().isValue() && !parameters.isEmpty()) {
      Set<Element> definingElements = Sets.newHashSet();
      for (ValueAttribute attribute : parameters) {
        definingElements.add(attribute.element.getEnclosingElement());
      }
      if (definingElements.size() != 1) {
        constitution.protoclass()
            .report()
            .error("Constructor parameters could not be defined on a different level of inheritance hierarchy, "
                + " generated constructor API would be unstable."
                + " To resolve, you can redeclare (override) each inherited"
                + " constuctor parameter in this abstract value type.");
      }
    }
  }

  public Set<ValueAttribute> computeConstructorArguments() {
    return ImmutableSet.copyOf(attributes()
        .filter(Predicates.compose(Predicates.not(Predicates.equalTo(-1)), ToConstructorArgumentOrder.FUNCTION))
        .toSortedList(Ordering.natural().onResultOf(ToConstructorArgumentOrder.FUNCTION)));
  }

  public List<ValueAttribute> getConstructorOmited() {
    return FluentIterable.from(getImplementedAttributes())
        .filter(Predicates.not(Predicates.in(getConstructorArguments())))
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

  @Nullable
  private List<ValueAttribute> settableAttributes;

  public List<ValueAttribute> getSettableAttributes() {
    if (settableAttributes == null) {
      settableAttributes = attributes()
          .filter(Predicates.or(
              ValueAttributeFunctions.isGenerateAbstract(),
              ValueAttributeFunctions.isGenerateDefault()))
          .toList();
    }
    return settableAttributes;
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

  public List<ValueAttribute> getMandatoryAttributes() {
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

  @Nullable
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

  @Nullable
  private ImmutableList<ValueAttribute> allMarshalingAttributes;
  private TypeHierarchyCollector hierarchiCollector;

  public List<ValueAttribute> allMarshalingAttributes() {
    if (allMarshalingAttributes == null) {
      class Collector {
        Map<String, ValueAttribute> byNames = new LinkedHashMap<>();

        ImmutableList<ValueAttribute> collect() {
          addUnique(getMarshaledAttributes());
          addUnique(getUnmarshaledAttributes());
          return ImmutableList.copyOf(byNames.values());
        }

        void addUnique(List<ValueAttribute> attributes) {
          for (ValueAttribute attribute : attributes) {
            String name = attribute.getMarshaledName();
            ValueAttribute existing = byNames.get(name);
            if (existing == null) {
              byNames.put(name, attribute);
            } else if (existing != attribute) {
              attribute.report()
                  .error("Attribute has duplicate marshaled name, check @%s annotation", NamedMirror.simpleName());
            }
          }
        }
      }
      allMarshalingAttributes = new Collector().collect();
    }
    return allMarshalingAttributes;
  }

  public List<ValueAttribute> getDefaultAttributes() {
    if (!hasDefaultAttributes()) {
      return ImmutableList.of();
    }
    ImmutableList.Builder<ValueAttribute> builder = ImmutableList.builder();
    for (ValueAttribute attribute : getImplementedAttributes()) {
      if (attribute.isGenerateDefault) {
        builder.add(attribute);
      }
    }
    return builder.build();
  }

  public List<ValueAttribute> getRequiresTrackedIsSetNonMandatoryAttributes() {
    ImmutableList.Builder<ValueAttribute> builder = ImmutableList.builder();
    for (ValueAttribute attribute : getSettableAttributes()) {
      if (attribute.requiresTrackIsSet()) {
        builder.add(attribute);
      }
    }
    return builder.build();
  }

  public boolean isUseStrictBuilder() {
    return constitution.protoclass()
        .styles()
        .style()
        .strictBuilder();
  }

  public boolean isGeneratePrivateNoargConstructor() {
    return constitution.protoclass()
        .styles()
        .style()
        .privateNoargConstructor();
  }

  public String getThrowForInvalidImmutableState() {
    return constitution.protoclass()
        .styles()
        .style()
        .throwForInvalidImmutableStateName();
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

  private static class HasJdkKind implements Predicate<ValueAttribute> {
    private final AttributeTypeKind kind;

    HasJdkKind(AttributeTypeKind kind) {
      this.kind = kind;
    }

    @Override
    public boolean apply(ValueAttribute attribute) {
      return attribute.typeKind() == kind
          && !attribute.isGuavaImmutableDeclared();
    }
  }

  public boolean isUseListUtility() {
    return useCollectionUtility(new HasJdkKind(AttributeTypeKind.LIST));
  }

  public boolean isUseSetUtility() {
    return useCollectionUtility(new HasJdkKind(AttributeTypeKind.SET));
  }

  public boolean isUseEnumSetUtility() {
    return useCollectionUtility(new HasJdkKind(AttributeTypeKind.ENUM_SET));
  }

  public boolean isUseSortedSetUtility() {
    return useCollectionUtility(new HasJdkKind(AttributeTypeKind.SORTED_SET));
  }

  public boolean isUseMapUtility() {
    return useCollectionUtility(new HasJdkKind(AttributeTypeKind.MAP));
  }

  public boolean isUseEnumMapUtility() {
    return useCollectionUtility(new HasJdkKind(AttributeTypeKind.ENUM_MAP));
  }

  public boolean isUseSortedMapUtility() {
    return useCollectionUtility(new HasJdkKind(AttributeTypeKind.SORTED_MAP));
  }

  private boolean useCollectionUtility(Predicate<ValueAttribute> predicate) {
    for (ValueType n : nested) {
      if (Iterables.any(n.getSettableAttributes(), predicate)) {
        return true;
      }
    }
    return Iterables.any(getSettableAttributes(), predicate);
  }

  public Set<String> getRequiredSourceStarImports() {
    if (!hasSomeUnresolvedTypes()) {
      return Collections.emptySet();
    }
    Set<String> starImports = Sets.newLinkedHashSet();

    for (ValueType n : FluentIterable.from(nested).append(this)) {
      for (ValueAttribute a : n.attributes) {
        if (a.hasSomeUnresolvedTypes) {
          DeclaringType topLevel = a.getDeclaringType().associatedTopLevel();

          SourceExtraction.Imports sourceImports =
              topLevel.sourceImports();

          for (String importStatement : sourceImports.all) {
            if (importStatement.indexOf('*') > 0) {
              starImports.add(importStatement);
            }
          }

          if (!topLevel.packageOf().equals(constitution.protoclass().packageOf())) {
            String prefix = topLevel.packageOf().asPrefix();
            // guard against unnamed packages
            if (!prefix.isEmpty()) {
              starImports.add(prefix + '*');
            }
          }
        }
      }
    }
    return starImports;
  }

  private boolean hasSomeUnresolvedTypes() {
    for (ValueType n : nested) {
      for (ValueAttribute a : n.attributes) {
        if (a.hasSomeUnresolvedTypes) {
          return true;
        }
      }
    }
    for (ValueAttribute a : attributes) {
      if (a.hasSomeUnresolvedTypes) {
        return true;
      }
    }
    return false;
  }

  public boolean hasCollectionAttribute() {
    for (ValueAttribute attribute : getSettableAttributes()) {
      if (attribute.isCollectionType() || attribute.isMapType()) {
        return true;
      }
    }
    return false;
  }

  public boolean isUseNullSafeUtilities() {
    for (ValueType n : nested) {
      for (ValueAttribute a : n.attributes) {
        if (a.isNullable()) {
          return true;
        }
      }
    }
    for (ValueAttribute a : attributes) {
      if (a.isNullable()) {
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

  public boolean isGenerateConstructorUseCopyConstructor() {
    return isUseCopyMethods()
        && hasNonNullCheckableParametersInDefaultOrder()
        && getConstructorExcluded().isEmpty();
  }

  private boolean hasNonNullCheckableParametersInDefaultOrder() {
    for (ValueAttribute c : getConstructorArguments()) {
      if (c.hasConstructorParameterCustomOrder()) {
        return false;
      }
      if (!c.isPrimitive() && !c.isNullable()) {
        return false;
      }
    }
    return true;
  }

  public boolean isSynthCopyConstructor() {
    return isUseConstructor()
        && !isGenerateConstructorUseCopyConstructor()
        && getConstructorExcluded().isEmpty();
  }

  public boolean isGenerateBuilderUseCopyConstructor() {
    return isUseBuilder()
        && isUseCopyMethods()
        && !isOrdinalValue()
        && getDefaultAttributes().isEmpty();
  }

  public boolean isGenerateBuilderConstructor() {
    return isUseBuilder()
        && !(isUseSingleton() && settableAttributes.isEmpty())
        && !isGenerateBuilderUseCopyConstructor();
  }

  public boolean isGenerateClearBuilder() {
    return constitution.style().clearBuilder();
  }

  @Override
  protected TypeHierarchyCollector collectTypeHierarchy(TypeMirror typeMirror) {
    this.hierarchiCollector = super.collectTypeHierarchy(typeMirror);
    return hierarchiCollector;
  }

  ImmutableList<DeclaredType> extendedClasses() {
    ensureTypeIntrospected();
    return hierarchiCollector.extendedClasses();
  }

  ImmutableSet<DeclaredType> implementedInterfaces() {
    ensureTypeIntrospected();
    return hierarchiCollector.implementedInterfaces();
  }

  @Nullable
  private Boolean generateBuilderFrom;

  public boolean isGenerateBuilderFrom() {
    if (generateBuilderFrom == null) {
      generateBuilderFrom = !isUseStrictBuilder() && noAttributeInitializerIsNamedAsFrom();
    }
    return generateBuilderFrom;
  }

  public boolean isGenerateFilledFrom() {
    return kind().isModifiable() && noAttributeSetterIsNamedAsFrom();
  }

  private boolean noAttributeInitializerIsNamedAsFrom() {
    for (ValueAttribute a : getSettableAttributes()) {
      if (a.names.init.equals(names().from)) {
        a.report().warning(
            "Attribute initializer named '%s' clashes with special builder method, "
                + "which will not be generated to not have ambiguous overload or conflict",
            names().from);
        return false;
      }
    }
    return true;
  }

  // Used for modifiable
  private boolean noAttributeSetterIsNamedAsFrom() {
    for (ValueAttribute a : getSettableAttributes()) {
      if (a.names.set().equals(names().from)) {
        a.report().warning(
            "Attribute setter named '%s' clashes with special builder method, "
                + "which will not be generated to not have ambiguous overload or conflict",
            names().from);
        return false;
      }
    }
    return true;
  }

  public boolean hasDeprecatedAttributes() {
    for (ValueAttribute a : getImplementedAttributes()) {
      if (a.deprecated) {
        return true;
      }
    }
    return false;
  }

  public boolean hasSettableCollection() {
    for (ValueAttribute a : getSettableAttributes()) {
      if (a.isCollectionType()) {
        return true;
      }
    }
    return false;
  }

  public boolean hasSettableMapping() {
    for (ValueAttribute a : getSettableAttributes()) {
      if (a.isMapType()) {
        return true;
      }
    }
    return false;
  }

  ImmutableListMultimap<String, TypeElement> accessorMapping;

  @Nullable
  private FromSupertypesModel buildFromTypes;

  public FromSupertypesModel getBuildFromTypes() {
    if (buildFromTypes == null) {
      buildFromTypes = new FromSupertypesModel(
          typeAbstract().toString(),
          getSettableAttributes(),
          accessorMapping);
    }
    return buildFromTypes;
  }

  public Serialization serial = Serialization.NONE;

  public ImmutableList<String> throwing = ImmutableList.of();

  public enum Serialization {
    NONE, STRUCTURAL, STRUCTURAL_IMPLEMENTS, IMPLEMENTS, SERIAL_VERSION;

    public boolean isEnabled() {
      return this != NONE;
    }

    public boolean isStructural() {
      return this == STRUCTURAL
          || this == STRUCTURAL_IMPLEMENTS;
    }

    public boolean isSimple() {
      return this == IMPLEMENTS
          || this == SERIAL_VERSION;
    }

    public boolean shouldImplement() {
      return this == STRUCTURAL
          || this == SERIAL_VERSION;
    }
  }

  void detectSerialization() {
    Protoclass p = constitution.protoclass();
    boolean isSerializable = isSerializable();
    if (p.isSerialStructural()) {
      serial = isSerializable
          ? Serialization.STRUCTURAL_IMPLEMENTS
          : Serialization.STRUCTURAL;
    } else if (isSerializable) {
      serial = Serialization.IMPLEMENTS;
    } else if (p.serialVersion().isPresent()) {
      serial = Serialization.SERIAL_VERSION;
    }
  }

  public boolean isGenerateSuppressAllWarnings() {
    return constitution.protoclass().styles().style().generateSuppressAllWarnings()
        || SuppressedWarnings.forElement(element).generated;
  }

  public boolean isUseCompactBuilder() {
    return !kind().isFactory()
        && !isUseStrictBuilder()
        && !isGenerateBuildOrThrow()
        && getThrowForInvalidImmutableState().equals(IllegalStateException.class.getName());
  }

  DeclaringType inferDeclaringType(Element element) {
    @Nullable TypeElement declaringType = null;
    for (Element e = element; e != null;) {
      e = e.getEnclosingElement();
      if (e instanceof TypeElement) {
        declaringType = (TypeElement) e;
        break;
      }
    }
    if (declaringType == null) {
      throw new NoSuchElementException();
    }
    return round.declaringTypeFrom(declaringType);
  }

  public Set<String> getNonAttributeAbstractMethodSignatures() {
    if (element.getKind().isClass() || element.getKind().isInterface()) {
      Set<String> signatures = new LinkedHashSet<>();

      List<? extends Element> members = round.environment()
          .processing()
          .getElementUtils()
          .getAllMembers(CachingElements.getDelegate((TypeElement) element));

      for (ExecutableElement m : ElementFilter.methodsIn(members)) {
        if (!m.getParameters().isEmpty()) {
          if (m.getModifiers().contains(Modifier.ABSTRACT)) {
            signatures.add(toSignature(m));
          }
        }
      }

      return signatures;
    }
    return Collections.emptySet();
  }

  public List<ValueAttribute> getFunctionalAttributes() {
    if (!constitution.protoclass().hasFunctionalModule()) {
      return ImmutableList.of();
    }

    Optional<DeclaringType> declaringType = constitution.protoclass().declaringType();

    if (declaringType.isPresent()) {
      if (FunctionalMirror.isPresent(declaringType.get().element())) {
        return getAllAccessibleAttributes();
      }
    }

    if (FunctionalMirror.isPresent(element)) {
      return getAllAccessibleAttributes();
    }

    List<ValueAttribute> params = Lists.newArrayList();

    for (ValueAttribute a : getAllAccessibleAttributes()) {
      if (FunctionalMirror.isPresent(a.element)) {
        params.add(a);
      }
    }

    return params;
  }

  public List<ValueAttribute> getBuilderParameters() {
    if (!constitution.protoclass().hasBuilderModule()) {
      return ImmutableList.of();
    }

    List<ValueAttribute> params = Lists.newArrayList();

    for (ValueAttribute a : getSettableAttributes()) {
      if (a.isBuilderParameter) {
        params.add(a);
      }
    }

    return params;
  }

  private String toSignature(ExecutableElement m) {
    StringBuilder signature = new StringBuilder();

    if (m.getModifiers().contains(Modifier.PUBLIC)) {
      signature.append("public ");
    } else if (m.getModifiers().contains(Modifier.PROTECTED)) {
      signature.append("protected ");
    }

    DeclaringType declaringType = inferDeclaringType(m);

    signature.append(printType(m, m.getReturnType(), declaringType));
    signature.append(" ").append(m.getSimpleName());
    signature.append("(");

    boolean notFirst = false;
    for (VariableElement p : m.getParameters()) {
      if (notFirst) {
        signature.append(", ");
      }
      signature.append(printType(p, p.asType(), declaringType));
      signature.append(" ").append(p.getSimpleName());
      notFirst = true;
    }

    signature.append(")");
    return signature.toString();
  }

  private String printType(Element element, TypeMirror type, DeclaringType declaringType) {
    TypeStringProvider provider =
        new TypeStringProvider(constitution.protoclass().report(), element, type, declaringType);
    provider.process();
    return provider.returnTypeName();
  }

  /**
   * Used for type snapshoting
   */
  @Override
  public int hashCode() {
    return Objects.hash(constitution.protoclass().name());
  }

  @Override
  public String toString() {
    return "Type[" + name() + "]";
  }
}
