package org.immutables.criteria.matcher;

import org.immutables.criteria.expression.Expression;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Objects;

public class Matchers {


  /**
   * Hacky (and temporary) reflection until we define proper sub-classes for criterias
   * (to hide Expressional implementation).
   */
  static Expression extract(Object object) {
    Objects.requireNonNull(object, "object");
    try {
      Class<?> current = object.getClass();
      while(current.getSuperclass() != null){
        if (Arrays.stream(current.getDeclaredFields()).anyMatch(f -> f.getName().equals("context"))) {
          Field field = current.getDeclaredField("context");
          field.setAccessible(true);
          CriteriaContext<?> context = (CriteriaContext<?>) field.get(object);
          return context.expression();
        }
        current = current.getSuperclass();
      }
    } catch (NoSuchFieldException|IllegalAccessException e) {
      throw new RuntimeException("No field in " + object.getClass().getName(), e);
    }

    throw new UnsupportedOperationException("No field context found in " + object.getClass().getName());
  }
}
