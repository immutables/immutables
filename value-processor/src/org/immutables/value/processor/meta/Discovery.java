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

import javax.lang.model.util.ElementFilter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.tools.Diagnostic;
import org.immutables.generator.SourceOrdering;
import org.immutables.value.Value;

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

  public Discovery(
      ProcessingEnvironment processing,
      RoundEnvironment round,
      Set<TypeElement> annotations) {
    this.processing = processing;
    this.round = round;
    this.annotations = annotations;
  }

  public List<DiscoveredValue> discover() {
    Set<Element> allElemenents = Sets.newLinkedHashSet();
    for (TypeElement annotationType : annotations) {
      allElemenents.addAll(round.getElementsAnnotatedWith(annotationType));
    }
    return checkForANumberOfAttributes(discoverValueTypes(allElemenents));
  }

  private List<DiscoveredValue> discoverValueTypes(Set<Element> allElemenents) {
    List<DiscoveredValue> generateTypes = Lists.newArrayListWithExpectedSize(allElemenents.size());

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

  private List<DiscoveredValue> checkForANumberOfAttributes(List<DiscoveredValue> generateTypes) {
    ImmutableList.Builder<DiscoveredValue> builder = ImmutableList.builder();

    for (DiscoveredValue discoveredValue : generateTypes) {
      if (!discoveredValue.isEmptyNesting()) {
        if (discoveredValue.getImplementedAttributes().size() > 124) {
          processing.getMessager().printMessage(
              Diagnostic.Kind.ERROR,
              "Value objects with more than 124 attributes (including inherited) are not supported."
                  + " Please decompose your object into a smaller ones",
              discoveredValue.internalTypeElement());
          continue;
        }
      }
      builder.add(discoveredValue);
    }

    return builder.build();
  }

  private void collectDiscoveredTypeDescriptors(List<DiscoveredValue> generateTypes, TypeElement type) {
    Value.Immutable genImmutable = type.getAnnotation(Value.Immutable.class);
    Value.Nested genNested = type.getAnnotation(Value.Nested.class);
    if (genImmutable != null) {
      DiscoveredValue generateType = inspectDiscoveredType(type, genImmutable);

      if (genNested != null) {
        generateType.setNestedChildren(extractNestedChildren(type));
      }

      generateTypes.add(generateType);
    } else if (genNested != null) {
      List<DiscoveredValue> nestedChildren = extractNestedChildren(type);
      if (!nestedChildren.isEmpty()) {
        DiscoveredValue emptyNestingType = DiscoveredValues.builder()
            .internalTypeElement(type)
            .isUseBuilder(false)
            .isGenerateCompact(false)
            .build();

        emptyNestingType.setEmptyNesting(true);
        emptyNestingType.setSegmentedName(SegmentedName.from(type.getQualifiedName()));
        emptyNestingType.setNestedChildren(nestedChildren);

        generateTypes.add(emptyNestingType);
      }
    }
  }

  private List<DiscoveredValue> extractNestedChildren(TypeElement parent) {
    ImmutableList.Builder<DiscoveredValue> children = ImmutableList.builder();
    for (Element element : parent.getEnclosedElements()) {
      switch (element.getKind()) {
      case INTERFACE:
      case CLASS:
        Value.Immutable annotation = element.getAnnotation(Value.Immutable.class);
        if (annotation != null) {
          children.add(inspectDiscoveredType((TypeElement) element, annotation));
        }
        break;
      default:
      }
    }
    return children.build();
  }

  private boolean isDiscoveredType(TypeElement type, Value.Immutable annotation) {
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

  DiscoveredValue inspectDiscoveredType(TypeElement type, Value.Immutable annotation) {
    if (!isDiscoveredType(type, annotation)) {
      error(type,
          "Type '%s' annotated with @%s must be non-final class, interface or annotation type",
          type.getSimpleName(),
          Value.Immutable.class.getSimpleName());
    }

    SegmentedName segmentedName = SegmentedName.from(processing, type);

    boolean useBuilder = annotation.builder();

    DiscoveredValues.Builder typeBuilder =
        DiscoveredValues.builder()
            .internalTypeElement(type)
            .isUseBuilder(useBuilder)
            .isGenerateCompact(false);

    collectGeneratedCandidateMethods(type, typeBuilder);

    DiscoveredValue generateType = typeBuilder.build();
    generateType.setSegmentedName(segmentedName);
    return generateType;
  }

  private void collectGeneratedCandidateMethods(TypeElement type, DiscoveredValues.Builder typeBuilder) {
    for (Element element : getAccessorsInSourceOrder(type)) {
      if (isElegibleAccessorMethod(element)) {
        processGenerationCandidateMethod(typeBuilder, (ExecutableElement) element);
      }
    }
    // This is to restore previous behavior.
    // Now we pass only explicitly defined equals, hashCode, toString
    for (ExecutableElement element : ElementFilter.methodsIn(type.getEnclosedElements())) {
      switch (element.getSimpleName().toString()) {
      case EQUALS_METHOD: //$FALL-THROUGH$
      case HASH_CODE_METHOD: //$FALL-THROUGH$
      case TO_STRING_METHOD: //$FALL-THROUGH$
        processGenerationCandidateMethod(typeBuilder, element);
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
      DiscoveredValues.Builder type,
      ExecutableElement attributeMethodCandidate) {

    Name name = attributeMethodCandidate.getSimpleName();
    List<? extends VariableElement> parameters = attributeMethodCandidate.getParameters();
    if (name.contentEquals(EQUALS_METHOD)
        && parameters.size() == 1
        && parameters.get(0).asType().toString().equals(Object.class.getName())
        && !attributeMethodCandidate.getModifiers().contains(Modifier.ABSTRACT)) {
      type.isEqualToDefined(true);
      return;
    }

    if (name.contentEquals(HASH_CODE_METHOD)
        && parameters.isEmpty()
        && !attributeMethodCandidate.getModifiers().contains(Modifier.ABSTRACT)) {
      type.isHashCodeDefined(true);
      return;
    }

    if (name.contentEquals(TO_STRING_METHOD)
        && parameters.isEmpty()
        && !attributeMethodCandidate.getModifiers().contains(Modifier.ABSTRACT)) {
      type.isToStringDefined(true);
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
        type.validationMethodName(attributeMethodCandidate.getSimpleName().toString());
      } else {
        error(attributeMethodCandidate,
            "Method '%s' annotated with @%s must be non-private parameter-less method and have void return type.",
            attributeMethodCandidate.getSimpleName(),
            Value.Check.class.getSimpleName());
      }
    }

    if (isDiscoveredAttribute(attributeMethodCandidate)) {
      TypeMirror returnType = attributeMethodCandidate.getReturnType();

      DiscoveredAttributes.Builder attributeBuilder = DiscoveredAttributes.builder();

      if (isAbstract(attributeMethodCandidate)) {
        attributeBuilder.isGenerateAbstract(true);
        if (attributeMethodCandidate.getDefaultValue() != null) {
          attributeBuilder.isGenerateDefault(true);
        }
      } else if (hasAnnotation(attributeMethodCandidate, Value.Default.class)) {
        attributeBuilder.isGenerateDefault(true);
      } else if (hasAnnotation(attributeMethodCandidate, Value.Derived.class)) {
        attributeBuilder.isGenerateDerived(true);
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
          attributeBuilder.isGenerateLazy(true);
        }
      }

      attributeBuilder.internalName(name.toString());
      attributeBuilder.internalTypeName(returnType.toString());
      attributeBuilder.internalTypeMirror(returnType);

      DiscoveredAttribute generateAttribute = attributeBuilder.build();
      generateAttribute.setAttributeElement(attributeMethodCandidate);

      type.addAttributes(generateAttribute);
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
