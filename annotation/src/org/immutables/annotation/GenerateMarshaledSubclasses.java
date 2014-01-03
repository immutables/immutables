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
 * Expected subclasses for marshaling could be specified on attribute level or an abstract
 * supertype directly, however the former declaration site has precedence.
 * @see #value()
 * @see GenerateMarshaled
 */
@Documented
@Retention(RetentionPolicy.SOURCE)
@Target({ ElementType.METHOD, ElementType.TYPE })
public @interface GenerateMarshaledSubclasses {

  /**
   * Specifies expected subclasses of an abstract type that is matched during parsing by
   * occurrence of unique settable attributes ({@link GenerateDerived derived} does not count, also
   * be careful with non-mandatory {@link GenerateDefault default} attributes). If all attributes of
   * subclasses are the same, then it will result in error due to undecidable situation.
   * @return subclasses of an abstract type that annotated with {@link GenerateMarshaler}
   */
  Class<?>[] value() default {};
}
