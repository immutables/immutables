/*
   Copyright 2018 Immutables Authors and Contributors

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

import javax.lang.model.element.TypeElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import static com.google.common.base.Preconditions.checkState;

final class CheckedExceptionProbe {
  private final Types types;
  private final DeclaredType exceptionType;
  private final DeclaredType runtimeExceptionType;

  CheckedExceptionProbe(Types types, Elements elements) {
    this.types = types;
    this.exceptionType = getTypeMirror(types, elements, Exception.class);
    this.runtimeExceptionType = getTypeMirror(types, elements, RuntimeException.class);
  }

  private static DeclaredType getTypeMirror(Types types, Elements elements, Class<?> reflected) {
    TypeElement typeElement = elements.getTypeElement(reflected.getName());
    checkState(typeElement != null, "type must be present: %s", reflected.getName());
    return types.getDeclaredType(typeElement);
  }

  boolean isCheckedException(TypeMirror throwable) {
    return types.isSubtype(throwable, exceptionType)
        && !types.isSubtype(throwable, runtimeExceptionType);
  }
}
