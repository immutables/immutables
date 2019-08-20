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

package org.immutables.criteria.inmemory;

import org.immutables.criteria.expression.Path;

import javax.annotation.Nullable;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.Optional;

class ReflectionFieldExtractor<T> implements ValueExtractor<T> {
  private final T object;

  ReflectionFieldExtractor(T object) {
    this.object = object;
  }

  @Nullable
  @Override
  public Object extract(Path path) {
    Objects.requireNonNull(path, "path");

    Object result = object;

    for (AnnotatedElement member: path.paths()) {
      result = extract(result, (Member) member);
      result = maybeUnwrapOptional(result);
      if (result == null) {
        return null;
      }
    }

    return result;
  }

  private static Object extract(Object instance, Member member) {
    if (instance == null) {
      return null;
    }

    try {
      // TODO caching
      final Object result;
      if (member instanceof Method) {
        final Method method = (Method) member;
        if (!method.isAccessible()) {
          method.setAccessible(true);
        }
        result = method.invoke(instance);
      } else if (member instanceof Field) {
        Field field = (Field) member;
        if (!field.isAccessible()) {
          field.setAccessible(true);
        }
        result = field.get(instance);
      } else {
        throw new IllegalArgumentException(String.format("%s is not field or method", member));
      }

      return result;
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new RuntimeException(e);
    }
  }

  private static Object maybeUnwrapOptional(Object maybeOptional) {
    if ((maybeOptional instanceof Optional)) {
      return  ((Optional) maybeOptional).orElse(null);
    }

    if (maybeOptional instanceof com.google.common.base.Optional) {
      return ((com.google.common.base.Optional) maybeOptional).orNull();
    }

    return maybeOptional;
  }


}
