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

package org.immutables.criteria.mongo.bson4jackson;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector;
import org.immutables.criteria.Criteria;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;

/**
 * Allows mapping of {@code ID} property (declared as {@link Criteria.Id})
 * to {@code _id} attribute in mongo document.
 */
public class IdAnnotationModule extends Module {

  private static final Class<Criteria.Id> CRITERIA_ID_CLASS = Criteria.Id.class;

  @Override
  public String getModuleName() {
    return "Criteria Module";
  }

  @Override
  public Version version() {
    return Version.unknownVersion();
  }

  @Override
  public void setupModule(SetupContext context) {
    IdAnnotationIntrospector introspector = new IdAnnotationIntrospector(m -> m.isAnnotationPresent(CRITERIA_ID_CLASS));
    context.insertAnnotationIntrospector(introspector);
  }

  /**
   * Introspector for serializing and deserializing attributes maked with {@literal @}{@code Id} annotation
   * as {@code _id}
   */
  private static class IdAnnotationIntrospector extends NopAnnotationIntrospector {

    private final Predicate<? super AnnotatedElement> idPredicate;

    IdAnnotationIntrospector(Predicate<? super AnnotatedElement> predicate) {
      this.idPredicate = Objects.requireNonNull(predicate, "predicate");
    }

    @Override
    public PropertyName findNameForDeserialization(Annotated annotated) {
      return derivePropertyName(annotated);
    }

    @Override
    public PropertyName findNameForSerialization(Annotated annotated) {
      return derivePropertyName(annotated);
    }

    private PropertyName derivePropertyName(Annotated annotated) {
      return findOriginalMethod(annotated.getAnnotated())
              .map(m -> PropertyName.construct("_id"))
              .orElse(null);
    }

    /**
     * Uses reflection to find original method marked with {@link Criteria.Id}.
     */
    private Optional<Member> findOriginalMethod(AnnotatedElement annotated) {

      if (annotated instanceof Field) {
        // for fields try to find corresponding method (annotated with @Id)
        // usually field is defined in ImmutableFoo.Json class and implements Foo
        final Field field = (Field) annotated;
        final Optional<Method> maybe = Arrays.stream(field.getDeclaringClass().getMethods())
                .filter(m -> m.getParameterCount() == 0)
                .filter(m -> m.getName().equals(field.getName()))
                .findAny();

        return maybe.flatMap(this::findOriginalMethod);
      }

      if (!(annotated instanceof Method)) {
        return Optional.empty();
      }

      final Method method = (Method) annotated;

      if (idPredicate.test(method)) {
        return Optional.of(method);
      }

      final String name = method.getName();
      final Class<?>[] params = method.getParameterTypes();
      // check for available interfaces
      for (Class<?> iface: method.getDeclaringClass().getInterfaces()) {
        try {
          final Method maybe = iface.getMethod(name, params);
          if (idPredicate.test(maybe)) {
            return Optional.of(maybe);
          }
        } catch(NoSuchMethodException ignore) {
          // continue
        }
      }

      return Optional.empty();
    }
  }
}
