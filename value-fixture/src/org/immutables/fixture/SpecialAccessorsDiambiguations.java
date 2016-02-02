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
package org.immutables.fixture;

import org.immutables.value.Value;

/**
 * Compilation test for identifier disambiguation like local variable 'h' in hashCode computation
 * or prehashed hashCode field in present of getHashCode attribute. "getHashCode" and "isToString"
 * are not turned into "hashCode" and "toString" to not class with attribute-like methods from
 * Object.
 */
@Value.Immutable(prehash = true)
@Value.Style(get = {"is*", "get*"})
public abstract class SpecialAccessorsDiambiguations {
  public abstract int getHashCode();

  public abstract int computeHashCode();

  public abstract String isToString();

  public abstract int h();

  public abstract int get__();
}
