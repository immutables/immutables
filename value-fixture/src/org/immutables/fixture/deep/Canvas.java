package org.immutables.fixture.deep;

import org.immutables.value.Value;

@Value.Style(deepImmutablesDetection = true)
public interface Canvas {

  @Value.Immutable
  public interface Color {
    @Value.Parameter
    double red();

    @Value.Parameter
    double green();

    @Value.Parameter
    double blue();
  }

  @Value.Immutable
  public interface Line {
    Point start();

    Point end();

    Color color();
  }

  @Value.Immutable
  public interface Point {
    @Value.Parameter
    int x();

    @Value.Parameter
    int y();
  }

  default void use() {
    ImmutableLine line = ImmutableLine.builder()
        .startOf(1, 2)
        .endOf(2, 3)
        .colorOf(0.9, 0.7, 0.4)
        .build();

    ImmutablePoint start = line.start();
    ImmutablePoint end = line.end();
    ImmutableColor color = line.color();

    ImmutableLine.builder()
        .start(start)
        .end(end)
        .color(color)
        .build();
  }
}
