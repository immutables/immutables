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
package org.immutables.generator;

import com.google.common.base.Predicate;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.lang.model.element.TypeElement;

public final class ClasspathAvailability {
  // static non-thread-safe cache? ok!
  private static final Map<String, Boolean> availableClasses =
      Collections.synchronizedMap(new HashMap<String, Boolean>());

  public final Predicate<String> available = new Predicate<String>() {
    @Override
    public boolean apply(String input) {
      /*@Nullable*/Boolean available = availableClasses.get(input);
      if (available == null) {
        TypeElement element = StaticEnvironment.processing()
            .getElementUtils()
            .getTypeElement(input);

        available = element != null;
        availableClasses.put(input, available);
      }

      return available;
    }

    @Override
    public String toString() {
      return "classpath.available";
    }
  };
}
