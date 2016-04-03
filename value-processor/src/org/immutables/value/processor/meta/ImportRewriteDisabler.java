package org.immutables.value.processor.meta;

import com.google.common.base.Splitter;
import com.google.common.base.Ascii;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;

class ImportRewriteDisabler {
  private static final Splitter DOT_SPLITTER = Splitter.on('.');
  private static final String WARNING_START =
      "Import rewriter will be disabled for generated source files to not mess up with";

  static boolean shouldDisableFor(ValueType type) {
    for (String segment : DOT_SPLITTER.split(type.constitution.implementationPackage())) {
      if (!segment.isEmpty() && Ascii.isUpperCase(segment.charAt(0))) {
        type.constitution.protoclass().report().warning(
            WARNING_START + " uppercase package names");
        return true;
      }
    }

    Element element = type.constitution.protoclass().sourceElement();
    while (element != null) {
      if (element.getKind() == ElementKind.PACKAGE) {
        for (String segment : DOT_SPLITTER.split(((PackageElement) element).getQualifiedName())) {
          if (!segment.isEmpty() && Ascii.isUpperCase(segment.charAt(0))) {
            type.constitution.protoclass().report().warning(
                WARNING_START + " uppercase package names");
            return true;
          }
        }
      }
      if (element.getKind().isClass() || element.getKind().isInterface()) {
        if (Ascii.isLowerCase(element.getSimpleName().charAt(0))) {
          type.constitution.protoclass().report().warning(
              WARNING_START + " lowercase class names");
          return true;
        }
      }
      element = element.getEnclosingElement();
    }

    for (ValueAttribute attribute : type.attributes) {
      if (Ascii.isUpperCase(attribute.names.get.charAt(0))) {
        type.constitution.protoclass().report().warning(
            WARNING_START + " uppercase attribute names");
        return true;
      }
    }

    for (ValueType nested : type.nested) {
      if (shouldDisableFor(nested)) {
        return true;
      }
    }
    return false;
  }
}
