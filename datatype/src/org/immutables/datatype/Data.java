package org.immutables.datatype;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Data {
  /**
   * This data element should be ignored during reading and writing the data. It is usually a mistake
   * to make required attribute having no defaults as ignorable.
   */
  @Retention(RetentionPolicy.RUNTIME)
  @Target(ElementType.METHOD)
  @interface Ignore {}

  /**
   * Marks that the type is strongly typed alias (newtype) for the other type it wraps.
   * For now, it requires either single parameter to be inlined or otherwise multiple will be
   * "inlined" as heterogeneous array/tuple in their data representation/serialization, in positional
   * order, for example. However, in our Datatypes metadata framework we only mark those as such on
   * a model level, we leave it up to further codecs/serialization to implement it.
   */
  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.RUNTIME)
  @interface Inline {}
}
