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

import com.google.common.annotations.Beta;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Generate modifiable implementation of abstract value class.
 * This annotation could be used only as companion to {@link GenerateImmutable} to provide
 * modifiable variant that is convertible back and forth to immutable form.
 * <p>
 * Generated class will have name with "Modifiable" prefix. Use {@code create()} factory method to
 * create instances. Generated modifiable class will have setter methods that return {@code this}
 * for chained invocation. If you need bean-style getters consider using {@link GenerateGetters}
 * <p>
 * There's additional usage for such modifiable classes: generation of highly compact data holders
 * by having special integer-encoded data attributes annotated with {@link GeneratePackedBits}.
 * <p>
 * <em>Note: unlike {@literal @}{@link GenerateImmutable}, this annotation works only for top level classes</em>
 * @see GeneratePackedBits
 * @see GenerateGetters
 */
@Documented
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.CLASS)
@Beta
public @interface GenerateModifiable {}
