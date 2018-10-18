/*
   Copyright 2013-2018 Immutables Authors and Contributors

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
package org.immutables.ordinal;

/**
 * Objects implementing this interface has stable ordinal attribute that could be
 * used to arrange these object among other elements of the same type. Instances of the same kind
 * are expected to be {@link Object#equals(Object) equal} if they have the same value of
 * {@link #ordinal()}.
 * <p>
 * In essence, this type expresses enumeration for object types that cannot be represented as Java
 * {@code enum}s. One of the justifications of such usage is sophisticated optimizations possible
 * with data-structures that relies on the fact that number of different values of some type is
 * countable and limited (usually, in correspondence to the problem domain that is being modeled).
 * @see ImmutableOrdinalSet
 * @param <E> element type
 */
public interface OrdinalValue<E extends OrdinalValue<E>> {
  /**
   * Zero based ordinal value
   * @return the ordinal value
   */
  int ordinal();

  /**
   * Domain that contains family of objects, arranged by ordinal
   * @return the domain
   */
  OrdinalDomain<E> domain();
}
