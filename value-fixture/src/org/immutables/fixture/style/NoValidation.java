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
package org.immutables.fixture.style;

import org.immutables.value.Value.Style.ValidationMethod;
import org.immutables.value.Value;

@Value.Immutable
@Value.Style(validationMethod = ValidationMethod.NONE)
public interface NoValidation {
  @Value.Parameter
  String a();

  @Value.Parameter
  Integer b();

  @Value.Parameter
  boolean z();

  @Value.Parameter
  int i();
}
