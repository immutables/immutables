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

package org.immutables.criteria.expression;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static org.immutables.check.Checkers.check;

class PathTest {

  @Test
  void root() {
    check(Path.ofClass(Dummy.class).isRoot());
    check(Path.ofClass(Dummy.class).root()).is(Path.ofClass(Dummy.class));
    check(Path.ofClass(Dummy.class).root().root().isRoot());
    check(Path.ofClass(Dummy.class).root().root()).is(Path.ofClass(Dummy.class));
    check(Path.ofClass(Dummy.class).members()).isEmpty();
    check(Path.ofClass(Dummy.class).element()).is(Dummy.class);
    check(Path.ofClass(Dummy.class).toStringPath()).is("");
  }

  @Test
  void method() throws NoSuchMethodException {
    Method method = Dummy.class.getDeclaredMethod("getString");
    Path path = Path.ofMember(method);
    check(!path.isRoot());
    check(path.root()).is(Path.ofClass(Dummy.class));
    check(path.members()).isOf(method);
    check(path.toStringPath()).is("getString");
    check(path.element()).is(method);
  }

  @Test
  void field() throws NoSuchFieldException {
    Field field = Dummy.class.getDeclaredField("string");
    Path path = Path.ofMember(field);
    check(!path.isRoot());
    check(path.root()).is(Path.ofClass(Dummy.class));
    check(path.members()).isOf(field);
    check(path.toStringPath()).is("string");
    check(path.element()).is(field);
  }

  @Test
  void dummy2() throws NoSuchFieldException {
    Field dummy = Dummy.class.getDeclaredField("dummy2");
    Field dummy2 = Dummy2.class.getDeclaredField("string");
    Path path = Path.ofMember(dummy).append(dummy2);

    check(!path.isRoot());
    check(path.root()).is(Path.ofClass(Dummy.class));
    check(path.toStringPath()).is("dummy2.string");
    check(path.members()).isOf(dummy, dummy2);
  }

  static class Dummy {
    private String string;
    private Dummy2 dummy2;

    public String getString() {
      return string;
    }
  }

  static class Dummy2 {
    private String string;
  }
}