/*
    Copyright 2013 Immutables.org authors

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
package org.immutables.common.collect;

import com.google.common.annotations.Beta;
import javax.annotation.concurrent.ThreadSafe;

/**
 * Ordinal domain represent (potentially growing) set of objects of some kind (among {@code E})
 * which has distinct and contiguos range of {@link OrdinalValue#ordinal() ordinal} values. Equal
 * objects expected to have same ordinal value. This, usually, imply that object will be interned in
 * {@link OrdinalDomain}, but it is not strictly required.
 * <p>
 * <em>Implementations must be thread safe.</em>
 * </p>
 * @param <E> type with ordinal values
 */
@ThreadSafe
@Beta
public interface OrdinalDomain<E extends OrdinalValue<E>> {
  /**
   * Gets element from domain by corresponding ordinal value. It is guaranteed that returned element
   * will have {@link OrdinalValue#ordinal()} value equal to supplied {@code ordinal} parameter.
   * @param ordinal ordinal value
   * @return the element by ordinal value
   * @throws IndexOutOfBoundsException if no such element exists
   */
  E get(int ordinal);

  /**
   * Current length of ordinal domain. Be caution that length is not required to be stable and could
   * grow, as such it could be used as a hint.
   * @return the domain length: current max ordinal value plus one
   */
  int length();
}
