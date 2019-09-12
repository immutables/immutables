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
package org.immutables.fixture.deep;

import com.google.common.collect.ImmutableList;
import org.immutables.fixture.deep.Canvas.Line;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.Collections;
import static org.immutables.check.Checkers.check;

public class DeepImmutablesDetectionTest {
  private static final ModifiableColor MODIFIABLE_COLOR = ModifiableColor.create(0.5, 0.5, 0.5);
  private static final ImmutableColor IMMUTABLE_COLOR = MODIFIABLE_COLOR.toImmutable();
  private static final ModifiablePoint MODIFIABLE_POINT = ModifiablePoint.create(1, 2);
  private static final ImmutablePoint IMMUTABLE_POINT = MODIFIABLE_POINT.toImmutable();

  @Test
  public void modifiableFieldIsConvertedOnToImmutable() {
    Line line = ModifiableLine.create().setColor(MODIFIABLE_COLOR).toImmutable();

    check(line.color()).isA(ImmutableColor.class);
    check(line.color()).is(IMMUTABLE_COLOR);
  }

  @Test
  public void modifiableCollectionFieldIsConvertedOnToImmutable() {
    Line line = ModifiableLine.create()
        .setColor(MODIFIABLE_COLOR)
        .setPoints(Collections.singleton(MODIFIABLE_POINT))
        .addPoint(MODIFIABLE_POINT)
        .addPoints(MODIFIABLE_POINT, MODIFIABLE_POINT)
        .addAllPoints(Collections.singleton(MODIFIABLE_POINT))
        .toImmutable();

    check(line.points()).isA(ImmutableList.class);
    check(line.points().size()).is(5);

    for (Canvas.Point point : line.points()) {
      check(point).isA(ImmutablePoint.class);
      check(point).is(IMMUTABLE_POINT);
    }
  }

  @Test
  public void modifiableFieldIsConvertedInBuilder() {
    Line line = ImmutableLine.builder().color(MODIFIABLE_COLOR).build();

    check(line.color()).isA(ImmutableColor.class);
    check(line.color()).is(IMMUTABLE_COLOR);
  }

  @Test
  public void modifiableCollectionFieldIsConvertedInBuilder() {
    Line line = ImmutableLine.builder()
        .color(MODIFIABLE_COLOR)
        .points(Collections.singleton(MODIFIABLE_POINT))
        .addPoint(MODIFIABLE_POINT)
        .addPoints(MODIFIABLE_POINT, MODIFIABLE_POINT)
        .addAllPoints(Collections.singleton(MODIFIABLE_POINT))
        .build();

    check(line.points()).isA(ImmutableList.class);
    check(line.points().size()).is(5);

    for (Canvas.Point point : line.points()) {
      check(point).isA(ImmutablePoint.class);
      check(point).is(IMMUTABLE_POINT);
    }
  }

  @Test
  public void immutableFieldIsConvertedInModifiableFrom() {
    Line line = ImmutableLine.builder()
        .color(IMMUTABLE_COLOR)
        .addPoint(IMMUTABLE_POINT)
        .build();

    ModifiableLine modifiableLine = ModifiableLine.create().from(line);

    check(modifiableLine.color()).isA(ModifiableColor.class);
    check(modifiableLine.color()).is(MODIFIABLE_COLOR);
  }

  @Test
  public void immutableFieldIsConvertedInModifiableSetter() {
    ModifiableLine modifiableLine = ModifiableLine.create().setColor(IMMUTABLE_COLOR);

    check(modifiableLine.color()).isA(ModifiableColor.class);
    check(modifiableLine.color()).is(MODIFIABLE_COLOR);
  }

  @Test
  public void immutableCollectionFieldIsConvertedInModifiableSetter() {
    ModifiableLine modifiableLine = ModifiableLine.create()
        .setPoints(Collections.singleton(IMMUTABLE_POINT))
        .addPoint(IMMUTABLE_POINT)
        .addPoints(IMMUTABLE_POINT, IMMUTABLE_POINT)
        .addAllPoints(Collections.singleton(IMMUTABLE_POINT));

    check(modifiableLine.points()).isA(ArrayList.class);
    check(modifiableLine.points()).hasSize(5);

    for (Canvas.Point point : modifiableLine.points()) {
      check(point).isA(ModifiablePoint.class);
      check(point).is(MODIFIABLE_POINT);
    }
  }
}
