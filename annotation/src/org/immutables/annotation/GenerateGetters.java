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

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Generate bean-style getter accesor method for each attribute.
 * It needed sometimes for interoperability.
 * Methods with 'get' (or 'is') prefixes and attribute name joined in camel-case.
 * <p>
 * Generated methods have annotation copied from original accessor method. However
 * {@code org.immutables.*} and {@code java.lang.*} are not copied. This allow some frameworks to
 * work with immutable types as they can with beans, using getters and annotations on them.
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
@Deprecated
public @interface GenerateGetters {}
