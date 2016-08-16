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
package org.immutables.value.processor.encode;

import java.util.Objects;

public abstract class Eq<Q extends Eq<Q>> {
  private final int hash;

  protected Eq(Object... hash) {
    this.hash = Objects.hash(hash);
  }

	protected abstract boolean eq(Q other);

  @SuppressWarnings("unchecked")
  @Override
  public final boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() == obj.getClass()) {
      return eq((Q) obj);
    }
    return false;
  }

  @Override
  public final int hashCode() {
    return hash;
  }
}
