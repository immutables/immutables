/*
  Copyright 2017 Immutables Authors and Contributors

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
package org.immutables.fixture.with;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Optional;
import org.immutables.value.Value;

@Value.Immutable
public abstract class ForEquals {
    public abstract int myInt();

    public abstract Optional<Integer> myOptinalInt();

    public abstract List<Integer> myIntList();

    public abstract BigDecimal myBigDecimal();

    public abstract Optional<BigDecimal> myOptinalBigDecimal();

    public abstract List<BigDecimal> myBigDecimalList();

    public abstract RoundingMode myRoundingMode();

    public abstract Optional<RoundingMode> myOptinalRoundingMode();

    public abstract List<RoundingMode> myRoundingModeList();

    public abstract Object myObject();

    public abstract Optional<Object> myOptinalObject();

    public abstract List<Object> myObjectList();
}
