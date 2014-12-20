package org.immutables.generator;

import com.google.common.base.Predicate;
import com.google.common.collect.Maps;
import java.util.Map;
import javax.lang.model.element.TypeElement;

public final class ClasspathAvailability {
  // static non-thread-safe cache? ok!
  private static final Map<String, Boolean> availableClasses = Maps.newHashMap();

  public final Predicate<String> available = new Predicate<String>() {
    @Override
    public boolean apply(String input) {
      /*@Nullable*/Boolean available = availableClasses.get(input);
      if (available == null) {
        TypeElement element = StaticEnvironment.processing()
            .getElementUtils()
            .getTypeElement(input);

        available = element != null;
        availableClasses.put(input, available);
      }

      return available;
    }

    @Override
    public String toString() {
      return "classpath.available";
    }
  };
}
