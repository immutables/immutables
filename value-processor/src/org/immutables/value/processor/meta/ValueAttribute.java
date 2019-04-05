/*
   Copyright 2013-2018 Immutables Authors and Contributors

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

import com.google.common.base.Ascii;
import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.lang.annotation.ElementType;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import org.immutables.generator.AnnotationMirrors;
import org.immutables.generator.StringLiterals;
import org.immutables.generator.TypeHierarchyCollector;
import org.immutables.value.processor.encode.Instantiation;
import org.immutables.value.processor.encode.Instantiator.InstantiationCreator;
import org.immutables.value.processor.meta.AnnotationInjections.AnnotationInjection;
import org.immutables.value.processor.meta.AnnotationInjections.InjectAnnotation.Where;
import org.immutables.value.processor.meta.AnnotationInjections.InjectionInfo;
import org.immutables.value.processor.meta.Generics.Parameter;
import org.immutables.value.processor.meta.Proto.DeclaringType;
import org.immutables.value.processor.meta.Proto.Environment;
import org.immutables.value.processor.meta.Proto.MetaAnnotated;
import org.immutables.value.processor.meta.Proto.Protoclass;
import org.immutables.value.processor.meta.Reporter.About;
import org.immutables.value.processor.meta.Styles.UsingName.AttributeNames;
import org.immutables.value.processor.meta.ValueMirrors.Style.ImplementationVisibility;
import org.immutables.value.processor.meta.ValueMirrors.Style.ValidationMethod;

/**
 * It's pointless to refactor this mess until
 * 1) Some sort of type calculus toolkit used/created
 * 2) Facets/Implicits in Generator toolkit with auto-memoising implemented
 */
public final class ValueAttribute extends TypeIntrospectionBase implements HasStyleInfo {
  private static final WholeTypeVariable NON_WHOLE_TYPE_VARIABLE = new WholeTypeVariable(-1);
  private static final int CONSTRUCTOR_PARAMETER_DEFAULT_ORDER = 0;
  private static final int CONSTRUCTOR_NOT_A_PARAMETER = -1;
  private static final String GUAVA_IMMUTABLE_PREFIX = UnshadeGuava.typeString("collect.Immutable");
  private static final String VALUE_ATTRIBUTE_NAME = "value";
  private static final String ID_ATTRIBUTE_NAME = "_id";
  private static final String[] EMPTY_SERIALIZED_NAMES = {};

  public AttributeNames names;
  public boolean isGenerateDefault;
  public boolean isGenerateDerived;
  public boolean isGenerateAbstract;
  public boolean isGenerateLazy;
  public boolean isAttributeBuilder;
  public ImmutableList<String> typeParameters = ImmutableList.of();
  public ImmutableList<AnnotationInjection> annotationInjections = ImmutableList.of();
  // Replace with delegation?
  public Reporter reporter;

  public ValueType containingType;

  @Nullable
  public ValueType attributeValueType;

  TypeMirror returnType;
  Element element;
  String returnTypeName;
  // Set only if isAttributeBuilder is true
  @Nullable
  private AttributeBuilderDescriptor attributeBuilderDescriptor;

  public boolean hasEnumFirstTypeParameter;

  @Nullable
  TypeElement containedTypeElement;

  @Nullable
  private TypeElement containedSecondaryTypeElement;

  private boolean generateOrdinalValueSet;
  private TypeMirror arrayComponent;

  @Nullable
  private NullabilityAnnotationInfo nullability;

  @Nullable
  private NullabilityAnnotationInfo nullabilityInSupertype;

  @Nullable
  private String rawTypeName;

  public String name() {
    return names.var;
  }

  public boolean isBoolean() {
    return returnType.getKind() == TypeKind.BOOLEAN;
  }

  public boolean isInt() {
    return returnType.getKind() == TypeKind.INT;
  }

  public boolean isShort() {
    return returnType.getKind() == TypeKind.SHORT;
  }

  public boolean isChar() {
    return returnType.getKind() == TypeKind.CHAR;
  }

  public boolean isByte() {
    return returnType.getKind() == TypeKind.BYTE;
  }

  public boolean isLong() {
    return returnType.getKind() == TypeKind.LONG;
  }

  public boolean isStringType() {
    return String.class.getName().equals(rawTypeName);
  }

  public boolean charType() {
    return returnType.getKind() == TypeKind.CHAR;
  }

  public String atNullability() {
    return nullability != null ? nullability.asPrefix() : "";
  }

  public String atNullabilityOriginal() {
    return nullability != null ? nullability.asPrefixOriginal() : "";
  }

  public String atNullabilityLocal() {
    return nullability != null ? nullability.asLocalPrefix() : "";
  }

  public boolean isSimpleLiteralType() {
    return isPrimitive()
        || isStringType()
        || isEnumType();
  }

  public boolean hasSimpleScalarElementType() {
    ensureTypeIntrospected();

    String type = getWrappedElementType();
    return isStringType()
        || String.class.getName().equals(type)
        || isPrimitiveWrappedType(type)
        || hasEnumContainedElementType()
        || isEnumType()
        || isJdkSpecializedOptional()
        || extendedClassesNames.contains(Number.class.getName());
  }

  public boolean requiresAlternativeStrictConstructor() {
    return typeKind.isCollectionKind()
        || (typeKind.isMappingKind()
            && !typeKind.isPlainMapKind()
            && !typeKind.isMultimap());
  }

  public boolean isMandatory() {
    return isGenerateAbstract
        && !isGenerateDefault // is the case for defaulted abstract annotation attribute
        && !isContainerType()
        && !isNullable()
        && !isEncoding()
        && !hasBuilderSwitcherDefault()
        && !(isPrimitive() && protoclass().styles().style().validationMethod() != ValidationMethod.SIMPLE);
  }

  /**
   * Checks if type defined by this attribute has associated criteria (see {@code @Criteria})
   */
  public boolean hasCriteria() {
    return attributeValueType != null && attributeValueType.isGenerateCriteria();
  }

  public boolean isNullable() {
    return nullability != null;
  }

  public ImmutableList<String> docComment = ImmutableList.of();

  public boolean deprecated;

  public boolean isMaybeComparableKey() {
    return isContainerType()
        && (super.isComparable() || this.containedTypeElement == null);
  }

  @Override
  public boolean isComparable() {
    return isNumberType()
        || isStringType()
        || (!(isCollectionType() || isMapType()) && super.isComparable());
  }

  private List<CharSequence> jsonQualifierAnnotations;

  public List<CharSequence> getJsonQualiferAnnotations() {
    if (jsonQualifierAnnotations == null) {
      List<CharSequence> annotationStrings = Collections.emptyList();
      for (AnnotationMirror m : element.getAnnotationMirrors()) {
        if (MetaAnnotated.from(m, protoclass().environment()).isJsonQualifier()) {
          if (annotationStrings.isEmpty()) {
            annotationStrings = Lists.newArrayList();
          }
          annotationStrings.add(AnnotationMirrors.toCharSequence(m));
        }
      }
      jsonQualifierAnnotations = annotationStrings;
    }
    return jsonQualifierAnnotations;
  }

