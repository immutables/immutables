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
import org.immutables.value.processor.meta.UnshadeJackson;
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

  protected final String jackson = UnshadeJackson.prefix();

  protected final LongBits longsFor = new LongBits();

  protected final Function<Object, String> asDiamond =
      input -> ObscureFeatures.noDiamonds() ? ("<" + input + ">") : "<>";

  private @Nullable String jaxartaPackage;

  protected final void setJaxarta(String jaxartaPackage) {
    this.jaxartaPackage = jaxartaPackage;
  }

  public final String jaxarta() {
    return jaxartaPackage != null ? jaxartaPackage : inferJaxarta() ? "jakarta" : "javax";
  }

  protected boolean inferJaxarta() {
    return classpath.available.apply(JAKARTA_NULLABLE);
  }

  // Same as in .meta.Annotations.JAKARTA_NULLABLE
  private static final String JAKARTA_NULLABLE = "jakarta.annotation.Nullable";

  protected final Function<Object, String> docEscaped = input -> input.toString()
      .replace("<", "&lt;")
      .replace(">", "&gt;")
      .replace("&", "&amp;")
      .replace("java.lang.", "")
      .replace("java.util.", "");

  protected final Templates.Binary<HasStyleInfo, String, Boolean> allowsClasspathAnnotation =
      (hasStyle, annotationClass) -> {
        StyleInfo style = hasStyle.style();
        setJaxartaFrom(style);

        if (annotationClass.startsWith(CLASSNAME_TAG_JAXARTA)) {
          annotationClass = annotationClass.replace(CLASSNAME_TAG_JAXARTA, jaxarta());
        }
        return (style.allowedClasspathAnnotationsNames().isEmpty()
            || style.allowedClasspathAnnotationsNames().contains(annotationClass))
            && classpath.available.apply(annotationClass);
      };

  private void setJaxartaFrom(StyleInfo style) {
    // setup jakarta context for subsequent calls for jaxarta()
    // regardless if it's relevant for this call
    jaxartaPackage = style.jakarta() ? "jakarta" : "javax";
  }

  protected final Function<HasStyleInfo, String> atForceTypeuseNullable =
      input -> {
        // these are surrounded by spaces, so far, intentionally.
        // primarily to insert between array element type and brackets/(ellipsis for varargs)
        switch (input.fallbackNullableKind()) {
          case JSPECIFY:
            return " @org.jspecify.annotations.Nullable ";
          case SPECIFIED_TYPEUSE:
            return " @" + input.style().fallbackNullableAnnotationName() + " ";
          default:
            return "";
        }
      };

  protected final Function<HasStyleInfo, String> atFallbackNullable =
      input -> {
        String annotation = input.style().fallbackNullableAnnotationName();
        switch (input.fallbackNullableKind()) {
          case JSPECIFY:
            return "/*!typeuse @org.jspecify.annotations.Nullable*/ ";
          case SPECIFIED_TYPEUSE:
            return "/*!typeuse @" + annotation + "*/ ";
          case SPECIFIED: {
            break;
          }
          case UNSPECIFIED: {
            setJaxartaFrom(input.style());
            String defaultNullable = jaxarta() + ".annotation.Nullable";
            annotation = allowsClasspathAnnotation.apply(input, defaultNullable) ? defaultNullable : "";
            break;
          }
          default:
            return "";
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
    public final Predicate<Object> add = set::add;
    public final Predicate<Object> includes = set::contains;
    public final Predicate<Object> excludes = input -> !set.contains(input);
  }

  // this particular style mimics template syntax, but is handled separately
  protected static final String CLASSNAME_TAG_JAXARTA = "[jaxarta]";
}
