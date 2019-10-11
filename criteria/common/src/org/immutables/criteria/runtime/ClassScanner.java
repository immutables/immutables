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

package org.immutables.criteria.runtime;

import org.immutables.value.Value;

import javax.annotation.concurrent.NotThreadSafe;
import java.lang.reflect.Member;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Deque;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * Lazily iterates through class hierarchy using reflection api looking for methods, fields
 * and constructors.
 *
 * <p>This class is not thread-safe</p>
 */
@NotThreadSafe
public class ClassScanner implements Iterable<Member> {
  private final ImmutableScannerSetup setup;

  private ClassScanner(ImmutableScannerSetup setup) {
    this.setup = Objects.requireNonNull(setup, "setup");
  }

  public static ClassScanner of(Class<?> type) {
    return new ClassScanner(ImmutableScannerSetup.of(type));
  }

  public ClassScanner excludeFields() {
    return new ClassScanner(setup.withIncludeFields(false));
  }

  public ClassScanner excludeMethods() {
    return new ClassScanner(setup.withIncludeMethods(false));
  }

  public ClassScanner includeConstructors() {
    return new ClassScanner(setup.withIncludeConstructors(true));
  }

  public ClassScanner excludeInterfaces() {
    return new ClassScanner(setup.withIncludeInterfaces(false));
  }

  public ClassScanner excludeSuperclass() {
    return new ClassScanner(setup.withIncludeSuperclass(false));
  }

  @Override
  public Iterator<Member> iterator() {
    return new InternalScanner();
  }

  public Stream<Member> stream() {
    return StreamSupport.stream(spliterator(), false);
  }

  private class InternalScanner implements Iterator<Member> {
    private final Set<Class<?>> visited = new HashSet<>();
    private final Deque<Class<?>> toVisit = new ArrayDeque<>();
    private final Queue<Member> queue = new ArrayDeque<>();

    private InternalScanner() {
      visited.add(Object.class);
      toVisit.add(setup.type());
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

        if (setup.includeFields()) {
          queue.addAll(Arrays.asList(current.getDeclaredFields()));
        }

        if (setup.includeMethods()) {
          queue.addAll(Arrays.asList(current.getDeclaredMethods()));
        }

        if (setup.includeConstructors()) {
          queue.addAll(Arrays.asList(current.getDeclaredConstructors()));
        }

        if (!current.isInterface() && !visited.contains(current.getSuperclass()) && setup.includeSuperclass()) {
          toVisit.push(current.getSuperclass());
        }

        if (setup.includeInterfaces()) {
          toVisit.addAll(Arrays.asList(current.getInterfaces()));
        }
      }

      return !queue.isEmpty();
    }

    @Override
    public Member next() {
      if (!hasNext()) {
        throw new NoSuchElementException("No more elements to scan for " + setup.type());
      }
      return queue.remove();
    }
  }

  @Value.Immutable
  interface ScannerSetup {

    @Value.Parameter
    Class<?> type();

    @Value.Default
    default boolean includeFields() {
      return true;
    }

    @Value.Default
    default boolean includeMethods() {
      return true;
    }

    @Value.Default
    default boolean includeConstructors() {
      return false;
    }

    @Value.Default
    default boolean includeSuperclass() {
      return true;
    }

    @Value.Default
    default boolean includeInterfaces() {
      return true;
    }
  }
}
