package org.immutables.modeling;

import java.util.Objects;

/**
 * Experimental built-in operators. Planned to be moved to import/implicit resolution rather that
 * inheritance
 */
public class Operators {
  public final Templates.Unary<Object, Boolean> not =
      new Templates.Unary<Object, Boolean>() {
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
}
