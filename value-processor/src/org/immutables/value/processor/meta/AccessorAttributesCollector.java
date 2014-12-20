/*
    Copyright 2014 Immutables Authors and Contributors

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

import com.google.common.collect.Lists;
import org.immutables.generator.SourceOrdering;
import org.immutables.value.Value;
import org.immutables.value.processor.meta.Proto.Protoclass;

import javax.annotation.Nullable;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.*;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;

final class AccessorAttributesCollector {
  /**
   * Something less than half of 255 parameter limit in java methods (not counting 2-slot double
   * and long parameters and reserved slots for technical parameters).
   */
  private static final int USEFUL_PARAMETER_COUNT_LIMIT = 120;
  private static final String ORDINAL_VALUE_TYPE = "org.immutables.common.collect.OrdinalValue";

  private static final String EQUALS_METHOD = "equals";
  private static final String TO_STRING_METHOD = "toString";
  private static final String HASH_CODE_METHOD = "hashCode";

  private final Protoclass protoclass;
  private final ValueType type;
  private final ProcessingEnvironment processing;
  private final List<ValueAttribute> attributes = Lists.newArrayList();
  private final Styles styles;
  private final Reporter reporter;

  AccessorAttributesCollector(Protoclass protoclass, ValueType type) {
    this.protoclass = protoclass;
    this.processing = protoclass.processing();
    this.styles = protoclass.styles();
    this.type = type;
    this.reporter = protoclass.report();
  }

  void collect() {
    collectGeneratedCandidateMethods((TypeElement) type.element);

    if (attributes.size() > USEFUL_PARAMETER_COUNT_LIMIT) {
      ArrayList<ValueAttribute> list = Lists.newArrayListWithCapacity(USEFUL_PARAMETER_COUNT_LIMIT);
      list.addAll(attributes);
      attributes.clear();
      attributes.addAll(list);

      protoclass.report().error(
          "Value objects with more than %d attributes (including inherited) are not supported."
              + " Please decompose '%s' class into a smaller ones",
          USEFUL_PARAMETER_COUNT_LIMIT,
          protoclass.sourceElement().getQualifiedName());
    }

    for (ValueAttribute attribute : attributes) {
      attribute.initAndValidate();
    }

    type.attributes.addAll(attributes);
  }

  private void collectGeneratedCandidateMethods(TypeElement type) {
    for (Element element : getAccessorsInSourceOrder(type)) {
      if (isElegibleAccessorMethod(element)) {
        processGenerationCandidateMethod((ExecutableElement) element);
      }
    }
    // Now we pass only explicitly defined equals, hashCode, toString
    for (ExecutableElement element : ElementFilter.methodsIn(type.getEnclosedElements())) {
      switch (element.getSimpleName().toString()) {
      case EQUALS_METHOD:
      case HASH_CODE_METHOD:
      case TO_STRING_METHOD:
        processGenerationCandidateMethod(element);
        break;
      default:
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
    case HASH_CODE_METHOD:
    case TO_STRING_METHOD:
      return false;
    default:
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

  private void processGenerationCandidateMethod(ExecutableElement attributeMethodCandidate) {
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

    @Nullable Value.Check validateAnnotation = attributeMethodCandidate.getAnnotation(Value.Check.class);
    if (validateAnnotation != null) {
      if (attributeMethodCandidate.getReturnType().getKind() == TypeKind.VOID
          && attributeMethodCandidate.getParameters().isEmpty()
          && !attributeMethodCandidate.getModifiers().contains(Modifier.PRIVATE)
          && !attributeMethodCandidate.getModifiers().contains(Modifier.ABSTRACT)
          && !attributeMethodCandidate.getModifiers().contains(Modifier.STATIC)
          && !attributeMethodCandidate.getModifiers().contains(Modifier.NATIVE)) {
        type.validationMethodName = attributeMethodCandidate.getSimpleName().toString();
      } else {
        report(attributeMethodCandidate)
            .error("Method '%s' annotated with @%s must be non-private parameter-less method and have void return type.",
                attributeMethodCandidate.getSimpleName(),
                Value.Check.class.getSimpleName());
      }
    }

    if (isDiscoveredAttribute(attributeMethodCandidate)) {
      TypeMirror returnType = attributeMethodCandidate.getReturnType();

      ValueAttribute attribute = new ValueAttribute();

      boolean isFinal = isFinal(attributeMethodCandidate);
      boolean isAbstract = isAbstract(attributeMethodCandidate);
      boolean defaultAnnotationPresent = hasAnnotation(attributeMethodCandidate, Value.Default.class);
      boolean derivedAnnotationPresent = hasAnnotation(attributeMethodCandidate, Value.Derived.class);

      if (isAbstract) {
        attribute.isGenerateAbstract = true;
        if (attributeMethodCandidate.getDefaultValue() != null) {
          attribute.isGenerateDefault = true;
        } else if (defaultAnnotationPresent) {
          report(attributeMethodCandidate)
              .forAnnotation(Value.Default.class)
              .error("@Value.Default should have initializer body", name);
        } else if (derivedAnnotationPresent) {
          report(attributeMethodCandidate)
              .forAnnotation(Value.Derived.class)
              .error("@Value.Derived should have initializer body", name);
        }
      } else if (defaultAnnotationPresent && derivedAnnotationPresent) {
        report(attributeMethodCandidate)
            .forAnnotation(Value.Derived.class)
            .error("Attribute '%s' cannot be both @Value.Default and @Value.Derived", name);
        attribute.isGenerateDefault = true;
        attribute.isGenerateDerived = false;
      } else if ((defaultAnnotationPresent || derivedAnnotationPresent) && isFinal) {
        report(attributeMethodCandidate)
            .error("Annotated attribute '%s' will be overriden and cannot be final", name);
      } else if (defaultAnnotationPresent) {
        attribute.isGenerateDefault = true;
      } else if (derivedAnnotationPresent) {
        attribute.isGenerateDerived = true;
      }

      if (hasAnnotation(attributeMethodCandidate, Value.Lazy.class)) {
        if (isAbstract || isFinal) {
          report(attributeMethodCandidate)
              .error("@Value.Lazy attribute '%s' must be non abstract and non-final", name);
        } else if (defaultAnnotationPresent || derivedAnnotationPresent) {
          report(attributeMethodCandidate)
              .error("@Value.Lazy attribute '%s' cannot be @Value.Derived or @Value.Default", name);
        } else {
          attribute.isGenerateLazy = true;
        }
      }

      attribute.reporter = reporter;
      attribute.processing = processing;
      attribute.returnTypeName = returnType.toString();
      attribute.returnType = returnType;
      attribute.names = styles.forAccessor(name.toString());
      attribute.element = attributeMethodCandidate;
      attributes.add(attribute);
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

  private Reporter report(Element type) {
    return Reporter.from(protoclass.processing()).withElement(type);
  }
}
