package org.immutables.json;

import com.google.common.annotations.Beta;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.immutables.value.Value;

@Beta
@Retention(RetentionPolicy.SOURCE)
public @interface Json {

  /**
   * Instructs generator to generate marshaler.
   * When applied to abstract {@link Value.Immutable immutable} it will
   * generate corresponding
   * marshaler class in the same package. It will have name of abstract immutable class with
   * 'Marshaler' suffix.
   * When applied to a package, it is used to specify imports of marshaling routines for
   * each generated marshaler in a package.
   */
  @Documented
  @Retention(RetentionPolicy.RUNTIME)
  @Target({ ElementType.TYPE, ElementType.PACKAGE })
  public @interface Marshaled {}

  /**
   * Specify classes whose members will be imported with a static star-import
   * ({@code import static ...*;}).
   * In order to share imported routines among classes in a package, you can place this
   * annotation on a enclosing package (using package-info.java).
   */
  public @interface Import {
    /**
     * Classes to static-import routines from.
     * @return class literals
     */
    Class<?>[] value();
  }

  /**
   * Expected subclasses for marshaling could be specified on attribute level or an abstract
   * supertype directly, however the former declaration site has precedence.
   * @see #value()
   * @see Named
   */
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target({ ElementType.METHOD, ElementType.TYPE })
  public @interface Subclasses {

    /**
     * Specifies expected subclasses of an abstract type that is matched during parsing by
     * occurrence of unique settable attributes ({@link Value.Derived derived} does not count, also
     * be careful with non-mandatory ({@link Value.Default default} attributes).
     * If all attributes of subclasses are the same, then it will result in error due to undecidable
     * situation.
     * @return subclasses of an abstract type that annotated with {@link Marshaled}
     */
    Class<?>[] value();
  }

  /**
   * Specify attribute's custom name in JSON/BSON representation.
   * <p>
   * This example used to define JSON attribute name as "_id" during marshaling and unmarshaling.
   * 
   * <pre>
   * &#064;Json.Named(&quot;_id&quot;)
   * public abstract String id();
   * </pre>
   */
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target(ElementType.METHOD)
  public @interface Named {
    /**
     * @return custom name string.
     */
    String value();
  }

  /**
   * Indicates if marshaler should skip this attribute during marshaling.
   * Applies only to non-mandatory attributes.
   */
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target(ElementType.METHOD)
  public @interface Ignore {}

  /**
   * For {@link java.util.Set Set} or {@link java.util.List List} this will force output of
   * JSON empty array if given collection is empty. By default, empty collection attribute will
   * just
   * be omitted.
   * <p>
   * For {@link com.google.common.base.Optional Optional} attributes it forces of output JSON
   * {@code null} value for missing value, otherwise (by default) no absent attribute is written
   */
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target(ElementType.METHOD)
  public @interface ForceEmpty {}
}