  @Nullable
  private String serializedName;

  private String[] alternateSerializedNames = EMPTY_SERIALIZED_NAMES;

  public String[] getAlternateSerializedNames() {
    getSerializedName();// trigger lazy init
    return alternateSerializedNames;
  }

  /**
   * Serialized name, actully specified via annotation
   * @return name for JSON as overriden.
   */
  public String getSerializedName() {
    if (serializedName == null) {
      Optional<SerializedNameMirror> serializedNameAnnotation = SerializedNameMirror.find(element);
      if (serializedNameAnnotation.isPresent()) {
        SerializedNameMirror m = serializedNameAnnotation.get();
        serializedName = m.value();
        alternateSerializedNames = m.alternate();
        return serializedName;
      }
      Optional<NamedMirror> namedAnnotation = NamedMirror.find(element);
      if (namedAnnotation.isPresent()) {
        String value = namedAnnotation.get().value();
        if (!value.isEmpty()) {
          serializedName = value;
          return serializedName;
        }
      }
      Optional<OkNamedMirror> okNamedAnnotation = OkNamedMirror.find(element);
      if (okNamedAnnotation.isPresent()) {
        String value = okNamedAnnotation.get().name();
        if (!value.isEmpty()) {
          serializedName = value;
          return serializedName;
        }
      }
      if (isMarkedAsMongoId()) {
        serializedName = ID_ATTRIBUTE_NAME;
        return serializedName;
      }
      serializedName = "";
      return serializedName;
    }
    return serializedName;
  }

  /**
   * Marshaled name for compatibility with repository.
   * @return get JSON name either specified or default.
   */
  public String getMarshaledName() {
    String serializedName = getSerializedName();
    if (!serializedName.isEmpty()) {
      return serializedName;
    }
    return names.raw;
  }

  private @Nullable Boolean isGsonOther = null;

  public boolean isGsonOther() {
    if (isGsonOther == null) {
      if (GsonOtherMirror.isPresent(element)) {
        if (!isGenerateAbstract || !rawTypeName.equals(GsonMirrors.JSON_OBJECT_TYPE)) {
          report().error(
              "@Gson.Other attribute must be abstract accessor of type %s",
              GsonMirrors.JSON_OBJECT_TYPE);
          isGsonOther = false;
        } else {
          isGsonOther = true;
        }
      } else {
        isGsonOther = false;
      }
    }
    return isGsonOther;
  }

  public boolean isForcedEmpty() {
    return !containingType.gsonTypeAdapters().emptyAsNulls();
  }

  @Override
  protected TypeMirror internalTypeMirror() {
    return returnType;
  }

  public String getType() {
    return returnTypeName;
  }

  public List<CharSequence> getAnnotations() {
    if (containingType.isGenerateJacksonProperties()) {
      return extractAnnotationsForElement(
          ElementType.METHOD,
          protoclass().styles().style().passAnnotationsNames());

    }
    return Annotations.getAnnotationLines(element,
        protoclass().styles().style().passAnnotationsNames(),
        false,
        ElementType.METHOD,
        importsResolver,
        nullability);
  }

  public List<CharSequence> getFieldAnnotations() {
    return Annotations.getAnnotationLines(element,
        protoclass().styles().style().passAnnotationsNames(),
        false,
        ElementType.FIELD,
        importsResolver,
        null/* do not propagate nullable here */);
  }

  public CharSequence getConstructorParameterAnnotations() {
    List<CharSequence> annotations =
        Annotations.getAnnotationLines(element,
            protoclass().styles().style().passAnnotationsNames(),
            false,
            ElementType.PARAMETER,
            importsResolver,
            nullability);

    if (!annotations.isEmpty()) {
      return Joiner.on(' ').join(annotations).concat(" ");
    }

    return "";
  }

  public List<CharSequence> getJacksonFieldsAnnotations() {
    return extractAnnotationsForElement(ElementType.FIELD, Collections.<String>emptySet());
  }

  private CharSequence jacksonPropertyAnnotation() {
    StringBuilder propertyAnnotation = new StringBuilder("@").append(Annotations.JACKSON_PROPERTY);
    if (protoclass().styles().style().forceJacksonPropertyNames()) {
      propertyAnnotation.append('(').append(StringLiterals.toLiteral(names.raw)).append(')');
    }
    return propertyAnnotation;
  }

  public List<CharSequence> getBuilderAttributeAnnotation() {
    if (containingType.isGenerateJacksonProperties()
        && protoclass().isJacksonDeserialized()) {
      List<CharSequence> jacksonPropertyAnnotation = Annotations.getAnnotationLines(element,
          Collections.singleton(Annotations.JACKSON_PROPERTY),
          false,
          ElementType.METHOD,
          importsResolver,
          nullability);
      List<CharSequence> annotations = Lists.newArrayList();
      if (jacksonPropertyAnnotation.isEmpty()) {
        annotations.add(jacksonPropertyAnnotation());
      }
      annotations.addAll(Annotations.getAnnotationLines(element,
          Collections.<String>emptySet(),
          protoclass().environment().hasJacksonLib(),
          ElementType.METHOD,
          importsResolver,
          nullability));
      return annotations;
    }
    return ImmutableList.of();
  }

  private List<CharSequence> extractAnnotationsForElement(ElementType elementType, Set<String> additionalAnnotations) {
    List<CharSequence> allAnnotations = Lists.newArrayListWithCapacity(1);

    boolean dontHaveJsonPropetyAnnotationAlready = Annotations.getAnnotationLines(element,
        Collections.singleton(Annotations.JACKSON_PROPERTY),
        false,
        elementType,
        importsResolver,
        nullability).isEmpty();

    if (dontHaveJsonPropetyAnnotationAlready) {
      allAnnotations.add(jacksonPropertyAnnotation());
    }

    allAnnotations.addAll(
        Annotations.getAnnotationLines(element,
            Sets.union(additionalAnnotations,
                protoclass().styles().style().additionalJsonAnnotationsNames()),
            protoclass().environment().hasJacksonLib(),
            elementType,
            importsResolver,
            nullability));

    return allAnnotations;
  }

  public boolean isJsonIgnore() {
    return IgnoreMirror.isPresent(element)
        || OkIgnoreMirror.isPresent(element);
  }

  public List<String> typeParameters() {
    ensureTypeIntrospected();
    return arrayComponent != null ? ImmutableList.of(arrayComponent.toString()) : typeParameters;
  }

  public boolean isMapType() {
    return typeKind.isMappingKind();
  }

  public boolean isMultimapType() {
    return typeKind.isMultimapKind();
  }

  public boolean isListType() {
    return typeKind.isList();
  }

  private OrderKind orderKind = OrderKind.NONE;

  private enum OrderKind {
    NONE, NATURAL, REVERSE
  }

  public boolean isSetType() {
    return typeKind.isSet();
  }

  public boolean hasNaturalOrder() {
    return orderKind == OrderKind.NATURAL;
  }

  public boolean hasReverseOrder() {
    return orderKind == OrderKind.REVERSE;
  }

