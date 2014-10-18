package org.immutables.generator;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import javax.annotation.processing.SupportedAnnotationTypes;

/**
 * Namespasing annotation used to group nested Generator - related annotations.
 */
@Retention(RetentionPolicy.SOURCE)
public @interface Generator {

  /**
   * Generates "generator" subclass of annotated template definition.
   * It is recommended in most cases that annotated class also extend {@link AbstractTemplate} so
   * default templating language capabilities will be accessible.
   */
  @Documented
  @Target(ElementType.TYPE)
  public @interface Template {}

  /**
   * Imports classes as simple name aliases to be available to template language.
   * Special kind of annotation inheritance works. All imports combined together from
   * {@link Generator.Import} annotations of template classes and enclosing packages, as well as
   * from superclasses and their respective packages. Where the formerd declaration override the
   * latter ones.
   */
  @Inherited
  @Target({ ElementType.TYPE, ElementType.PACKAGE })
  public @interface Import {
    Class<?>[] value();
  }

  /**
   * Used to introduce complex types as simple type names available to template language.
   * Annotate fields with this annotations to introduce such typedef. Fields should be named
   * starting with uppercase, so template will recognize it as type identifier.
   * 
   * <pre>
   * {@literal @}Generator.Typedef
   * java.util.Map.Entry&lt;String, String&gt; Pair;
   * </pre>
   * 
   * In the example above, new type Pair will be introduced in template and will serve as alias to
   * declared generic Map.Entry type of the fields.
   * Field's value doesn't matter and can be left uninitialized).
   */
  @Target(ElementType.FIELD)
  public @interface Typedef {}

  @Documented
  @Target({ ElementType.METHOD, ElementType.TYPE })
  @Retention(RetentionPolicy.SOURCE)
  public @interface Memoised {}

  /**
   * Applies to the annotation processor extending {@link AbstractGenerator} to supply annotation
   * names that processor will handle. Could be used instead of or in addition to
   * {@link SupportedAnnotationTypes}.
   */
  @Target({ ElementType.TYPE, ElementType.PACKAGE })
  @Retention(RetentionPolicy.RUNTIME)
  public @interface SupportedAnnotations {
    Class<? extends Annotation>[] value();
  }
}
