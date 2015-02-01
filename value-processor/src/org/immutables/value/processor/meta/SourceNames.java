package org.immutables.value.processor.meta;

import java.util.Collections;
import java.util.List;
import javax.lang.model.element.ElementKind;
import com.google.common.base.Joiner;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

final class SourceNames {
  private SourceNames() {}

  static String sourceQualifiedNameFor(Element element) {
    if (isTypeElement(element)) {
      return ((TypeElement) element).getQualifiedName().toString();
    }
    Element enclosingElement = element.getEnclosingElement();
    if (isTypeElement(enclosingElement)) {
      return Joiner.on('.').join(
          ((TypeElement) enclosingElement).getQualifiedName(),
          element.getSimpleName());
    }
    return element.getSimpleName().toString();
  }

  private static boolean isTypeElement(Element element) {
    return element.getKind().isClass() || element.getKind().isInterface();
  }

  static Element collectClassSegments(Element start, List<String> classSegments) {
    Element e = start;
    for (; e.getKind() != ElementKind.PACKAGE; e = e.getEnclosingElement()) {
      classSegments.add(e.getSimpleName().toString());
    }
    Collections.reverse(classSegments);
    return e;
  }

}
