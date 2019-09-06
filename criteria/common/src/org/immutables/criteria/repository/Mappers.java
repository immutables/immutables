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

package org.immutables.criteria.repository;

import org.immutables.criteria.backend.ProjectedTuple;

import java.util.function.Function;

/**
 * Set of functions to process a {@link ProjectedTuple}
 */
public final class Mappers {
  private Mappers() {}

  public static <R> Function<ProjectedTuple, R> fromTuple() {
    return tuple -> {
      if (tuple.values().size() != 1) {
        throw new IllegalArgumentException(String.format("Expected tuple of size 1 got %d", tuple.values().size()));
      }

      @SuppressWarnings("unchecked")
      R result = (R) tuple.values().get(0);
      return result;
    };
  }

  @SuppressWarnings("unchecked")
  public static <R, T1, T2> Function<ProjectedTuple, R> fromTuple(MapperFunction2<T1, T2, R> fn) {
    return tuple -> {
      if (tuple.values().size() != 2) {
        throw new IllegalArgumentException(String.format("Expected tuple of size 2 got %d", tuple.values().size()));
      }

      T1 t1 = (T1) tuple.values().get(0);
      T2 t2 = (T2) tuple.values().get(1);

      return fn.apply(t1, t2);
    };
  }

  @SuppressWarnings("unchecked")
  public static <R, T1, T2, T3> Function<ProjectedTuple, R> fromTuple(MapperFunction3<T1, T2, T3, R> fn) {
    return tuple -> {
      if (tuple.values().size() != 3) {
        throw new IllegalArgumentException(String.format("Expected tuple of size 3 got %d", tuple.values().size()));
      }

      T1 t1 = (T1) tuple.values().get(0);
      T2 t2 = (T2) tuple.values().get(1);
      T3 t3 = (T3) tuple.values().get(2);
      return fn.apply(t1, t2, t3);
    };
  }

  @SuppressWarnings("unchecked")
  public static <R, T1, T2, T3, T4> Function<ProjectedTuple, R> fromTuple(MapperFunction4<T1, T2, T3, T4, R> fn) {
    return tuple -> {
      if (tuple.values().size() != 4) {
        throw new IllegalArgumentException(String.format("Expected tuple of size 4 got %d", tuple.values().size()));
      }

      T1 t1 = (T1) tuple.values().get(0);
      T2 t2 = (T2) tuple.values().get(1);
      T3 t3 = (T3) tuple.values().get(2);
      T4 t4 = (T4) tuple.values().get(3);
      return fn.apply(t1, t2, t3, t4);
    };
  }

  @SuppressWarnings("unchecked")
  public static <R, T1, T2, T3, T4, T5> Function<ProjectedTuple, R> fromTuple(MapperFunction5<T1, T2, T3, T4, T5, R> fn) {
    return tuple -> {
      if (tuple.values().size() != 5) {
        throw new IllegalArgumentException(String.format("Expected tuple of size 5 got %d", tuple.values().size()));
      }

      T1 t1 = (T1) tuple.values().get(0);
      T2 t2 = (T2) tuple.values().get(1);
      T3 t3 = (T3) tuple.values().get(2);
      T4 t4 = (T4) tuple.values().get(3);
      T5 t5 = (T5) tuple.values().get(4);
      return fn.apply(t1, t2, t3, t4, t5);
    };
  }


}
