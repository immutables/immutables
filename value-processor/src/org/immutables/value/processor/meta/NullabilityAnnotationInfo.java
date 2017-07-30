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
package org.immutables.value.processor.meta;

import java.lang.annotation.ElementType;
import javax.lang.model.element.TypeElement;
import org.immutables.value.Value;

@Value.Immutable(intern = true, builder = false)
abstract class NullabilityAnnotationInfo {
  @Value.Parameter
  @Value.Auxiliary
  abstract TypeElement element();

  @Value.Derived
  String qualifiedName() {
    return element().getQualifiedName().toString();
  }

  @Value.Derived
  String simpleName() {
    return element().getSimpleName().toString();
  }

  @Value.Lazy
  String asPrefix() {
    return "@" + qualifiedName() + " ";
  }

  String asPrefixOriginal() {
    return asPrefix();
  }

  @Value.Lazy
  String asLocalPrefix() {
    boolean applicableToLocal = Annotations.annotationMatchesTarget(
        element(),
        ElementType.LOCAL_VARIABLE);

    return applicableToLocal
        ? asPrefix()
        : "";
  }

  /** Ad-hoc implementation for type-use level nullable annotation. */
  static NullabilityAnnotationInfo forTypeUse() {
    return new NullabilityAnnotationInfo() {
      @Override
      TypeElement element() {
        throw new UnsupportedOperationException("expecting this will not be accessed");
      }

      @Override
      String qualifiedName() {
        return "";
      }

      @Override
      String simpleName() {
        return "";
      }

      /** Empty as we expect type string extraction to handle this for us where possible. */
      @Override
      String asLocalPrefix() {
        return "";
      }

      /** Empty as we expect type string extraction to handle this for us where possible. */
      @Override
      String asPrefix() {
        return "";
      }
    };
  }
}
