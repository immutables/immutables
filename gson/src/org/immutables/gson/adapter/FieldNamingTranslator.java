/*
    Copyright 2015 Immutables Authors and Contributors

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */
package org.immutables.gson.adapter;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.FieldNamingStrategy;
import com.google.gson.Gson;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Support class to apply field naming strategy supplied, or extracted from {@link Gson} instance
 * using internals-aware reflection.
 * Usage of this class applies restrictions on {@link FieldNamingStrategy} implementations,
 * only following properties may be queried from a field inside
 * {@link FieldNamingStrategy#translateName(Field)} method:
 * <ul>
 * <li>{@link Field#getDeclaringClass()} - Type which declares this field, will be actually abstract
 * class</li>
 * <li>{@link Field#getType()} - Raw type of field</li>
 * <li>{@link Field#getName()} - Attribute name</li>
 * </ul>
 * Standard implementations provided by {@link FieldNamingPolicy} works properly.
 * <p>
 * <em>
 * Note: Uses {@code sun.reflect.ReflectionFactory}, which is not portable.
 * Uses knowledge of {@link Gson} internals which might change in future versions of gson.
 * </em>
 */
@SuppressWarnings("restriction")
public final class FieldNamingTranslator {
  private static final AtomicBoolean complained = new AtomicBoolean();
  private static final sun.reflect.ReflectionFactory REFLECTION_FACTORY =
      sun.reflect.ReflectionFactory.getReflectionFactory();

  private final FieldNamingStrategy fieldNamingStrategy;

  public FieldNamingTranslator(Gson gson) {
    this(namingStrategyFrom(gson));
  }

  public FieldNamingTranslator(FieldNamingStrategy fieldNamingStrategy) {
    this.fieldNamingStrategy = fieldNamingStrategy;
  }

  public String translateName(
      Class<?> declaringClass,
      Class<?> type,
      String name,
      String overridenSerializedName) {

    if (!overridenSerializedName.isEmpty()) {
      return overridenSerializedName;
    }

    Field synthethicField = REFLECTION_FACTORY.newField(
        declaringClass,
        name,
        type,
        0,
        0,
        "",
        new byte[0]);

    return fieldNamingStrategy.translateName(synthethicField);
  }

  private static FieldNamingStrategy namingStrategyFrom(Gson gson) {
    Object factories = readPrivateField(gson, "factories");
    if (factories instanceof Iterable<?>) {
      for (Object factory : (Iterable<?>) factories) {
        if (factory instanceof com.google.gson.internal.bind.ReflectiveTypeAdapterFactory) {
          Object strategy = readPrivateField(factory, "fieldNamingPolicy");
          if (strategy instanceof FieldNamingStrategy) {
            return (FieldNamingStrategy) strategy;
          }
        }
      }
    }
    if (complained.compareAndSet(false, true)) {
      Logger.getLogger(FieldNamingTranslator.class.getName())
          .warning("Gson FieldNamingStrategy will not work for some serialized immutable objects: runtime incompatibility");
    }
    return FieldNamingPolicy.IDENTITY;
  }

  private static Object readPrivateField(Object object, String name) {
    try {
      java.lang.reflect.Field field = object.getClass().getDeclaredField(name);
      field.setAccessible(true);
      return field.get(object);
    } catch (Exception e) {
      return null;
    }
  }
}
