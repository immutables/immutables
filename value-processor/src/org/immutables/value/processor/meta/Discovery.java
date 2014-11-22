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

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.immutables.generator.SourceOrdering;
import org.immutables.value.Value;

import javax.annotation.Nullable;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;

/**
 * MOST CODE CARRIED FORWARD FROM OLD IMPLEMENTATION. IT NEED HEAVY REFACTORING TO REMOVE LEGACY
 * STUFF AND IMPROVE CORRECTNESS OF DISCOVERIES (GET RID OF SOME HEURISTICS AND SHORTCUTS).
 */
public class Discovery {
  private static final String ORDINAL_VALUE_TYPE = "org.immutables.common.collect.OrdinalValue";
  private static final String EQUALS_METHOD = "equals";
  private static final String TO_STRING_METHOD = "toString";
  private static final String HASH_CODE_METHOD = "hashCode";

  private final Set<TypeElement> annotations;
  private final RoundEnvironment round;
  private final ProcessingEnvironment processing;
  private final String typeMoreObjects;

  public Discovery(
      ProcessingEnvironment processing,
      RoundEnvironment round,
      Set<TypeElement> annotations) {
    this.processing = processing;
    this.round = round;
    this.annotations = annotations;
    this.typeMoreObjects = inferTypeMoreObjects();
  }

  private String inferTypeMoreObjects() {
    @Nullable
    TypeElement typeElement =
        this.processing.getElementUtils().getTypeElement(UnshadeGuava.typeString("base.MoreObjects"));

    return typeElement != null ? "MoreObjects" : "Objects";
  }

  public List<ValueType> discover() {
    Set<Element> allElemenents = Sets.newLinkedHashSet();
    for (TypeElement annotationType : annotations) {
      allElemenents.addAll(round.getElementsAnnotatedWith(annotationType));
    }
    return checkForANumberOfAttributes(discoverValueTypes(allElemenents));
  }

  private List<ValueType> discoverValueTypes(Set<Element> allElemenents) {
    List<ValueType> generateTypes = Lists.newArrayListWithExpectedSize(allElemenents.size());

    for (Element typeElement : allElemenents) {
      if (typeElement instanceof TypeElement) {
        TypeElement type = (TypeElement) typeElement;

        if (type.getEnclosingElement().getAnnotation(Value.Nested.class) == null) {
          collectDiscoveredTypeDescriptors(generateTypes, type);
        }
      }
    }
    return generateTypes;
  }

  private List<ValueType> checkForANumberOfAttributes(List<ValueType> valueTypes) {
    ImmutableList.Builder<ValueType> builder = ImmutableList.builder();

    for (ValueType valueType : valueTypes) {
      if (!valueType.emptyNesting) {
        if (valueType.getImplementedAttributes().size() > 124) {
          processing.getMessager().printMessage(
              Diagnostic.Kind.ERROR,
              "Value objects with more than 124 attributes (including inherited) are not supported."
                  + " Please decompose your object into a smaller ones",
              valueType.element);
          continue;
        }
      }
      builder.add(valueType);
    }

    return builder.build();
  }

  private void collectDiscoveredTypeDescriptors(List<ValueType> generateTypes, TypeElement type) {
    Value.Immutable genImmutable = type.getAnnotation(Value.Immutable.class);
    Value.Nested genNested = type.getAnnotation(Value.Nested.class);
    if (genImmutable != null) {
      ValueType generateType = inspectValueType(type, genImmutable);

      if (genNested != null) {
        generateType.setNestedChildren(extractNestedChildren(type));
      }

      generateTypes.add(generateType);
    } else if (genNested != null) {
      List<ValueType> nestedChildren = extractNestedChildren(type);
      if (!nestedChildren.isEmpty()) {
        ValueType emptyNestingType = new ValueType();
        emptyNestingType.typeMoreObjects = typeMoreObjects;
        emptyNestingType.element = type;
        emptyNestingType.emptyNesting = true;
        emptyNestingType.segmentedName = SegmentedName.from(type.getQualifiedName());
        emptyNestingType.setNestedChildren(nestedChildren);

        generateTypes.add(emptyNestingType);
      }
    }
  }

