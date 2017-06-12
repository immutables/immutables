/*
   Copyright 2015 Immutables Authors and Contributors

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
   * <p>
   * Abstract immutable classes annotated with this annotation will have repository generated to
   * store and retrieve documents from MongoDB collection named by class name or explicitly named as
   * specified by {@link #value()}. Repository classes are generated in the same package and are
   * named after annotated value type: {@code [name_of_annotated_type]Repository}.
   * </p>
   * <p>
   * {@code @Gson.Repository} should only be used with value types annotated with
   * {@code @Value.Immutable}. It also requires that gson type adapter is registerd on {@link Gson}
   * instance. However, care should be taken for all attribute types to be recursively marshalable
   * by being either built-in types or have type adapters registered.
   * </p>
   */
  @Documented
  @Target(ElementType.TYPE)
  @Retention(RetentionPolicy.SOURCE)
  public @interface Repository {
    /**
     * Specify document collection name. If not specified, then collection name will be given
     * automatically by document class name, i.e. {@code "myDocument"} for {@code MyDocument} class.
     * @return name
     * @deprecated use {@link #collection()} attribute instead
     */
    @Deprecated
    String value() default "";

    /**
     * Specify document collection name. If not specified, then collection name will be given
     * automatically by document class name, i.e. {@code "myDocument"} for {@code MyDocument} class.
     * @return name
     */
    String collection() default "";

    /**
     * Force repository to be readonly. In this case no collection write methods (eg. {@code insert},
     * {@code update}, {@code deleteAll} etc.) will be generated.
     *
     * @return true for a repository without write actions, false otherwise.
     */
    boolean readonly() default false;

    /**
     * Generate index helper which builds mongo indexes.
     *
     * @return true for index operations to be available to the repository, false otherwise.
     */
    boolean index() default true;
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
