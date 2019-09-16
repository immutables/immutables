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

package org.immutables.criteria.elasticsearch;

import com.google.common.base.Preconditions;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.stream.Collectors;
import java.util.stream.Stream;

class Mappings {

  /**
   * Reflectively build  <a href="https://www.elastic.co/guide/en/elasticsearch/reference/current/mapping.html">elastic mapping</a>
   * for a class. Currently works only for immutable interfaces and is
   * very limited in its functionality.
   */
  static Mapping of(Class<?> clazz) {
    Objects.requireNonNull(clazz, "clazz");
    Preconditions.checkArgument(clazz.isInterface(), "Expected %s to be an interface", clazz);
    Map<String, String> map = new LinkedHashMap<>();

    Stream<Method> methods = Arrays.stream(clazz.getDeclaredMethods())
           .filter(m -> m.getParameterCount() == 0)
           .filter(m -> m.getReturnType() != Void.class)
           .filter(m -> Modifier.isPublic(m.getModifiers()))
           .filter(m -> !Modifier.isStatic(m.getModifiers()))
           .filter(m -> m.getDeclaringClass() != Object.class);

    for (Method method: methods.collect(Collectors.toSet())) {
      Class<?> returnType = method.getReturnType();
      // skip arrays and iterables
      if (returnType.isArray() || Iterable.class.isAssignableFrom(returnType)) {
        continue;
      }
      Type type = method.getGenericReturnType();
      map.put(method.getName(), elasticType(type));
    }
    return Mapping.ofElastic(map);
  }

  private static String elasticType(Type type) {
    Objects.requireNonNull(type, "type");
    if (type instanceof ParameterizedType) {
      ParameterizedType parametrized = (ParameterizedType) type;
      if (parametrized.getActualTypeArguments().length == 1) {
        // unwrap
        return elasticType(parametrized.getActualTypeArguments()[0]);
      }
    }
    if (type == String.class || type == Character.class || type == char.class) {
      return "keyword";
    } else if (type == Boolean.class || type == boolean.class) {
      return "boolean";
    } else if (type == Byte.class || type == byte.class) {
      return "byte";
    } else if (type == Short.class || type == short.class) {
      return "short";
    } else if (type == Integer.class || type == int.class || type == OptionalInt.class) {
      return "integer";
    } else if (type == Long.class || type == long.class || type == OptionalLong.class) {
      return "long";
    } else if (type == Double.class || type == double.class || type == OptionalDouble.class) {
      return "double";
    } else if (type == Float.class || type == float.class) {
      return "float";
    } else if (type == LocalDate.class || type == Instant.class || type == LocalDateTime.class) {
      return "date";
    }

    throw new IllegalArgumentException("Don't know how to map " + type);
  }

}
