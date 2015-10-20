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

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.List;
import java.util.Set;

import javax.annotation.Nullable;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;

import org.immutables.generator.AnnotationMirrors;

import com.google.common.collect.Lists;

final class Annotations {
  private Annotations() {}

  private static final String PREFIX_JAVA_LANG = "java.lang.";
  private static final String PREFIX_IMMUTABLES = "org.immutables.";
  private static final String PREFIX_JACKSON = "com.fasterxml.jackson.annotation.";
  private static final String PREFIX_JACKSON_DATABIND = "com.fasterxml.jackson.databind.annotation.";
  private static final String PREFIX_JACKSON_IGNORE_PROPERTIES =
      "com.fasterxml.jackson.annotation.JsonIgnoreProperties";

  static final String NULLABLE_SIMPLE_NAME = "Nullable";

  static List<CharSequence> getAnnotationLines(
      Element element,
      Set<String> includeAnnotations,
      boolean includeJacksonAnnotations,
      ElementType elementType) {
    List<CharSequence> lines = Lists.newArrayList();

    for (AnnotationMirror annotation : element.getAnnotationMirrors()) {
      TypeElement annotationElement = (TypeElement) annotation.getAnnotationType().asElement();

      if (annotationTypeMatches(annotationElement, includeAnnotations, includeJacksonAnnotations)
          && annotationMatchesTarget(annotationElement, elementType)) {
        lines.add(AnnotationMirrors.toCharSequence(annotation));
      }
    }
    return lines;
  }

  private static boolean annotationTypeMatches(
      TypeElement annotationElement,
      Set<String> includeAnnotations,
      boolean includeJacksonAnnotations) {
    String qualifiedName = annotationElement.getQualifiedName().toString();

    if (qualifiedName.startsWith(PREFIX_IMMUTABLES)
        || qualifiedName.startsWith(PREFIX_JAVA_LANG)) {
      // skip immutables and core java annotations (like Override etc)
      // Also skip JsonProperty annotation as we will add it separately
      return false;
    }

    if (annotationElement.getSimpleName().contentEquals(NULLABLE_SIMPLE_NAME)) {
      // we expect to propagate nullability separately
      return false;
    }
    
    if (qualifiedName.equals(PREFIX_JACKSON_IGNORE_PROPERTIES)) {
      // this is just very often used exception
      // but preferred way is to use additionalJsonAnnotations style attribute.
      return true;
    }

    if (includeJacksonAnnotations
        && (qualifiedName.startsWith(PREFIX_JACKSON) || qualifiedName.startsWith(PREFIX_JACKSON_DATABIND))) {
      return true;
    }

    return includeAnnotations.contains(qualifiedName);
  }

  static boolean annotationMatchesTarget(Element annotationElement, ElementType elementType) {
    @Nullable Target target = annotationElement.getAnnotation(Target.class);
    if (target != null) {
      ElementType[] targetTypes = target.value();
      if (targetTypes.length == 0) {
        return false;
      }
      boolean found = false;
      for (ElementType t : targetTypes) {
        if (t == elementType) {
          found = true;
        }
      }
      if (!found) {
        return false;
      }
    }
    return true;
  }
}