  private List<ValueType> extractNestedChildren(TypeElement parent) {
    ImmutableList.Builder<ValueType> children = ImmutableList.builder();
    for (Element element : parent.getEnclosedElements()) {
      switch (element.getKind()) {
      case INTERFACE:
      case CLASS:
        Value.Immutable annotation = element.getAnnotation(Value.Immutable.class);
        if (annotation != null) {
          children.add(inspectValueType((TypeElement) element, annotation));
        }
        break;
      default:
      }
    }
    return children.build();
  }

  private boolean isValueType(TypeElement type, Value.Immutable annotation) {
    boolean isStaticOrTopLevel =
        type.getKind() == ElementKind.INTERFACE
            || type.getKind() == ElementKind.ANNOTATION_TYPE
            || (type.getKind() == ElementKind.CLASS
            && (type.getEnclosingElement().getKind() == ElementKind.PACKAGE || type.getModifiers()
                .contains(Modifier.STATIC)));

    return annotation != null
        && isStaticOrTopLevel
        && isNonFinal(type);
  }

  private boolean isNonFinal(TypeElement type) {
    return !type.getModifiers().contains(Modifier.FINAL);
  }

  ValueType inspectValueType(TypeElement type, Value.Immutable annotation) {
    if (!isValueType(type, annotation)) {
      error(type,
          "Type '%s' annotated with @%s must be non-final class, interface or annotation type",
          type.getSimpleName(),
          Value.Immutable.class.getSimpleName());
    }

    SegmentedName segmentedName = SegmentedName.from(processing, type);

    ValueType valueType = new ValueType();
    valueType.typeMoreObjects = typeMoreObjects;
    valueType.element = type;

    collectGeneratedCandidateMethods(type, valueType);

    valueType.segmentedName = segmentedName;
    return valueType;
  }

  private void collectGeneratedCandidateMethods(TypeElement type, ValueType valueType) {
    for (Element element : getAccessorsInSourceOrder(type)) {
      if (isElegibleAccessorMethod(element)) {
        processGenerationCandidateMethod(valueType, (ExecutableElement) element);
      }
    }
    // This is to restore previous behavior.
    // Now we pass only explicitly defined equals, hashCode, toString
    for (ExecutableElement element : ElementFilter.methodsIn(type.getEnclosedElements())) {
      switch (element.getSimpleName().toString()) {
      case EQUALS_METHOD: //$FALL-THROUGH$
      case HASH_CODE_METHOD: //$FALL-THROUGH$
      case TO_STRING_METHOD: //$FALL-THROUGH$
        processGenerationCandidateMethod(valueType, element);
      }
    }
  }

  private List<? extends Element> getAccessorsInSourceOrder(TypeElement type) {
    return type.getKind() == ElementKind.ANNOTATION_TYPE
        ? SourceOrdering.getEnclosedElements(type)
        : SourceOrdering.getAllAccessors(processing.getElementUtils(), type);
  }

  private boolean isElegibleAccessorMethod(Element element) {
    if (element.getKind() != ElementKind.METHOD) {
      return false;
    }
    if (element.getModifiers().contains(Modifier.STATIC)) {
      return false;
    }
    switch (element.getSimpleName().toString()) {
    case HASH_CODE_METHOD: //$FALL-THROUGH$
    case TO_STRING_METHOD:
      return false;
    }
    String definitionType = element.getEnclosingElement().toString();
    if (definitionType.equals(Object.class.getName())) {
      return false;
    }
    if (definitionType.startsWith(ORDINAL_VALUE_TYPE)) {
      return false;
    }
    return true;
  }

