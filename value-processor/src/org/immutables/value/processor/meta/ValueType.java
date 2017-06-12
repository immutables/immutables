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

import javax.lang.model.element.Parameterizable;
import org.immutables.value.processor.encode.TypeExtractor;
import com.google.common.base.CaseFormat;
import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.base.Splitter;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import com.google.common.collect.Sets;
import java.lang.annotation.ElementType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
import org.immutables.generator.Output;
import org.immutables.generator.SourceExtraction;
import org.immutables.generator.TypeHierarchyCollector;
import org.immutables.value.processor.meta.Constitution.AppliedNameForms;
import org.immutables.value.processor.meta.Constitution.InnerBuilderDefinition;
import org.immutables.value.processor.meta.Constitution.NameForms;
import org.immutables.value.processor.meta.Proto.DeclaringType;
import org.immutables.value.processor.meta.Proto.Environment;
import org.immutables.value.processor.meta.Proto.JacksonMode;
import org.immutables.value.processor.meta.Proto.Protoclass;
import org.immutables.value.processor.meta.Styles.UsingName.TypeNames;

/**
 * It's pointless to refactor this mess until
 * 1) Some sort of type calculus toolkit used/created
 * 2) Facets/Implicits in Generator toolkit with auto-memoising implemented
 */
public final class ValueType extends TypeIntrospectionBase {
  private static final String SERIAL_VERSION_FIELD_NAME = "serialVersionUID";
  public Element element;
  public List<ValueAttribute> attributes = Lists.newArrayList();
  public boolean isHashCodeDefined;
  public boolean isEqualToDefined;
  public boolean isToStringDefined;
  public Constitution constitution;
  public int defaultAttributesCount;
  public int derivedAttributesCount;

  public Generics generics() {
    return constitution.generics();
  }

  /**
   * Should be called when it is known that there type adapters generation provided.
   * @return the type adapters annotation
   */
  public GsonMirrors.TypeAdapters gsonTypeAdapters() {
    return constitution.protoclass().gsonTypeAdapters().get();
  }

  private @Nullable CharSequence sourceHeader;

  public CharSequence sourceHeader() {
    if (this.sourceHeader == null) {
      String noImportsPragma = ImportRewriteDisabler.shouldDisableFor(this)
          ? Output.NO_IMPORTS
          : "";

      if (constitution.style().headerComments()) {
        Optional<DeclaringType> declaringType = constitution.protoclass().declaringType();
        if (declaringType.isPresent()) {
          CharSequence headerComments = declaringType.get().associatedTopLevel().headerComments();
          return !noImportsPragma.isEmpty()
              ? new StringBuilder(noImportsPragma).append('\n').append(headerComments)
              : headerComments;
        }
      }
      this.sourceHeader = noImportsPragma;
    }
    return sourceHeader;
  }

  @Nullable
  public String typeMoreObjects() {
    return constitution.protoclass().environment().typeMoreObjects();
  }

  public boolean hasDefaultAttributes() {
    return defaultAttributesCount > 0;
  }

  public boolean hasOptionalAttributes() {
    for (ValueAttribute attribute : attributes()) {
      if (attribute.isOptionalType()) {
        return true;
      }
    }
    return false;
  }

  public boolean isDeferCollectionAllocation() {
    return constitution.style().deferCollectionAllocation() && !isUseStrictBuilder();
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
    return immutableFeatures.builder() || constitution.innerBuilder().isExtending;
  }

  public boolean isImplementationHidden() {
    return constitution.isImplementationHidden();
  }

  public boolean isGenerateTransformer() {
    return constitution.protoclass().isTransformer();
  }

