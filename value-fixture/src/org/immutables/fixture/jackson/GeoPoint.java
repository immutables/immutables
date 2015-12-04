package org.immutables.fixture.jackson;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@JsonFormat(shape = JsonFormat.Shape.ARRAY)
@JsonPropertyOrder({"longitude", "latitude", "altitude"})
@JsonInclude(JsonInclude.Include.NON_EMPTY)
@JsonDeserialize(as = ImmutableGeoPoint.class)
@JsonSerialize(as = ImmutableGeoPoint.class)
@Value.Immutable
public abstract class GeoPoint {
	public abstract double longitude();
	public abstract double latitude();
	public abstract double altitude();
}