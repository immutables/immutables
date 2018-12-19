package org.immutables.value.processor;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import javax.annotation.Nullable;
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

  protected final Function<String, String> typeSnippet = new Function<String, String>() {
    @Override
    public String apply(String input) {
      return input
          .replace("<", "&lt;")
          .replace(">", "&gt;")
          .replace("java.lang.", "")
          .replace("java.util.", "");
    }
  };

  protected final Templates.Binary<HasStyleInfo, String, Boolean> allowsClasspathAnnotation =
      new Templates.Binary<HasStyleInfo, String, Boolean>() {
        @Override
        public Boolean apply(HasStyleInfo hasStyle, String annotationClass) {
          StyleInfo style = hasStyle.style();
          return (style.allowedClasspathAnnotationsNames().isEmpty()
              || style.allowedClasspathAnnotationsNames().contains(annotationClass))
              && classpath.available.apply(annotationClass);
        }
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
}
