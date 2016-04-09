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
package org.immutables.fixture.jackson;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

/**
 * Compilation test for correct override of both equals and hashCode in Json integration class
 */
@JsonDeserialize(as = ImmutableHashCodeAbstract.class)
@Value.Immutable
public abstract class HashCodeAbstract extends InheritedWithHashCode {
  public abstract int someAttributeHaveToBeDefined();

  @Override
  public abstract int hashCode();

  @Override
  public abstract boolean equals(Object obj);
}

class InheritedWithHashCode {
  @Override
  public int hashCode() {
    return 0;
  }

  @Override
  public boolean equals(Object obj) {
    return false;
  }
}
