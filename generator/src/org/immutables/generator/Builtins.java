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

import com.google.common.base.CaseFormat;
import com.google.common.base.CharMatcher;
import com.google.common.base.Function;
import com.google.common.base.Functions;
import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import java.util.Map;
import java.util.Objects;

/**
 * Built-in operations, functions and accessors.
 * Planned to be moved to import/implicit resolution
 * rather that inheritance.
 */
public class Builtins {
  public final boolean $$true = true;
  public final boolean $$false = false;

  public final Output output = new Output();
  public final ClasspathAvailability classpath = new ClasspathAvailability();

  public final Predicate<Object> not =
      new Predicate<Object>() {
        @Override
        public boolean apply(Object input) {
          return !Intrinsics.$if(input);
        }

        @Override
        public String toString() {
          return Builtins.class.getSimpleName() + ".not";
        }
      };

  public final Predicate<Object> singular =
      new Predicate<Object>() {
        @Override
        public boolean apply(Object input) {
          return size.apply(input) == 1;
        }

        @Override
        public String toString() {
          return Builtins.class.getSimpleName() + ".singular";
        }
      };

  public final Function<Object, Integer> size =
      new Function<Object, Integer>() {
        @Override
        public Integer apply(Object input) {
          if (input instanceof Map<?, ?>) {
            return ((Map<?, ?>) input).size();
          }
          if (input instanceof Iterable<?>) {
            return Iterables.size((Iterable<?>) input);
          }
          if (input instanceof CharSequence) {
            return ((CharSequence) input).length();
          }
          return 1;
        }

        @Override
        public String toString() {
          return Builtins.class.getSimpleName() + ".size";
        }
      };

  public final Templates.Binary<Object, Object, Boolean> eq =
      new Templates.Binary<Object, Object, Boolean>() {
        @Override
        public Boolean apply(Object left, Object right) {
          if (left instanceof String || right instanceof String) {
            return String.valueOf(left).equals(String.valueOf(right));
          }
          return Objects.equals(left, right);
        }

        @Override
        public String toString() {
          return Builtins.class.getSimpleName() + ".eq";
        }
      };

  public final Templates.Binary<Object, Object, Boolean> eqref =
      new Templates.Binary<Object, Object, Boolean>() {
        @Override
        public Boolean apply(Object left, Object right) {
          return left == right;
        }

        @Override
        public String toString() {
          return Builtins.class.getSimpleName() + ".eq";
        }
      };

  public final Templates.Binary<Object, Object, Boolean> ne =
      new Templates.Binary<Object, Object, Boolean>() {
        @Override
        public Boolean apply(Object left, Object right) {
          return !eq.apply(left, right);
        }

        @Override
        public String toString() {
          return Builtins.class.getSimpleName() + ".ne";
        }
      };

  public final Templates.Binary<Object, Object, Boolean> and =
      new Templates.Binary<Object, Object, Boolean>() {
        @Override
        public Boolean apply(Object left, Object right) {
          return Intrinsics.$if(left) && Intrinsics.$if(right);
        }

        @Override
        public String toString() {
          return Builtins.class.getSimpleName() + ".and";
        }
      };

  public final Templates.Binary<Object, Object, Boolean> andnot =
      new Templates.Binary<Object, Object, Boolean>() {
        @Override
        public Boolean apply(Object left, Object right) {
          return Intrinsics.$if(left) && !Intrinsics.$if(right);
        }

        @Override
        public String toString() {
          return Builtins.class.getSimpleName() + ".andnot";
        }
      };

  public final Templates.Binary<Object, Object, Boolean> or =
      new Templates.Binary<Object, Object, Boolean>() {
        @Override
        public Boolean apply(Object left, Object right) {
          return Intrinsics.$if(left) || Intrinsics.$if(right);
        }

        @Override
        public String toString() {
          return Builtins.class.getSimpleName() + ".or";
        }
      };

  public final Templates.Binary<Object, Object, Boolean> ornot =
      new Templates.Binary<Object, Object, Boolean>() {
        @Override
        public Boolean apply(Object left, Object right) {
          return Intrinsics.$if(left) || !Intrinsics.$if(right);
        }

        @Override
        public String toString() {
          return Builtins.class.getSimpleName() + ".ornot";
        }
      };

  public final Literal literal = new Literal();

  public static final class Literal implements Function<Object, String> {

    @Override
    public String apply(Object input) {
      return StaticEnvironment.processing().getElementUtils().getConstantExpression(input);
    }

    public final Function<Object, String> string = new Function<Object, String>() {
      @Override
      public String apply(Object input) {
        return StringLiterals.toLiteral(String.valueOf(input));
      }
    };

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

      @Override
      public String toString() {
        return Literal.this.toString() + ".hex";
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

      @Override
      public String toString() {
        return Literal.this.toString() + ".bin";
      }
    };

    @Override
    public String toString() {
      return Builtins.class.getSimpleName() + ".literal";
    }
  }

  public final Function<Integer, String> emptyIfZero = new Function<Integer, String>() {
    @Override
    public String apply(Integer input) {
      return input == 0 ? "" : input.toString();
    }
  };

  public final Function<Object, String> toUpper =
      Functions.compose(
          CaseFormat.LOWER_CAMEL.converterTo(CaseFormat.UPPER_CAMEL),
          Functions.toStringFunction());

  public final Function<Object, String> toLower =
      Functions.compose(
          CaseFormat.UPPER_CAMEL.converterTo(CaseFormat.LOWER_CAMEL),
          Functions.toStringFunction());

  public final Function<Object, String> toConstant =
      Functions.compose(
          CaseFormat.LOWER_CAMEL.converterTo(CaseFormat.UPPER_UNDERSCORE),
          Functions.toStringFunction());

  // Does it belongs here?
  public final Function<Object, String> toSafeIdentifier = new Function<Object, String>() {
    private final CharMatcher digits = CharMatcher.inRange('0', '9');
    private final CharMatcher nonIdentifierChars =
        CharMatcher.is('_')
            .or(digits)
            .or(CharMatcher.inRange('a', 'z'))
            .or(CharMatcher.inRange('A', 'Z'))
            .negate();

    @Override
    public String apply(Object input) {
      String inputString = String.valueOf(input);
      if (inputString.isEmpty()) {
        return "_";
      }
      String safeString = nonIdentifierChars.replaceFrom(inputString, '_');
      if (digits.matches(safeString.charAt(0))) {
        return "_" + safeString;
      }
      return safeString;
    }

    @Override
    public String toString() {
      return Builtins.class.getSimpleName() + ".toSafeIdentifier";
    }
  };
}
