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
package nonimmutables;

import java.util.Collections;
import java.util.Iterator;

// Dummy custom collection, mostly for compilation testing
public final class CustColl<E> implements Iterable<E> {

  @Override
  public Iterator<E> iterator() {
    return Collections.emptyIterator();
  }

  public static <E> CustColl<E> from(Iterable<? extends E> elements) {
    elements.toString();
    return new CustColl<>();
  }

  public static <E> CustColl<E> of() {
    return new CustColl<>();
  }

  public static <E> Builder<E> builder() {
    return new Builder<>();
  }

  public static class Builder<E> {
    public Builder<E> addAll(Iterable<? extends E> elements) {
      elements.toString();
      return this;
    }

    public Builder<E> add(E element) {
      element.toString();
      return this;
    }

    public CustColl<E> build() {
      return new CustColl<>();
    }
  }
}
