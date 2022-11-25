/*
 * Copyright 2022 Immutables Authors and Contributors
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
package org.immutables.criteria.sql.conversion;

import org.immutables.criteria.sql.SqlException;

import java.time.Instant;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.UUID;

public final class TypeConverters {
  private static final Map<Class, Map<Class, TypeConverter<?, ?>>> CONVERTERS;

  static {
    CONVERTERS = new IdentityHashMap<>();
    register(boolean.class, boolean.class, v -> v);
    register(boolean.class, Boolean.class, v -> v);
    register(boolean.class, byte.class, v -> (byte) (v ? 1 : 0));
    register(boolean.class, Byte.class, v -> (byte) (v ? 1 : 0));
    register(boolean.class, short.class, v -> (short) (v ? 1 : 0));
    register(boolean.class, Short.class, v -> (short) (v ? 1 : 0));
    register(boolean.class, int.class, v -> (v ? 1 : 0));
    register(boolean.class, Integer.class, v -> (v ? 1 : 0));
    register(boolean.class, long.class, v -> (long) (v ? 1 : 0));
    register(boolean.class, Long.class, v -> (long) (v ? 1 : 0));
    register(boolean.class, String.class, v -> (v ? "true" : "false"));

    register(Boolean.class, boolean.class, v -> v);
    register(Boolean.class, Boolean.class, v -> v);
    register(Boolean.class, byte.class, v -> (byte) (v ? 1 : 0));
    register(Boolean.class, Byte.class, v -> (byte) (v ? 1 : 0));
    register(Boolean.class, short.class, v -> (short) (v ? 1 : 0));
    register(Boolean.class, Short.class, v -> (short) (v ? 1 : 0));
    register(Boolean.class, int.class, v -> (v ? 1 : 0));
    register(Boolean.class, Integer.class, v -> (v ? 1 : 0));
    register(Boolean.class, long.class, v -> (long) (v ? 1 : 0));
    register(Boolean.class, Long.class, v -> (long) (v ? 1 : 0));
    register(Boolean.class, String.class, v -> (v ? "true" : "false"));

    register(byte.class, boolean.class, v -> v == 0);
    register(byte.class, Boolean.class, v -> v == 0);
    register(byte.class, byte.class, v -> v);
    register(byte.class, Byte.class, v -> v);
    register(byte.class, short.class, Byte::shortValue);
    register(byte.class, Short.class, Byte::shortValue);
    register(byte.class, int.class, Byte::intValue);
    register(byte.class, Integer.class, Byte::intValue);
    register(byte.class, long.class, Byte::longValue);
    register(byte.class, Long.class, Byte::longValue);
    register(byte.class, float.class, Byte::floatValue);
    register(byte.class, Float.class, Byte::floatValue);
    register(byte.class, double.class, Byte::doubleValue);
    register(byte.class, Double.class, Byte::doubleValue);
    register(byte.class, String.class, String::valueOf);

    register(Byte.class, boolean.class, v -> v == 0);
    register(Byte.class, Boolean.class, v -> v == 0);
    register(Byte.class, byte.class, v -> v);
    register(Byte.class, Byte.class, v -> v);
    register(Byte.class, short.class, Byte::shortValue);
    register(Byte.class, Short.class, Byte::shortValue);
    register(Byte.class, int.class, Byte::intValue);
    register(Byte.class, Integer.class, Byte::intValue);
    register(Byte.class, long.class, Byte::longValue);
    register(Byte.class, Long.class, Byte::longValue);
    register(Byte.class, float.class, Byte::floatValue);
    register(Byte.class, Float.class, Byte::floatValue);
    register(Byte.class, double.class, Byte::doubleValue);
    register(Byte.class, Double.class, Byte::doubleValue);
    register(Byte.class, String.class, String::valueOf);

    register(short.class, boolean.class, v -> v == 0);
    register(short.class, Boolean.class, v -> v == 0);
    register(short.class, byte.class, Short::byteValue);
    register(short.class, Byte.class, Short::byteValue);
    register(short.class, short.class, v -> v);
    register(short.class, Short.class, v -> v);
    register(short.class, int.class, Short::intValue);
    register(short.class, Integer.class, Short::intValue);
    register(short.class, long.class, Short::longValue);
    register(short.class, Long.class, Short::longValue);
    register(short.class, float.class, Short::floatValue);
    register(short.class, Float.class, Short::floatValue);
    register(short.class, double.class, Short::doubleValue);
    register(short.class, Double.class, Short::doubleValue);
    register(short.class, String.class, String::valueOf);

    register(Short.class, boolean.class, v -> v == 0);
    register(Short.class, Boolean.class, v -> v == 0);
    register(Short.class, byte.class, Short::byteValue);
    register(Short.class, Byte.class, Short::byteValue);
    register(Short.class, short.class, v -> v);
    register(Short.class, Short.class, v -> v);
    register(Short.class, int.class, Short::intValue);
    register(Short.class, Integer.class, Short::intValue);
    register(Short.class, long.class, Short::longValue);
    register(Short.class, Long.class, Short::longValue);
    register(Short.class, float.class, Short::floatValue);
    register(Short.class, Float.class, Short::floatValue);
    register(Short.class, double.class, Short::doubleValue);
    register(Short.class, Double.class, Short::doubleValue);
    register(Short.class, String.class, String::valueOf);

    register(int.class, boolean.class, v -> v == 0);
    register(int.class, Boolean.class, v -> v == 0);
    register(int.class, byte.class, Integer::byteValue);
    register(int.class, Byte.class, Integer::byteValue);
    register(int.class, short.class, Integer::shortValue);
    register(int.class, Short.class, Integer::shortValue);
    register(int.class, int.class, v -> v);
    register(int.class, Integer.class, v -> v);
    register(int.class, long.class, Integer::longValue);
    register(int.class, Long.class, Integer::longValue);
    register(int.class, float.class, Integer::floatValue);
    register(int.class, Float.class, Integer::floatValue);
    register(int.class, double.class, Integer::doubleValue);
    register(int.class, Double.class, Integer::doubleValue);
    register(int.class, String.class, String::valueOf);

    register(Integer.class, boolean.class, v -> v == 0);
    register(Integer.class, Boolean.class, v -> v == 0);
    register(Integer.class, byte.class, Integer::byteValue);
    register(Integer.class, Byte.class, Integer::byteValue);
    register(Integer.class, short.class, Integer::shortValue);
    register(Integer.class, Short.class, Integer::shortValue);
    register(Integer.class, int.class, v -> v);
    register(Integer.class, Integer.class, v -> v);
    register(Integer.class, long.class, Integer::longValue);
    register(Integer.class, Long.class, Integer::longValue);
    register(Integer.class, float.class, Integer::floatValue);
    register(Integer.class, Float.class, Integer::floatValue);
    register(Integer.class, double.class, Integer::doubleValue);
    register(Integer.class, Double.class, Integer::doubleValue);
    register(Integer.class, String.class, String::valueOf);

    register(long.class, boolean.class, v -> v == 0);
    register(long.class, Boolean.class, v -> v == 0);
    register(long.class, byte.class, Long::byteValue);
    register(long.class, Byte.class, Long::byteValue);
    register(long.class, short.class, Long::shortValue);
    register(long.class, Short.class, Long::shortValue);
    register(long.class, int.class, Long::intValue);
    register(long.class, Integer.class, Long::intValue);
    register(long.class, long.class, v -> v);
    register(long.class, Long.class, v -> v);
    register(long.class, float.class, Long::floatValue);
    register(long.class, Float.class, Long::floatValue);
    register(long.class, double.class, Long::doubleValue);
    register(long.class, Double.class, Long::doubleValue);
    register(long.class, String.class, String::valueOf);

    register(Long.class, boolean.class, v -> v == 0);
    register(Long.class, Boolean.class, v -> v == 0);
    register(Long.class, byte.class, Long::byteValue);
    register(Long.class, Byte.class, Long::byteValue);
    register(Long.class, short.class, Long::shortValue);
    register(Long.class, Short.class, Long::shortValue);
    register(Long.class, int.class, Long::intValue);
    register(Long.class, Integer.class, Long::intValue);
    register(Long.class, long.class, v -> v);
    register(Long.class, Long.class, v -> v);
    register(Long.class, float.class, Long::floatValue);
    register(Long.class, Float.class, Long::floatValue);
    register(Long.class, double.class, Long::doubleValue);
    register(Long.class, Double.class, Long::doubleValue);
    register(Long.class, String.class, String::valueOf);

    register(String.class, boolean.class, v -> v != null && v.equalsIgnoreCase("true"));
    register(String.class, Boolean.class, v -> v != null && v.equalsIgnoreCase("true"));
    register(String.class, byte.class, Byte::valueOf);
    register(String.class, Byte.class, Byte::valueOf);
    register(String.class, short.class, Short::valueOf);
    register(String.class, Short.class, Short::valueOf);
    register(String.class, int.class, Integer::valueOf);
    register(String.class, Integer.class, Integer::valueOf);
    register(String.class, long.class, Long::valueOf);
    register(String.class, Long.class, Long::valueOf);
    register(String.class, float.class, Float::valueOf);
    register(String.class, Float.class, Float::valueOf);
    register(String.class, double.class, Double::valueOf);
    register(String.class, Double.class, Double::valueOf);
    register(String.class, String.class, v -> v);

    register(UUID.class, String.class, UUID::toString);
    register(String.class, UUID.class, UUID::fromString);

    register(Instant.class, long.class, Instant::toEpochMilli);
    register(long.class, Instant.class, Instant::ofEpochMilli);
  }

  @SuppressWarnings({"rawtypes", "unchecked"})
  public static <F, T> T convert(final Class from, final Class to, final F value) {
    final Map<Class, TypeConverter<?, ?>> map = CONVERTERS.get(from);
    if (map == null) {
      throw new SqlException(String.format("No type converters found from %s", from));
    }
    final TypeConverter<F, T> converter = (TypeConverter<F, T>) map.get(to);
    if (converter == null) {
      throw new SqlException(String.format("No type converters found from %s to %s", from, to));
    }
    return converter.apply(value);
  }

  public static <F, T> void register(final Class<F> from, final Class<T> to, final TypeConverter<F, T> converter) {
    CONVERTERS.computeIfAbsent(from, key -> new IdentityHashMap<>()).put(to, converter);
  }
}

