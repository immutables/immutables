package org.immutables.value;

import com.google.common.annotations.Beta;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.TYPE)
public @interface Parboil {

  /**
   * Generate parboiled AST building actions and extractors
   */
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target({ ElementType.TYPE })
  @Beta
  public @interface Ast {}
}
