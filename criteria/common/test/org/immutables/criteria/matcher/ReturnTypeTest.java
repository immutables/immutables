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

import com.google.common.reflect.TypeToken;
import org.immutables.criteria.expression.Path;
import org.immutables.criteria.typemodel.BooleanHolderCriteria;
import org.immutables.criteria.typemodel.EnumHolderCriteria;
import org.immutables.criteria.typemodel.IntegerHolderCriteria;
import org.immutables.criteria.typemodel.LocalDateHolderCriteria;
import org.immutables.criteria.typemodel.StringHolderCriteria;
import org.immutables.criteria.typemodel.TypeHolder;
import org.junit.jupiter.api.Test;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import static org.immutables.check.Checkers.check;

/**
 * Check return and projection types for different types of matchers
 */
class ReturnTypeTest {

  @Test
  void localDate() {
    final LocalDateHolderCriteria criteria = LocalDateHolderCriteria.localDateHolder;
    check(Matchers.toExpression(criteria.value.min()).returnType()).is(LocalDate.class);
    check(Matchers.toExpression(criteria.value.max()).returnType()).is(LocalDate.class);
    check(Matchers.toExpression(criteria.value.count()).returnType()).is(Long.class);
    check(Matchers.toExpression(criteria.value).returnType()).is(LocalDate.class);
    check(Matchers.toExpression(criteria.optional).returnType()).is(new TypeToken<Optional<LocalDate>>() {}.getType());
    check(Matchers.toExpression(criteria.nullable).returnType()).is(LocalDate.class);
  }

  @Test
  void integer() {
    IntegerHolderCriteria criteria = IntegerHolderCriteria.integerHolder;
    check(Matchers.toExpression(criteria.value.min()).returnType()).is(Integer.class);
    check(Matchers.toExpression(criteria.value.max()).returnType()).is(Integer.class);
    check(Matchers.toExpression(criteria.value.avg()).returnType()).is(Double.class);
    check(Matchers.toExpression(criteria.value.sum()).returnType()).is(Long.class);
    check(Matchers.toExpression(criteria.value.count()).returnType()).is(Long.class);
    check(Matchers.toExpression(criteria.value).returnType()).is(int.class);

    check(Matchers.toExpression(criteria.optional.min()).returnType()).is(OptionalInt.class);
    check(Matchers.toExpression(criteria.optional.max()).returnType()).is(OptionalInt.class);
    check(Matchers.toExpression(criteria.optional.avg()).returnType()).is(OptionalDouble.class);
    check(Matchers.toExpression(criteria.optional.sum()).returnType()).is(OptionalLong.class);
    check(Matchers.toExpression(criteria.optional.count()).returnType()).is(Long.class);

    check(Matchers.toExpression(criteria.nullable.min()).returnType()).is(OptionalInt.class);
    check(Matchers.toExpression(criteria.nullable.max()).returnType()).is(OptionalInt.class);
    check(Matchers.toExpression(criteria.nullable.avg()).returnType()).is(OptionalDouble.class);
    check(Matchers.toExpression(criteria.nullable.sum()).returnType()).is(OptionalLong.class);
    check(Matchers.toExpression(criteria.nullable.count()).returnType()).is(Long.class);
  }

  @Test
  void booleanType() {
    BooleanHolderCriteria criteria = BooleanHolderCriteria.booleanHolder;
    check(Matchers.toExpression(criteria.value).returnType()).is(boolean.class);
    check(Matchers.toExpression(criteria.value.count()).returnType()).is(Long.class);

    check(projectionType(criteria.value)).is(Boolean.class);
    check(projectionType(criteria.nullable)).is(Boolean.class);
    check(projectionType(criteria.optional)).is(new TypeToken<Optional<Boolean>>() {}.getType());

  }

  @Test
  void enumType() {
    EnumHolderCriteria criteria = EnumHolderCriteria.enumHolder;
    check(Matchers.toExpression(criteria.value).returnType()).is(TypeHolder.Foo.class);
    check(Matchers.toExpression(criteria.nullable).returnType()).is(TypeHolder.Foo.class);
    check(Matchers.toExpression(criteria.optional).returnType()).is(new TypeToken<Optional<TypeHolder.Foo>>() {}.getType());
    check(projectionType(criteria.value)).is(TypeHolder.Foo.class);
    check(projectionType(criteria.nullable)).is(TypeHolder.Foo.class);
    check(projectionType(criteria.optional)).is(new TypeToken<Optional<TypeHolder.Foo>>() {}.getType());
  }

  @Test
  void string() {
    StringHolderCriteria criteria = StringHolderCriteria.stringHolder;
    check(Matchers.toExpression(criteria.value).returnType()).is(String.class);
    check(Matchers.toExpression(criteria.value.min()).returnType()).is(String.class);
    check(Matchers.toExpression(criteria.value.max()).returnType()).is(String.class);
    check(Matchers.toExpression(criteria.value.count()).returnType()).is(Long.class);
    check(Matchers.toExpression(criteria.optional).returnType()).is(new TypeToken<Optional<String>>() {}.getType());
    check(Matchers.toExpression(criteria.optional.min()).returnType()).is(new TypeToken<Optional<String>>() {}.getType());
    check(Matchers.toExpression(criteria.optional.max()).returnType()).is(new TypeToken<Optional<String>>() {}.getType());
    check(Matchers.toExpression(criteria.value.count()).returnType()).is(Long.class);

    check(projectionType(criteria.value)).is(String.class);
    check(projectionType(criteria.nullable)).is(String.class);
    check(projectionType(criteria.optional)).is(new TypeToken<Optional<String>>() {}.getType());
  }


  private static Type projectionType(Projection<?> projection) {
    try {
      return projectionTypeInternal(projection);
    } catch (ClassNotFoundException|NoSuchFieldException e) {
      final Path path = (Path) Matchers.toExpression(projection);
      throw new RuntimeException("For path " + path, e);
    }
  }
  /**
   * Get compile-time version of projection. P parameter for {@code Projection<P>}
   */
  private static Type projectionTypeInternal(Projection<?> projection) throws ClassNotFoundException, NoSuchFieldException {
    Objects.requireNonNull(projection, "projection");
    final Path path = (Path) Matchers.toExpression(projection);
    AnnotatedElement element = path.annotatedElement();
    final Class<?> declaringClass;
    if (element instanceof Field) {
      declaringClass = ((Field) element).getDeclaringClass();
    } else if (element instanceof Method) {
      declaringClass = ((Method) element).getDeclaringClass();
    } else {
      throw new IllegalArgumentException(String.format("Expected %s to be either field or method but was %s", path, element.getClass().getName()));
    }

    // manually get criteria class
    Class<?> criteriaClass = Class.forName(declaringClass.getPackage().getName() + "." + declaringClass.getSimpleName() + "Criteria");

    Field field = criteriaClass.getField(path.toStringPath());
    for (Type type: field.getType().getGenericInterfaces()) {
      if (type instanceof ParameterizedType && ((ParameterizedType) type).getRawType() == Projection.class) {
        // resolve Projection<P>
        Type resolved = TypeToken.of(field.getGenericType()).resolveType(type).getType();
        return ((ParameterizedType) resolved).getActualTypeArguments()[0];
      }
    }

    throw new IllegalArgumentException(String.format("Couldn't resolve type variable (%s) for %s %s", Projection.class.getSimpleName(), path, element));
  }

}