  private void processGenerationCandidateMethod(
      ValueType type,
      ExecutableElement attributeMethodCandidate) {

    NamingStyles namingStyle = NamingStyles.using(NamingStyles.defaultStyle());

    Name name = attributeMethodCandidate.getSimpleName();
    List<? extends VariableElement> parameters = attributeMethodCandidate.getParameters();
    if (name.contentEquals(EQUALS_METHOD)
        && parameters.size() == 1
        && parameters.get(0).asType().toString().equals(Object.class.getName())
        && !attributeMethodCandidate.getModifiers().contains(Modifier.ABSTRACT)) {
      type.isEqualToDefined = true;
      return;
    }

    if (name.contentEquals(HASH_CODE_METHOD)
        && parameters.isEmpty()
        && !attributeMethodCandidate.getModifiers().contains(Modifier.ABSTRACT)) {
      type.isHashCodeDefined = true;
      return;
    }

    if (name.contentEquals(TO_STRING_METHOD)
        && parameters.isEmpty()
        && !attributeMethodCandidate.getModifiers().contains(Modifier.ABSTRACT)) {
      type.isToStringDefined = true;
      return;
    }

    @Nullable
    Value.Check validateAnnotation = attributeMethodCandidate.getAnnotation(Value.Check.class);
    if (validateAnnotation != null) {
      if (attributeMethodCandidate.getReturnType().getKind() == TypeKind.VOID
          && attributeMethodCandidate.getParameters().isEmpty()
          && !attributeMethodCandidate.getModifiers().contains(Modifier.PRIVATE)
          && !attributeMethodCandidate.getModifiers().contains(Modifier.ABSTRACT)
          && !attributeMethodCandidate.getModifiers().contains(Modifier.STATIC)
          && !attributeMethodCandidate.getModifiers().contains(Modifier.NATIVE)) {
        type.validationMethodName = attributeMethodCandidate.getSimpleName().toString();
      } else {
        error(attributeMethodCandidate,
            "Method '%s' annotated with @%s must be non-private parameter-less method and have void return type.",
            attributeMethodCandidate.getSimpleName(),
            Value.Check.class.getSimpleName());
      }
    }

    if (isDiscoveredAttribute(attributeMethodCandidate)) {
      TypeMirror returnType = attributeMethodCandidate.getReturnType();

      ValueAttribute attribute = new ValueAttribute();

      if (isAbstract(attributeMethodCandidate)) {
        attribute.isGenerateAbstract = true;
        if (attributeMethodCandidate.getDefaultValue() != null) {
          attribute.isGenerateDefault = true;
        }
      } else if (hasAnnotation(attributeMethodCandidate, Value.Default.class)) {
        attribute.isGenerateDefault = true;
      } else if (hasAnnotation(attributeMethodCandidate, Value.Derived.class)) {
        attribute.isGenerateDerived = true;
      }
/*!!
      if (hasAnnotation(attributeMethodCandidate, GeneratePredicate.class)
          && returnType.getKind() == TypeKind.BOOLEAN) {
        attributeBuilder.isGeneratePredicate(true);
      } else if (hasAnnotation(attributeMethodCandidate, GenerateFunction.class)) {
        attributeBuilder.isGenerateFunction(true);
      }
*/
      if (hasAnnotation(attributeMethodCandidate, Value.Lazy.class)) {
        if (isAbstract(attributeMethodCandidate) || isFinal(attributeMethodCandidate)) {
          error(attributeMethodCandidate,
              "Method '%s' annotated with @%s must be non abstract and non-final",
              attributeMethodCandidate.getSimpleName(),
              Value.Lazy.class.getSimpleName());
        } else {
          attribute.isGenerateLazy = true;
        }
      }

      attribute.returnTypeName = returnType.toString();
      attribute.returnType = returnType;
      attribute.names = namingStyle.forAccessor(name.toString());
      attribute.element = attributeMethodCandidate;

      type.attributes.add(attribute);
    }
  }

  private static boolean isAbstract(Element element) {
    return element.getModifiers().contains(Modifier.ABSTRACT);
  }

  private static boolean isFinal(Element element) {
    return element.getModifiers().contains(Modifier.FINAL);
  }

  private static boolean isDiscoveredAttribute(ExecutableElement attributeMethodCandidate) {
    return attributeMethodCandidate.getParameters().isEmpty()
        && attributeMethodCandidate.getReturnType().getKind() != TypeKind.VOID
        && (isAbstract(attributeMethodCandidate) || hasGenerateAnnotation(attributeMethodCandidate));
  }

  private static boolean hasGenerateAnnotation(ExecutableElement attributeMethodCandidate) {
    return hasAnnotation(attributeMethodCandidate, Value.Default.class)
        || hasAnnotation(attributeMethodCandidate, Value.Derived.class)
        || hasAnnotation(attributeMethodCandidate, Value.Lazy.class);
  }

  private static boolean hasAnnotation(Element element, Class<? extends Annotation> annotationType) {
    return element.getAnnotation(annotationType) != null;
  }

  private void error(Element type, String message, Object... parameters) {
    processing.getMessager().printMessage(Diagnostic.Kind.ERROR, String.format(message, parameters), type);
  }
}
