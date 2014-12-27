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
package org.immutables.value.processor;

import com.google.common.base.Function;
import org.immutables.generator.Generator;

//@Generator.Template
abstract class Transformers extends ValuesTemplate {

  public final Function<String, String> simplifyName = new Function<String, String>() {
    @Override
    public String apply(String className) {
      int indexOfLastDot = className.lastIndexOf('.');
      return indexOfLastDot < 0 ? className : className.substring(indexOfLastDot + 1);
    }
  };
}
