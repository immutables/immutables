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
package org.immutables.value.processor.meta;

import com.google.common.collect.Ordering;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import org.immutables.value.Value.Immutable.ImplementationVisibility;

/**
 * Type visibility interpretation. Treat protected as package.
 */
public enum TypeVisibility {
  PRIVATE, PACKAGE, PUBLIC;

  public boolean isPublic() {
    return this == PUBLIC;
  }

  public boolean isPrivate() {
    return this == PRIVATE;
  }

  public TypeVisibility forImplementation(ImplementationVisibility visibility) {
    switch (visibility) {
    case PACKAGE:
      return PACKAGE;
    case PRIVATE:
      return PRIVATE;
    case PUBLIC:
      return PUBLIC;
    case SAME:
    default:
      return this;
    }
  }

  public TypeVisibility max(TypeVisibility visibility) {
    return Ordering.natural().max(this, visibility);
  }

  public static TypeVisibility of(Element element) {
    if (element.getModifiers().contains(Modifier.PUBLIC)) {
      return PUBLIC;
    }
    if (element.getModifiers().contains(Modifier.PRIVATE)) {
      return PRIVATE;
    }
    return PACKAGE;
  }

  public boolean isMoreRestrictiveThan(TypeVisibility visibility) {
    return this.compareTo(visibility) < 0;
  }
}
