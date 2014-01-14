/*
    Copyright 2013-2014 Immutables.org authors

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
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import java.util.Iterator;
import java.util.List;
import javax.annotation.concurrent.Immutable;
import static com.google.common.base.Preconditions.*;

@Beta
@Immutable
public final class Polygon {
  private static final double EPS = 1e-6;

  private final ImmutableList<Point> vertices;

  private final double maxLatitude;
  private final double maxLongitude;
  private final double minLatitude;
  private final double minLongitude;

  Polygon(ImmutableList<Point> vertices) {
    checkArgument(vertices.size() >= 3, "shape must have at least 3 vertex");
    this.vertices = vertices;

    Point first = vertices.get(0);

    double maxLatitude = first.latitude();
    double maxLongitude = first.longitude();
    double minLatitude = first.latitude();
    double minLongitude = first.longitude();

    for (Point point : vertices) {
      if (point.latitude() > maxLatitude) {
        maxLatitude = point.latitude();
      }
      if (point.latitude() < minLatitude) {
        minLatitude = point.latitude();
      }
      if (point.longitude() > maxLongitude) {
        maxLongitude = point.longitude();
      }
      if (point.longitude() < minLongitude) {
        minLongitude = point.longitude();
      }
    }

    this.maxLatitude = maxLatitude;
    this.maxLongitude = maxLongitude;
    this.minLatitude = minLatitude;
    this.minLongitude = minLongitude;
  }

  public static Polygon of(List<Point> points) {
    return new Polygon(ImmutableList.copyOf(points));
  }

  public List<Point> shape() {
    return vertices;
  }

  public double maxLatitude() {
    return maxLatitude;
  }

  public double maxLongitude() {
    return maxLongitude;
  }

  public double minLatitude() {
    return minLatitude;
  }

  public double minLongitude() {
    return minLongitude;
  }

  public boolean withinBounds(Point spatial) {
    return isBetween(spatial.latitude(), minLatitude, maxLatitude)
        && isBetween(spatial.longitude(), minLongitude, maxLongitude);
  }

  @SuppressWarnings("incomplete-switch")
  public boolean contains(Point spatial) {
    if (!withinBounds(spatial)) {
      return false;
    }

    boolean inside = false;

    Iterator<Point> iterator = Iterables.cycle(vertices).iterator();
    Point first = iterator.next();
    Point start = first;
    Point originalStart = start;
    Point end = first;
    Point originalEnd = end;
    boolean repeat = false;
    boolean lastWasRepeat;

    do {
      lastWasRepeat = false;
      if (!repeat) {
        end = iterator.next();
        originalEnd = end;
      } else {
        repeat = false;
        lastWasRepeat = true;
      }

      switch (intersectionType(start, end, spatial)) {
      case EDGE:
        inside = !inside;
        break;
      case ON_BOUND:
        return true;
      case END_VERTEX:
        end = Point.of(end.latitude() + 2 * EPS, end.longitude());
        repeat = true;
        break;
      }

      if (!repeat) {
        start = end;
        originalStart = originalEnd;
      }
    } while (!originalStart.equals(first));

    if (lastWasRepeat) {
      end = iterator.next();

      switch (intersectionType(start, end, spatial)) {
      case EDGE:
        inside = !inside;
        break;
      case ON_BOUND:
        return true;
      case NONE:
      }
    }

    return inside;
  }

  private enum IntersectionType {
    NONE, EDGE, ON_BOUND, END_VERTEX
  }

  private static IntersectionType intersectionType(Point start, Point end, Point spatial) {
    if (isSameLatitude(start, end)) {
      if (isSameLatitude(spatial, start)) {
        if (isBetween(spatial.longitude(), start.longitude(), end.longitude())) {
          return IntersectionType.ON_BOUND;
        }
      }
    }

    if (!isLess(start.longitude(), spatial.longitude())
        || !isLess(end.longitude(), spatial.longitude())) {
      if (isEqual(start, spatial) || isEqual(end, spatial)) {
        return IntersectionType.ON_BOUND;
      }

      double intersectionLatitude = spatial.latitude();
      double intersectionLongitude = intersectionLongitude(start, end, intersectionLatitude);

      if (areEqual(intersectionLongitude, spatial.longitude())
          && isBetween(intersectionLongitude, start.longitude(), end.longitude())
          && isBetween(intersectionLatitude, start.latitude(), end.latitude())) {
        return IntersectionType.ON_BOUND;
      }

      if (areEqual(start.latitude(), intersectionLatitude)
          && areEqual(start.longitude(), intersectionLongitude)
          && isLess(spatial.longitude(), intersectionLongitude)) {
        return IntersectionType.NONE;
      }

      if (areEqual(end.latitude(), intersectionLatitude)
          && areEqual(end.longitude(), intersectionLongitude)
          && isLess(spatial.longitude(), intersectionLongitude)) {
        return IntersectionType.END_VERTEX;
      }

      if (isLess(spatial.longitude(), intersectionLongitude)
          && isBetween(intersectionLongitude, start.longitude(), end.longitude())
          && isBetween(intersectionLatitude, start.latitude(), end.latitude())) {
        return IntersectionType.EDGE;
      }
    }

    return IntersectionType.NONE;
  }

  private static double intersectionLongitude(Point start, Point end, double intersectionLatitude) {
    return (intersectionLatitude * (start.longitude() - end.longitude())
        - start.longitude() * end.latitude()
        + end.longitude() * start.latitude())
        / (start.latitude() - end.latitude());
  }

  private static boolean isSameLatitude(Point spatial1, Point spatial2) {
    return areEqual(spatial1.latitude(), spatial2.latitude());
  }

  private static boolean areEqual(double a, double b) {
    return Math.abs(a - b) < EPS;
  }

  private static boolean isEqual(Point spatial1, Point spatial2) {
    return areEqual(spatial1.latitude(), spatial2.latitude())
        && areEqual(spatial1.longitude(), spatial2.longitude());
  }

  private static boolean isBetween(double value, double min, double max) {
    if (min > max) {
      double temp = min;
      min = max;
      max = temp;
    }

    return min - EPS < value
        && max + EPS > value;
  }

  private static boolean isLess(double value1, double value2) {
    return value2 - value1 > EPS;
  }
}
