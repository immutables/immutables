package org.immutables.mirror;

import java.lang.annotation.Documented;
import com.google.common.annotations.Beta;
import java.lang.annotation.ElementType;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * @see Annotation
 */
@Beta
@Target({})
public @interface Mirror {
  /**
   * Generate annotation mirror handler, by annotation special structurally matching annotation.
   */
  @Beta
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target(ElementType.ANNOTATION_TYPE)
  public @interface Annotation {
    /**
     * Fully qualified canonical name of annotation being modelled.
     * @return fully qualified name.
     */
    String value();
  }
}
