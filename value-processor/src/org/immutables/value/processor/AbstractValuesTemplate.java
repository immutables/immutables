package org.immutables.value.processor;

import org.immutables.generator.AbstractTemplate;
import org.immutables.generator.Generator;
import org.immutables.generator.Templates;
import org.immutables.value.processor.meta.HasStyleInfo;
import org.immutables.value.processor.meta.LongBits;
import org.immutables.value.processor.meta.StyleInfo;
import org.immutables.value.processor.meta.UnshadeGuava;
import org.immutables.value.processor.meta.ValueAttribute;
import org.immutables.value.processor.meta.ValueType;
import org.immutables.value.processor.meta.Proto.DeclaringPackage;

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

  protected final String guava = UnshadeGuava.prefix();

  protected final LongBits longsFor = new LongBits();

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
