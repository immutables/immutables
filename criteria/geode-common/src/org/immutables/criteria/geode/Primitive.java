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

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

enum Primitive {
  BOOLEAN(Boolean.TYPE, Boolean.class),
  BYTE(Byte.TYPE, Byte.class),
  CHAR(Character.TYPE, Character.class),
  SHORT(Short.TYPE, Short.class),
  INT(Integer.TYPE, Integer.class),
  LONG(Long.TYPE, Long.class),
  FLOAT(Float.TYPE, Float.class),
  DOUBLE(Double.TYPE, Double.class),
  VOID(Void.TYPE, Void.class),
  OTHER(null, null);

  public final Class primitiveClass;
  public final Class boxClass;

  private static final Map<Class, Primitive> PRIMITIVE_MAP = new HashMap<>();
  private static final Map<Class, Primitive> BOX_MAP = new HashMap<>();

  static {
    Primitive[] values = Primitive.values();
    for (Primitive value : values) {
      if (value.primitiveClass != null) {
        PRIMITIVE_MAP.put(value.primitiveClass, value);
      }
      if (value.boxClass != null) {
        BOX_MAP.put(value.boxClass, value);
      }
    }
  }

  Primitive(Class primitiveClass, Class boxClass) {
    this.primitiveClass = primitiveClass;
    this.boxClass = boxClass;
  }

  /**
   * Returns the Primitive object for a given primitive class.
   *
   * <p>For example, <code>of(long.class)</code> returns {@link #LONG}.
   * Returns {@code null} when applied to a boxing or other class; for example
   * <code>of(Long.class)</code> and <code>of(String.class)</code> return
   * {@code null}.
   */
  private static Primitive of(Type type) {
    return PRIMITIVE_MAP.get(type);
  }

  /**
   * Returns the Primitive object for a given boxing class.
   *
   * <p>For example, <code>ofBox(java.util.Long.class)</code>
   * returns {@link #LONG}.
   */
  private static Primitive ofBox(Type type) {
    //noinspection SuspiciousMethodCalls
    return BOX_MAP.get(type);
  }

  public static Primitive ofAny(Type type) {
    Primitive found = of(type);
    return found != null ? found : ofBox(type);
  }


  /**
   * Returns whether a given type is primitive.
   */
  public static boolean is(Type type) {
    //noinspection SuspiciousMethodCalls
    return PRIMITIVE_MAP.containsKey(type);
  }


  public Number cast(Number value) {
    switch (this) {
      case BYTE:
        return Byte.valueOf(value.byteValue());
      case DOUBLE:
        return Double.valueOf(value.doubleValue());
      case FLOAT:
        return Float.valueOf(value.floatValue());
      case INT:
        return Integer.valueOf(value.intValue());
      case LONG:
        return Long.valueOf(value.longValue());
      case SHORT:
        return Short.valueOf(value.shortValue());
      default:
        throw new AssertionError(this + ": " + value);
    }
  }

}
