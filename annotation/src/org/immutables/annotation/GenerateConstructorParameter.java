/*
    Copyright 2013-2014 Immutables.org authors

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

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Works with {@link GenerateImmutable} classes to mark abstract accessor method be included as
 * "{@code of(..)}" constructor parameter.
 * <p>
 * Following rules applies:
 * <ul>
 * <li>No constructor generated, if none of methods have {@link GenerateConstructorParameter}
 * annotation</li>
 * <li>For object to be constructible with a constructor - all non-default and non-derived
 * attributes should be annotated with {@link GenerateConstructorParameter}.
 * </ul>
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.SOURCE)
@Deprecated
public @interface GenerateConstructorParameter {
  /**
   * Used to specify order of constructor argument. It's defaults to zero and allows for
   * non-contiguous order values (arguments are sorted ascending by this order value).
   * <p>
   * <em>This attribute was introduced as JDT annotation processor internally tracks alphabetical order
   * of members (non-standard as of Java 6), this differs from Javac, which uses order of declaration appearance
   * in a source file. Thus, in order to support portable constructor argument definitions,
   * developer should supply argument order explicitly.</em>
   * @return order
   */
  int order() default 0;
}
