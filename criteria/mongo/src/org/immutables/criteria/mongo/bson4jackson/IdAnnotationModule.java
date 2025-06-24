/*
 * Copyright 2019-2025 Immutables Authors and Contributors
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

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.PropertyName;
import com.fasterxml.jackson.databind.introspect.Annotated;
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector;
import org.immutables.criteria.Criteria;

/**
 * Allows mapping of {@code ID} property (usually declared as {@link Criteria.Id})
 * to {@code _id} attribute in mongo document.
 *
 * <p>Uses simple heuristics to identify ID attribute:
 * <ul>
 *   <li>For fields, it will apply predicate directly</li>
 *   <li>For methods, check current (or parent) class method as well interface definition of the method</li>
 * </ul>
 *
 * <p>It is also possible to register custom {@code ID} annotation using {@link #fromAnnotation(Class)} </p>
 */
public class IdAnnotationModule extends Module {

  private static final Class<Criteria.Id> DEFAULT_ID_ANNOTATION = Criteria.Id.class;

  private final Predicate<? super AnnotatedElement> predicate;

  public IdAnnotationModule() {
    this(a -> a.isAnnotationPresent(DEFAULT_ID_ANNOTATION));
  }

  private IdAnnotationModule(Predicate<? super AnnotatedElement> predicate) {
    super();
    this.predicate = Objects.requireNonNull(predicate, "predicate");
  }

  /**
   * Derive {@code _id} property from existing annotation. Allows using other annotations
   * than {@link Criteria.Id}
   */
  public static IdAnnotationModule fromAnnotation(Class<? extends Annotation> annotation) {
    Objects.requireNonNull(annotation, "annotation");
    return fromPredicate(m -> m instanceof AnnotatedElement && ((AnnotatedElement) m).isAnnotationPresent(annotation));
  }

  /**
   * Derive {@code _id} property from {@link Member} predicate.
   */
  public static IdAnnotationModule fromPredicate(Predicate<? super Member> predicate) {
    Objects.requireNonNull(predicate, "predicate");
    Predicate<AnnotatedElement> newPred = a -> a instanceof Member && predicate.test((Member) a);
    return new IdAnnotationModule(newPred);
  }

  @Override
  public String getModuleName() {
    return getClass().getSimpleName();
  }

  @Override
  public Version version() {
    return Version.unknownVersion();
  }

  @Override
  public void setupModule(SetupContext context) {
    context.insertAnnotationIntrospector(new IdAnnotationIntrospector(predicate));
  }

  /**
   * Introspector for serializing and deserializing attributes maked with {@literal @}{@code Id} annotation
   * as {@code _id}
   */
  private static class IdAnnotationIntrospector extends NopAnnotationIntrospector {

    private final Predicate<? super AnnotatedElement> predicate;

    IdAnnotationIntrospector(Predicate<? super AnnotatedElement> predicate) {
      this.predicate = Objects.requireNonNull(predicate, "predicate");
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
    private Optional<AnnotatedElement> findOriginalMethod(AnnotatedElement annotated) {
      if (annotated != null && predicate.test(annotated)) {
        return Optional.of(annotated);
      }

      if (annotated instanceof Field) {
        // for fields try to find corresponding method (maybe annotated with @Id)
        // usually field is defined in ImmutableFoo.Json class and implements Foo
        final Field field = (Field) annotated;
        final Optional<Method> maybe = Arrays.stream(field.getDeclaringClass().getMethods())
                .filter(m -> m.getParameterCount() == 0)
                .filter(m -> m.getName().equals(field.getName()))
                .findAny();

        return maybe.flatMap(this::findOriginalMethod);
      }

      if (!(annotated instanceof Method)) {
        // something else. We support only Fields and Methods
        return Optional.empty();
      }

      final Method method = (Method) annotated;

      if (predicate.test(method)) {
        return Optional.of(method);
      }

      // Here we want may want to find from associated setter or getter,
      // including switching between two - i.e. having a setter - look at a getter
      // and its hierarchy

      String name = method.getName();
      Class<?>[] params = method.getParameterTypes();
      Class<?> declaringClass = method.getDeclaringClass();

      boolean maybeGetter = params.length == 0;
      boolean maybeSetter = params.length == 1;

      if (maybeSetter) {
        String baseName = name.startsWith("set") ? name.substring("set".length()) : name;
        if (baseName.isEmpty()) return Optional.empty();

        String decapitalize = Character.toLowerCase(baseName.charAt(0)) + baseName.substring(1);
        Optional<AnnotatedElement> getter = tryFindMatchingGetter(declaringClass, decapitalize);
        if (getter.isPresent()) return getter;

        getter = tryFindMatchingGetter(declaringClass, "get" + baseName);
        if (getter.isPresent()) return getter;
      }

      if (maybeGetter) {
        // we've already tried to match method itself, so we go to supertypes
        Optional<AnnotatedElement> getter = tryFindMatchingGetterFromSupertypes(declaringClass, name);
        if (getter.isPresent()) return getter;
      }

      return Optional.empty();
    }

    private Optional<AnnotatedElement> tryFindMatchingGetter(Class<?> declaringClass, String name) {
      try {
        Method method = declaringClass.getDeclaredMethod(name);
        if (predicate.test(method)) {
          return Optional.of(method);
        }
      } catch (NoSuchMethodException e) {
        //continue
      }
      return tryFindMatchingGetterFromSupertypes(declaringClass, name);
    }

    private Optional<AnnotatedElement> tryFindMatchingGetterFromSupertypes(Class<?> declaringClass, String name) {
      Class<?> superclass = declaringClass.getSuperclass();
      if (superclass != null) {
        Optional<AnnotatedElement> getter = tryFindMatchingGetter(superclass, name);
        if (getter.isPresent()) return getter;
      }

      for (Class<?> iface : declaringClass.getInterfaces()) {
        Optional<AnnotatedElement> getter = tryFindMatchingGetter(iface, name);
        if (getter.isPresent()) return getter;
      }

      return Optional.empty();
    }
  }
}
