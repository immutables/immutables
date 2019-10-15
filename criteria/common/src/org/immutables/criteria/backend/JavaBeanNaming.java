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

package org.immutables.criteria.backend;

import org.immutables.criteria.expression.Path;

import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.function.Predicate;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;

/**
 * Applies <a href="https://en.wikipedia.org/wiki/JavaBeans">JavaBean</a> naming strategy
 * to paths. JavaBean method usually start with {@code set}, {@code get} or {@code is}.
 *
 * This class derives path name {@code foo} from getter method {@code getFoo} or {@code isFoo}.
 * Note for {@code is*} prefix, method return type has to be boolean (or boxed boolean).
 */
public class JavaBeanNaming implements PathNaming {

  private static final Predicate<Method> MAYBE_GETTER = method -> Modifier.isPublic(method.getModifiers())
          && !Modifier.isStatic(method.getModifiers())
          && method.getReturnType() != Void.class
          && method.getParameterCount() == 0;

  private static final Predicate<Method> BOOLEAN_GETTER = method -> MAYBE_GETTER.test(method)
          && method.getName().startsWith("is")
          && method.getName().length() > "is".length()
          && (method.getReturnType() == boolean.class || method.getReturnType() == Boolean.class);

  private static final Predicate<Method> GENERIC_GETTER = method -> MAYBE_GETTER.test(method)
          && method.getName().startsWith("get")
          && method.getName().length() > "get".length();

  static final Predicate<Method> IS_GETTER = GENERIC_GETTER.or(BOOLEAN_GETTER);

  private static final Predicate<Method> IS_SETTER = method -> Modifier.isPublic(method.getModifiers())
          && !Modifier.isStatic(method.getModifiers())
          && method.getReturnType() == Void.class
          && method.getParameterCount() == 1
          && method.getName().startsWith("set")
          && method.getName().length() > "set".length();

  /**
   * See 8.8 Capitalization of inferred names (JavaBean spec)
   */
  private static final UnaryOperator<String> DECAPITALIZE = name -> {
    if (name.length() > 1 && Character.isUpperCase(name.charAt(0)) && Character.isUpperCase(name.charAt(1))) {
      // javabean specification return name unchanged if first 2 chars are upper case
      return name;
    }

    // first lowercase
    return Character.toLowerCase(name.charAt(0)) + name.substring(1);
  };

  @Override
  public String name(Path path) {
    return path.paths().stream().map(p -> maybeNameFromGetterSetter((Member) p)).collect(Collectors.joining("."));
  }

  private String maybeNameFromGetterSetter(Member member) {
    if (!(member instanceof Method)) {
      return member.getName();
    }

    Method method = (Method) member;
    String name = member.getName();
    if (IS_SETTER.test(method)) {
      return DECAPITALIZE.apply(name.substring("set".length()));
    }

    if (IS_GETTER.test(method)) {
      for (String prefix: Arrays.asList("is", "get")) {
        if (name.startsWith(prefix) && name.length() > prefix.length()) {
          return DECAPITALIZE.apply(name.substring(prefix.length()));
        }
      }
    }

    return name;
  }
}
