/*
    Copyright 2014 Immutables Authors and Contributors

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
package org.immutables.generator;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.junit.Test;
import static org.immutables.check.Checkers.*;

public class BuiltinOperationsTest {
  final Builtins operations = new Builtins();

  @Test
  public void not() {
    check(operations.not.apply(""));
    check(operations.not.apply(ImmutableList.of()));
    check(!operations.not.apply(ImmutableList.of(1)));
    check(operations.not.apply(null));
    check(operations.not.apply(0));
    check(!operations.not.apply(1));
    check(!operations.not.apply("false"));
  }

  @Test
  public void literal() {
    check(StringLiterals.toLiteral("x\u1111")).is("\"x\\u1111\"");
    check(operations.literal.bin.apply(0b1)).is("0b1");
    check(operations.literal.bin.apply(0b0L)).is("0b0L");
    check(operations.literal.hex.apply(0xff)).is("0xff");
    check(operations.literal.hex.apply(0xffL)).is("0xffL");
  }

  @Test
  public void eq() {
    check(operations.eq.apply(1, 1));
    check(!operations.eq.apply("", "_"));
  }

  @Test
  public void cases() {
    check(operations.toConstant.convert("attName")).is("ATT_NAME");
    check(operations.toLower.convert("AttName")).is("attName");
    check(operations.toUpper.convert("attName")).is("AttName");
  }

  @Test
  public void size() {
    check(operations.size.apply(ImmutableList.of(1))).is(1);
    check(operations.size.apply(ImmutableList.of())).is(0);
    check(operations.size.apply("")).is(0);
    check(operations.size.apply("12")).is(2);
    check(operations.size.apply(ImmutableMap.of())).is(0);
    check(operations.size.apply(ImmutableMap.of(1, 1))).is(1);
  }

  @Test
  public void toSafeIdentifier() {
    check(operations.toSafeIdentifier.apply("&&&")).is("___");
    check(operations.toSafeIdentifier.apply("a_B1")).is("a_B1");
    check(operations.toSafeIdentifier.apply("")).is("_");
    check(operations.toSafeIdentifier.apply("-11")).is("_11");
    check(operations.toSafeIdentifier.apply("987")).is("_987");
  }
}
