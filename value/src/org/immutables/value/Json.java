/*
    Copyright 2014 Immutables Authors and Contributors

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.immutables.value;

import com.google.common.annotations.Beta;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Json marshaling annotations used to generate and customize marshalers.
 */
@Beta
@Retention(RetentionPolicy.SOURCE)
public @interface Json {

  /**
   * Instructs generator to generate marshaler.
   * When applied to abstract {@link org.immutables.value.Value.Immutable immutable} it will
   * generate corresponding
   * marshaler class in the same package. It will have name of abstract immutable class with
   * 'Marshaler' suffix.
   */
  @Documented
  @Retention(RetentionPolicy.SOURCE)
  @Target({ElementType.TYPE})
  public @interface Marshaled {}

  /**
   * Specify classes whose members will be imported with a static star-import
   * ({@code import static ...*;}).
   * In order to share imported routines among classes in a package, you can place this
   * annotation on a enclosing package (using package-info.java).
   */
  @Retention(RetentionPolicy.SOURCE)
  @Target({ElementType.TYPE, ElementType.PACKAGE})
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
  @Retention(RetentionPolicy.SOURCE)
  @Target({ElementType.METHOD, ElementType.TYPE})
  public @interface Subclasses {

    /**
     * Specifies expected subclasses of an abstract type that is matched during parsing by
     * occurrence of unique settable attributes ({@link org.immutables.value.Value.Derived derived}
     * does not count, also
     * be careful with non-mandatory ({@link org.immutables.value.Value.Default default}
     * attributes).
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
