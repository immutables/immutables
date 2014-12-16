package org.immutables.value.processor.meta;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import javax.annotation.Nullable;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.Name;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.ElementFilter;
import org.immutables.generator.SourceOrdering;
import org.immutables.value.Value;
import com.google.common.collect.Lists;
import org.immutables.value.processor.meta.Proto.Protoclass;

final class AttributesCollector {
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

  AttributesCollector(Protoclass protoclass, ValueType type) {
    this.protoclass = protoclass;
    this.processing = protoclass.processing();
    this.styles = protoclass.styles();
    this.type = type;
    this.reporter = protoclass.report();
  }

  void collect() {
    collectGeneratedCandidateMethods(type.element);

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
/*!!  if (hasAnnotation(attributeMethodCandidate, GeneratePredicate.class)
          && returnType.getKind() == TypeKind.BOOLEAN) {
        attributeBuilder.isGeneratePredicate(true);
      } else if (hasAnnotation(attributeMethodCandidate, GenerateFunction.class)) {
        attributeBuilder.isGenerateFunction(true);
      }
*/
      if (hasAnnotation(attributeMethodCandidate, Value.Lazy.class)) {
        if (isAbstract(attributeMethodCandidate) || isFinal(attributeMethodCandidate)) {
          report(attributeMethodCandidate)
              .error("Method '%s' annotated with @%s must be non abstract and non-final",
                  attributeMethodCandidate.getSimpleName(),
                  Value.Lazy.class.getSimpleName());
        } else {
          attribute.isGenerateLazy = true;
        }
      }

      attribute.reporter = reporter;
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
