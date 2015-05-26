package org.immutables.cases;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Umbrella annotation which groups nested annotation for generation of visitor-like constructs for
 * ADT-like case classes.
 */
@Target({})
public @interface Cases {
  @Target({ElementType.PACKAGE, ElementType.TYPE})
  public @interface Chain {

  }
}
