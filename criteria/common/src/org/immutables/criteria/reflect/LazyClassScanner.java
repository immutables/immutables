/*
 * Copyright 2020 Immutables Authors and Contributors
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

package org.immutables.criteria.reflect;

import javax.annotation.concurrent.NotThreadSafe;
import java.lang.reflect.Member;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;

/**
 * Lazily iterate through class hierarchy
 */
@NotThreadSafe
class LazyClassScanner implements ClassScanner {

  private final ImmutableBuilder builder;

  LazyClassScanner(ImmutableBuilder builder) {
    this.builder = builder;
  }

  @Override
  public Iterator<Member> iterator() {
    return new LazyMemberIterator(builder);
  }

  private static class LazyMemberIterator implements Iterator<Member> {
    private final Set<Class<?>> visited = new HashSet<>();
    private final Deque<Class<?>> toVisit = new ArrayDeque<>();
    private final Queue<Member> queue = new ArrayDeque<>();
    private final Builder builder;

    private LazyMemberIterator(Builder builder) {
      this.builder = builder;
      visited.add(Object.class);
      toVisit.add(builder.type());
    }

    @Override
    public boolean hasNext() {
      return tryNext();
    }

    private boolean tryNext() {
      while (queue.isEmpty() && !toVisit.isEmpty()) {
        final Class<?> current = toVisit.pop();
        if (!visited.add(current)) {
          continue;
        }

        if (builder.fields()) {
          queue.addAll(Arrays.asList(current.getDeclaredFields()));
        }

        if (builder.methods()) {
          queue.addAll(Arrays.asList(current.getDeclaredMethods()));
        }

        if (builder.constructors()) {
          queue.addAll(Arrays.asList(current.getDeclaredConstructors()));
        }

        if (!current.isInterface() && !visited.contains(current.getSuperclass()) && builder.superclass()) {
          toVisit.push(current.getSuperclass());
        }

        if (builder.interfaces()) {
          toVisit.addAll(Arrays.asList(current.getInterfaces()));
        }
      }

      return !queue.isEmpty();
    }

    @Override
    public Member next() {
      if (!hasNext()) {
        throw new NoSuchElementException("No more elements to scan for " + builder.type());
      }
      return queue.remove();
    }
  }

}
