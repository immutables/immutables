package org.immutables.value.processor.meta;

import com.google.common.base.Optional;
import java.lang.annotation.Annotation;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.tools.Diagnostic;
import org.immutables.value.Value;

@Value.Immutable(builder = false)
abstract class Reporter {

  @Value.Parameter
  abstract Messager messager();

  abstract Optional<Element> element();

  abstract Optional<AnnotationMirror> annotation();

  abstract Reporter withAnnotation(AnnotationMirror mirror);

  abstract Reporter withElement(Element mirror);

  static Reporter from(ProcessingEnvironment processing) {
    return ImmutableReporter.of(processing.getMessager());
  }

  Reporter forAnnotation(Class<? extends Annotation> annotationType) {
    return annotationNamed(annotationType.getSimpleName());
  }

  Reporter annotationNamed(String simpleName) {
    if (element().isPresent()) {
      for (AnnotationMirror mirror : element().get().getAnnotationMirrors()) {
        if (mirror.getAnnotationType().asElement().getSimpleName().contentEquals(simpleName)) {
          return withAnnotation(mirror);
        }
      }
    }
    return this;
  }

  void error(String message, Object... parameters) {
    reportMessage(Diagnostic.Kind.ERROR, message, parameters);
  }

  void warning(String message, Object... parameters) {
    reportMessage(Diagnostic.Kind.WARNING, message, parameters);
  }

  private void reportMessage(Diagnostic.Kind messageKind, String message, Object... parameters) {
    String formattedMessage = String.format(message, parameters);
    if (element().isPresent() && annotation().isPresent()) {
      messager().printMessage(messageKind, formattedMessage, element().get(), annotation().get());
    } else if (element().isPresent()) {
      messager().printMessage(messageKind, formattedMessage, element().get());
    } else {
      messager().printMessage(messageKind, formattedMessage);
    }
  }
}
