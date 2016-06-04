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

import com.google.common.base.Joiner;
import com.google.common.base.MoreObjects;
import com.google.common.base.Optional;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.lang.annotation.ElementType;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.type.ArrayType;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import org.immutables.generator.AnnotationMirrors;
import org.immutables.generator.StringLiterals;
import org.immutables.generator.TypeHierarchyCollector;
import org.immutables.value.Value;
import org.immutables.value.processor.meta.Generics.Parameter;
import org.immutables.value.processor.meta.Proto.DeclaringType;
import org.immutables.value.processor.meta.Proto.Environment;
import org.immutables.value.processor.meta.Proto.MetaAnnotated;
import org.immutables.value.processor.meta.Proto.Protoclass;
import org.immutables.value.processor.meta.Styles.UsingName.AttributeNames;
import org.immutables.value.processor.meta.ValueMirrors.Style.ImplementationVisibility;

/**
 * It's pointless to refactor this mess until
 * 1) Some sort of type calculus toolkit used/created
 * 2) Facets/Implicits in Generator toolkit with auto-memoising implemented
 */
public final class ValueAttribute extends TypeIntrospectionBase {
  private static final WholeTypeVariable NON_WHOLE_TYPE_VARIABLE = new WholeTypeVariable(-1);
  private static final int CONSTRUCTOR_PARAMETER_DEFAULT_ORDER = 0;
  private static final int CONSTRUCTOR_NOT_A_PARAMETER = -1;
  private static final String GUAVA_IMMUTABLE_PREFIX = UnshadeGuava.typeString("collect.Immutable");
  private static final String VALUE_ATTRIBUTE_NAME = "value";
  private static final String ID_ATTRIBUTE_NAME = "_id";
  private static final Splitter DOC_COMMENT_LINE_SPLITTER = Splitter.on('\n').omitEmptyStrings();
  private static final String[] EMPTY_SERIALIZED_NAMES = {};

  public AttributeNames names;
  public boolean isGenerateDefault;
  public boolean isGenerateDerived;
  public boolean isGenerateAbstract;
  public boolean isGenerateLazy;
  public ImmutableList<String> typeParameters = ImmutableList.of();
  // Replace with delegation?
  public Reporter reporter;

  public ValueType containingType;

  @Nullable
  public ValueType attributeValueType;

  TypeMirror returnType;
  Element element;
  String returnTypeName;

  public boolean hasEnumFirstTypeParameter;

  @Nullable
  private TypeElement containedTypeElement;
  @Nullable
  private TypeElement containedSecondaryTypeElement;

  private boolean generateOrdinalValueSet;
  private TypeMirror arrayComponent;

  @Nullable
  private NullabilityAnnotationInfo nullability;

  @Nullable
  private String rawTypeName;

  public String name() {
    return names.raw;
  }

  public boolean isBoolean() {
    return returnType.getKind() == TypeKind.BOOLEAN;
  }

  public boolean isInt() {
    return returnType.getKind() == TypeKind.INT;
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
    return isNullable() ? nullability.asPrefix() : "";
  }

  public String atNullabilityLocal() {
    return isNullable() ? nullability.asLocalPrefix() : "";
  }

  public boolean isSimpleLiteralType() {
    return isPrimitive()
        || isStringType()
        || isEnumType();
  }

  public boolean hasSimpleScalarElementType() {
    ensureTypeIntrospected();
    String type = getWrappedElementType();
    return type.equals(String.class.getName())
        || isPrimitiveWrappedType(type)
        || hasEnumContainedElementType()
        || isEnumType()
        || isJdkSpecializedOptional(); // the last is not needed, probably
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
        && !hasBuilderSwitcherDefault();
  }

  public boolean isNullable() {
    return nullability != null;
  }

  public ImmutableList<String> docComment = ImmutableList.of();

  public boolean deprecated;

  public boolean isComparableKey() {
    return isContainerType()
        && super.isComparable();
  }

  @Override
  public boolean isComparable() {
    return isNumberType()
        || isStringType()
        || (!isContainerType() && super.isComparable());
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
    return name();
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
    if (containingType.isGenerateJacksonMapped()) {
      return extractAnnotationsForElement(
          ElementType.METHOD,
          protoclass().styles().style().passAnnotationsNames());

    }
    return Annotations.getAnnotationLines(element,
        protoclass().styles().style().passAnnotationsNames(),
        false,
        ElementType.METHOD);
  }

  public CharSequence getConstructorParameterAnnotations() {
    List<CharSequence> annotations =
        Annotations.getAnnotationLines(element,
            protoclass().styles().style().passAnnotationsNames(),
            false,
            ElementType.PARAMETER);

    if (!annotations.isEmpty()) {
      return Joiner.on(' ').join(annotations).concat(" ");
    }

    return "";
  }

