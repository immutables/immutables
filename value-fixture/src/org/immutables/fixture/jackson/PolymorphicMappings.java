package org.immutables.fixture.jackson;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

public interface PolymorphicMappings {
  @JsonSubTypes({
      @Type(DatasetIdLocator.class),
      @Type(DatasetPathLocator.class),
      @Type(ImmutableDatasetIdLocator.class),
      @Type(ImmutableDatasetPathLocator.class)
  })
  @JsonTypeInfo(use = Id.NAME, include = As.PROPERTY, property = "@class", visible = false)
  public interface DatasetLocator {}

  @Value.Immutable
  @Value.Style(visibility = Value.Style.ImplementationVisibility.PACKAGE)
  @JsonSerialize(as = ImmutableDatasetIdLocator.class)
  @JsonDeserialize(as = ImmutableDatasetIdLocator.class)
  public interface DatasetIdLocator extends DatasetLocator {
    String getDatasetId();
    class Builder extends ImmutableDatasetIdLocator.Builder {}
  }

  @Value.Immutable
  @Value.Style(visibility = Value.Style.ImplementationVisibility.PACKAGE)
  @JsonSerialize(as = ImmutableDatasetIdLocator.class)
  @JsonDeserialize(as = ImmutableDatasetIdLocator.class)
  public interface DatasetPathLocator extends DatasetLocator {
    String getDatasetPath();
    class Builder extends ImmutableDatasetPathLocator.Builder {}
  }
}
