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