  public boolean isSortedSetType() {
    return typeKind.isSortedSet();
  }

  public boolean isSortedMapType() {
    return typeKind.isSortedMap();
  }

  public boolean isGenerateSortedSet() {
    return typeKind.isSortedSet();
  }

  public boolean isGenerateSortedMap() {
    return typeKind.isSortedMap();
  }

  public boolean isGenerateSortedMultiset() {
    return typeKind.isSortedMultiset();
  }

  private void checkOrderAnnotations() {
    Optional<NaturalOrderMirror> naturalOrderAnnotation = NaturalOrderMirror.find(element);
    Optional<ReverseOrderMirror> reverseOrderAnnotation = ReverseOrderMirror.find(element);

    if (naturalOrderAnnotation.isPresent() && reverseOrderAnnotation.isPresent()) {
      report()
          .error("@Value.Natural and @Value.Reverse annotations cannot be used on the same attribute");
    } else if (naturalOrderAnnotation.isPresent()) {
      configureOrdering(OrderKind.NATURAL, NaturalOrderMirror.simpleName());
    } else if (reverseOrderAnnotation.isPresent()) {
      configureOrdering(OrderKind.REVERSE, ReverseOrderMirror.simpleName());
    }
  }

  private void configureOrdering(OrderKind orderKind, String annotationName) {
    if (typeKind.isSortedKind()) {
      if (isMaybeComparableKey()) {
        this.orderKind = orderKind;
      } else {
        reportOrderingError(annotationName, "requires that a (multi)set's elements or a map's keys are Comparable");
      }
    } else {
      reportOrderingError(annotationName, "can be applied only to SortedSet, SortedMap and SortedMultiset attributes");
    }
  }

  private void reportOrderingError(String annotationName, String msg) {
    report().annotationNamed(annotationName).error(String.format("@Value.%s %s", annotationName, msg));
  }

  public boolean isJdkOptional() {
    return typeKind.isOptionalKind() && typeKind.isJdkOnlyContainerKind();
  }

  public boolean isFugueOptional() {
    return typeKind.isOptionFugue();
  }

  public boolean isGuavaOptional() {
    return typeKind.isOptionalGuava();
  }

  public boolean isJavaslangOptional() {
    return typeKind.isOptionJavaslang();
  }

  public boolean isJdkSpecializedOptional() {
    return typeKind.isOptionalSpecializedJdk();
  }

  public boolean isOptionalType() {
    return typeKind.isOptionalKind();
  }

  public boolean isCollectionType() {
    return typeKind.isCollectionKind();
  }

  public boolean isGenerateEnumSet() {
    return typeKind.isEnumSet();
  }

  public boolean isGuavaImmutableDeclared() {
    return typeKind.isContainerKind() && rawTypeName.startsWith(GUAVA_IMMUTABLE_PREFIX);
  }

  @Nullable
  private String defaultInterface;

  public String defaultInterface() {
    if (defaultInterface == null) {
      defaultInterface = inferDefaultInterface();
    }
    return defaultInterface;
  }

  private String inferDefaultInterface() {
    if (isInterfaceDefaultMethod()) {
      if (containingType.element.getKind() == ElementKind.INTERFACE) {
        return containingType.typeAbstract().relativeRaw();
      }
    }
    return "";
  }

  public boolean isInterfaceDefaultMethod() {
    return element.getEnclosingElement().getKind() == ElementKind.INTERFACE
        && !element.getModifiers().contains(Modifier.ABSTRACT);
  }

  public boolean isGenerateTransientDerived() {
    return isGenerateDerived
        && style().transientDerivedFields()
        && !containingType.serial.isSimple();
  }

  public boolean isGenerateEnumMap() {
    return typeKind.isEnumMap();
  }

  public boolean hasVirtualImpl() {
    return isEncoding() && instantiation.hasVirtualImpl();
  }

  public String getUnwrappedElementType() {
    return isContainerType() && nullElements.ban()
        ? unwrapType(firstTypeParameter())
        : getElementType();
  }

  public String getUnwrappedValueElementType() {
    return isMapType()
        ? getUnwrappedSecondaryElementType()
        : getUnwrappedElementType();
  }

  public String getWrappedElementType() {
    if (returnType.getKind().isPrimitive()) {
      return wrapType(Ascii.toLowerCase(returnType.getKind().name()));
    }
    return wrapType(hasContainedElementType()
        ? firstTypeParameter()
        : returnTypeName);
  }

  private boolean hasContainedElementType() {
    return isArrayType() || isContainerType();
  }

  public String getRawType() {
    return rawTypeName;
  }

  public String getConsumedElementType() {
    return (isUnwrappedElementPrimitiveType()
        || isStringType()
        || (hasContainedElementType() && firstTypeParameter().equals(String.class.getName()))
        || hasEnumFirstTypeParameter())
            ? getWrappedElementType()
            : "? extends " + getWrappedElementType();
  }

  public boolean hasEnumFirstTypeParameter() {
    return typeKind().isEnumKeyed()
        && containedTypeElement.getKind() == ElementKind.ENUM;
  }

  private String extractRawType(String className) {
    String rawType = className;
    int indexOfGenerics = rawType.indexOf('<');
    if (indexOfGenerics > 0) {
      rawType = rawType.substring(0, indexOfGenerics);
    }
    int endOfTypeAnnotations = rawType.lastIndexOf(' ');
    if (endOfTypeAnnotations > 0) {
      rawType = rawType.substring(endOfTypeAnnotations + 1);
    }
    return rawType;
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
    return hasContainedElementType() ? firstTypeParameter() : returnTypeName;
  }

  @Nullable
  private List<String> expectedSubtypes;

  public List<String> getExpectedSubtypes() {
    if (expectedSubtypes == null) {
      ensureTypeIntrospected();
      if (containedTypeElement != null || containedSecondaryTypeElement != null) {
        TypeElement supertypeElement = MoreObjects.firstNonNull(containedSecondaryTypeElement, containedTypeElement);
        Optional<ExpectedSubtypesMirror> annotationOnAttribute = ExpectedSubtypesMirror.find(element);
        if (annotationOnAttribute.isPresent()) {
          expectedSubtypes = ImmutableList.copyOf(annotationOnAttribute.get().valueName());
          if (expectedSubtypes.isEmpty()) {
            expectedSubtypes = tryFindSubtypes(supertypeElement);
          }
        } else {
          Optional<ExpectedSubtypesMirror> annotationOnType = ExpectedSubtypesMirror.find(supertypeElement);
          if (annotationOnType.isPresent()) {
            expectedSubtypes = ImmutableList.copyOf(annotationOnType.get().valueName());
            if (expectedSubtypes.isEmpty()) {
              expectedSubtypes = tryFindSubtypes(supertypeElement);
            }
          }
        }
      }
      if (expectedSubtypes == null) {
        expectedSubtypes = ImmutableList.of();
      }
    }
    return expectedSubtypes;
  }

