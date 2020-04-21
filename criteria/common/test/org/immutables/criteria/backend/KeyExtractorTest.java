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

import org.immutables.criteria.expression.Visitors;
import org.immutables.criteria.javabean.JavaBean1;
import org.immutables.criteria.personmodel.ImmutablePerson;
import org.immutables.criteria.personmodel.Person;
import org.immutables.criteria.personmodel.PersonGenerator;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Member;

import static org.immutables.check.Checkers.check;

class KeyExtractorTest {

  @Test
  void immutables() {
    KeyExtractor extractor = KeyExtractor.defaultFactory().create(Person.class);
    check(extractor.metadata().isKeyDefined());
    check(extractor.metadata().isExpression());
    check(extractor.metadata().keys()).hasSize(1);

    Member member = (Member) Visitors.toPath(extractor.metadata().keys().get(0)).element();
    check(member.getName()).is("id");

    ImmutablePerson person = new PersonGenerator().next();
    check(extractor.extract(person)).is(person.id());
  }

  @Test
  void immutables2() {
    KeyExtractor extractor = KeyExtractor.defaultFactory().create(ImmutablePerson.class);
    check(extractor.metadata().isKeyDefined());
    check(extractor.metadata().isExpression());
    check(extractor.metadata().keys()).hasSize(1);

    Member member = (Member) Visitors.toPath(extractor.metadata().keys().get(0)).element();
    check(member.getName()).is("id");
  }

  @Test
  void javaBean() {
    KeyExtractor extractor = KeyExtractor.defaultFactory().create(JavaBean1.class);
    check(extractor.metadata().isKeyDefined());
    check(extractor.metadata().isExpression());
    check(extractor.metadata().keys()).hasSize(1);

    Member member = (Member) Visitors.toPath(extractor.metadata().keys().get(0)).element();
    check(member.getName()).is("string1");

    JavaBean1 bean1 = new JavaBean1();
    bean1.setString1("foo");
    check(extractor.extract(bean1)).is("foo");
  }

  @Test
  void generic() {
    KeyExtractor e1 = KeyExtractor.generic(obj -> ((WithAnnotation) obj).id123).create(WithAnnotation.class);
    check(!e1.metadata().isExpression());
    Assertions.assertThrows(UnsupportedOperationException.class, () -> e1.metadata().keys());
    check(e1.metadata().isKeyDefined());
    check(e1.extract(new WithAnnotation("foo"))).is("foo");
  }

  @Test
  void generic_withoutAnnotation() {
    KeyExtractor e2 = KeyExtractor.generic(obj -> ((WithoutAnnotation) obj).id123).create(WithoutAnnotation.class);
    check(!e2.metadata().isExpression());
    Assertions.assertThrows(UnsupportedOperationException.class, () -> e2.metadata().keys());
    check(e2.metadata().isKeyDefined());
    check(e2.extract(new WithoutAnnotation("foo"))).is("foo");
  }

  /**
   * Check that annotation name is exposed in error message
   */
  @Test
  void fromAnnotation() {
    KeyExtractor extractor = KeyExtractor.fromAnnotation(MyAnnotation.class).create(WithAnnotation.class);

    check(extractor.metadata().keys()).hasSize(1);
    Member member = (Member) Visitors.toPath(extractor.metadata().keys().get(0)).element();
    check(member.getName()).is("id123");
    check(extractor.extract(new WithAnnotation("foo"))).is("foo");
  }

  @Test
  void withoutAnnotation() {
    KeyExtractor.Factory factory = KeyExtractor.fromAnnotation(MyAnnotation.class);
    Throwable ex = Assertions.assertThrows(IllegalArgumentException.class, () -> factory.create(WithoutAnnotation.class));
    check(ex.getMessage()).contains(MyAnnotation.class.getSimpleName());
  }

  @Test
  void noKey() {
    KeyExtractor extractor = KeyExtractor.noKey().create(WithAnnotation.class);
    check(!extractor.metadata().isExpression());
    check(!extractor.metadata().isKeyDefined());
    check(extractor.metadata().keys()).isEmpty();
    Assertions.assertThrows(UnsupportedOperationException.class, () -> extractor.extract(new WithAnnotation("foo")));
  }

  @Test
  void multipleAnnotations() {
    KeyExtractor extractor = KeyExtractor.fromAnnotation(MyAnnotation.class).create(MultipleAnnotations.class);

    check(extractor.metadata().isKeyDefined());
    check(extractor.metadata().isExpression());
    check(extractor.metadata().keys()).hasSize(2);

    check((Iterable<String>) extractor.extract(new MultipleAnnotations("foo"))).isOf("foo", "getter:foo");
  }

  @Retention(RetentionPolicy.RUNTIME)
  @interface MyAnnotation {}

  static class WithAnnotation {
    @MyAnnotation
    private final String id123;

    WithAnnotation(String id123) {
      this.id123 = id123;
    }
  }

  static class WithoutAnnotation {
    private final String id123;

    WithoutAnnotation(String id123) {
      this.id123 = id123;
    }
  }

  static class MultipleAnnotations {
    @MyAnnotation
    private final String id;

    private MultipleAnnotations(String id) {
      this.id = id;
    }

    @MyAnnotation
    public String getOtherId() {
      return "getter:" + id;
    }
  }
}