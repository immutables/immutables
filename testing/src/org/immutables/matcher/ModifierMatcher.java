package org.immutables.matcher;

import static org.hamcrest.CoreMatchers.not;

import java.lang.reflect.Method;

import org.hamcrest.Matcher;
import org.immutables.matcher.internal.IsFinal;

/**
 * Some utilitary methods that check modifier
 */
public class ModifierMatcher {
  /**
   * Create a matcher for {@link Method} that check it can't be overrided.
   *
   * @return the created matcher
   */
  public static final Matcher<Object> finalModifier() {
    return new IsFinal();
  }

  /**
   * Create a matcher for {@link Method} that check it can be overrided.
   *
   * @return the created matcher
   */
  public static final Matcher<Object> notFinalModifier() {
    return not(finalModifier());
  }

  /**
   * Unreachable constructor
   */
  private ModifierMatcher() {}
}