  private ImmutableList<String> tryFindSubtypes(TypeElement supertypeElement) {
    ValueType surroundingType =
        MoreObjects.firstNonNull(containingType.enclosingValue, containingType);
    Set<ValueType> subtypes =
        surroundingType.getCases().knownSubtypesOf(supertypeElement.getQualifiedName().toString());
    ImmutableList.Builder<String> builder = ImmutableList.builder();
    for (ValueType valueType : subtypes) {
      builder.add(valueType.typeAbstract().toString());
    }
    return builder.build();
  }

  public boolean isGenerateJdkOnly() {
    return containingType.isGenerateJdkOnly()
        && !typeKind.isGuavaContainerKind()
        && !isGuavaImmutableDeclared()
        && !isCustomCollectionType();
  }

  public boolean isGenerateOrdinalValueSet() {
    if (!isSetType()) {
      return false;
    }
    ensureTypeIntrospected();
    return generateOrdinalValueSet;
  }

  public boolean isArrayType() {
    return typeKind.isArray();
  }

  public boolean isReferenceArrayType() {
    return isArrayType() && !isPrimitiveType(firstTypeParameter());
  }

  public boolean isPrimitiveArrayType() {
    return isArrayType() && isPrimitiveType(firstTypeParameter());
  }

  public boolean isOptionalAcceptNullable() {
    return isOptionalType()
        && !typeKind.isOptionalSpecializedJdk()
        && containingType.isOptionalAcceptNullable();
  }

  @Override
  protected void introspectType() {
    TypeMirror typeMirror = returnType;

    // Special case for primitive Optional, may become a pattern for specialized types
    if (typeKind.isOptionalSpecializedJdk()) {
      typeParameters = ImmutableList.of(optionalSpecializedType());
      // no delegation to introspect further
      return;
    }

    if (isContainerType()) {
      if (typeMirror.getKind() == TypeKind.DECLARED
          || typeMirror.getKind() == TypeKind.ERROR) {

        DeclaredType declaredType = (DeclaredType) typeMirror;

        List<? extends TypeMirror> typeArguments = declaredType.getTypeArguments();

        if (!typeArguments.isEmpty()) {
          final TypeMirror typeArgument = typeArguments.get(0);

          if (isSetType() && protoclass().environment().hasOrdinalModule()) {
            this.generateOrdinalValueSet = new TypeIntrospectionBase() {
              @Override
              protected TypeMirror internalTypeMirror() {
                return typeArgument;
              }

              @Override
              protected TypeHierarchyCollector collectTypeHierarchy(TypeMirror typeMirror) {
                TypeHierarchyCollector collector = containingType.createTypeHierarchyCollector(reporter, element);
                collector.collectFrom(typeMirror);
                return collector;
              }
            }.isOrdinalValue();
          }
          if (isMapType()) {
            TypeMirror typeSecondArgument = typeArguments.get(1);
            if (typeSecondArgument.getKind() == TypeKind.DECLARED) {
              TypeElement typeElement = (TypeElement) ((DeclaredType) typeSecondArgument).asElement();
              this.containedSecondaryTypeElement = typeElement;
            }
          }

          typeMirror = typeArgument;
        }
      }
    } else if (isArrayType()) {
      arrayComponent = ((ArrayType) typeMirror).getComponentType();
      typeMirror = arrayComponent;
    }

    if (typeMirror.getKind() == TypeKind.DECLARED) {
      TypeElement typeElement = (TypeElement) ((DeclaredType) typeMirror).asElement();
      this.containedTypeElement = typeElement;
    }

    introspectTypeMirror(typeMirror);
    introspectSupertypes();
  }

  private String optionalSpecializedType() {
    switch (typeKind) {
    case OPTIONAL_INT_JDK:
      return int.class.getName();
    case OPTIONAL_LONG_JDK:
      return long.class.getName();
    case OPTIONAL_DOUBLE_JDK:
      return double.class.getName();
    default:
      throw new AssertionError();
    }
  }

  public AttributeTypeKind typeKind() {
    return typeKind;
  }

  public boolean isPrimitiveWrapperType() {
    return isPrimitiveWrappedType(returnTypeName);
  }

  private static boolean isRegularMarshalableType(String name) {
    return String.class.getName().equals(name)
        || isPrimitiveType(name);
  }

  public boolean isRequiresMarshalingAdapter() {
    return !isRegularMarshalableType(getElementType())
        || isPrimitiveArrayType();
  }

  public boolean isRequiresMarshalingSecondaryAdapter() {
    return isMapType() && !isRegularMarshalableType(getSecondaryElementType());
  }

  public boolean wrapArrayToIterable() {
    return containingType.isGenerateJdkOnly()
        || isUnwrappedElementPrimitiveType()
        || !(typeKind.isList() || typeKind.isSet() || typeKind.isMultiset());
  }

  /**
   * Suitable for JavaDocs, intemediate name mangling and for Guava intergration.
   * @return the raw collection type
   */
  public String getRawCollectionType() {
    return typeKind.rawSimpleName(rawTypeName);
  }

  public boolean isMultisetType() {
    return typeKind.isMultisetKind();
  }

  public boolean isCustomCollectionType() {
    return typeKind.isCustomCollection();
  }

  public String getRawMapType() {
    return typeKind.rawSimpleName(rawTypeName);
  }

  public String getSecondaryElementType() {
    return secondTypeParameter();
  }

  public String getUnwrappedSecondaryElementType() {
    return nullElements.ban()
        ? unwrapType(secondTypeParameter())
        : getSecondaryElementType();
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
    TypeKind kind = returnType.getKind();
    return kind.isPrimitive()
        && kind != TypeKind.CHAR
        && kind != TypeKind.BOOLEAN;
  }

  public boolean isFloatType() {
    return isFloat() || isDouble();
  }

  public boolean isFloat() {
    return returnType.getKind() == TypeKind.FLOAT;
  }

  public boolean isDouble() {
    return returnType.getKind() == TypeKind.DOUBLE;
  }

  public boolean hasTypeAnnotations() {
    return returnTypeName.indexOf('@') >= 0;
  }

  public boolean isNonRawElementType() {
    return getElementType().indexOf('<') > 0;
  }

  public boolean isNonRawSecondaryElementType() {
    return getSecondaryElementType().indexOf('<') > 0;
  }

  public boolean isContainerType() {
    // TBD replace with typeKind.isContainerKind() ?
    return isCollectionType()
        || isOptionalType()
        || isMapType();
  }

  public String getWrapperType() {
    return isPrimitive()
        ? wrapType(rawTypeName)
        : returnTypeName;
  }

  public boolean isPrimitive() {
    return returnType.getKind().isPrimitive();
  }

  public boolean isAttributeBuilder() {
    return isAttributeBuilder;
  }

  // undefined value is any less than CONSTRUCTOR_NOT_A_PARAMETER
  private int parameterOrder = Integer.MIN_VALUE;

  private AttributeTypeKind typeKind;
  public boolean jacksonAnyGetter;
  public boolean jacksonValue;
  public boolean hasTypeVariables;
  private ImportsTypeStringResolver importsResolver;

  public @Nullable Instantiation instantiation;

