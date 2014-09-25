package org.immutables.modeling;

import java.lang.annotation.Inherited;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Retention;

@Retention(RetentionPolicy.SOURCE)
public @interface Template {

  @Inherited
  @Target({ ElementType.TYPE, ElementType.PACKAGE })
  @Retention(RetentionPolicy.SOURCE)
  public @interface Import {
    Class<?>[] value();
  }

  @Target(ElementType.FIELD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Typedef {}
}
