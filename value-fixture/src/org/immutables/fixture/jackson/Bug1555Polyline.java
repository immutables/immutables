package org.immutables.fixture.jackson;

import java.util.List;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonDeserialize(as = ImmutableBug1555Polyline.class)
public abstract class Bug1555Polyline implements WithBug1555Polyline {
  public abstract String getType();

  public abstract List<List<Double>> getCoordinates();

  public abstract List<String>[] lines();

  public static final class Builder extends ImmutableBug1555Polyline.Builder {
  }
}
