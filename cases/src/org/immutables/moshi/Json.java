/*
   Copyright 2016 Immutables Authors and Contributors

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
     * @return if empty should be omitted as nulls
     */
    boolean emptyAsNulls() default false;
  }

  /**
   * <p>
   * Specify attribute's custom name in JSON representation.
   * </p>
   * <p>
   * This example used to define JSON attribute name as "_id" during marshaling and unmarshaling.
   * </p>
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
