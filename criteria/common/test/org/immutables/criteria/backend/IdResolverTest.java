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

import org.immutables.criteria.backend.IdResolver;
import org.immutables.criteria.javabean.JavaBean1;
import org.immutables.criteria.personmodel.Person;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Member;
import java.lang.reflect.Method;

import static org.immutables.check.Checkers.check;


class IdResolverTest {

  @Test
  void immutables() throws NoSuchMethodException {
    IdResolver resolver = IdResolver.defaultResolver();
    Member member = resolver.resolve(Person.class);
    check(member.getName()).is("id");
    check(resolver.asPredicate().test(member));

    Method method = Person.class.getDeclaredMethod("fullName");
    check(!resolver.asPredicate().test(method));
  }

  @Test
  void javaBean() throws NoSuchMethodException {
    IdResolver resolver = IdResolver.defaultResolver();
    Member member = resolver.resolve(JavaBean1.class);
    check(member.getName()).is("string1");
    check(resolver.asPredicate().test(member));

    Method method = JavaBean1.class.getDeclaredMethod("getInt1");
    check(!resolver.asPredicate().test(method));
  }
}