  public boolean isGenerateVisitor() {
    return constitution.protoclass().isVisitor();
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

  public boolean isBeanFriendlyModifiable() {
    return constitution.style().beanFriendlyModifiables();
  }

  private boolean noGuavaInClasspath() {
    return !constitution.protoclass().environment().hasGuavaLib();
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

  private List<CharSequence> constructorAnnotations;

  public List<CharSequence> getConstructorAnnotations() {
    if (constructorAnnotations == null) {
      List<ExecutableElement> constructors = ElementFilter.constructorsIn(element.getEnclosedElements());
      for (ExecutableElement c : constructors) {
        if (c.getParameters().isEmpty()) {
          Set<Modifier> modifiers = c.getModifiers();
          if (modifiers.contains(Modifier.PRIVATE)) {
            report()
                .withElement(c)
                .error("Constructor in an abstract value type should not be private");
          }
          constructorAnnotations =
              Annotations.getAnnotationLines(
                  c,
                  Collections.<String>emptySet(),
                  true,
                  false,
                  ElementType.CONSTRUCTOR,
                  newTypeStringResolver());
        }
      }
      if (constructorAnnotations == null) {
        for (ExecutableElement c : constructors) {
          report()
              .withElement(c)
              .error("Constructor should not have parameters in an abstract value type to be extended");
        }
        constructorAnnotations = ImmutableList.of();
      }
    }
    return constructorAnnotations;
  }

  public List<CharSequence> getBuilderAnnotations() {
    Optional<DeclaringType> declaringType = constitution.protoclass().declaringType();
    if (declaringType.isPresent() && declaringType.get().jacksonSerializeMode() == JacksonMode.BUILDER) {
      return Annotations.getAnnotationLines(
          element,
          Collections.<String>emptySet(),
          true,
          ElementType.TYPE,
          newTypeStringResolver());
    }
    return ImmutableList.of();
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
    if (isGenerateOrdinalValue() || !validationMethods.isEmpty() || isUseJavaValidationApi()) {
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

  public boolean isGenerateJacksonProperties() {
    return constitution.protoclass().isJacksonProperties();
  }

  public boolean isGenerateJacksonIngoreFields() {
    return isGenerateJacksonProperties()
        && constitution.style().forceJacksonIgnoreFields();
  }

  public boolean isJacksonDeserialized() {
    return constitution.protoclass().isJacksonDeserialized();
  }

  public boolean isJacksonJsonTypeInfo() {
    if (constitution.protoclass().isJacksonJsonTypeInfo()) {
      return true;
    }
    for (TypeElement t : implementedInterfaces()) {
      if (Proto.isJacksonJsonTypeInfoAnnotated(t)) {
        return true;
      }
    }
    for (TypeElement t : extendedClasses()) {
      if (Proto.isJacksonJsonTypeInfoAnnotated(t)) {
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

  public MongoMirrors.Repository getRepository() {
    return constitution.protoclass().repository().get();
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
    Optional<TreesIncludeMirror> include = constitution.protoclass().getTreesInclude();
    if (include.isPresent()) {
      for (Protoclass p : environment.protoclassesFrom(includedElements(include.get()))) {
        values.add(environment.composeValue(p));
      }
    }

    return values;
  }

  private List<Element> includedElements(TreesIncludeMirror include) {
    List<Element> includedElements = Lists.newArrayList();
    for (TypeMirror mirror : include.valueMirror()) {
      if (mirror.getKind() == TypeKind.DECLARED) {
        includedElements.add(((DeclaredType) mirror).asElement());
      }
    }
    return includedElements;
  }

  public List<CharSequence> passedAnnotations() {
    return Annotations.getAnnotationLines(
        element,
        Sets.union(
            constitution.style().passAnnotationsNames(),
            constitution.style().additionalJsonAnnotationsNames()),
        false,
        ElementType.TYPE,
        newTypeStringResolver());
  }

  private ImportsTypeStringResolver newTypeStringResolver() {
    @Nullable DeclaringType type = constitution.protoclass().declaringType().orNull();
    return new ImportsTypeStringResolver(type, type);
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

  public ImmutableList<ValidationMethod> validationMethods = ImmutableList.of();

  public static class ValidationMethod {
    public final String name;
    public final boolean normalize;

    ValidationMethod(String name, boolean normalize) {
      this.name = name;
      this.normalize = normalize;
    }
  }

  void addNormalizeMethod(String name, boolean normalize) {
    validationMethods = ImmutableList.<ValidationMethod>builder()
        .add(new ValidationMethod(name, normalize))
        .addAll(validationMethods)
        .build();
  }

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

  public boolean isGenerateWithInterface() {
    ensureTypeIntrospected();
    return implementedInterfacesNames.contains(typeWith().relative());
  }

  public boolean isUseCopyMethods() {
    return !getSettableAttributes().isEmpty()
        && (isGenerateWithInterface()
            || (immutableFeatures.copy()
                && !constitution.isImplementationHidden()));
  }

  public boolean isUseCopyConstructor() {
    return immutableFeatures.copy()
        && (isUseConstructor() || isUseBuilder());
  }

  public boolean isUseSingleton() {
    return generics().isEmpty()
        && (immutableFeatures.singleton()
            || useAttributelessSingleton()
            || useSingletonNoOtherWay());
  }

  public boolean isUseInterned() {
    return generics().isEmpty()
        && immutableFeatures.intern()
        && !isUseSingletonOnly();
  }

  public boolean isUsePrehashed() {
    return immutableFeatures.prehash()
        && !isGeneratePrivateNoargConstructor();
  }

  public InnerBuilderDefinition getInnerBuilder() {
    return constitution.innerBuilder();
  }

  public String getDocumentName() {
    Optional<RepositoryMirror> repositoryAnnotation = RepositoryMirror.find(element);
    if (repositoryAnnotation.isPresent()) {
      RepositoryMirror mirror = repositoryAnnotation.get();
      if (!mirror.collection().isEmpty()) {
        return mirror.collection();
      } else if (!mirror.value().isEmpty()) {
        return mirror.value();
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

  public boolean isUseSingletonOnlyForConstruction() {
    return isUseSingleton()
        && !useAttributelessSingleton()
        && useSingletonNoOtherWay();
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
        report().warning("Constructor parameters should be better defined on the same level of inheritance hierarchy, "
            + " otherwise generated constructor API would be unstable: "
            + " parameter list can change the order of arguments."
            + " It is better redeclare (override) each inherited"
            + " attribute parameter in this abstract value type to avoid this warning."
            + " Or better have constructor parameters defined by only single supertype.");
      }
    }
  }

  public Set<ValueAttribute> computeConstructorArguments() {
    return ImmutableSet.copyOf(
        FluentIterable.from(getSettableAttributes())
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

  public List<ValueAttribute> getMandatoryOrEncodingAttributes() {
    List<ValueAttribute> mandatory = Lists.newArrayList();
    for (ValueAttribute attribute : getSettableAttributes()) {
      if (attribute.isMandatory() || attribute.isEncoding()) {
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
    return constitution.style().strictBuilder()
        || constitution.style().stagedBuilder();
  }

  public boolean isUseJavaValidationApi() {
    return constitution.style().validationMethod() == ValueMirrors.Style.ValidationMethod.VALIDATION_API;
  }

  private @Nullable TelescopicBuild telescopicBuild;

  public @Nullable TelescopicBuild getTelescopicBuild() {
    if (telescopicBuild == null) {
      if (constitution.style().stagedBuilder()
          && !getMandatoryAttributes().isEmpty()) {
        TelescopicBuild tb = TelescopicBuild.from(getSettableAttributes());
        if (!tb.stages.isEmpty()) {
          telescopicBuild = tb;
        }
      }
    }
    return telescopicBuild;
  }

  public boolean isGeneratePrivateNoargConstructor() {
    return constitution.style().privateNoargConstructor();
  }

  public String getThrowForInvalidImmutableState() {
    return constitution.style().throwForInvalidImmutableStateName();
  }

  public boolean isCustomizedThrowForInvalidImmutableState() {
    return !getThrowForInvalidImmutableState()
        .equals(IllegalStateException.class.getName());
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

  public boolean hasAuxiliaryAttributes() {
    for (ValueAttribute a : getImplementedAttributes()) {
      if (a.isAuxiliary()) {
        return true;
      }
    }
    return false;
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

  private @Nullable Set<String> additionalImports;

  public void additionalImports(Set<String> imports) {
    if (!imports.isEmpty()) {
      this.additionalImports = imports;
    }
  }

  public Set<String> getRequiredSourceStarImports() {
    if (!hasSomeUnresolvedTypes()) {
      return additionalImports != null
          ? additionalImports
          : ImmutableSet.<String>of();
    }

    Set<String> starImports = Sets.newLinkedHashSet();

    if (additionalImports != null) {
      starImports.addAll(additionalImports);
    }

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
        if (a.isNullable() || a.isJdkOptional()) {
          return true;
        }
      }
    }
    for (ValueAttribute a : attributes) {
      if (a.isNullable() || a.isJdkOptional()) {
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
      if (!c.typeKind().isRegular() || !(c.isPrimitive() || c.isNullable())) {
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
        && allAttributesSupportsThis()
        && !isOrdinalValue()
        && getDefaultAttributes().isEmpty();
  }

  private boolean allAttributesSupportsThis() {
    for (ValueAttribute a : implementedAttributes) {
      if (!a.supportsInternalImplConstructor()) {
        return false;
      }
    }
    return true;
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
  protected TypeHierarchyCollector collectTypeHierarchy(final TypeMirror typeMirror) {
    this.hierarchiCollector = createTypeHierarchyCollector(report(), element);
    this.hierarchiCollector.collectFrom(typeMirror);
    return hierarchiCollector;
  }

  TypeHierarchyCollector createTypeHierarchyCollector(final Reporter reporter, final Element element) {
    return new TypeHierarchyCollector() {
      @Override
      protected String stringify(DeclaredType input, TypevarContext context) {
        TypeStringProvider provider = new TypeStringProvider(
            reporter,
            element,
            input,
            newTypeStringResolver(),
            context.parameters.toArray(new String[0]),
            context.arguments.toArray(new String[0]));
        provider.collectUnresolvedYetArgumentsTo(this.unresolvedYetArguments);
        provider.process();
        return provider.returnTypeName();
      }
    };
  }

  ImmutableList<TypeElement> extendedClasses() {
    ensureTypeIntrospected();
    return hierarchiCollector.extendedClasses();
  }

  ImmutableSet<TypeElement> implementedInterfaces() {
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
    for (ValueAttribute a : getAllAccessibleAttributes()) {
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
          report(),
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

  private boolean hasCreatorDefined;

  void detectParcelableCreator() {
    for (VariableElement v : ElementFilter.fieldsIn(element.getEnclosedElements())) {
      if (v.getSimpleName().contentEquals(Proto.PARCELABLE_CREATOR_FIELD)) {
        hasCreatorDefined = true;
        break;
      }
    }
  }

  public boolean isGenerateParcelable() {
    return isParcelable() && !hasCreatorDefined;
  }

  public Set<String> getImmutableCopyOfRoutines() {
    Set<String> routines = new LinkedHashSet<>();
    routines.addAll(constitution.style().immutableCopyOfRoutinesNames());
    for (ValueType v : nested) {
      routines.addAll(v.constitution.style().immutableCopyOfRoutinesNames());
    }
    return routines;
  }

  private SuppressedWarnings suppressedWarnings;

  public Set<String> generatedSuppressWarnings() {
    return getSuppressedWarnings().generatedSuppressions;
  }

  private SuppressedWarnings getSuppressedWarnings() {
    if (suppressedWarnings == null) {
      suppressedWarnings =
          SuppressedWarnings.forElement(
              element,
              constitution.style().generateSuppressAllWarnings(),
              hasDeprecatedAttributes());
    }
    return suppressedWarnings;
  }

  public boolean isGenerateSuppressAllWarnings() {
    return getSuppressedWarnings().generated;
  }

  public boolean isUseCompactBuilder() {
    return !kind().isFactory()
        && !isUseStrictBuilder()
        && !isGenerateBuildOrThrow()
        && getThrowForInvalidImmutableState().equals(IllegalStateException.class.getName());
  }

  public boolean isDeprecated() {
    return constitution
        .protoclass()
        .processing()
        .getElementUtils()
        .isDeprecated(CachingElements.getDelegate(element));
  }

  public ImmutableList<String> extractDocComment(Element element) {
    // Only extract for generated type which is public
    if (constitution.implementationVisibility().isPublic()) {
      @Nullable String docComment = constitution
          .protoclass()
          .processing()
          .getElementUtils()
          .getDocComment(CachingElements.getDelegate(element));

      if (docComment != null) {
        return ImmutableList.copyOf(DOC_COMMENT_LINE_SPLITTER.split(docComment));
      }
    }
    return ImmutableList.of();
  }

  private ImmutableList<String> docComment;

  public ImmutableList<String> getDocComment() {
    if (docComment == null) {
      this.docComment = constitution.isImplementationPrimary()
          || constitution.style().getStyles().isImmutableIdentityNaming()
              ? extractDocComment(element)
              : ImmutableList.<String>of();
    }
    return docComment;
  }

  DeclaringType inferDeclaringType(Element element) {
    return constitution.protoclass().environment().round().inferDeclaringTypeFor(element);
  }

  public Set<String> getNonAttributeAbstractMethodSignatures() {
    if (element.getKind().isClass() || element.getKind().isInterface()) {
      Set<String> signatures = new LinkedHashSet<>();

      List<? extends Element> members = constitution.protoclass()
          .environment()
          .processing()
          .getElementUtils()
          .getAllMembers(CachingElements.getDelegate((TypeElement) element));

      for (ExecutableElement m : ElementFilter.methodsIn(members)) {
        if (!m.getParameters().isEmpty()
            || m.getSimpleName().contentEquals(AccessorAttributesCollector.HASH_CODE_METHOD)
            || m.getSimpleName().contentEquals(AccessorAttributesCollector.TO_STRING_METHOD)) {

          if (m.getModifiers().contains(Modifier.ABSTRACT)) {
            TypeMirror returnType = m.getReturnType();
            if (!AccessorAttributesCollector.isEclipseImplementation(m)) {
              returnType = AccessorAttributesCollector.asInheritedMemberReturnType(
                  constitution.protoclass().processing(),
                  CachingElements.getDelegate((TypeElement) element),
                  m);
            }
            signatures.add(toSignature(m, returnType));
          }
        }
      }

      return signatures;
    }
    return Collections.emptySet();
  }

  public FuncData getFunctionalData() {
    return new FuncData();
  }

  public class FuncData {
    public final List<ValueAttribute> functionalAttributes = new ArrayList<>();
    public final List<BoundElement> boundElements = new ArrayList<>();

    FuncData() {
      List<ValueAttribute> allAccessibleAttributes = getAllAccessibleAttributes();

      if (constitution.protoclass().declaringType().isPresent()) {
        if (FunctionalMirror.isPresent(constitution.protoclass().declaringType().get().element())) {
          functionalAttributes.addAll(allAccessibleAttributes);
        }
      }
      if (functionalAttributes.isEmpty() && FunctionalMirror.isPresent(element)) {
        functionalAttributes.addAll(allAccessibleAttributes);
      }

      if (functionalAttributes.isEmpty()) {
        for (ValueAttribute a : getAllAccessibleAttributes()) {
          if (FunctionalMirror.isPresent(a.element)) {
            functionalAttributes.add(a);
          }
        }
      }

      if (element.getKind().isClass() || element.getKind().isInterface()) {
        List<ExecutableElement> methods =
            ElementFilter.methodsIn(
                constitution.protoclass()
                    .environment()
                    .processing()
                    .getElementUtils()
                    .getAllMembers(CachingElements.getDelegate((TypeElement) element)));

        for (ExecutableElement m : methods) {
          if (BindParamsMirror.isPresent(m)
              && !m.getModifiers().contains(Modifier.STATIC)
              && !m.getModifiers().contains(Modifier.PRIVATE)
              && !m.getParameters().isEmpty()) {
            this.boundElements.add(new FuncData.BoundElement(m));
          }
        }
      }
    }

    public boolean is() {
      return !functionalAttributes.isEmpty() || !boundElements.isEmpty();
    }

    public class BoundElement {
      public final CharSequence access;
      public final CharSequence name;
      public final CharSequence type;
      public final CharSequence parameters;
      public final CharSequence arguments;

      BoundElement(ExecutableElement method) {
        DeclaringType declaringType = inferDeclaringType(method);
        this.access = appendAccessModifier(method, new StringBuilder());
        this.name = method.getSimpleName();
        this.type = method.getReturnType().getKind().isPrimitive()
            ? wrapType(method.getReturnType().toString())
            : appendReturnType(method, new StringBuilder(), declaringType, method.getReturnType());
        this.parameters = appendParameters(method, new StringBuilder(), declaringType, true, true);
        this.arguments = appendParameters(method, new StringBuilder(), declaringType, false, false);
      }
    }
  }

  public List<ValueAttribute> getBuilderParameters() {
    if (!constitution.protoclass().environment().hasBuilderModule()) {
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

  private String toSignature(ExecutableElement m, TypeMirror returnType) {
    DeclaringType declaringType = inferDeclaringType(m);
    StringBuilder signature = new StringBuilder();
    appendAccessModifier(m, signature);
    appendReturnType(m, signature, declaringType, returnType);
    signature.append(" ").append(m.getSimpleName());
    appendParameters(m, signature, declaringType, true, false);
    return signature.toString();
  }

  private CharSequence appendAccessModifier(ExecutableElement m, StringBuilder signature) {
    if (m.getModifiers().contains(Modifier.PUBLIC)) {
      return signature.append("public ");
    }
    if (m.getModifiers().contains(Modifier.PROTECTED)) {
      return signature.append("protected ");
    }
    return signature;
  }

  private CharSequence appendReturnType(
      ExecutableElement m,
      StringBuilder signature,
      DeclaringType declaringType,
      TypeMirror returnType) {
    return signature.append(printType(m, returnType, declaringType));
  }

  private CharSequence appendParameters(
      ExecutableElement m,
      StringBuilder signature,
      DeclaringType declaringType,
      boolean withTypes,
      boolean allFinal) {
    signature.append("(");

    boolean notFirst = false;
    for (VariableElement p : m.getParameters()) {
      if (notFirst) {
        signature.append(", ");
      }
      if (allFinal) {
        signature.append("final ");
      }
      if (withTypes) {
        signature.append(printType(p, p.asType(), declaringType));
        signature.append(" ");
      }
      signature.append(p.getSimpleName());
      notFirst = true;
    }

    return signature.append(")");
  }

  private String printType(Element element, TypeMirror type, DeclaringType declaringType) {
    TypeStringProvider provider =
        new TypeStringProvider(
            report(),
            element,
            type,
            new ImportsTypeStringResolver(constitution.protoclass().declaringType().orNull(), declaringType),
            constitution.generics().vars(),
            null);
    provider.process();
    return provider.returnTypeName();
  }

  private @Nullable GsonTypeTokens gsonTypeTokens;

  public GsonTypeTokens getGsonTypeTokens() {
    if (gsonTypeTokens == null) {
      this.gsonTypeTokens = new GsonTypeTokens(
          generics(),
          getTypeExtractor());
    }
    return gsonTypeTokens;
  }

  private @Nullable TypeExtractor typeExtractor;

  private TypeExtractor getTypeExtractor() {
    if (typeExtractor == null) {
      this.typeExtractor = new TypeExtractor(
          Proto.TYPE_FACTORY,
          (Parameterizable) element);
    }
    return typeExtractor;
  }

  public Reporter report() {
    return constitution.protoclass().report();
  }

  public List<String> getDebugLines() {
    return constitution.protoclass().getDebugLines();
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

  private static final Splitter DOC_COMMENT_LINE_SPLITTER = Splitter.on('\n').omitEmptyStrings();
}
