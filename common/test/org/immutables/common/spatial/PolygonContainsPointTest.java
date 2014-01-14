/*
    Copyright 2013 Immutables.org authors

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

import com.google.common.base.CharMatcher;
import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.List;
import org.junit.Test;
import static com.google.common.base.Preconditions.*;
import static org.immutables.check.Checkers.*;

public class PolygonContainsPointTest {
  private static final Splitter FROM_STRING_SPLITTER =
      Splitter.on(CharMatcher.anyOf(",()[]{}")).trimResults().omitEmptyStrings();
  private static final Splitter MULTIPLE_FROM_STRING_SPLITTER =
      Splitter.on("),(").trimResults().omitEmptyStrings();

  /**
   * Function, that convert String to Point.
   * Accepted formats : "(x, y)", "x, y", "(x,y)", "x,y", where x and y are string presentations of
   * double values.
   * @return String => Point
   */
  public static Function<String, Point> fromString() {
    return FromString.FUNCTION;
  }

  private enum FromString implements Function<String, Point> {
    FUNCTION;

    @Override
    public Point apply(String input) {
      ImmutableList<String> coordinates =
          ImmutableList.copyOf(FROM_STRING_SPLITTER.split(checkNotNull(input).replace(" ", "")));
      checkState(coordinates.size() == 2);

      return Point.of(Double.parseDouble(coordinates.get(0)), Double.parseDouble(coordinates.get(1)));
    }
  }

  /**
   * Function, that convert String to list of Point.
   * Accepted formats : "[(x1, y1), (x2, y2)]", "[(x1,y1),(x2,y2)]", "(x1, y1), (x2, y2)",
   * "(x1,y1),(x2,y2)",
   * where x and y are string presentations of double values.
   * @return String => List<Point>
   */
  public static Function<String, List<Point>> multipleFromString() {
    return MultipleFromString.FUNCTION;
  }

  private enum MultipleFromString implements Function<String, List<Point>> {
    FUNCTION;

    @Override
    public List<Point> apply(String input) {
      List<String> stringPoints =
          ImmutableList.copyOf(MULTIPLE_FROM_STRING_SPLITTER.split(checkNotNull(input).replace(" ", "")));

      return Lists.transform(stringPoints, fromString());
    }
  }

  private static final Polygon SHAPE_1 = Polygon.of(
      multipleFromString().apply("(1,1),(-2,0),(2,-2)"));

  private static final Polygon SHAPE_2 = Polygon.of(
      multipleFromString().apply("(1,-3),(-1,-1),(1,1),(3,-1)"));

  private static final Polygon SHAPE_3 = Polygon.of(
      multipleFromString().apply("(5,2),(4,1),(1,1),(1,3),(5,7),(5,4),(4,4)"));

  @Test
  public void withinBounds() {
    for (Point point : SHAPE_1.vertices()) {
      check(SHAPE_1.withinBounds(point));
    }

    check(SHAPE_1.withinBounds(Point.of(0, 0)));

    check(SHAPE_1.withinBounds(Point.of(-1, -1)));
    check(SHAPE_1.withinBounds(Point.of(2, 1)));

    check(!SHAPE_1.withinBounds(Point.of(-1, 1.00001)));
    check(!SHAPE_1.withinBounds(Point.of(3, 0)));
    check(!SHAPE_1.withinBounds(Point.of(0, -5)));
    check(!SHAPE_1.withinBounds(Point.of(-2.11111, 1)));
  }

  @Test
  public void containsInTriangle() {
    check(!SHAPE_1.contains(Point.of(-1, 1.00001)));
    check(!SHAPE_1.contains(Point.of(3, 0)));
    check(!SHAPE_1.contains(Point.of(0, -5)));
    check(!SHAPE_1.contains(Point.of(-2.11111, 1)));

    for (Point point : SHAPE_1.vertices()) {
      check(SHAPE_1.contains(point));
    }

    check(SHAPE_1.contains(Point.of(0, 0)));

    check(SHAPE_1.contains(Point.of(1, 0)));
    check(SHAPE_1.contains(Point.of(1, -1)));
    check(SHAPE_1.contains(Point.of(-1, 0)));

    check(!SHAPE_1.contains(Point.of(-1, -1)));
    check(!SHAPE_1.contains(Point.of(-1, 1)));
    check(!SHAPE_1.contains(Point.of(-1, 0.5)));
    check(!SHAPE_1.contains(Point.of(2, 0)));
  }

  @Test
  public void containsInSquare() {
    for (Point point : SHAPE_2.vertices()) {
      check(SHAPE_2.contains(point));
    }

    check(SHAPE_2.contains(Point.of(1, -1)));
    check(SHAPE_2.contains(Point.of(0, 0)));
    check(SHAPE_2.contains(Point.of(0, -1)));
    check(SHAPE_2.contains(Point.of(1, 0)));
    check(SHAPE_2.contains(Point.of(2, -1)));
    check(SHAPE_2.contains(Point.of(1, -2)));

    check(!SHAPE_2.contains(Point.of(-1, 1)));
    check(!SHAPE_2.contains(Point.of(3, 1)));
    check(!SHAPE_2.contains(Point.of(3, -3)));
    check(!SHAPE_2.contains(Point.of(-1, -3)));
    check(!SHAPE_2.contains(Point.of(0, 0.000001)));

    check(!SHAPE_2.contains(Point.of(2.5, 0.5)));
    check(!SHAPE_2.contains(Point.of(-0.5, 0.5)));
    check(!SHAPE_2.contains(Point.of(-0.5, -2.5)));
    check(!SHAPE_2.contains(Point.of(2.5, -2.5)));
  }

  @Test
  public void containsInNonConvexPolygon() {
    for (Point point : SHAPE_3.vertices()) {
      check(SHAPE_3.contains(point));
    }

    check(SHAPE_3.contains(Point.of(5, 5)));
    check(SHAPE_3.contains(Point.of(4, 2)));
    check(SHAPE_3.contains(Point.of(4.5, 2)));
    check(SHAPE_3.contains(Point.of(2, 4)));
    check(SHAPE_3.contains(Point.of(2, 2)));
    check(SHAPE_3.contains(Point.of(3, 1)));
    check(SHAPE_3.contains(Point.of(1, 1)));
    check(SHAPE_3.contains(Point.of(1, 2)));
    check(SHAPE_3.contains(Point.of(1, 3)));
    check(SHAPE_3.contains(Point.of(4, 5)));

    check(!SHAPE_3.contains(Point.of(4.5, 1.2)));
    check(!SHAPE_3.contains(Point.of(5, 1.5)));
    check(!SHAPE_3.contains(Point.of(5, 3)));
    check(!SHAPE_3.contains(Point.of(5, 1)));
    check(!SHAPE_3.contains(Point.of(1, 4)));
    check(!SHAPE_3.contains(Point.of(1, 6)));
    check(!SHAPE_3.contains(Point.of(2, 6)));
  }
}
