/*
   Copyright 2016 Immutables Authors and Contributors

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
package org.immutables.fixture.encoding.defs;

import java.util.OptionalInt;
import org.immutables.encode.Encoding;
import static com.google.common.base.Preconditions.checkArgument;

@Encoding
final class OptionalIntWithSentinel {
  // TODO make sentinel an annotation parameter (or a static constant?) (non-static field would
// waste space!)
  private static final int SENTINEL_VALUE = 0;

  @Encoding.Impl
  private int value;

  public boolean equals(final OptionalIntWithSentinel that) {
    return this.value == that.value;
  }

  @Encoding.Expose
  public OptionalInt get() {
    if (value != SENTINEL_VALUE) {
      return OptionalInt.of(value);
    }
    return OptionalInt.empty();
  }

  @Override
  public int hashCode() {
    return value;
  }

  @Override
  public String toString() {
    return String.valueOf(get());
  }

  @Encoding.Of
  static int initFrom(OptionalInt optional) {
    if (optional.isPresent()) {
      return safeValue(optional.getAsInt());
    }
    return SENTINEL_VALUE;
  }

  @Encoding.Copy
  int withOptional(OptionalInt optional) {
    return initFrom(optional);
  }

  @Encoding.Copy
  int withInt(int intValue) {
    return safeValue(intValue);
  }

  @Encoding.Copy
  int withInteger(Integer integer) {
    return integer != null ? safeValue(integer.intValue()) : SENTINEL_VALUE;
  }

  private static int safeValue(int intValue) {
    checkArgument(intValue != SENTINEL_VALUE, "Field value can't match the sentinel");
    return intValue;
  }

//  @Encoding.Init
//  static int initFrom(Integer value) {
//    if (value != null) {
//      checkArgument(value != 0, "Field value can't match the sentinel");
//      return value;
//    } else {
//      return 0;
//    }
//  }
//
//  @Encoding.Init
//  static int initFrom(int value) {
//    checkArgument(value != 0, "Field value can't match the sentinel");
//    return value;
//  }

  @Encoding.Builder
  static final class Builder {
    private int value = SENTINEL_VALUE;

    @Encoding.Build
    int build() {
      return value;
    }

    // wrong? YES it's wrong, return type should match impl field name
//    @Encoding.Build
//    OptionalInt build() {
//      if (value != 0) {
//        return OptionalInt.of(value);
//      } else {
//        return OptionalInt.empty();
//      }
//    }

    @Encoding.Init
    @Encoding.Copy
    void setFrom(OptionalInt optional) {
      if (optional.isPresent()) {
        setFromInt(optional.getAsInt());
      } else {
        value = SENTINEL_VALUE;
      }
    }

    @Encoding.Init
    void setFromInteger(Integer value) {
      if (value != null) {
        setFromInt(value.intValue());
      } else {
        value = SENTINEL_VALUE;
      }
    }

    @Encoding.Init
    void setFromInt(int value) {
      this.value = safeValue(value);
    }
  }
}
