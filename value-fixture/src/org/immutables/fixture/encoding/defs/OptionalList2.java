/*
 * Copyright 2013-2016 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.immutables.fixture.encoding.defs;

import java.util.stream.Stream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.immutables.encode.Encoding;
import org.immutables.encode.Encoding.StandardNaming;

@Encoding
final class OptionalList2<T> {

  @Encoding.Impl
  private final List<T> list = null;

  public boolean equals(OptionalList2<T> other) {
    return Objects.equals(this.list, other.list);
  }

  @Encoding.Expose
  public Optional<List<T>> get() {
    return Optional.ofNullable(this.list);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(this.list);
  }

  @Override
  public String toString() {
    return Objects.toString(this.list);
  }

  @Encoding.Of
  static <T> List<T> initFromOptional(Optional<List<T>> optional) {
    Objects.requireNonNull(optional, "optional"); // TODO: Adding this line causes methods to be
// created twice in generated code

    return optional
        .map(elements -> createSafeList(elements)) // TODO: Method reference name mangling doesn't
// happen in generated code
        .orElse(null);
  }

  // TODO: @Encoding.Init duplicate method 'initFromVarargs'. Cannot have more than one init method
  // TODO: Needed to reproduce withX(T...) + withX(Optional<List<T>>)
//    @Encoding.Init
//    static <T> List<T> initFromVarargs(T... elements) {
//        return elements == null ? null : createSafeList(Arrays.asList(elements));
//    }

  // TODO: @Encoding.Init duplicate method 'initFromIterable'. Cannot have more than one init method
  // TODO: Needed to reproduce withX(Iterable<T>) + withX(Optional<List<T>>)
//    @Encoding.Init
//    static <T> List<T> initFromIterable(Iterable<T> iterable) {
//        return elements == null ? null : createSafeList(iterable);
//    }

  private static <T> List<T> createSafeList(Iterable<T> iterable) {
    List<T> list = StreamSupport.stream(iterable.spliterator(), false)
        .map(element -> Objects.requireNonNull(element, "element"))
        .collect(Collectors.toList());

    return Collections.unmodifiableList(list);
  }

  @Encoding.Builder
  static final class Builder<T> {

    private List<T> list;

    @Encoding.Naming(standard = StandardNaming.ADD_ALL)
    @Encoding.Init
    void addAll(Iterable<T> iterable) {
      Objects.requireNonNull(iterable, "iterable");

      if (this.list == null) {
        this.list = new ArrayList<>();
      }

      StreamSupport.stream(iterable.spliterator(), false)
          .map(element -> Objects.requireNonNull(element, "element"))
          .forEach(element -> this.list.add(element));
    }

    @Encoding.Naming(standard = StandardNaming.ADD)
    @Encoding.Init
    void addSingle(T element) {
      if (this.list == null) {
        this.list = new ArrayList<>();
      }

      this.list.add(Objects.requireNonNull(element, "element"));
    }

    // TODO: Adding this method results in a silent generation failure; the class just doesn't get
// created
    // TODO: Needed to reproduce (addX(T...))
    @Encoding.Naming(standard = StandardNaming.ADD)
    @Encoding.Init
    @SuppressWarnings("unchecked")
    void addVarargs(T... elements) {
      if (this.list == null) {
        this.list = new ArrayList<>();
      }

      Stream.of(elements)
          .forEach(element -> this.list.add(Objects.requireNonNull(element, "element")));
    }

    @Encoding.Build
    List<T> build() {
      return this.list;
    }

    @Encoding.Naming(standard = StandardNaming.INIT)
    @Encoding.Init
    void setFromIterable(Iterable<T> iterable) {
      if (this.list == null) {
        this.list = null;
      } else {
        this.list = StreamSupport.stream(iterable.spliterator(), false)
            .map(element -> Objects.requireNonNull(element, "element"))
            .collect(Collectors.toList());
      }
    }

    @Encoding.Naming(standard = StandardNaming.INIT)
    @Encoding.Init
    @Encoding.Copy
    void setFromOptional(Optional<List<T>> optional) {
      this.list = optional.orElse(null);
    }

  }

}
