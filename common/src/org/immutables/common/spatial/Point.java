/*
    Copyright 2013-2014 Immutables Authors and Contributors

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
package org.immutables.common.spatial;

import com.google.common.annotations.Beta;
import com.google.common.primitives.Doubles;
import javax.annotation.concurrent.Immutable;

@Beta
@Immutable
public final class Point {
  private static final Point ZERO = new Point(0, 0);

  private final double latitude;
  private final double longitude;

  private Point(double latitude, double longitude) {
    this.latitude = latitude;
    this.longitude = longitude;
  }

  public static Point of(double latitude, double longitude) {
    if (latitude == 0 && longitude == 0) {
      return zero();
    }
    return new Point(latitude, longitude);
  }

  public static Point zero() {
    return ZERO;
  }

  public double latitude() {
    return latitude;
  }

  public double longitude() {
    return longitude;
  }

  @Override
  public int hashCode() {
    int hash = 17;
    hash *= 31 + Doubles.hashCode(latitude);
    hash *= 31 + Doubles.hashCode(longitude);
    return hash;
  }

  @Override
  public boolean equals(Object o) {
    return (o instanceof Point)
        && ((Point) o).latitude() == latitude
        && ((Point) o).longitude() == longitude;
  }

  @Override
  public String toString() {
    return "(" + latitude + ", " + longitude + ")";
  }
}
