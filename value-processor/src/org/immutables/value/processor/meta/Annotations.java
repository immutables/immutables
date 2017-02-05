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

import javax.lang.model.element.ElementKind;
import com.google.common.base.Function;
import com.google.common.collect.Lists;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import org.immutables.generator.AnnotationMirrors;

final class Annotations {
  private Annotations() {}

  private static final String PREFIX_JAVA_LANG = "java.lang.";
  private static final String PREFIX_IMMUTABLES = "org.immutables.";
  private static final String PREFIX_JACKSON = "com.fasterxml.jackson.annotation.";
  private static final String PREFIX_JACKSON_DATABIND = "com.fasterxml.jackson.databind.annotation.";
  private static final String PREFIX_JACKSON_IGNORE_PROPERTIES =
      "com.fasterxml.jackson.annotation.JsonIgnoreProperties";

  static final String JACKSON_ANY_GETTER =
      "com.fasterxml.jackson.annotation.JsonAnyGetter";

  static final String NULLABLE_SIMPLE_NAME = "Nullable";

  static List<CharSequence> getAnnotationLines(
      Element element,
      Set<String> includeAnnotations,
      boolean includeJacksonAnnotations,
      ElementType elementType,
      Function<String, String> importsResolver) {
    return getAnnotationLines(element,
        includeAnnotations,
        false,
        includeJacksonAnnotations,
        elementType,
        importsResolver);
  }

  static List<CharSequence> getAnnotationLines(
      Element element,
      Set<String> includeAnnotations,
      boolean includeAllAnnotations,
      boolean includeJacksonAnnotations,
      ElementType elementType,
      Function<String, String> importsResolver) {
    List<CharSequence> lines = Lists.newArrayList();

    Set<String> seenAnnotations = new HashSet<>();
    for (AnnotationMirror annotation : element.getAnnotationMirrors()) {
      TypeElement annotationElement = (TypeElement) annotation.getAnnotationType().asElement();

      if (annotationTypeMatches(element, annotationElement,
          includeAnnotations,
          includeAllAnnotations,
          includeJacksonAnnotations,
          seenAnnotations)
          && annotationMatchesTarget(annotationElement, elementType)) {
        lines.add(AnnotationMirrors.toCharSequence(annotation, importsResolver));
      }
    }
    return lines;
  }

  private static boolean annotationTypeMatches(
      Element element,
      TypeElement annotationElement,
      Set<String> includeAnnotations,
      boolean includeAllAnnotations,
      boolean includeJacksonAnnotations,
      Set<String> seenAnnotations) {
    String qualifiedName = annotationElement.getQualifiedName().toString();

    if (seenAnnotations.contains(qualifiedName)
        || qualifiedName.startsWith(PREFIX_IMMUTABLES)
        || qualifiedName.startsWith(PREFIX_JAVA_LANG)) {
      // skip immutables and core java annotations (like Override etc)
      // Also skip JsonProperty annotation as we will add it separately
      // also skip any we've already seen, since we're recursing.
      return false;
    }

    seenAnnotations.add(qualifiedName);

    if (annotationElement.getSimpleName().contentEquals(NULLABLE_SIMPLE_NAME)) {
      // we expect to propagate nullability separately
      return false;
    }

    if (includeAllAnnotations) {
      return true;
    }

    if (qualifiedName.equals(PREFIX_JACKSON_IGNORE_PROPERTIES)) {
      // this is just very often used exception
      // but preferred way is to use additionalJsonAnnotations style attribute.
      return true;
    }

    if (includeJacksonAnnotations) {
      if (element.getKind() != ElementKind.METHOD) {
        if (qualifiedName.equals(Proto.JACKSON_DESERIALIZE) || qualifiedName.equals(Proto.JACKSON_SERIALIZE)) {
          return false;
        }
      }
      if (qualifiedName.startsWith(PREFIX_JACKSON) || qualifiedName.startsWith(PREFIX_JACKSON_DATABIND)) {
        return true;
      }
    }

    if (includeAnnotations.contains(qualifiedName)) {
      return true;
    }

    // This block of code can include annotation if it's parent annotation is included
    if (includeJacksonAnnotations || !includeAnnotations.isEmpty()) {
      for (AnnotationMirror parentAnnotation : annotationElement.getAnnotationMirrors()) {
        TypeElement parentElement = (TypeElement) parentAnnotation.getAnnotationType().asElement();
        if (annotationTypeMatches(element,
            parentElement,
            includeAnnotations,
            false,
            includeJacksonAnnotations,
            seenAnnotations)) {
          return true;
        }
      }
    }
    return false;
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
