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
import java.util.List;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import org.immutables.generator.AnnotationMirrors;

/**
 * The gymnastics that exists only to workaround annotation processors that do not fully print
 * annotation values (like Eclipse compiler)
 */
final class AnnotationPrinting {
  private AnnotationPrinting() {}

  private static final String PREFIX_JAVA_LANG = "@java.lang.";
  private static final String PREFIX_ORG_IMMUTABLES = "@org.immutables.";

  // This one is somehow very Ad Hoc
  private static final String PREFIX_JSON_PROPERTY_ANNOTATION = "@" + JsonPropertyMirror.ANNOTATION_NAME;

  static List<CharSequence> getAnnotationLines(Element element) {
    List<CharSequence> lines = Lists.newArrayList();

    for (AnnotationMirror annotation : element.getAnnotationMirrors()) {
      String string = annotation.toString();
      if (string.startsWith(PREFIX_ORG_IMMUTABLES)
          || string.startsWith(PREFIX_JAVA_LANG)
          || string.startsWith(PREFIX_JSON_PROPERTY_ANNOTATION)) {
        continue;
      }
      lines.add(AnnotationMirrors.toCharSequence(annotation));
    }

    return lines;
  }
}
