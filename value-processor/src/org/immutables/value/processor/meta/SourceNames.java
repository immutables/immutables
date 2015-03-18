/*
    Copyright 2015 Immutables Authors and Contributors

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
import java.util.Collections;
import java.util.List;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.PackageElement;
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
    ElementKind kind = element.getKind();
    return kind.isClass()
        || kind.isInterface();
  }

  static String parentPackageName(PackageElement element) {
    String qualifiedName = element.getQualifiedName().toString();
    int lastIndexOfDot = qualifiedName.lastIndexOf('.');
    if (lastIndexOfDot > 0) {
      return qualifiedName.substring(0, lastIndexOfDot);
    }
    return "";
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
