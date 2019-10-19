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

package org.immutables.criteria.reflect;

import org.junit.jupiter.api.Test;

import java.lang.reflect.Member;
import java.util.List;
import java.util.stream.Collectors;

import static org.immutables.check.Checkers.check;

class ClassScannerTest {

  private final ClassScanner scanner = ClassScanner.of(MyClass.class);

  @Test
  void fields() {
    List<String> names = scanner.skipMethods().stream().map(Member::getName).collect(Collectors.toList());
    check(names).isOf("anotherField", "base");
  }

  @Test
  void methods() {
    List<String> names = scanner.skipFields().stream().map(Member::getName).collect(Collectors.toList());
    check(names).hasContentInAnyOrder("anotherMethod", "fromIface", "fromIface", "getFromBase");
  }

  static abstract class Base {

    private int base = 0;

    public int getFromBase() {
      return 0;
    }
  }

  interface Iface {
    int fromIface();
  }

  static class MyClass extends Base implements Iface {
    private final String anotherField = "another";

    public String anotherMethod() {
      return "aaa";
    }

    @Override
    public int fromIface() {
      return 0;
    }
  }
}