/*
 * Copyright 2019 Immutables Authors and Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.immutables.criteria.geode;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static org.immutables.check.Checkers.check;

@SuppressWarnings("unchecked")
class BindVariableConverterTest {

  private final BindVariableConverter converter = new BindVariableConverter();

  @Test
  void generic() {
    check(converter.apply(null)).isNull();
    check(converter.apply("a")).is("a");
    check(converter.apply(1)).is(1);
    check(converter.apply(true)).is(true);
  }

  @Test
  void list() {
    List<?> l0 = (List<?>) converter.apply(ImmutableList.of());
    check(l0).isEmpty();
    check(l0).isA(ArrayList.class);

    List<String> l1 = (List<String>) converter.apply(ImmutableList.of("a"));
    check(l1).isOf("a");
    check(l1).isA(ArrayList.class);

    List<String> l2 = (List<String>) converter.apply(ImmutableList.of("a", "b"));
    check(l2).isOf("a", "b");
    check(l2).isA(ArrayList.class);

    check((List<?>) converter.apply(Collections.emptyList())).isEmpty();

  }

  @Test
  void set() {
    check((Set<?>) converter.apply(Collections.emptySet())).isEmpty();

    Set<?> s0 = (Set<?>) converter.apply(ImmutableSet.of());
    check(s0).isEmpty();
    check(s0).isA(HashSet.class);

    Set<String> s1 = (Set<String>) converter.apply(ImmutableSet.of("a"));
    check(s1).isOf("a");
    check(s1).isA(HashSet.class);

    Set<String> s2 = (Set<String>) converter.apply(ImmutableSet.of("a", "b"));
    check(s2).hasContentInAnyOrder("a", "b");
    check(s2).isA(HashSet.class);
  }

  @Test
  void iterable() {
    Iterable<?> i0 = (Iterable<?>) converter.apply(new CustomIterable<>(Collections.emptySet()));
    check(i0).isEmpty();
    check(i0.getClass().getPackage().getName()).startsWith("java.");

    Iterable<String> i1 = (Iterable<String>) converter.apply(new CustomIterable<>(Arrays.asList("a")));
    check(i1).isOf("a");
    check(i1.getClass().getPackage().getName()).startsWith("java.");
  }

  private static class CustomIterable<T> implements Iterable<T> {

    private final Iterable<T> delegate;

    private CustomIterable(Iterable<T> delegate) {
      this.delegate = delegate;
    }

    @Override
    public Iterator<T> iterator() {
      return delegate.iterator();
    }
  }
}