  int getConstructorParameterOrder() {
    boolean parameterOrderIsUndefined = parameterOrder < CONSTRUCTOR_NOT_A_PARAMETER;

    if (parameterOrderIsUndefined) {
      parameterOrder = computeConstructorParameterOrder();
    }

    return parameterOrder;
  }

  private int computeConstructorParameterOrder() {
    boolean enabledAsAllParameters =
        style().allParameters();

    boolean enabledAsAllMandatoryParameters =
        style().allMandatoryParameters() && isMandatory();

    Optional<ParameterMirror> parameterAnnotation = ParameterMirror.find(element);
    if (parameterAnnotation.isPresent()) {
      if (!parameterAnnotation.get().value()) {
        // Cancelled out parameter identified, can return immediately
        return CONSTRUCTOR_NOT_A_PARAMETER;
      }

      int order = parameterAnnotation.get().order();
      boolean orderActuallySpecified = order >= CONSTRUCTOR_PARAMETER_DEFAULT_ORDER;
      // when order is unspecified it would be -1, but we should not treat is as
      // CONSTRUCTOR_NOT_A_PARAMETER, so we assign CONSTRUCTOR_PARAMETER_DEFAULT_ORDER
      // in this case
      if (orderActuallySpecified) {
        return order;
      }
      // We issue this warning only when order is not specified explicitly
      // and it is not a cancelling-out annotation thus the annoation
      // is truly superfluos when allParameters enabled
      if (enabledAsAllParameters) {
        report().annotationNamed(ParameterMirror.simpleName())
            .warning(About.INCOMPAT,
                "Annotation @Value.Parameter is superfluous when Style(allParameters = true)");
      }
      if (enabledAsAllMandatoryParameters) {
        report().annotationNamed(ParameterMirror.simpleName())
            .warning(About.INCOMPAT,
                "Annotation @Value.Parameter is superfluous when Style(allMandatoryParameters = true)"
                    + " and it is mandatory");
      }
      return CONSTRUCTOR_PARAMETER_DEFAULT_ORDER;
    }
    if (enabledAsAllParameters || enabledAsAllMandatoryParameters) {
      return CONSTRUCTOR_PARAMETER_DEFAULT_ORDER;
    }
    if (isAnnotationValueAttribute() && thereAreNoOtherMandatoryAttributes()) {
      // for annotation type, if annotation only contain single mandatory
      // attribute which is called 'value' it will be automatically turned
      // into constructor parameter
      return CONSTRUCTOR_PARAMETER_DEFAULT_ORDER;
    }
    return CONSTRUCTOR_NOT_A_PARAMETER;
  }

  private boolean isAnnotationValueAttribute() {
    return containingType.isAnnotationType()
        && names.get.equals(VALUE_ATTRIBUTE_NAME);
  }

  boolean thereAreNoOtherMandatoryAttributes() {
    List<ValueAttribute> mandatories = containingType.getMandatoryAttributes();
    for (ValueAttribute m : mandatories) {
      if (m != this) {
        return false;
      }
    }
    return true;
  }

  public String toSignature() {
    StringBuilder signature = new StringBuilder();

    signature.append(getAccess());

    return signature.append(returnTypeName)
        .append(" ")
        .append(names.get)
        .append("()")
        .toString();
  }

  public String getAccess() {
    if (element.getModifiers().contains(Modifier.PUBLIC)
        || style().visibility() == ImplementationVisibility.PUBLIC) {
      return "public ";
    } else if (element.getModifiers().contains(Modifier.PROTECTED)) {
      return "protected ";
    }
    return "";
  }

  public String getIntializerAccess() {
    if (style().stagedBuilder()
        || style().alwaysPublicInitializers()
        || element.getModifiers().contains(Modifier.PUBLIC)) {
      return "public ";
    }
    if (element.getModifiers().contains(Modifier.PROTECTED)) {
      return "protected ";
    }
    return "";
  }

  public boolean isPrimitiveElement() {
    return isPrimitiveType(getUnwrappedElementType());
  }

  public boolean isSafeUncheckedCovariantCast() {
    return isOptionalType()
        && !getConsumedElementType().equals(getWrappedElementType());
  }

  public boolean isAuxiliary() {
    return AuxiliaryMirror.isPresent(element);
  }

  public boolean isEncoding() {
    return typeKind == AttributeTypeKind.ENCODING;
  }

  private boolean isMarkedAsMongoId() {
    return IdMirror.isPresent(element);
  }

  boolean isIdAttribute() {
    return isMarkedAsMongoId()
        || ID_ATTRIBUTE_NAME.equals(getSerializedName());
  }

  private boolean isRedacted() {
    return RedactedMirror.isPresent(element);
  }

  public boolean isRedactedCompletely() {
    return isRedacted()
        && getRedactedMask().isEmpty();
  }

  public String getRedactedMask() {
    return isRedacted()
        ? protoclass().styles().style().redactedMask()
        : "";
  }

  /**
   * Initialized Validates things that were not validated otherwise
   * @param instantiationCreator can instantiate encodings
   */
  void initAndValidate(@Nullable InstantiationCreator instantiationCreator) {
    initTypeName();

    if (instantiationCreator != null
        && !isGenerateLazy) {
      this.instantiation = instantiationCreator.tryInstantiateFor(
          reporter,
          returnTypeName,
          names,
          containingType,
          isGenerateDefault || isGenerateDerived);
    }

    initTypeKind();
    initOrderKind();
    initBuilderParamsIfApplicable();
    initMiscellaneous();

    initSpecialAnnotations();
    validateThrowsClause();
    validateTypeAndAnnotations();

    initAttributeValueType();
    if (supportBuiltinContainerTypes()) {
      initImmutableCopyOf();
    }

    initAttributeBuilder();
  }

  private Set<String> thrownCheckedExceptions = ImmutableSet.of();

  private void validateThrowsClause() {
    if (element.getKind() != ElementKind.METHOD) {
      return;
    }
    ImmutableSet<String> checkedExceptions = collectThrownCheckedExceptions();
    if (!checkedExceptions.isEmpty()) {
      if (isGenerateLazy) {
        this.thrownCheckedExceptions = checkedExceptions;
      } else {
        report().warning(About.INCOMPAT,
            "Checked exceptions in 'throws' clause are not supported for regular abstract,"
                + " @Value.Derived, and @Value.Default attributes due to implementation difficulties"
                + " and unclear semantics. This message reported as a warning to preserve compatibility,"
                + " but there are high chances you'll see compile error in generated classed caused by this."
                + " NOTE: for @Value.Lazy attributes, checked exceptions are supported by simple propagation:"
                + " neither runtime nor checked exceptions will not be memoised for lazy attributes,"
                + " leading to lazy computation to be re-evaluated again and again until regular value"
                + " returned and memoised.");
      }
    }
  }

  private ImmutableSet<String> collectThrownCheckedExceptions() {
    Set<String> exceptions = new HashSet<>(2);
    Environment env = protoclass().environment();
    for (TypeMirror thrown : ((ExecutableElement) element).getThrownTypes()) {
      if (env.isCheckedException(thrown)) {
        exceptions.add(thrown.toString());
      }
    }
    return ImmutableSet.copyOf(exceptions);
  }

