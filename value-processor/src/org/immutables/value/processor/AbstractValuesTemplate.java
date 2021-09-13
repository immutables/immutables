package org.immutables.value.processor;

import java.lang.annotation.Inherited;
import java.util.HashSet;
import javax.annotation.Nullable;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import org.immutables.generator.AbstractTemplate;
import org.immutables.generator.Generator;
import org.immutables.generator.Templates;
import org.immutables.value.processor.meta.HasStyleInfo;
import org.immutables.value.processor.meta.LongBits;
import org.immutables.value.processor.meta.ObscureFeatures;
import org.immutables.value.processor.meta.Proto.DeclaringPackage;
import org.immutables.value.processor.meta.StyleInfo;
import org.immutables.value.processor.meta.UnshadeGuava;
import org.immutables.value.processor.meta.ValueAttribute;
import org.immutables.value.processor.meta.ValueType;

/** Groups typedefs and useful utilities. */
public abstract class AbstractValuesTemplate extends AbstractTemplate {
  @Generator.Typedef
  protected ValueType Type;

  @Generator.Typedef
  protected ValueAttribute Attribute;

  @Generator.Typedef
  protected HasStyleInfo HasStyleInfo;

  @Generator.Typedef
  protected LongBits.LongPositions LongPositions;

  @Generator.Typedef
  protected LongBits.BitPosition BitPosition;

  @Generator.Typedef
  protected DeclaringPackage Package;

  protected final ImmutableList<ValueAttribute> noAttributes = ImmutableList.of();

  protected final String guava = UnshadeGuava.prefix();

  protected final LongBits longsFor = new LongBits();

  protected final Function<Object, String> asDiamond = new Function<Object, String>() {
    @Override
    public String apply(Object input) {
      return ObscureFeatures.noDiamonds() ? ("<" + input + ">") : "<>";
    }
  };

  protected final Function<Object, String> docEscaped = new Function<Object, String>() {
    @Override
    public String apply(Object input) {
      return input.toString()
          .replace("<", "&lt;")
          .replace(">", "&gt;")
          .replace("&", "&amp;")
          .replace("java.lang.", "")
          .replace("java.util.", "");
    }
  };

  protected final Templates.Binary<HasStyleInfo, String, Boolean> allowsClasspathAnnotation =
      (hasStyle, annotationClass) -> {
        StyleInfo style = hasStyle.style();
        return (style.allowedClasspathAnnotationsNames().isEmpty()
            || style.allowedClasspathAnnotationsNames().contains(annotationClass))
            && classpath.available.apply(annotationClass);
      };

  protected final Function<HasStyleInfo, String> atFallbackNullable =
      input -> {
        String annotation = input.style().fallbackNullableAnnotationName();
        if (annotation.equals(Inherited.class.getCanonicalName())) {
          String defaultNullable = "javax.annotation.Nullable";
          annotation = allowsClasspathAnnotation.apply(input, defaultNullable) ? defaultNullable : "";
        }
        return !annotation.isEmpty() ? "@" + annotation + " " : "";
      };

  protected final Flag flag = new Flag();

  protected static class Flag {
    public boolean is;

    public String set() {
      this.is = true;
      return "";
    }

    public String clear() {
      this.is = false;
      return "";
    }
  }

  public TrackingSet newTrackingSet() {
    return new TrackingSet();
  }

  public static class TrackingSet {
    private final HashSet<Object> set = new HashSet<>();
    public final Predicate<Object> add = new Predicate<Object>() {
      @Override public boolean apply(@Nullable Object input) {
        return set.add(input);
      }
    };
    public final Predicate<Object> includes = new Predicate<Object>() {
      @Override public boolean apply(@Nullable Object input) {
        return set.contains(input);
      }
    };
    public final Predicate<Object> excludes = new Predicate<Object>() {
      @Override public boolean apply(@Nullable Object input) {
        return !set.contains(input);
      }
    };
  }
}
