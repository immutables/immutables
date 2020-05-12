package org.immutables.value.processor.meta;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Joiner.MapJoiner;
import com.google.common.collect.FluentIterable;
import com.google.common.collect.ImmutableMap;
import java.lang.annotation.Annotation;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import javax.annotation.Nullable;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import org.immutables.generator.AnnotationMirrors;
import org.immutables.generator.StringLiterals;
import org.immutables.mirror.Mirror;
import org.immutables.value.processor.meta.AnnotationInjections.InjectAnnotation.Where;
import org.immutables.value.processor.meta.Proto.Environment;

public final class AnnotationInjections {
  private static final String P_ANNOTATION = "@";
  private static final String P_R = "]]";
  private static final String P_L = "[[";
  private static final String P_ALL = "[[*]]";
  private static final String P_SIMPLE_NAME = "[[!name]]";
  private static final String P_ALL_NAMES = "[[*names]]";
  private static final Joiner COMMA_JOINER = Joiner.on(", ");
  private static final MapJoiner ATTR_JOINER = COMMA_JOINER.withKeyValueSeparator(" = ");

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
      IMMUTABLE_TYPE,
      MODIFIABLE_TYPE,
      CONSTRUCTOR
    }
  }
  
  @Mirror.Annotation("org.immutables.annotate.InjectManyAnnotations")
  public @interface InjectManyAnnotations {
    InjectAnnotation[] value();
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

  @SafeVarargs
  public static Collection<String> collectInjections(
      Element element,
      Where target,
      Collection<String> attributeNames,
      Iterable<AnnotationInjection>... injections) {
    Map<String, String> injectionCode = new HashMap<>();
    for (Iterable<AnnotationInjection> inj : injections) {
      for (AnnotationInjection a : inj) {
        a.addIfApplicable(element, target, attributeNames, injectionCode);
      }
    }
    return injectionCode.values();
  }

  public static final class AnnotationInjection {
    private final InjectionInfo info;
    private final ImmutableMap<String, String> literals;

    AnnotationInjection(InjectionInfo info, ImmutableMap<String, String> literals) {
      this.info = info;
      this.literals = literals;
    }

    void addIfApplicable(
        Element element,
        Where target,
        Collection<String> attributeNames,
        Map<String, String> annotationCode) {
      String simpleName = element.getSimpleName().toString();

      if (annotationCode.containsKey(info.deduplicationKey)) {
        return;
      }

      if (!info.targets.contains(target)) {
        return;
      }

      if (info.ifPresent && !info.annotationType.isEmpty()) {
        @Nullable AnnotationMirror presentAnnotation =
            AnnotationMirrors.findAnnotation(element.getAnnotationMirrors(), info.annotationType);

        if (presentAnnotation == null) {
          // annotation not present, not applicable
          return;
        }

        if (info.code.isEmpty()) {
          annotationCode.put(
              info.deduplicationKey,
              AnnotationMirrors.toCharSequence(presentAnnotation).toString());
          // No code, then we just copy present annotation
          // otherwise we handle code as usual
          return;
        }
      }

      String code = info.hasPlaceholders
          ? interpolateCode(simpleName, attributeNames)
          : info.code;

      annotationCode.put(
          info.deduplicationKey,
          prependAnnotationTypeIfNecessary(code));
    }

    private String interpolateCode(String simpleName, Collection<String> attributeNames) {
      // The implementation is pretty dumb, sorry: no regex, parsing or fancy libraries
      String c = info.code;

      if (c.isEmpty()) {
        return "(" + ATTR_JOINER.join(literals) + ")";
      }

      if (c.contains(P_ALL)) {
        c = c.replace(P_ALL, ATTR_JOINER.join(literals));
      }

      c = c.replace(P_SIMPLE_NAME, simpleName);

      if (c.contains(P_ALL_NAMES)) {
        String literals = FluentIterable.from(attributeNames)
            .transform(ToLiteral.FUNCTION)
            .join(COMMA_JOINER);

        c = c.replace(P_ALL_NAMES, "{" + literals + "}");
      }

      for (Entry<String, String> e : literals.entrySet()) {
        c = c.replace(P_L + e.getKey() + P_R, e.getValue());
      }
      return c;
    }

    private String prependAnnotationTypeIfNecessary(String code) {
      if (!info.annotationType.isEmpty() && !code.startsWith(P_ANNOTATION)) {
        return P_ANNOTATION + info.annotationType + code;
      }
      return code;
    }
  }

  public static final class InjectionInfo {
    final String code;
    /** empty if injected annotations. */
    final String annotationType;
    final boolean ifPresent;
    final String deduplicationKey;
    final EnumSet<Where> targets;
    final boolean hasPlaceholders;

    private InjectionInfo(
        String code,
        String annotationType,
        String deduplicationKey,
        boolean ifPresent,
        Where[] targets) {
      this.code = code.trim();
      this.annotationType = annotationType;
      this.ifPresent = ifPresent;
      this.targets = targets.length == 0
          ? EnumSet.allOf(Where.class)
          : EnumSet.copyOf(Arrays.asList(targets));
      this.hasPlaceholders = hasPlaceholders(code);
      this.deduplicationKey = deduplicationKeyFor(deduplicationKey, annotationType, code);
    }

    private boolean hasPlaceholders(String code) {
      return code.contains(P_L) && code.contains(P_R);
    }

    AnnotationInjection injectionFor(AnnotationMirror annotation, Environment environment) {
      return new AnnotationInjection(
          this,
          hasPlaceholders
              ? extractPlaceholderValues(annotation, environment)
              : ImmutableMap.<String, String>of());
    }

    private ImmutableMap<String, String> extractPlaceholderValues(
        AnnotationMirror annotation,
        Environment environment) {
      ImmutableMap.Builder<String, String> literals = ImmutableMap.builder();
      for (Entry<? extends ExecutableElement, ? extends AnnotationValue> e : environment.processing()
          .getElementUtils()
          .getElementValuesWithDefaults(CachingElements.getDelegate(annotation))
          .entrySet()) {
        String key = e.getKey().getSimpleName().toString();
        String value = AnnotationMirrors.toCharSequence(e.getValue()).toString();
        literals.put(key, value);
      }
      return literals.build();
    }

    private static String deduplicationKeyFor(String deduplicationKey, String annotationType, String code) {
      if (!deduplicationKey.isEmpty()) {
        return deduplicationKey;
      }
      if (!annotationType.isEmpty()) {
        return annotationType;
      }
      return code;
    }
  }

  private enum ToLiteral implements Function<String, String> {
    FUNCTION;
    @Override
    public String apply(String input) {
      return StringLiterals.toLiteral(input);
    }
  }
}