  public Set<String> getThrownCheckedExceptions() {
    return thrownCheckedExceptions;
  }

  private void initAttributeBuilder() {
    AttributeBuilderReflection attributeBuilderReflection =
        AttributeBuilderReflection.forValueType(this);
    isAttributeBuilder = attributeBuilderReflection.isAttributeBuilder();

    if (isAttributeBuilder) {
      attributeBuilderDescriptor = attributeBuilderReflection.getAttributeBuilderDescriptor();
    }
  }

  private void initImmutableCopyOf() {
    ensureTypeIntrospected();
    this.isGenerateImmutableCopyOf = containingType.kind().isValue()
        && !style().immutableCopyOfRoutinesNames().isEmpty()
        && (typeKind.isRegular() || typeKind.isOptionalKind())
        && !isPrimitiveOrWrapped(rawTypeName)
        && !isUnwrappedElementPrimitiveType()
        && !isStringType()
        && !isEnumType()
        && attributeValueType == null;
  }

  private void initOrderKind() {
    if (typeKind.isSortedKind()) {
      checkOrderAnnotations();
      if (orderKind == OrderKind.NONE) {
        this.typeKind = AttributeTypeKind.REGULAR;
      }
    }
  }

  private void initTypeName() {
    this.importsResolver = new ImportsTypeStringResolver(protoclass().declaringType().orNull(), getDeclaringType());
    this.importsResolver.hierarchyTraversalForUnresolvedTypes(
        protoclass().environment().round(),
        this.containingType.extendedClasses(),
        this.containingType.implementedInterfaces(),
        this.containingType.unresolvedYetArguments);

    TypeStringProvider provider = new TypeStringProvider(
        reporter,
        element,
        returnType,
        importsResolver,
        protoclass().constitution().generics().vars(),
        null);

    provider.sourceExtractionCache = containingType;
    provider.forAttribute = true;
    provider.processNestedTypeUseAnnotations = true;
    provider.process();

    this.hasSomeUnresolvedTypes = provider.hasSomeUnresovedTypes();
    this.rawTypeName = provider.rawTypeName();
    this.returnTypeName = provider.returnTypeName();
    this.typeParameters = provider.typeParameters();
    this.hasTypeVariables = provider.hasTypeVariables;
    this.nullElements = provider.nullElements;
    if (provider.nullableTypeAnnotation) {
      this.nullability = NullabilityAnnotationInfo.forTypeUse();
    }
  }

  public NullElements nullElements = NullElements.BAN;
  public boolean isSuppressedOptional;

  public enum NullElements {
    BAN,
    ALLOW,
    SKIP,
    NOP;

    public boolean ban() {
      return this == BAN;
    }

    public boolean allow() {
      return this == ALLOW || this == NOP;
    }

    public boolean skip() {
      return this == SKIP;
    }
  }

  private void initAttributeValueType() {

    if ((style().deepImmutablesDetection()
        || style().attributeBuilderDetection()
        || containingType.isGenerateCriteria())
        && containedTypeElement != null) {
      // prevent recursion in case we have the same type
      if (CachingElements.equals(containedTypeElement, containingType.element)) {
        // We don't propagate type arguments so we don't support it, sorry
        if (containingType.generics().isEmpty()) {
          this.attributeValueType = containingType;
        }
      } else {
        Environment environment = protoclass().environment();
        for (Protoclass p : environment.protoclassesFrom(Collections.singleton(containedTypeElement))) {
          if ((p.kind().isDefinedValue() || p.kind().isModifiable())
              && canAccessImplementation(p)
              && p.constitution().generics().isEmpty()) {
            this.attributeValueType = environment.composeValue(p);
          }
          break;
        }
      }
    }
  }

  private boolean canAccessImplementation(Protoclass p) {
    return p.constitution().implementationVisibility().isPublic()
        || (!p.constitution().implementationVisibility().isPrivate()
            && p.constitution().implementationPackage().equals(p.constitution().definingPackage()));
  }

  public String implementationModifiableType() {
    if (isAttributeValueKindModifyFrom()) {
      return attributeValueType.constitution.typeModifiable().toString();
    }
    return getType();
  }

  public String implementationType() {
    if (isAttributeValueKindCopy()) {
      return attributeValueType.typeValue().toString();
    }
    return getType();
  }

  public boolean hasAttributeValue() {
    return attributeValueType != null;
  }

  public boolean attributeValueKindIsCollectionOfModifiable() {
    return attributeValueType != null
        && typeKind.isCollectionKind()
        && attributeValueType.kind().isModifiable()
        && attributeValueType.isGenerateFilledFrom();
  }

  public boolean isAttributeValueKindCopy() {
    return attributeValueType != null
        && typeKind.isRegular()
        && attributeValueType.kind().isValue()
        && attributeValueType.isUseCopyConstructor();
  }

  public boolean isAttributeValueKindModifyFrom() {
    return attributeValueType != null
        && typeKind.isRegular()
        && attributeValueType.kind().isModifiable()
        && attributeValueType.isGenerateFilledFrom();
  }

  public Set<ValueAttribute> getConstructorParameters() {
    if (attributeValueType != null && attributeValueType.isUseConstructor()) {
      return attributeValueType.getConstructorArguments();
    }
    return Collections.emptySet();
  }

  private void initTypeKind() {
    if (instantiation != null) {
      typeKind = AttributeTypeKind.ENCODING;
    } else if (isGenerateDerived) {
      typeKind = AttributeTypeKind.REGULAR;
      ensureTypeIntrospected();
    } else if (returnType.getKind() == TypeKind.ARRAY) {
      typeKind = AttributeTypeKind.ARRAY;
      ensureTypeIntrospected();
    } else {
      typeKind = AttributeTypeKind.forRawType(rawTypeName);
      ensureTypeIntrospected();
      typeKind = typeKind.havingEnumFirstTypeParameter(hasEnumContainedElementType());
    }
  }

  private boolean supportBuiltinContainerTypes() {
    return protoclass().styles().style().builtinContainerAttributes();
  }

  public static class WholeTypeVariable {
    public final boolean is;
    public final boolean not;
    public final int index;

    WholeTypeVariable(int index) {
      this.index = index;
      this.is = index >= 0;
      this.not = !is;
    }
  }

  public WholeTypeVariable getWholeTypeVariable() {
    return getWholeTypeVariable(false);
  }

  public WholeTypeVariable getSecondaryWholeTypeVariable() {
    return getWholeTypeVariable(true);
  }

  private WholeTypeVariable getWholeTypeVariable(boolean secondary) {
    if (!hasTypeVariables) {
      return NON_WHOLE_TYPE_VARIABLE;
    }

    if (secondary && !isMapType()) {
      return NON_WHOLE_TYPE_VARIABLE;
    }

    String typeString = secondary
        ? getSecondaryElementType()
        : getElementType();

    if (!containingType.generics().isEmpty()) {
      for (Parameter p : containingType.generics().parameters) {
        if (p.var.equals(typeString)) {
          return new WholeTypeVariable(p.index);
        }
      }
    }

    return NON_WHOLE_TYPE_VARIABLE;
  }

