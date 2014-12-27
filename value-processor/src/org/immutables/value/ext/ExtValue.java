package org.immutables.value.ext;

import com.google.common.annotations.Beta;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.immutables.value.Value.Nested;

public @interface ExtValue {
  /**
   * Generate visitor for a set of nested classes.Should only be used on {@link Nested} umbrella
   * classes. Then generated *Visitor class used to switch on type trees nested inside
   */
  @Beta
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target(ElementType.TYPE)
  public @interface Visitor {}

  /**
   * Generate transformer for a set of nested classes. Should only be used on {@link Nested}
   * umbrella classes. Then generated *Transformer class used to exploit and refine transformation
   * of immutable graph.
   */
  @Beta
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target(ElementType.TYPE)
  public @interface Transformer {}

}
