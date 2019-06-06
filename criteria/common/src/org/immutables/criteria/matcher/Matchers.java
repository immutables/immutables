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

package org.immutables.criteria.matcher;

import org.immutables.criteria.expression.Expression;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

public final class Matchers {

  private Matchers() {}

  interface HasContext {
    CriteriaContext context();
  }

  public static <R> BooleanMatcher<R> booleanMatcher(CriteriaContext context) {
    Objects.requireNonNull(context, "context");
    class Local implements BooleanMatcher.Self, HasContext {
      @Override
      public CriteriaContext context() {
        return context;
      }
    }

    return (BooleanMatcher<R>) new Local();
  }

  public static <R> StringMatcher<R> stringMatcher(CriteriaContext context) {
    Objects.requireNonNull(context, "context");
    class Local implements StringMatcher.Self, HasContext {
      @Override
      public CriteriaContext context() {
        return context;
      }
    }

    return (StringMatcher<R>) new Local();
  }

  public static <R, V extends Comparable<V>> ComparableMatcher<R, V> comparableMatcher(CriteriaContext context) {
    Objects.requireNonNull(context, "context");
    class Local implements ComparableMatcher.Self, HasContext {
      @Override
      public CriteriaContext context() {
        return context;
      }
    }
    return (ComparableMatcher<R, V>) new Local();
  }

  public static <R, V> ObjectMatcher<R, V> objectMatcher(CriteriaContext context) {
    Objects.requireNonNull(context, "context");
    class Local implements ObjectMatcher.Self<V>, HasContext {
      @Override
      public CriteriaContext context() {
        return context;
      }
    }
    return (ObjectMatcher<R, V>) new Local();
  }

  public static <R, S, C> OptionalMatcher<R, S, C> optionalMatcher(CriteriaContext context) {
    Objects.requireNonNull(context, "context");
    class Local implements OptionalMatcher, HasContext {
      @Override
      public CriteriaContext context() {
        return context;
      }
    }
    return new Local();
  }

  public static <R, S, C, V> CollectionMatcher<R, S, C, V> collectionMatcher(CriteriaContext context) {
    Objects.requireNonNull(context, "context");

    class Local implements CollectionMatcher.Self, HasContext {
      @Override
      public CriteriaContext context() {
        return context;
      }
    }

    return (CollectionMatcher<R, S, C, V>) new Local();
  }

  public static <T> T create(Class<T> type, CriteriaContext context) {
    Objects.requireNonNull(type, "type");

//    if (type.getSimpleName().equals("Self") && !type.isInterface()) {
//      return createWithReflection(type, context);
//    }

    if (type == BooleanMatcher.class || type == BooleanMatcher.Self.class) {
      return (T) booleanMatcher(context);
    } else if (type == StringMatcher.class || type == StringMatcher.Self.class) {
      return (T) stringMatcher(context);
    } else if (type == ComparableMatcher.class) {
      return (T) comparableMatcher(context);
    } else if (type == ObjectMatcher.class) {
      return (T) objectMatcher(context);
    } else if (type == OptionalMatcher.class) {
      return (T) optionalMatcher(context);
    } else if (type == CollectionMatcher.class) {
      return (T) collectionMatcher(context);
    }

    throw new UnsupportedOperationException("Don't know how to create matcher " + type.getName());
  }

  private static <T> T createWithReflection(Class<T> type, CriteriaContext context) {
    try {
      Constructor<T> ctor = type.getConstructor(CriteriaContext.class);
      return  ctor.newInstance(context);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  /**
   * Hacky (and temporary) reflection until we define proper sub-classes for criterias
   * (to hide Expressional implementation).
   */
  static CriteriaContext extract(Object object) {
    Objects.requireNonNull(object, "object");

    if (object instanceof HasContext) {
      return ((HasContext) object).context();
    }

    // TODO should be removed later
    return extractWithReflection(object);
  }

  static <C> UnaryOperator<Expression> toExpressionOperator(Supplier<C> supplier, UnaryOperator<C> expr) {
    return expression -> {
      final C initial = supplier.get();
      final C changed = expr.apply(initial);
      return Matchers.extract(changed).expression();
    };
  }

  static <C> UnaryOperator<Expression> toExpressionOperator3(CriteriaContext context, UnaryOperator<C> expr) {
    return expression -> {
      // creator1 changes ObjectCriteria.creator1() == Person
      // need to override creator1 otherwise ClassCastException
      // final C initial = factory.creator3().create(); // does not work
      final CriteriaCreator.TriFactory<?, ?, C> factory = context.factory3();
      final C initial = (C) context.withCreators(factory.creator3(), factory.creator2(), factory.creator3()).factory3().create3();
      final C changed = expr.apply(initial);
      return Matchers.extract(changed).expression();
    };
  }


  private static CriteriaContext extractWithReflection(Object object) {
    try {
      Class<?> current = object.getClass();
      while (current.getSuperclass() != null) {
        if (Arrays.stream(current.getDeclaredFields()).anyMatch(f -> f.getName().equals("context"))) {
          Field field = current.getDeclaredField("context");
          field.setAccessible(true);
          CriteriaContext context = (CriteriaContext) field.get(object);
          return context;
        }
        current = current.getSuperclass();
      }
    } catch (NoSuchFieldException | IllegalAccessException e) {
      throw new RuntimeException("No field in " + object.getClass().getName(), e);
    }

    throw new UnsupportedOperationException("No field context found in " + object.getClass().getName());
  }

}