  private boolean hasEnumContainedElementType() {
    return containedTypeElement != null
        && containedTypeElement.getKind() == ElementKind.ENUM;
  }

  DeclaringType getDeclaringType() {
    return containingType.inferDeclaringType(element);
  }

  private void validateTypeAndAnnotations() {
    boolean wasOptional = isOptionalType();

    if (!typeKind.isRegular() && !supportBuiltinContainerTypes()) {
      typeKind = AttributeTypeKind.REGULAR;
    }

    if (typeKind.isContainerKind() && typeParameters.isEmpty()) {
      typeKind = AttributeTypeKind.REGULAR;
      if (!SuppressedWarnings.forElement(element, false, false).rawtypes) {
        report().warning(About.UNTYPE,
            "Raw container types treated as regular attributes, nothing special generated."
                + " It is better to avoid raw types at all times");
      }
    }

    if (typeKind.isContainerKind()) {
      boolean hasWildcardInType = returnTypeName.indexOf('?') >= 0;
      if (hasWildcardInType) {
        if (hasNakedWildcardArguments()) {
          typeKind = AttributeTypeKind.REGULAR;
          report()
              .annotationNamed(DefaultMirror.simpleName())
              .warning(About.UNTYPE,
                  "Wildcards are not supported as elements or key/values. Make it lose its special treatment");
        }
      }
    }

    if (isNullable()) {
      if (isOptionalType()) {
        typeKind = AttributeTypeKind.REGULAR;
        report()
            .annotationNamed(DefaultMirror.simpleName())
            .warning(About.UNTYPE, "@Nullable on a Optional attribute make it lose its special treatment");
      } else if (isPrimitive()) {
        report()
            .annotationNamed(nullability.simpleName())
            .error("@%s could not be used with primitive type attibutes", nullability.simpleName());
      } else if (containingType.isAnnotationType()) {
        report()
            .annotationNamed(nullability.simpleName())
            .error("@%s could not be used with annotation attribute, use default value", nullability.simpleName());
      }
    }

    if (isGenerateDefault && isOptionalType()) {
      typeKind = AttributeTypeKind.REGULAR;
      report()
          .annotationNamed(DefaultMirror.simpleName())
          .warning(About.UNTYPE, "@Value.Default on a optional attribute make it lose its special treatment");
    }

    if (isContainerType() && containingType.isUseStrictBuilder()) {
      if (isGenerateDefault) {
        typeKind = AttributeTypeKind.REGULAR;
        report()
            .annotationNamed(DefaultMirror.simpleName())
            .warning(About.UNTYPE,
                "@Value.Default on a container attribute make it lose its special treatment (when strictBuilder = true)");
      } else if (isNullable()) {
        typeKind = AttributeTypeKind.REGULAR;
        report()
            .annotationNamed(nullability.simpleName())
            .warning(About.UNTYPE,
                "@%s on a container attribute make it lose its special treatment (when strictBuilder = true)",
                nullability.simpleName());
      }
    }

    if (containingType.isAnnotationType() && isAuxiliary()) {
      report()
          .annotationNamed(AuxiliaryMirror.simpleName())
          .error("@Value.Auxiliary cannot be used on annotation attribute to not violate annotation spec");
    }

    if (!isGenerateJdkOnly()
        && (nullElements == NullElements.ALLOW || nullElements == NullElements.SKIP)) {
      report().warning(About.INCOMPAT,
          "Guava collection implementation does not allow null elements,"
              + " @AllowNulls/@SkipNulls annotation will be ignored."
              + " Switch Style.jdkOnly=true to use collections that permit nulls as values");
    }

    if (isOptionalType()
        && containedTypeElement != null // for specialized optional types it can be null
        && AttributeTypeKind.forRawType(containedTypeElement.getQualifiedName().toString()).isOptionalKind()) {
      typeKind = AttributeTypeKind.REGULAR;
      report().warning(About.UNTYPE,
          "Optional<Optional<*>> is turned into regular attribute to avoid ambiguity problems");
    }

    if (wasOptional && !isOptionalType()) {
      isSuppressedOptional = true;
    }
  }

  private boolean hasNakedWildcardArguments() {
    for (String t : typeParameters()) {
      if (t.startsWith("?")) {
        return true;
      }
    }
    return false;
  }

  private @Nullable NullabilityAnnotationInfo isAccessorNullableAccessor(Element element) {
    for (AnnotationMirror annotation : element.getAnnotationMirrors()) {
      TypeElement annotationElement = (TypeElement) annotation.getAnnotationType().asElement();
      Name simpleName = annotationElement.getSimpleName();
      Name qualifiedName = annotationElement.getQualifiedName();
      if (isNullableAnnotation(simpleName, qualifiedName)) {
        return ImmutableNullabilityAnnotationInfo.of(annotationElement);
      }
    }
    return null;
  }

  private void initSpecialAnnotations() {
    Environment environment = containingType.constitution.protoclass().environment();
    ImmutableList.Builder<AnnotationInjection> annotationInjections = null;

    for (AnnotationMirror annotation : element.getAnnotationMirrors()) {
      MetaAnnotated metaAnnotated = MetaAnnotated.from(annotation, environment);

      if (metaAnnotated.injectAnnotation().isPresent()) {
        InjectionInfo info = metaAnnotated.injectAnnotation().get();
        if (annotationInjections == null) {
          annotationInjections = ImmutableList.builder();
        }
        annotationInjections.add(info.injectionFor(annotation, environment));
      }

      TypeElement annotationElement = (TypeElement) annotation.getAnnotationType().asElement();
      Name simpleName = annotationElement.getSimpleName();
      Name qualifiedName = annotationElement.getQualifiedName();
      if (isNullableAnnotation(simpleName, qualifiedName)) {
        nullability = ImmutableNullabilityAnnotationInfo.of(annotationElement);
      } else if (simpleName.contentEquals(TypeStringProvider.EPHEMERAL_ANNOTATION_ALLOW_NULLS)) {
        nullElements = NullElements.ALLOW;
      } else if (simpleName.contentEquals(TypeStringProvider.EPHEMERAL_ANNOTATION_SKIP_NULLS)) {
        nullElements = NullElements.SKIP;
      }
    }
    if (containingType.isGenerateJacksonProperties()
        && typeKind.isMap()
        && Proto.isAnnotatedWith(element, Annotations.JACKSON_ANY_GETTER)) {
      jacksonAnyGetter = true;
    }
    if (containingType.isGenerateJacksonMapped()
        && (isGenerateAbstract || isGenerateDefault)
        && Proto.isAnnotatedWith(element, Annotations.JACKSON_VALUE)) {
      jacksonValue = true;
    }
    if (isCollectionType()
        && nullElements == NullElements.BAN
        && protoclass().styles().style().validationMethod() == ValidationMethod.NONE) {
      nullElements = NullElements.NOP;
    }
    if (annotationInjections != null) {
      this.annotationInjections = annotationInjections.build();
    }
  }

