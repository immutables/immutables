package org.immutables.serial;

import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Retention;
import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Use nested annotations to control serialization of value objects.
 * <p>
 * This umbrella annotaion does nothing.
 * @see Version
 * @see Structural
 */
@Target({})
@Retention(RetentionPolicy.SOURCE)
public @interface Serial {

  /**
   * Could be applied to types, enclosing types, enclosing packages and as meta-annotation.
   * Specify serial version over enclosing types. It has an effect of also making them implement
   * serializable if they don't already. May be used in combination with {@link Structural}, but it
   * is not strictly needed if structural is present.
   */
  @Retention(RetentionPolicy.CLASS)
  @Target({ElementType.TYPE, ElementType.PACKAGE, ElementType.ANNOTATION_TYPE})
  public @interface Version {
    long value();
  }

  /**
   * Annotation to generate structural serialization. Could be applied to
   * types, enclosing types, enclosing packages and as meta-annotation.
   * <p>
   * Structural serialization of value types use attribute names, collections, optional values and
   * map into the representation, rather than exact internal fields used to store the data. The
   * important aspect of this is that objects are being deserialized using it's constructors and
   * builders, and not the internal representation, thus all singletons, interning and other
   * invariants will be preserved and data migration made possible (using either optional or
   * nullable attributes) or changing types of collections, moving from scalar values to collections
   * etc. Constuction using builder and constructor (if builder is disabled) is supported.
   * <p>
   * If {@link Structural} serialization is used and no {@code serialVersionUID} is declared, i.e.
   * {@link Version} annotation is missing, serialized form will have serial version 0L assigned.
   */
  @Retention(RetentionPolicy.CLASS)
  @Target({ElementType.TYPE, ElementType.PACKAGE, ElementType.ANNOTATION_TYPE})
  public @interface Structural {}
}
