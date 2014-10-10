package org.immutables.generator;

import com.google.common.truth.TestVerb;
import com.google.common.truth.Truth;
import org.junit.Test;

public class BuiltinOperationsTest {
  final TestVerb check = Truth.assert_();
  final BuiltinOperations operations = new BuiltinOperations();

  @Test
  public void not() {
    check.that(operations.not.apply("")).isTrue();
    check.that(operations.not.apply(null)).isTrue();
    check.that(operations.not.apply(0)).isTrue();
    check.that(!operations.not.apply(1)).isTrue();
    check.that(!operations.not.apply("false"));// any non empty string is true, even "false"
  }

  @Test
  public void literal() {
    check.that(StringLiterals.toLiteral("x\u1111")).is("\"x\\u1111\"");
    check.that(operations.literal.bin.apply(0b1)).is("0b1");
    check.that(operations.literal.bin.apply(0b0L)).is("0b0L");
    check.that(operations.literal.hex.apply(0xff)).is("0xff");
    check.that(operations.literal.hex.apply(0xffL)).is("0xffL");
  }

  @Test
  public void eq() {
    check.that(operations.eq.apply(1, 1)).isTrue();
    check.that(operations.eq.apply("", "_")).isFalse();
  }

  @Test
  public void cases() {
    check.that(operations.toConstant.convert("attName")).is("ATT_NAME");
    check.that(operations.toLower.convert("AttName")).is("attName");
    check.that(operations.toUpper.convert("attName")).is("AttName");
  }
}
