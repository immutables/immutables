package org.immutables.value.processor.meta;

import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Map;
import org.immutables.mirror.Mirror;
import org.immutables.value.processor.meta.AnnotationInjections.InjectAnnotation.Where;

public final class AnnotationInjections {
  private AnnotationInjections() {}

  @Mirror.Annotation("org.immutables.annotate.InjectAnnotation")
  public @interface InjectAnnotation {
    String code() default "";

    Class<? extends Annotation> type() default InjectAnnotation.class;

    boolean ifPresent() default false;

    Where[] target();

    String deduplicationKey() default "";

    enum Where {
      FIELD,
      ACCESSOR,
      SYNTHETIC_FIELDS,
      CONSTRUCTOR_PARAMETER,
      INITIALIZER,
      ELEMENT_INITIALIZER,
      BUILDER_TYPE,
      IMMUTABLE_TYPE
    }
  }

  private static String emptyIfDefault(String annotationType) {
    if (annotationType.equals("org.immutables.annotate.InjectAnnotation") // original default
        || annotationType.equals(InjectAnnotation.class.getName())) { // default from the mirror
      return "";
    }
    return annotationType;
  }

  public static InjectionInfo infoFrom(InjectAnnotationMirror mirror) {
    return new InjectionInfo(
        mirror.code(),
        emptyIfDefault(mirror.typeName()),
        mirror.deduplicationKey(),
        mirror.ifPresent(),
        mirror.target());
  }

  public static final class InjectionInfo {
    final String code;
    /** empty if injected annotations. */
    final String annotationType;
    final boolean ifPresent;
    final String deduplicationKey;
    final EnumSet<Where> targets;

    private InjectionInfo(
        String code,
        String annotationType,
        String deduplicationKey,
        boolean ifPresent,
        Where[] targets) {
      this.code = code;
      this.annotationType = annotationType;
      this.ifPresent = ifPresent;
      this.targets = targets.length == 0
          ? EnumSet.allOf(Where.class)
          : EnumSet.copyOf(Arrays.asList(targets));

      this.deduplicationKey = deduplicationKeyFor(deduplicationKey, annotationType, code);
    }

    private String deduplicationKeyFor(String deduplicationKey, String annotationType, String code) {
      if (!deduplicationKey.isEmpty()) {
        return deduplicationKey;
      }
      if (!annotationType.isEmpty()) {
        return annotationType;
      }
      return code;
    }

    void collectApplicable(Where target, Map<String, String> annotations) {
      if (annotations.containsKey(deduplicationKey)
          || !targets.contains(target)) {
        return;
      }
    }
  }
}
