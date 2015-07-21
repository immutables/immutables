package org.immutables.moshi;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({})
@Retention(RetentionPolicy.SOURCE)
public @interface Json {
  @Documented
  @Target({ElementType.TYPE, ElementType.PACKAGE})
  public @interface Adapters {
    /**
     * When {@code emptyAsNulls=true}, empty arrays and objects will be omitted from output as
     * if they where {@code null}, both field name and value will be omited.
     * <p>
     */
    boolean emptyAsNulls() default false;
  }

  /**
   * Specify attribute's custom name in JSON representation.
   * <p>
   * This example used to define JSON attribute name as "_id" during marshaling and unmarshaling.
   * 
   * <pre>
   * &#064;OkJson.Named(&quot;_id&quot;)
   * public abstract String id();
   * </pre>.
   */
  @Documented
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
  @Target(ElementType.METHOD)
  public @interface Ignore {}
}
