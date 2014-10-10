package org.immutables.generator;

import com.google.common.base.CaseFormat;
import com.google.common.base.Converter;
import com.google.common.base.Function;
import java.util.Objects;

/**
 * Experimental built-in operators. Planned to be moved to import/implicit resolution rather that
 * inheritance.
 */
public class BuiltinOperations {
  public final Function<Object, Boolean> not =
      new Function<Object, Boolean>() {
        @Override
        public Boolean apply(Object input) {
          return !Intrinsics.$if(input);
        }
      };

  public final Templates.Binary<Object, Object, Boolean> eq =
      new Templates.Binary<Object, Object, Boolean>() {
        @Override
        public Boolean apply(Object left, Object right) {
          return Objects.equals(left, right);
        }
      };

  public final Output output = new Output();

  public final Literal literal = new Literal();

  public static final class Literal implements Function<Object, String> {

    @Override
    public String apply(Object input) {
      return StaticEnvironment.processing().getElementUtils().getConstantExpression(input);
    }

    public final Function<Number, String> hex = new Function<Number, String>() {
      @Override
      public String apply(Number input) {
        if (input instanceof Long) {
          return "0x" + Long.toHexString(input.longValue()) + "L";
        }
        if (input instanceof Integer) {
          return "0x" + Integer.toHexString(input.intValue());
        }
        throw new IllegalArgumentException("unsupported non integer or long " + input);
      }
    };

    public final Function<Number, String> bin = new Function<Number, String>() {
      @Override
      public String apply(Number input) {
        if (input instanceof Long) {
          return "0b" + Long.toBinaryString(input.longValue()) + "L";
        }
        if (input instanceof Integer) {
          return "0b" + Integer.toBinaryString(input.intValue());
        }
        throw new IllegalArgumentException("unsupported non integer or long " + input);
      }
    };
  }

  public final Converter<String, String> toUpper =
      CaseFormat.LOWER_CAMEL.converterTo(CaseFormat.UPPER_CAMEL);

  public final Converter<String, String> toLower =
      CaseFormat.UPPER_CAMEL.converterTo(CaseFormat.LOWER_CAMEL);

  public final Converter<String, String> toConstant =
      CaseFormat.LOWER_CAMEL.converterTo(CaseFormat.UPPER_UNDERSCORE);
}
