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
package org.immutables.fixture;

import org.immutables.value.Value;

interface HidingImplementation {
  @Value.Style(visibility = Value.Style.ImplementationVisibility.PACKAGE)
  @Value.Immutable
  public abstract class Point {
    @Value.Parameter
    public abstract double x();

    @Value.Parameter
    public abstract double y();

    public static Point of(double x, double y) {
      return ImmutablePoint.of(x, y);
    }

    public static Builder builder() {
      return ImmutablePoint.builder();
    }

    // Signatures of abstract methods should match to be
    // overridden by implementation builder
    public interface Builder {
      Builder x(double x);

      Builder y(double y);

      Point build();
    }
  }
}