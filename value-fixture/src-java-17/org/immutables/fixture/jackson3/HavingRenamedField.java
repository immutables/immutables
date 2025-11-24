package org.immutables.fixture.jackson3;

import tools.jackson.databind.annotation.JsonDeserialize;
import java.time.ZonedDateTime;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.immutables.value.Value;

@Value.Style(init = "with*")
@Value.Immutable
@JsonDeserialize(builder = ImmutableHavingRenamedField.Builder.class)
public interface HavingRenamedField {

  @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mmZ")
  @JsonProperty("start_date_time")
  ZonedDateTime getStartDateTime();
}
