/*
   Copyright 2014-2025 Immutables Authors and Contributors

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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.annotation.Nullable;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.TypeElement;
import com.google.common.base.Function;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.immutables.generator.AnnotationMirrors;

final class Annotations {
  private Annotations() {}

  private static final String PREFIX_JAVA_LANG = "java.lang.";
  private static final String PREFIX_IMMUTABLES = "org.immutables.";
  private static final String PREFIX_JACKSON = UnshadeJackson.qualify("");
  private static final String PREFIX_JACKSON_IGNORE_PROPERTIES =
      UnshadeJackson.qualify("annotation.JsonIgnoreProperties");
  static final String JACKSON_PROPERTY =
      UnshadeJackson.qualify("annotation.JsonProperty");

  static final String JACKSON_ANY_GETTER =
      UnshadeJackson.qualify("annotation.JsonAnyGetter");

  static final String JACKSON_VALUE =
      UnshadeJackson.qualify("annotation.JsonValue");

  static final String JAVAX_NULLABLE = "javax.annotation.Nullable";
  static final String JAVAX_CHECK_FOR_NULL = "javax.annotation.CheckForNull";
  static final String JAKARTA_NULLABLE = "jakarta.annotation.Nullable";
  static final String JAKARTA_CHECK_FOR_NULL = "jakarta.annotation.CheckForNull";

  static List<CharSequence> getAnnotationLines(
      Element element,
      Set<String> includeAnnotations,
      boolean includeJacksonAnnotations,
      ElementType elementType,
      Function<String, String> importsResolver,
      @Nullable NullabilityAnnotationInfo nullability) {
    return getAnnotationLines(element,
        includeAnnotations,
        false,
        includeJacksonAnnotations,
        elementType,
        importsResolver,
        nullability);
  }

  static List<CharSequence> getAnnotationLines(
      Element element,
      Set<String> includeAnnotations,
      boolean includeAllAnnotations,
      boolean includeJacksonAnnotations,
      ElementType elementType,
      Function<String, String> importsResolver,
      @Nullable NullabilityAnnotationInfo nullability) {
    List<CharSequence> lines = Lists.newArrayList();

    Set<String> seenAnnotations = new HashSet<>();
    for (AnnotationMirror annotation : element.getAnnotationMirrors()) {
      TypeElement annotationElement = (TypeElement) annotation.getAnnotationType().asElement();

      if (annotationTypeMatches(element,
          annotationElement,
          includeAnnotations,
          includeAllAnnotations,
          includeJacksonAnnotations,
          seenAnnotations,
          lines,
          importsResolver,
          elementType,
          nullability)
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
      Set<String> seenAnnotations,
      List<CharSequence> lines,
      Function<String, String> importsResolver,
      ElementType elementType,
      @Nullable NullabilityAnnotationInfo nullability) {
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

    // Includes meta annotated jackson annotations
    if (includeJacksonAnnotations
        || (includeAnnotations.size() == 1
            && hasJacksonPackagePrefix(Iterables.getOnlyElement(includeAnnotations)))) {
      if (Proto.isAnnotatedWith(annotationElement, Proto.JACKSON_ANNOTATIONS_INSIDE)) {
        for (AnnotationMirror metaAnnotation : annotationElement.getAnnotationMirrors()) {
          TypeElement metaAnnotationElement = (TypeElement) metaAnnotation.getAnnotationType().asElement();
          String qname = metaAnnotationElement.getQualifiedName().toString();

          if (seenAnnotations.contains(qname)) {
            continue;
          }
          seenAnnotations.add(qname);

          if (qname.equals(Proto.JACKSON_ANNOTATIONS_INSIDE)
              || qname.startsWith(PREFIX_IMMUTABLES)
              || qname.startsWith(PREFIX_JAVA_LANG)) {
            continue;
          }

          if (qname.equals(JACKSON_ANY_GETTER)) {
            if (!includeAnnotations.contains(JACKSON_ANY_GETTER)) {
              continue;
            }
          }

          if (qname.equals(JACKSON_VALUE)) {
            if (!includeAnnotations.contains(JACKSON_VALUE)) {
              continue;
            }
          }

          if (annotationMatchesTarget(metaAnnotationElement, elementType)) {
            lines.add(AnnotationMirrors.toCharSequence(metaAnnotation, importsResolver));
          }
        }
        return includeAllAnnotations;
      }
    }

    if (nullability != null && annotationElement.getQualifiedName().contentEquals(nullability.qualifiedName())) {
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
      // Any getter should be handled separately
      if (qualifiedName.equals(JACKSON_ANY_GETTER)) {
        return false;
      }
      // handled separately
      if (qualifiedName.equals(JACKSON_VALUE)) {
        return false;
      }
      if (hasJacksonPackagePrefix(qualifiedName)) {
        return true;
      }
    }

    if (includeAnnotations.contains(qualifiedName)) {
      return true;
    }

    if (includeJacksonAnnotations || !includeAnnotations.isEmpty()) {
      for (AnnotationMirror parentAnnotation : annotationElement.getAnnotationMirrors()) {
        TypeElement parentElement = (TypeElement) parentAnnotation.getAnnotationType().asElement();
        Set<String> throwawaySeenForParent = new HashSet<>(seenAnnotations);
        // This block of code can include annotation if it's parent annotation is included
        if (annotationTypeMatches(element,
            parentElement,
            includeAnnotations,
            false,
            includeJacksonAnnotations,
            throwawaySeenForParent,
            lines,
            importsResolver,
            elementType,
            nullability)) {
          return true;
        }
      }
    }
    return false;
  }

  static boolean annotationMatchesTarget(Element annotationElement, ElementType elementType) {
    @Nullable Target target = annotationElement.getAnnotation(Target.class);
    if (target != null) {
      for (ElementType t : target.value()) {
        if (t == elementType) return true;
      }
      return false;
    }
    return true;
  }

  static boolean hasJacksonPackagePrefix(String qualifiedName) {
    return qualifiedName.startsWith(PREFIX_JACKSON);
  }
}
