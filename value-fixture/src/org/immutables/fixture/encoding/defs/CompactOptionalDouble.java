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

import java.util.Objects;
import java.util.OptionalDouble;
import org.immutables.encode.Encoding;
import org.immutables.encode.Encoding.StandardNaming;

@Encoding
class CompactOptionalDouble {
  @Encoding.Impl(virtual = true)
  private final OptionalDouble opt = OptionalDouble.empty();

  private final double value = opt.orElse(0);
  private final boolean present = opt.isPresent();

  @Encoding.Of
  static OptionalDouble from(Object ddd) {
    if (Double.isNaN((Double) ddd)) {
      return OptionalDouble.empty();
    }
    return OptionalDouble.of((Double) ddd);
  }

  @Encoding.Expose
  OptionalDouble get() {
    return present
        ? OptionalDouble.of(value)
        : OptionalDouble.empty();
  }

  @Encoding.Builder
  static final class Builder {
    private OptionalDouble builder = OptionalDouble.empty();

    @Encoding.Naming(standard = StandardNaming.INIT)
    @Encoding.Init
    void set(double value) {
      this.builder = OptionalDouble.of(value);
    }

    @Encoding.Copy
    @Encoding.Init
    void setOpt(OptionalDouble value) {
      this.builder = Objects.requireNonNull(value);
    }

    @Encoding.IsInit
    boolean isSet() {
      return builder.isPresent();
    }

    @Encoding.Build
    OptionalDouble build() {
      return this.builder;
    }
  }
}
