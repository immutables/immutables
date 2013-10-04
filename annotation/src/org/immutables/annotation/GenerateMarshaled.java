/*
    Copyright 2013 Immutables.org authors

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
package org.immutables.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Use {@code GenerateMarshaledAs} to customize marshaling of annotated attribute.
 * Typical usage is to customize name in JSON/BSON representation
 * <p>
 * This example used to define JSON attribute name as "_id" during marshaling and unmarshaling.
 * 
 * <pre>
 * &#064;GenerateMarshaled(&quot;_id&quot;)
 * public abstract String id();
 * </pre>
 * @see GenerateMarshaler
 * @see GenerateMarshaledSubclasses
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target(ElementType.METHOD)
public @interface GenerateMarshaled {
  /**
   * Specify attribute's custom name in JSON/BSON representation.
   * @return custom name string, empty string if attribute's name should be used verbatim (the
   *         default).
   */
  String value() default "";

  /**
   * For {@link java.util.Set Set} or {@link java.util.List List} this will force output of
   * JSON empty array if given collection is empty. By default, empty collection attribute will just
   * be omitted.
   * <p>
   * For {@link com.google.common.base.Optional Optional} attributes it forces of output JSON
   * {@code null} value for missing value, otherwise (by default) no absent attribute is written
   * @return {@code true} if force output of empty value
   */
  boolean forceEmpty() default false;
}
