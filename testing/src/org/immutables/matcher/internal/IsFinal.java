package org.immutables.matcher.internal;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;

/**
 * Check that the method can't be overrided.
 */
public class IsFinal extends BaseMatcher<Object> {
  @Override
  public boolean matches(final Object item) {
    if (Method.class.isAssignableFrom(item.getClass())) {
      return Modifier.isFinal(((Method) item).getModifiers());
    } else if (Field.class.isAssignableFrom(item.getClass())) {
      return Modifier.isFinal(((Field) item).getModifiers());
    } else {
      throw new IllegalArgumentException("Wrong type! Method or Field allowed");
    }
  }

  @Override
  public void describeTo(final Description description) {
    description.appendText("final");
  }
}