  public List<CharSequence> getJacksonFieldsAnnotations() {
    return extractAnnotationsForElement(ElementType.FIELD, Collections.<String>emptySet());
  }

  private List<CharSequence> extractAnnotationsForElement(ElementType elementType, Set<String> additionalAnnotations) {
    List<CharSequence> allAnnotations = Lists.newArrayListWithCapacity(1);

    boolean dontHaveJsonPropetyAnnotationAlready = Annotations.getAnnotationLines(element,
        Collections.singleton(JsonPropertyMirror.qualifiedName()),
        false,
        elementType).isEmpty();

    if (dontHaveJsonPropetyAnnotationAlready) {
      String propertyAnnotation = "@" + JsonPropertyMirror.qualifiedName();

      if (protoclass().styles().style().forceJacksonPropertyNames()) {
        propertyAnnotation += "(" + StringLiterals.toLiteral(name()) + ")";
      }

      allAnnotations.add(propertyAnnotation);
    }

    allAnnotations.addAll(
        Annotations.getAnnotationLines(element,
            Sets.union(additionalAnnotations,
                protoclass().styles().style().additionalJsonAnnotationsNames()),
            true,
            elementType));

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

  private void checkOrderAnnotations() {
    Optional<NaturalOrderMirror> naturalOrderAnnotation = NaturalOrderMirror.find(element);
    Optional<ReverseOrderMirror> reverseOrderAnnotation = ReverseOrderMirror.find(element);

    if (naturalOrderAnnotation.isPresent() && reverseOrderAnnotation.isPresent()) {
      report()
          .error("@Value.Natural and @Value.Reverse annotations could not be used on the same attribute");
    } else if (naturalOrderAnnotation.isPresent()) {
      if (typeKind.isSortedKind()) {
        if (isComparableKey()) {
          orderKind = OrderKind.NATURAL;
        } else {
          report()
              .annotationNamed(NaturalOrderMirror.simpleName())
              .error("@Value.Natural should used on a set of Comparable elements (map keys)");
        }
      } else {
        report()
            .annotationNamed(NaturalOrderMirror.simpleName())
            .error("@Value.Natural should specify order for SortedSet, SortedMap, NavigableSet or NavigableMap attributes");
      }
    } else if (reverseOrderAnnotation.isPresent()) {
      if (typeKind.isSortedKind()) {
        if (isComparableKey()) {
          orderKind = OrderKind.REVERSE;
        } else {
          report()
              .annotationNamed(ReverseOrderMirror.simpleName())
              .error("@Value.Reverse should used with a set of Comparable elements");
        }
      } else {
        report()
            .annotationNamed(ReverseOrderMirror.simpleName())
            .error("@Value.Reverse should specify order for SortedSet, SortedMap, NavigableSet or NavigableMap attributes");
      }
    }
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

  public boolean isGenerateEnumMap() {
    return typeKind.isEnumMap();
  }

  public String getUnwrappedElementType() {
    return isContainerType() ? unwrapType(containmentTypeName()) : getElementType();
  }

  public String getUnwrappedValueElementType() {
    return isMapType()
        ? getUnwrappedSecondaryElementType()
        : getUnwrappedElementType();
  }

  public String getWrappedElementType() {
    return wrapType(containmentTypeName());
  }

  private String containmentTypeName() {
    return (isArrayType() || isContainerType()) ? firstTypeParameter() : returnTypeName;
  }

  public String getRawType() {
    return rawTypeName;
  }

  public String getConsumedElementType() {
    return (isUnwrappedElementPrimitiveType()
        || String.class.getName().equals(containmentTypeName())
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
    return containmentTypeName();
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
        && !isGuavaImmutableDeclared();
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

  private static boolean isRegularMarshalableType(String name, boolean couldBeWrapped) {
    return String.class.getName().equals(name)
        || (couldBeWrapped ? isPrimitiveOrWrapped(name) : isPrimitiveType(name));
  }

  public boolean isRequiresMarshalingAdapter() {
    return !isRegularMarshalableType(getElementType(), isContainerType());
  }

  public boolean isRequiresMarshalingSecondaryAdapter() {
    return isMapType() && !isRegularMarshalableType(getSecondaryElementType(), true);
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
    return typeKind.rawSimpleName();
  }

  public boolean isMultisetType() {
    return typeKind.isMultiset();
  }

  public String getRawMapType() {
    return typeKind.rawSimpleName();
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

  private int parameterOrder = Integer.MIN_VALUE;
  private AttributeTypeKind typeKind;
  public boolean anyGetter;
  public boolean hasTypeVariables;

  int getConstructorParameterOrder() {
    boolean parameterOrderIsNotDefined = parameterOrder < CONSTRUCTOR_NOT_A_PARAMETER;

    if (parameterOrderIsNotDefined) {
      Optional<ParameterMirror> parameterAnnotation = ParameterMirror.find(element);

      parameterOrder = containingType.constitution.style().allParameters()
          ? CONSTRUCTOR_PARAMETER_DEFAULT_ORDER
          : CONSTRUCTOR_NOT_A_PARAMETER;

      if (parameterAnnotation.isPresent()) {
        if (parameterAnnotation.get().value()
            && containingType.constitution.style().allParameters()) {
          report().annotationNamed(ParameterMirror.simpleName())
              .warning("Annotation @Value.Parameter is superfluous when Style(allParameters = true)");
        }

        parameterOrder = parameterAnnotation.get().value()
            ? parameterAnnotation.get().order()
            : CONSTRUCTOR_NOT_A_PARAMETER;
      }

      if (parameterOrder == CONSTRUCTOR_NOT_A_PARAMETER
          && containingType.isAnnotationType()
          && names.get.equals(VALUE_ATTRIBUTE_NAME)) {
        parameterOrder = thereAreNoOtherMandatoryAttributes()
            ? CONSTRUCTOR_PARAMETER_DEFAULT_ORDER
            : CONSTRUCTOR_NOT_A_PARAMETER;
      }
    }
    return parameterOrder;
  }

  private boolean thereAreNoOtherMandatoryAttributes() {
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
        || containingType.constitution.style().visibility() == ImplementationVisibility.PUBLIC) {
      return "public ";
    } else if (element.getModifiers().contains(Modifier.PROTECTED)) {
      return "protected ";
    }
    return "";
  }

  public boolean isPrimitiveElement() {
    return isPrimitiveType(getUnwrappedElementType());
  }

  public boolean isAuxiliary() {
    return AuxiliaryMirror.isPresent(element);
  }

  private boolean isMarkedAsMongoId() {
    return IdMirror.isPresent(element);
  }

  boolean isIdAttribute() {
    return isMarkedAsMongoId()
        || ID_ATTRIBUTE_NAME.equals(getSerializedName());
  }

  /** Initialized Validates things that were not validated otherwise */
  void initAndValidate() {
    initTypeName();
    initTypeKind();
    initOrderKind();
    initBuilderParamsIfApplicable();
    initMiscellaneous();

    initSpecialAnnotations();
    validateTypeAndAnnotations();

    initAttributeValueType();
    initImmutableCopyOf();
  }

  private void initImmutableCopyOf() {
    ensureTypeIntrospected();
    this.isGenerateImmutableCopyOf = containingType.kind().isValue()
        && !containingType.constitution.style().immutableCopyOfRoutinesNames().isEmpty()
        && typeKind.isRegular()
        && !isPrimitiveOrWrapped(rawTypeName)
        && !isStringType()
        && !isEnumType()
        && attributeValueType == null;
  }

  private void initOrderKind() {
    if (typeKind.isSortedKind()) {
      checkOrderAnnotations();
      if (orderKind == OrderKind.NONE) {
        typeKind = AttributeTypeKind.REGULAR;
      }
    }
  }

  private void initTypeName() {
    TypeStringProvider provider = new TypeStringProvider(
        reporter,
        element,
        returnType,
        ImmutableList.<DeclaringType>builder()
            .addAll(protoclass().declaringType().asSet())
            .add(getDeclaringType())
            .build(),
        protoclass().constitution().generics().vars());

    provider.process();

    this.hasSomeUnresolvedTypes = provider.hasSomeUnresovedTypes();
    this.rawTypeName = provider.rawTypeName();
    this.returnTypeName = provider.returnTypeName();
    this.typeParameters = provider.typeParameters();
    this.hasTypeVariables = provider.hasTypeVariables;
  }

  private void initAttributeValueType() {
    if (containingType.constitution.style().deepImmutablesDetection()
        && containedTypeElement != null) {
      Environment environment = protoclass().environment();

      // prevent recursion in case we have the same type
      if (CachingElements.equals(containedTypeElement, containingType.element)) {
        this.attributeValueType = containingType;
      } else {
        for (Protoclass p : environment.protoclassesFrom(Collections.singleton(containedTypeElement))) {
          if ((p.kind().isDefinedValue() || p.kind().isModifiable()) && canAccessImplementation(p)) {
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
        && p.constitution().implementationPackage().equals(p.constitution().implementationPackage()));
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
    if (isGenerateDerived) {
      typeKind = AttributeTypeKind.REGULAR;
      ensureTypeIntrospected();
    } else if (returnType.getKind() == TypeKind.ARRAY) {
      typeKind = AttributeTypeKind.ARRAY;
      ensureTypeIntrospected();
    } else {
      typeKind = AttributeTypeKind.forRawType(rawTypeName);
      ensureTypeIntrospected();
      typeKind = typeKind.havingEnumFirstTypeParameter(hasEnumContainedElementType());
      if (typeKind().isContainerKind() && typeParameters.isEmpty()) {
        typeKind = AttributeTypeKind.REGULAR;
        report().warning("Raw container types treated as regular attributes, nothing special generated."
            + " It is better to avoid raw types at all times");
      }
    }
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
    boolean hasWildcardInType = returnTypeName.indexOf('?') >= 0;
    if (hasWildcardInType && typeKind != AttributeTypeKind.REGULAR) {
      if (hasNakedWildcardArguments()) {
        typeKind = AttributeTypeKind.REGULAR;
        report()
            .annotationNamed(DefaultMirror.simpleName())
            .warning("Wildcards are not supported as elements or key/values. Make it lose its special treatment");
      }
    }

    if (isNullable()) {
      if (isOptionalType()) {
        typeKind = AttributeTypeKind.REGULAR;
        report()
            .annotationNamed(DefaultMirror.simpleName())
            .warning("@Nullable on a Optional attribute make it lose its special treatment");
      } else if (isPrimitive()) {
        report()
            .annotationNamed(Annotations.NULLABLE_SIMPLE_NAME)
            .error("@Nullable could not be used with primitive type attibutes");
      } else if (containingType.isAnnotationType()) {
        report()
            .annotationNamed(Annotations.NULLABLE_SIMPLE_NAME)
            .error("@Nullable could not be used with annotation attribute, use default value");
      }
    }

    if (isGenerateDefault && isOptionalType()) {
      typeKind = AttributeTypeKind.REGULAR;
      report()
          .annotationNamed(DefaultMirror.simpleName())
          .warning("@Value.Default on a optional attribute make it lose its special treatment");
    }

    if (containingType.isUseStrictBuilder() && isContainerType()) {
      if (isGenerateDefault) {
        typeKind = AttributeTypeKind.REGULAR;
        report()
            .annotationNamed(DefaultMirror.simpleName())
            .warning("@Value.Default on a container attribute make it lose its special treatment (when strictBuilder = true)");
      } else if (isNullable()) {
        typeKind = AttributeTypeKind.REGULAR;
        report()
            .annotationNamed(Annotations.NULLABLE_SIMPLE_NAME)
            .warning("@Nullable on a container attribute make it lose its special treatment (when strictBuilder = true)");
      }
    }

    if (containingType.isAnnotationType() && isAuxiliary()) {
      report()
          .annotationNamed(AuxiliaryMirror.simpleName())
          .error("@Value.Auxiliary cannot be used on annotation attribute to not violate annotation spec");
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

  private void initSpecialAnnotations() {
    for (AnnotationMirror annotation : element.getAnnotationMirrors()) {
      TypeElement annotationElement = (TypeElement) annotation.getAnnotationType().asElement();
      if (annotationElement.getSimpleName().contentEquals(Annotations.NULLABLE_SIMPLE_NAME)) {
        nullability = ImmutableNullabilityAnnotationInfo.of(annotationElement);
      } else if (containingType.isGenerateJacksonMapped()
          && annotationElement.getQualifiedName().toString().equals(Annotations.JACKSON_ANY_GETTER)) {
        anyGetter = typeKind.isMap();
      }
    }
  }

  @Value.Immutable(intern = true, builder = false)
  static abstract class NullabilityAnnotationInfo {
    @Value.Parameter
    @Value.Auxiliary
    abstract TypeElement element();

    @Value.Derived
    String qualifiedName() {
      return element().getQualifiedName().toString();
    }

    @Value.Lazy
    String asPrefix() {
      return "@" + qualifiedName() + " ";
    }

    @Value.Lazy
    String asLocalPrefix() {
      boolean applicableToLocal = Annotations.annotationMatchesTarget(
          element(),
          ElementType.LOCAL_VARIABLE);

      return applicableToLocal
          ? asPrefix()
          : "";
    }
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
    Elements elements = protoclass()
        .processing()
        .getElementUtils();

    this.deprecated = elements.isDeprecated(element);

    if (containingType.constitution.implementationVisibility().isPublic()) {
      @Nullable String docComment = elements.getDocComment(element);
      if (docComment != null) {
        this.docComment = ImmutableList.copyOf(DOC_COMMENT_LINE_SPLITTER.split(docComment));
      }
    }
  }

  private void initBuilderParamsIfApplicable() {
    if (!protoclass().hasBuilderModule()) {
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
        builderSwitcherModel = new SwitcherModel(switcher.get(), name(), containedTypeElement);
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
    return !isPrimitive() && !isNullable() && !typeKind.isCollectionOrMapping();
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
      // non mandatory attributes without add/put methods
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

  Reporter report() {
    return reporter.withElement(element);
  }

  @Override
  public String toString() {
    return "Attribute[" + name() + "]";
  }
}
