package org.immutables.mongo;

import com.google.gson.Gson;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.SOURCE)
public @interface Mongo {
  /**
   * Abstract immutable classes annotated with this annotation will have repository generated to
   * store and retrieve documents from MongoDB collection named by class name or explicitly named as
   * specified by {@link #value()}. Repository classes are generated in the same package and are
   * named after annotated value type: {@code [name_of_annotated_type]Repository}.
   * <p>
   * {@code @Gson.Repository} should only be used with value types annotated with
   * {@code @Value.Immutable}. It also requires that gson type adapter is registerd on {@link Gson}
   * instance. However, care should be taken for all attribute types to be recursively marshalable
   * by being either built-in types or have type adapters registered.
   * <p>
   */
  @Documented
  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Repository {
    /**
     * Specify document collection name. If not specified, then collection name will be given
     * automatically by document class name, i.e. {@code "myDocument"} for {@code MyDocument} class.
     */
    String value() default "";
  }

  /**
   * Marks attribute as MongoDB {@code _id} attribute.
   * Alias for {@literal @}{@code Gson.Named("_id")}.
   * @see org.immutables.gson.Gson.Named
   */
  @Documented
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Id {}
}
