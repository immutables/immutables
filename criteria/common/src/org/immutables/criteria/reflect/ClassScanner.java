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

package org.immutables.criteria.reflect;

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
  private final ImmutableSetup setup;

  private ClassScanner(ImmutableSetup setup) {
    this.setup = Objects.requireNonNull(setup, "setup");
  }

  public static ClassScanner of(Class<?> type) {
    return new ClassScanner(ImmutableSetup.of(type));
  }

  /**
   * Don't include fields (default behaviour is to include fields).
   */
  public ClassScanner skipFields() {
    return new ClassScanner(setup.withFields(false));
  }

  /**
   * Don't include methods (default behaviour is to include methods).
   */
  public ClassScanner skipMethods() {
    return new ClassScanner(setup.withMethods(false));
  }

  /**
   * Include constructors (default behaviour is to exclude constructors)
   */
  public ClassScanner includeConstructors() {
    return new ClassScanner(setup.withConstructors(true));
  }

  /**
   * Don't include (process) implemented interfaces (default behaviour is to include interfaces).
   */
  public ClassScanner skipInterfaces() {
    return new ClassScanner(setup.withInterfaces(false));
  }

  /**
   * Don't traverse superclass (default behaviour is to process superclass).
   */
  public ClassScanner skipSuperclass() {
    return new ClassScanner(setup.withSuperclass(false));
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

        if (setup.fields()) {
          queue.addAll(Arrays.asList(current.getDeclaredFields()));
        }

        if (setup.methods()) {
          queue.addAll(Arrays.asList(current.getDeclaredMethods()));
        }

        if (setup.constructors()) {
          queue.addAll(Arrays.asList(current.getDeclaredConstructors()));
        }

        if (!current.isInterface() && !visited.contains(current.getSuperclass()) && setup.superclass()) {
          toVisit.push(current.getSuperclass());
        }

        if (setup.interfaces()) {
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
  interface Setup {

    @Value.Parameter
    Class<?> type();

    /**
     * Include fields
     */
    @Value.Default
    default boolean fields() {
      return true;
    }

    /**
     * Include methods
     */
    @Value.Default
    default boolean methods() {
      return true;
    }

    /**
     * Include constructors
     */
    @Value.Default
    default boolean constructors() {
      return false;
    }

    /**
     * Scan superclass hierarchy
     */
    @Value.Default
    default boolean superclass() {
      return true;
    }

    @Value.Default
    default boolean interfaces() {
      return true;
    }
  }
}
