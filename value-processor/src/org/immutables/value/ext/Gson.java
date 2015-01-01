package org.immutables.value.ext;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

public @interface Gson {
  /** Instructs generator to generate Gson marshaler based on streaming parser/generator. */
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target({ElementType.TYPE})
  public @interface Streamed {}
}
