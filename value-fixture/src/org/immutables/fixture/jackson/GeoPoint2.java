package org.immutables.fixture.jackson;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@JsonPropertyOrder({"lat", "lon"})
@JsonDeserialize(as = ImmutableGeoPoint2.class)
@JsonSerialize(as = ImmutableGeoPoint2.class)
@Value.Immutable
public abstract class GeoPoint2 {
  public abstract double lon();

  public abstract double lat();
}
