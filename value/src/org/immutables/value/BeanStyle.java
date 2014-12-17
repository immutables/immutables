package org.immutables.value;

import org.immutables.value.Value.Immutable.ImplementationVisibility;
import com.google.common.annotations.Beta;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Bean related style-customizations. Serve mostly as examples
 */
//@Target({})
public @interface BeanStyle {

  /**
   * Annotations that applies speculative Java Bean-style accessor naming convention
   * to the generate immutable and other derived classes.
   * It works by being annotated with {@litera @}{@link Value.Style} annotation which specifies
   * customized naming templates. This annotation could be placed on a class, surrounding
   * {@link Value.Nested} class or even a package (declared in {@code package-info.java}). This
   * annotation more of example of how to define your own styles as meta-annotation rather than a
   * useful annotation.
   */
  @Beta
  @Value.Style(get = {"is*", "get*"})
  @Target({ElementType.TYPE, ElementType.PACKAGE, ElementType.ANNOTATION_TYPE})
  @Retention(RetentionPolicy.SOURCE)
  public @interface Accessors {}

  /**
   * Special combination of styles: separate top builder, private implementation,
   * set-get prefixes, new builder etc.
   */
  @Beta
  @Value.Style(
      get = {"is*", "get*"},
      init = "set*",
      of = "new*",
      instance = "getInstance",
      builder = "new",
      defaults = @Value.Immutable(visibility = ImplementationVisibility.PRIVATE))
  @Target({ElementType.TYPE, ElementType.PACKAGE, ElementType.ANNOTATION_TYPE})
  @Retention(RetentionPolicy.SOURCE)
  public @interface Conservative {}
}
