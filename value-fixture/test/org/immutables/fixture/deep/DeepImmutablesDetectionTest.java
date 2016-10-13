package org.immutables.fixture.deep;

import com.google.common.collect.ImmutableList;
import org.immutables.fixture.deep.Canvas.Line;
import org.junit.Test;

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
                .addPoint(MODIFIABLE_POINT, MODIFIABLE_POINT)
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
                .addPoint(MODIFIABLE_POINT, MODIFIABLE_POINT)
                .addAllPoints(Collections.singleton(MODIFIABLE_POINT))
                .build();

        check(line.points()).isA(ImmutableList.class);
        check(line.points().size()).is(5);

        for (Canvas.Point point : line.points()) {
            check(point).isA(ImmutablePoint.class);
            check(point).is(IMMUTABLE_POINT);
        }
    }
}
