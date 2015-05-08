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
import java.util.ArrayList;
import java.util.List;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.ExecutableType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import org.immutables.generator.SourceOrdering;
import org.immutables.value.processor.meta.Proto.Protoclass;

final class AccessorAttributesCollector {
  private static final String ORG_ECLIPSE = "org.eclipse";

  /**
   * Something less than half of 255 parameter limit in java methods (not counting 2-slot double
   * and long parameters and reserved slots for technical parameters).
   */
  private static final int USEFUL_PARAMETER_COUNT_LIMIT = 120;

  private static final String EQUALS_METHOD = "equals";
  private static final String TO_STRING_METHOD = "toString";
  private static final String HASH_CODE_METHOD = "hashCode";

  private final Protoclass protoclass;
  private final ValueType type;
  private final ProcessingEnvironment processing;
  private final List<ValueAttribute> attributes = Lists.newArrayList();
  private final Styles styles;
  private final Reporter reporter;

  private final boolean isEclipseImplementation;

  AccessorAttributesCollector(Protoclass protoclass, ValueType type) {
    this.protoclass = protoclass;
    this.processing = protoclass.processing();
    this.styles = protoclass.styles();
    this.type = type;
    this.reporter = protoclass.report();
    this.isEclipseImplementation = isEclipseImplementation(type.element);
  }

  void collect() {
    collectGeneratedCandidateMethods(getTypeElement());

    if (attributes.size() > USEFUL_PARAMETER_COUNT_LIMIT) {
      ArrayList<ValueAttribute> list = Lists.newArrayListWithCapacity(USEFUL_PARAMETER_COUNT_LIMIT);
      list.addAll(attributes.subList(0, USEFUL_PARAMETER_COUNT_LIMIT));
      attributes.clear();
      attributes.addAll(list);

      protoclass.report().error(
          "Value objects with more than %d attributes (including inherited) are not supported."
              + " You can decompose '%s' class into a smaller ones",
          USEFUL_PARAMETER_COUNT_LIMIT,
          protoclass.name());
    }

    for (ValueAttribute attribute : attributes) {
      attribute.initAndValidate();
    }

    type.attributes.addAll(attributes);
  }

  private TypeElement getTypeElement() {
    return (TypeElement) type.element;
  }

  private void collectGeneratedCandidateMethods(TypeElement type) {
    for (Element element : getAccessorsInSourceOrder(CachingElements.getDelegate(type))) {
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
        : SourceOrdering.getAllAccessors(processing.getElementUtils(), processing.getTypeUtils(), type);
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
    if (definitionType.startsWith(TypeIntrospectionBase.ORDINAL_VALUE_INTERFACE_TYPE)) {
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
        && parameters.isEmpty()) {
      if (!attributeMethodCandidate.getModifiers().contains(Modifier.ABSTRACT)) {
        type.isHashCodeDefined = true;
      }
      return;
    }

    if (name.contentEquals(TO_STRING_METHOD)
        && parameters.isEmpty()) {
      if (!attributeMethodCandidate.getModifiers().contains(Modifier.ABSTRACT)) {
        type.isToStringDefined = true;
      }
      return;
    }

    if (CheckMirror.isPresent(attributeMethodCandidate)) {
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
                CheckMirror.simpleName());
      }
    }

    if (isDiscoveredAttribute(attributeMethodCandidate)) {
      TypeMirror returnType = resolveReturnType(attributeMethodCandidate);

      ValueAttribute attribute = new ValueAttribute();

      boolean isFinal = isFinal(attributeMethodCandidate);
      boolean isAbstract = isAbstract(attributeMethodCandidate);
      boolean defaultAnnotationPresent = DefaultMirror.isPresent(attributeMethodCandidate);
      boolean derivedAnnotationPresent = DerivedMirror.isPresent(attributeMethodCandidate);

      if (isAbstract) {
        attribute.isGenerateAbstract = true;

        if (attributeMethodCandidate.getDefaultValue() != null) {
          attribute.isGenerateDefault = true;
        }

        if (defaultAnnotationPresent || derivedAnnotationPresent) {
          if (defaultAnnotationPresent) {
            if (attribute.isGenerateDefault) {
              report(attributeMethodCandidate)
                  .annotationNamed(DefaultMirror.simpleName())
                  .warning("@Value.Default annotation is superflous for default annotation attribute");
            } else {
              report(attributeMethodCandidate)
                  .annotationNamed(DefaultMirror.simpleName())
                  .error("@Value.Default attibute should have initializer body", name);
            }
          }
          if (derivedAnnotationPresent) {
            if (attribute.isGenerateDefault) {
              report(attributeMethodCandidate)
                  .annotationNamed(DerivedMirror.simpleName())
                  .error("@Value.Derived cannot be used with default annotation attribute");
            } else {
              report(attributeMethodCandidate)
                  .annotationNamed(DerivedMirror.simpleName())
                  .error("@Value.Derived attibute should have initializer body", name);
            }
          }
        }
      } else if (defaultAnnotationPresent && derivedAnnotationPresent) {
        report(attributeMethodCandidate)
            .annotationNamed(DerivedMirror.simpleName())
            .error("Attribute '%s' cannot be both @Value.Default and @Value.Derived", name);
        attribute.isGenerateDefault = true;
      } else if ((defaultAnnotationPresent || derivedAnnotationPresent) && isFinal) {
        report(attributeMethodCandidate)
            .error("Annotated attribute '%s' will be overriden and cannot be final", name);
      } else if (defaultAnnotationPresent) {
        attribute.isGenerateDefault = true;
      } else if (derivedAnnotationPresent) {
        attribute.isGenerateDerived = true;
      }

      if (LazyMirror.isPresent(attributeMethodCandidate)) {
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
      attribute.returnType = returnType;
      attribute.names = styles.forAccessor(name.toString());
      attribute.element = attributeMethodCandidate;
      attribute.containingType = type;
      attributes.add(attribute);
    }
  }

  private TypeMirror resolveReturnType(ExecutableElement method) {
    method = CachingElements.getDelegate(method);
    TypeMirror returnType = method.getReturnType();

    if (isEclipseImplementation) {
      return returnType;
    }

    // We do not support parametrized accessor methods,
    // but we do support inheriting parametrized accessors, which
    // we supposedly parametrized with actual type parameters as
    // our target class could not define formal type parameters also.
    if (returnType.getKind() == TypeKind.TYPEVAR) {
      return asInheritedMemberReturnType(method);
    } else if (returnType.getKind() == TypeKind.DECLARED
        || returnType.getKind() == TypeKind.ERROR) {
      if (!((DeclaredType) returnType).getTypeArguments().isEmpty()) {
        return asInheritedMemberReturnType(method);
      }
    }
    return returnType;
  }

  private TypeMirror asInheritedMemberReturnType(ExecutableElement method) {
    TypeElement typeElement = getTypeElement();

    ExecutableType asMethodOfType =
        (ExecutableType) processing.getTypeUtils()
            .asMemberOf((DeclaredType) typeElement.asType(), method);

    return asMethodOfType.getReturnType();
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
    return DefaultMirror.isPresent(attributeMethodCandidate)
        || DerivedMirror.isPresent(attributeMethodCandidate)
        || LazyMirror.isPresent(attributeMethodCandidate);
  }

  private Reporter report(Element type) {
    return Reporter.from(protoclass.processing()).withElement(type);
  }

  private static boolean isEclipseImplementation(Element element) {
    return CachingElements.getDelegate(element).getClass().getCanonicalName().startsWith(ORG_ECLIPSE);
  }
}