  private boolean isNullableAnnotation(Name simpleName, Name qualifiedName) {
    return qualifiedName.contentEquals(Annotations.JAVAX_CHECK_FOR_NULL)
        || qualifiedName.contentEquals(Annotations.JAVAX_NULLABLE)
        || simpleName.contentEquals(containingType.names().nullableAnnotation);
  }

  public boolean isNullableCollector() {
    return typeKind.isCollectionOrMapping()
        && (isNullable() || containingType.isDeferCollectionAllocation());
  }

  public boolean isDeferCollectionAllocation() {
    return typeKind.isCollectionOrMapping()
        && containingType.isDeferCollectionAllocation();
  }

  private void initMiscellaneous() {
    this.deprecated = protoclass()
        .processing()
        .getElementUtils()
        .isDeprecated(CachingElements.getDelegate(element));

    this.docComment = containingType.extractDocComment(element);
    if (!isPrimitive()
        && isMandatory()
        && protoclass().styles().style().validationMethod() != ValidationMethod.SIMPLE) {
      this.nullability = NullabilityAnnotationInfo.forTypeUse();
    }
  }

  private void initBuilderParamsIfApplicable() {
    if (!protoclass().environment().hasBuilderModule()) {
      return;
    }
    isBuilderParameter = FParameterMirror.isPresent(element);
    Optional<SwitchMirror> switcher = SwitchMirror.find(element);
    if (switcher.isPresent()) {
      if (isBuilderParameter) {
        report().annotationNamed(FParameterMirror.simpleName())
            .error("@%s and @%s annotations cannot be used on a same factory parameter",
                FParameterMirror.simpleName(),
                SwitchMirror.simpleName());
        isBuilderParameter = false;
      }
      if (!isEnumType()) {
        report().annotationNamed(SwitchMirror.simpleName())
            .error("@%s annotation applicable only to enum parameters", SwitchMirror.simpleName());
      } else {
        builderSwitcherModel = new SwitcherModel(switcher.get(), names, containedTypeElement);
      }
    }
  }

  @Nullable
  public SwitcherModel builderSwitcherModel;
  public boolean isBuilderParameter;
  boolean hasSomeUnresolvedTypes;

  public boolean hasBuilderSwitcherDefault() {
    return isBuilderSwitcher() && builderSwitcherModel.hasDefault();
  }

  public boolean isBuilderSwitcher() {
    return builderSwitcherModel != null;
  }

  public boolean canUseNullAsUndefined() {
    return !isPrimitive()
        && !isNullable()
        && !typeKind.isCollectionOrMapping()
        && !containingType.isUseStrictBuilder();
  }

  public boolean requiresTrackIsSet() {
    if (isGenerateDefault && isPrimitive()) {
      // because privimitive cannot be null
      return true;
    }
    if (isGenerateDefault && isNullable()) {
      // nullable arrays should be able to distinguish null from default
      return true;
    }
    if (typeKind.isCollectionOrMapping() && isGenerateDefault) {
      // becase builder/collector is used and have to distinguish non-default value is set
      return true;
    }
    if (containingType.isUseStrictBuilder()
        && !isMandatory()
        && !typeKind.isCollectionOrMapping()) {
      // non-mandatory attributes without add/put methods
      // should be checked if it was already initialized
      // for a strict builder
      return true;
    }
    return false;
  }

  public boolean isGenerateImmutableCopyOf;

  public Collection<TypeElement> getEnumElements() {
    if (isEnumType()) {
      return Collections.singletonList(containedTypeElement);
    }
    if (!isContainerType()) {
      List<TypeElement> elements = Lists.newArrayListWithCapacity(2);
      if (hasEnumContainedElementType()) {
        elements.add(containedTypeElement);
      }
      if (isMapType() && containedSecondaryTypeElement.getKind() == ElementKind.ENUM) {
        elements.add(containedSecondaryTypeElement);
      }
      return elements;
    }
    return Collections.emptyList();
  }

  boolean hasConstructorParameterCustomOrder() {
    return getConstructorParameterOrder() > ValueAttribute.CONSTRUCTOR_PARAMETER_DEFAULT_ORDER;
  }

  private Protoclass protoclass() {
    return containingType.constitution.protoclass();
  }

  public String getGenericArgs() {
    String type = getType();
    int indexOfGenerics = type.indexOf('<');
    if (indexOfGenerics > 0) {
      return type.substring(indexOfGenerics);
    }
    return "";
  }

  public AttributeBuilderDescriptor getAttributeBuilderDescriptor() {
    return attributeBuilderDescriptor;
  }

  Reporter report() {
    return reporter.withElement(element);
  }

  public CharSequence getTypeTokenOfElement() {
    return containingType.getGsonTypeTokens().sourceFor(getElementType());
  }

  public CharSequence getTypeTokenOfSecondaryElement() {
    return containingType.getGsonTypeTokens().sourceFor(getSecondaryElementType());
  }

  public Element originalElement() {
    return CachingElements.getDelegate(element);
  }

  public Element originalTypeElement() {
    return containingType.originalElement();
  }

  @Override
  public StyleInfo style() {
    return containingType.constitution.style();
  }

  public boolean supportsInternalImplConstructor() {
    return !isEncoding() || instantiation.supportsInternalImplConstructor();
  }

  public Collection<String> fieldInjectedAnnotations() {
    return collectInjections(Where.FIELD);
  }

  public Collection<String> accessorInjectedAnnotations() {
    return collectInjections(Where.ACCESSOR);
  }

  public Collection<String> syntheticFieldsInjectedAnnotations() {
    return collectInjections(Where.SYNTHETIC_FIELDS);
  }

  public Collection<String> initializerInjectedAnnotations() {
    return collectInjections(Where.INITIALIZER);
  }

  public Collection<String> constructorParameterInjectedAnnotations() {
    return collectInjections(Where.CONSTRUCTOR_PARAMETER);
  }

  public Collection<String> elementInitializerInjectedAnnotations() {
    return collectInjections(Where.ELEMENT_INITIALIZER);
  }

  private Collection<String> collectInjections(Where target) {
    return AnnotationInjections.collectInjections(element,
        target,
        Collections.singleton(name()),
        annotationInjections,
        containingType.getDeclaringTypeAnnotationInjections(),
        containingType.getDeclaringTypeEnclosingAnnotationInjections(),
        containingType.getDeclaringPackageAnnotationInjections());
  }

  @Override
  public String toString() {
    return "Attribute[" + name() + "]";
  }

  void initNullabilitySupertype(ExecutableElement accessor) {
    if (nullabilityInSupertype == null && !isPrimitive() && !isNullable()) {
      nullabilityInSupertype = isAccessorNullableAccessor(accessor);
    }
  }

  public boolean isNullableInSupertype() {
    return nullabilityInSupertype != null;
  }

  public String atNullableInSupertypeLocal() {
    return nullabilityInSupertype != null ? nullabilityInSupertype.asLocalPrefix() : "";
  }

  enum ToName implements Function<ValueAttribute, String> {
    FUNCTION;
    @Override
    public String apply(ValueAttribute input) {
      return input.name();
    }
  }
}
