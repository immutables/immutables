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
package org.immutables.fixture.encoding.defs;

import org.immutables.encode.Encoding.StandardNaming;
import java.util.Objects;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.immutables.encode.Encoding;

@Encoding
final class OptionalList<T> {

  @Encoding.Impl
  private final List<T> list = null;

  @Encoding.Expose
  public Optional<List<T>> get() {
    return Optional.ofNullable(this.list);
  }

  @Encoding.Of
  static <T> List<T> init(Optional<? extends List<T>> lst) {
    return lst.<List<T>>map(ArrayList<T>::new).orElse(null);
  }

  @Override
  public String toString() {
    return get().toString();
  }

  @Override
  public int hashCode() {
    return get().hashCode();
  }

  public boolean equals(OptionalList<T> obj) {
    return Objects.equals(list, obj.list);
  }

  @Encoding.Builder
  static final class Builder<T> {

    private List<T> list = null;

    @Encoding.Naming(standard = StandardNaming.ADD)
    @Encoding.Init
    void add(T element) {
      getOrCreate().add(element);
    }

    @Encoding.Naming(standard = StandardNaming.ADD_ALL)
    @Encoding.Init
    void addAll(List<? extends T> elements) {
      getOrCreate().addAll(elements);
    }

    @Encoding.Build
    List<T> build() {
      return this.list;
    }

    @Encoding.Init
    @Encoding.Copy
    void set(Optional<? extends List<? extends T>> elements) {
      this.list = null;

      elements.ifPresent(e -> getOrCreate().addAll(e));
    }

    private List<T> getOrCreate() {
      if (this.list == null) {
        this.list = new ArrayList<>();
      }

      return this.list;
    }
  }
}
